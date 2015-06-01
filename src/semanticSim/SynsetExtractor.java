package semanticSim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import util.Environment;
import util.Lemmatizer;
import util.Printer;

import com.ibm.icu.text.DecimalFormat;

import dataset.NgramExtractor;
import dataset.TextUtil;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IPointer;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.ISenseKey;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Ended up not being used in the thesis.
 * The task of the class is to extract WordNet-synsets from sentences so that the 
 * sentences can be semantically compared using synset-graph measures. 
 * (It proved difficult to extract a "good" set of synsets for a sentence)
 * @author jonathan
 *
 */
public class SynsetExtractor {
	
	private static Printer printer = new Printer(false);
	
	private static DecimalFormat f = new DecimalFormat("#0.00");
	List<String> upPointers = new ArrayList<String>(Arrays.asList(new String[] {
			// nouns
			"+", "@", "@i", "#m", "#s", "#p", ";c", ";r", ";u",

			// additional for verbs
			"*", ">",

			// additional for adj
			"&", "<", "\\", "="

			// adverb no additional
			}));

	private static final Pattern PUNCTUATION = Pattern.compile("[\\.\\?\\!\\;\\:]");
	
	final static List<POS> badPOS = new ArrayList<POS>(Arrays.asList(new POS[]{
			POS.ADJECTIVE, POS.ADVERB	
		}));
	
	private HashMap<ISynsetID, TIntIntHashMap> shortestPaths = new HashMap<ISynsetID, TIntIntHashMap>();
	private Queue<ISynsetID> synsetQueue = new LinkedList<ISynsetID>();
	private ArrayList<String> phrases;
	private HashMap<ISynsetID, TIntDoubleHashMap> wordSenseProbabilities = new HashMap<ISynsetID, TIntDoubleHashMap>();
	
	//Shared by many instances
	private final IDictionary dict;
	private final StanfordCoreNLP pipeline;
	private final TObjectIntHashMap<ISynset> depths;
	
	
	public static void main(String[] args) {
		TEST();
//		String dictDir = new File(Environment.resources(), "wordnet-dict").toString();
//		WordNet2 w = new WordNet2(createPipeline(), dictFromDir(dictDir));
//		w.test();
	}
	
	private static void printPOS(Annotation a){
		List<CoreMap> sentences = a.get(SentencesAnnotation.class);
		int sentenceIndex = 0;
		for (CoreMap sentence : sentences) {
			System.out.print("Sentence " + sentenceIndex + ": ");
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				System.out.print(pos + " ");
			}
			System.out.println();
		}
	}
	
	public static void TEST(){
		List<String> lemmas = Lemmatizer.instance().lemmatize("In this paper, we show that this approach misses much of the existing sentiment.");
		String dictDir = new File(Environment.resources(), "wordnet-dict").toString();
		IDictionary dict = dictFromDir(dictDir);
		SynsetExtractor s = new SynsetExtractor(createPipeline(), dict, new TObjectIntHashMap<ISynset>());
		List<ISynset> synsets = s.fromSentence(lemmas);
		s.prettyPrintList(synsets, 100);
		s.prettyPrintHashMap(s.wordSenseProbabilities);
	}
	
	public static StanfordCoreNLP createPipeline(){
		Properties props = new Properties();
		props.setProperty("annotators", "pos");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);
		return pipeline;
	}
	
	public static IDictionary dictFromDir(String dictDir){
		try {
			IDictionary dict = new RAMDictionary(new File(dictDir), ILoadPolicy.IMMEDIATE_LOAD);
			System.out.print("opening dict ... ");
			dict.open();
			System.out.println("[x]");
			return dict;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}

	private void test() {
		Scanner s = new Scanner(System.in);
		boolean c = true;
		while (c) {
			System.out.print("INPUT: ");
			String input1 = s.nextLine();
			fromSentence(Lemmatizer.instance().lemmatize(input1));
		}
		s.close();
	}
	
	public SynsetExtractor(StanfordCoreNLP pipeline, IDictionary dict, TObjectIntHashMap<ISynset> depths){
		this.pipeline = pipeline;
		this.dict = dict;
		this.depths = depths;
	}
	
	//Datasets can be created in parallel. Make sure this method is not invoked concurrently
	synchronized public List<ISynset>fromSentence(List<String> lemmas) {
		setup(lemmas);
//		System.out.println(lemmas); //TODOs
		printer.println("size: " + synsetQueue.size());
//		printPaths(shortestPaths);
		findShortestPaths();
		printer.println("PATHS: ");
//		printPaths(shortestPaths);
//		removeRedundant(shortestPaths);
//		printer.println("IMPORTANT PATHS: ");
//		printPaths(shortestPaths);
		List<SynsetWithPaths> paths = getList(shortestPaths);
//		prettyPrintList(sortedPaths, 15);
		List<SynsetWithPaths> bestSynsets = pickBest(paths);
		printer.println("EXTRACTED COVER:");
//		prettyPrintList(cover, 0);
		List<ISynset> foundSynsets =  bestSynsets.stream().map(fp -> fp.goal).collect(Collectors.toCollection(ArrayList::new));
		return foundSynsets;
	}
	

	void setup(List<String> lemmas) {
		shortestPaths.clear();
		synsetQueue.clear();
//		predecessors.clear();
		phrases = new ArrayList<String>();
		Annotation annotation = annotationFromLemmas(lemmas);
		List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
		int lemmaIndex = 0;
		for (CoreMap sentence : sentences) {
			int phraseIndex = 0;
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				String lemma = lemmas.get(lemmaIndex);
				lemmaIndex ++;
				if (!TextUtil.instance().isStopword(lemma)) {
					POS posTag = fromPennTreebank(pos);
					if (processPhrase(phraseIndex, lemma, posTag)) {
						phrases.add(lemma);
						phraseIndex++;
					}
				}
			}
		}
		List<String> phraseNgrams = NgramExtractor.allNgramPhrases(2, 3, phrases);
		int phraseIndex = phrases.size();
		for (String phraseNgram : phraseNgrams) {
			if (processPhrase(phraseIndex, phraseNgram, POS.NOUN)) {
				phraseIndex++;
				phrases.add(phraseNgram);
			}
		}
	}
	
	Annotation annotationFromLemmas(List<String> lemmas){
		ArrayList<CoreMap> sentences = new ArrayList<CoreMap>();
		ArrayList<CoreLabel> tokens = new ArrayList<CoreLabel>();
		for(String lemma : lemmas){
			CoreLabel label = new CoreLabel();
			label.setWord(lemma);
			tokens.add(label);
			if(PUNCTUATION.matcher(lemma).find()){
				CoreMap sentence = new ArrayCoreMap();
				sentence.set(TokensAnnotation.class, tokens);
				sentences.add(sentence);
				tokens = new ArrayList<CoreLabel>();
			}
		}
		CoreMap sentence = new ArrayCoreMap();
		sentence.set(TokensAnnotation.class, tokens);
		
		sentences.add(sentence);
		Annotation annotation = new Annotation(sentences);
		pipeline.annotate(annotation);
//		printPOS(annotation);
		return annotation;
	}
	
	public static POS fromPennTreebank(String pos){
		POS posTag;
		if (pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP")
				|| pos.equals("VBZ")) {
			posTag = POS.VERB;
		} else if (pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")) {
			posTag = POS.ADJECTIVE;
		} else if (pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS")) {
			posTag = POS.ADVERB;
		} else {
			posTag = POS.NOUN;
		}
		return posTag;
	}
	
	double getTotalSenseCounts(IIndexWord indexWord, int smoothing){
		double sumCounts = 0;
		synchronized (dict) {
			for(IWordID wordId : indexWord.getWordIDs()){
				IWord word = dict.getWord(wordId);
				if(word == null){
					continue;
				}
				ISenseKey senseKey = word.getSenseKey();
				ISenseEntry senseEntry = dict.getSenseEntry(senseKey);
				if(senseEntry == null){
					continue;
				}
				sumCounts += senseEntry.getTagCount() + smoothing;
			}
		}
		return sumCounts;
	}

	boolean processPhrase(int phraseIndex, String phrase, POS pos) {
		IIndexWord indexWord = getIndexWord(phrase, pos);
		if (indexWord != null) {
			printer.println(phraseIndex + ": " + phrase + " (" + pos + ")");
			final int smoothing = 1;
			double sumCounts = getTotalSenseCounts(indexWord, smoothing);
			for (IWordID id : indexWord.getWordIDs()) {
				ISynsetID synsetId = id.getSynsetID();
				double senseCount;
				synchronized(dict){
					ISenseEntry sense = dict.getSenseEntry(dict.getWord(id).getSenseKey());
					senseCount = (sense == null ? 0 : sense.getTagCount()) + smoothing;
				}
				double senseProbability = senseCount / sumCounts;
				wordSenseProbabilities(synsetId).put(phraseIndex, senseProbability);
//				System.out.println(dict.getSynset(synsetId).getWords());
//				System.out.println("count: " + senseCount);
				
				synsetQueue.add(synsetId);
				TIntIntHashMap pathsHere = new TIntIntHashMap();
				int artificialWordSynsetDistance = 3 - (int)Math.round(3*senseProbability);
				pathsHere.put(phraseIndex, artificialWordSynsetDistance);
				shortestPaths.put(synsetId, pathsHere);
			}
			return true;
		} else {
			return false;
		}
	}

	void findShortestPaths() {
		for(int i = 0; i < 5; i++) { //max depth-distance from word to representative synset //TODO
			// printer.println("ITERATION#" + i);
			int n = synsetQueue.size();
			//Loop through all new nodes (BFS)
			for (int j = 0; j < n; j++) {
				ISynset synset = getSynset(synsetQueue.remove());
				ISynsetID synsetId = synset.getID();
				TIntIntHashMap pathsHere = pathsToSynset(synsetId);
				for (Entry<IPointer, List<ISynsetID>> pointer : synset.getRelatedMap().entrySet()) {
					if (!pointsUp(pointer.getKey())) {
						continue;
					}
					for (ISynsetID parent : pointer.getValue()) {
						TIntIntHashMap pathsToParent = pathsToSynset(parent);
						boolean parentWasUpdated = false;
						TIntIntIterator it = pathsHere.iterator();
						while(it.hasNext()){
							it.advance();
							int wordIndex = it.key();
							int newLength = it.value() + 1;
							boolean existingShorter = pathsToParent.containsKey(wordIndex)
									&& pathsToParent.get(wordIndex) < newLength;
							if (!existingShorter) {
								pathsToParent.put(wordIndex, newLength);
								parentWasUpdated = true;
							}
						}
						if (parentWasUpdated) {
							synsetQueue.add(parent);
						}
					}
				}
			}
		}
//		
	}
	
	private TIntIntHashMap pathsToSynset(ISynsetID synsetId){
		if (!shortestPaths.containsKey(synsetId)) {
			shortestPaths.put(synsetId, new TIntIntHashMap());
		}
		TIntIntHashMap pathsHere = shortestPaths.get(synsetId);
		return pathsHere;
	}
	
	private TIntDoubleHashMap wordSenseProbabilities(ISynsetID synsetId){
		if(!wordSenseProbabilities.containsKey(synsetId)){
			wordSenseProbabilities.put(synsetId, new TIntDoubleHashMap());
		}
		return wordSenseProbabilities.get(synsetId);
	}

	<T> void prettyPrintList(List<T> list, int limit) {
		System.out.println("[");
		int n = (limit > 0 && limit < list.size()? limit : list.size());
		for (int i = 0; i < n; i++) {
			T e = list.get(i);
			System.out.println("  " + e + ",");
		}
		System.out.println("]");
	}
	
	<K,V> void prettyPrintHashMap(HashMap<K,V> map){
		for(K key : map.keySet()){
			System.out.print(key + ": ");
			System.out.println(map.get(key) + "\n");
		}
	}

	boolean strictlyBetterThan(TIntIntHashMap a, TIntIntHashMap b) {
		boolean possiblyBetter = false;
		TIntIntIterator bIt = b.iterator();
		while(bIt.hasNext()){
			bIt.advance();
			int key = bIt.key();
			int val = bIt.value();
			if (!a.containsKey(key) || a.get(key) > val) {
				return false;
			}
			if (a.containsKey(key) && a.get(key) < val) {
				possiblyBetter = true;
			}
		}
		return possiblyBetter;
	}

	List<SynsetWithPaths> getList(HashMap<ISynsetID, TIntIntHashMap> shortestPaths) {
		ArrayList<SynsetWithPaths> sorted = new ArrayList<SynsetWithPaths>();
		for (ISynsetID synsetId : shortestPaths.keySet()) {
			TIntIntHashMap paths = shortestPaths.get(synsetId);
			ISynset synset = getSynset(synsetId);
			sorted.add(new SynsetWithPaths(synset, paths));
		}
//		Collections.sort(sorted);
		return sorted;
	}

	class SynsetWithPaths implements Comparable<SynsetWithPaths> {
		IWordID firstWordId;
		ISynset goal;
		TIntIntHashMap paths;
		double score;
		int depth;
		
		
		private HashSet<Integer> ignoredWords = new HashSet<Integer>();
		
		SynsetWithPaths(ISynset goal, TIntIntHashMap paths) {
			firstWordId = goal.getWords().get(0).getID();
			this.goal = goal;
			this.depth = getDepth(goal);
			this.paths = paths;
			computeScore();
		}
		
		void ignoreWords(List<Integer> wordIndices){
			ignoredWords.addAll(wordIndices);
			computeScore();
		}
		
		void ignoreWord(int wordIndex){
			ignoredWords.add(wordIndex);
			computeScore();
		}
		
		private void computeScore(){
			score = 0;
			TIntIntIterator it = paths.iterator();
			TIntArrayList pathLengths = new TIntArrayList();
			while(it.hasNext()){
				it.advance();
				int wordIndex = it.key();
				if(!ignoredWords.contains(wordIndex)){
					int pathLength = it.value();
//					score += 1.0 / Math.pow(pathLength, 1);
					pathLengths.add(pathLength);
				}
			}
			pathLengths.sort();
			for(int i = 0; i < 3 && pathLengths.size() - 1 - i >= 0; i++){
				int pathLength = pathLengths.get(pathLengths.size() - 1 - i );
				score += 1.0 / Math.pow(pathLength, 0.5);
			}
			if(badPOS.contains(goal.getPOS())){
				score = 0;
			}
//			score *= Math.pow(numWords, 1.5);
//			score *= Math.pow(depth-0.5, 0.3);
		}

		public int compareTo(SynsetWithPaths other) {
			return (int) Math.signum(other.score - score);
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(goal.getPOS() + "#" + goal.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
			s.append(" [score: " + f.format(score) + ", depth: " + depth + "]");
			s.append(" ~ ");
			TIntIntIterator it = paths.iterator();
			while(it.hasNext()){
				it.advance();
				s.append(it.key() + ":" + phrases.get(it.key()) + "(" + it.value() + "), ");
			}
			return s.toString();
		}
	}

	List<SynsetWithPaths> pickBest(List<SynsetWithPaths> paths){
		int num = (int)Math.pow(paths.size(), 0.3);
		final int min = 1;
		final int max = 3;
		num = Math.min(max, Math.min(paths.size(), Math.max(min, num)));
		List<SynsetWithPaths> extracted = new ArrayList<SynsetWithPaths>();
		while(extracted.size() < num){
			SynsetWithPaths best = paths.stream().filter(p -> ! extracted.contains(p)).min(SynsetWithPaths::compareTo).get();
			extracted.add(best);
//			System.out.println(best.goal.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
//			System.out.println(best.depth + "   " + best.paths); //TODO
			int numBestCoveredWords = Math.max(1, (int)(best.paths.size()/2.5));
			HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
			TIntIntIterator it = best.paths.iterator();
			while(it.hasNext()){ //Expensive, but no easy way of finding best covered words without hashmap
				it.advance();
				map.put(it.key(), it.value());
			}
			List<Integer> bestCoveredWords = map.entrySet().stream().sorted(Comparator.comparing(Entry::getValue)).limit(numBestCoveredWords).map(e -> e.getKey()).collect(Collectors.toList());
//			System.out.println("ignoring " + bestCoveredWords + ": " + bestCoveredWords.stream().map(i -> phrases.get(i)).collect(Collectors.toList()));
			for(SynsetWithPaths path : paths){
				path.ignoreWords(bestCoveredWords);
			}
		}
//		System.out.println("\n\n"); //TODO
		return extracted;
	}

	void printPaths(HashMap<ISynsetID, TIntIntHashMap> paths) {
		printer.println("{");
		for (Entry<ISynsetID, TIntIntHashMap> e : paths.entrySet()) {
			TIntIntHashMap pathsHere = e.getValue();
			if (pathsHere.size() > 1) {
				System.out.print("  ");
				ISynset synset = getSynset(e.getKey());
				System.out.print(synset.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
				System.out.print(" :  " + pathsHere);
				printer.println("    from:");
			}
		}
		printer.println("}");
	}

	private boolean pointsUp(IPointer pointer) {
		return upPointers.contains(pointer.getSymbol());
	}

	int getDepth(ISynset startSynset) {
		synchronized (depths) {
			if (depths.containsKey(startSynset)) {
				return depths.get(startSynset);
			}
		}
		HashMap<ISynset, List<ISynset>> backLinks = new HashMap<ISynset, List<ISynset>>();
		HashSet<ISynset> visited = new HashSet<ISynset>();
		// int depth = 0;
		Queue<ISynset> queue = new LinkedList<ISynset>();
		queue.add(startSynset);
		visited.add(startSynset);
		// int progress = 0;
		while (true) {
			// progress++;
			// printer.println("up progress: " + progress);
			int n = queue.size();
			// printer.println(n);
			for (int i = 0; i < n; i++) {
				ISynset topSynset = queue.remove();
				boolean hasParent = false;
				for (Entry<IPointer, List<ISynsetID>> e : topSynset.getRelatedMap().entrySet()) {
					if (pointsUp(e.getKey())) {
						for (ISynsetID parentId : e.getValue()) {
							ISynset parent = getSynset(parentId);
							if (!visited.contains(parent)) {
								hasParent = true;
								queue.add(parent);
								visited.add(parent);
								if (!backLinks.containsKey(parent)) {
									backLinks.put(parent, new ArrayList<ISynset>());
								}
								backLinks.get(parent).add(topSynset);
							}
						}
					}
				}
				if (!hasParent) {
					// depths.put(startSynset, depth);
					int depth = 0;
					Queue<ISynset> backQueue = new LinkedList<ISynset>();
					HashSet<ISynset> backVisited = new HashSet<ISynset>(); // To
																			// avoid
																			// loops
																			// (there
																			// are
																			// reflexive
																			// relations)
					backQueue.add(topSynset);
					backVisited.add(topSynset);
					while (!backQueue.isEmpty()) {
						// printer.println("still not empty");
						int backQueueSize = backQueue.size();
						// printer.println("will loop through backqueue, size : "
						// + backQueueSize);
						for (int j = 0; j < backQueueSize; j++) {
							// System.out.print("popping..   ");
							ISynset syn = backQueue.remove();
							// printer.println("popped: " + syn);
							if (backLinks.containsKey(syn)) {
								// printer.println("#children: " +
								// backLinks.get(syn).size());
								for (ISynset child : backLinks.get(syn)) {
									// printer.println("CHILD: " + child);
									// if(!depths.containsKey(child) ||
									// depths.get(child) > depth){
									if (!backVisited.contains(child)) {
										// printer.println("Adding it!");
										backQueue.add(child);
										backVisited.add(child);
									} else {
										// printer.println("Already visited!");
									}
								}
							} else {
								// printer.println("NO CHILDREN!");
							}
							synchronized (depths) {
								depths.put(syn, depth);
							}
							// printer.println("Added it at depth: " +
							// depth);
						}
						depth++;
						// printer.println("increased depth to  " + depth);
					}
					// printer.println("now it's empty!");
					synchronized (depths) {
						return depths.get(startSynset);
					}
				}
			}
		}
	}
	
	private ISynset getSynset(ISynsetID id){
		synchronized (dict) {
			return dict.getSynset(id);
		}
	}
	
	private IIndexWord getIndexWord(String lemma, POS pos){
		synchronized(dict){
			return dict.getIndexWord(lemma, pos);
		}
	}
	

}
