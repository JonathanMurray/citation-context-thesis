package concepts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.stream.Collectors;

import util.Environment;

import com.ibm.icu.text.DecimalFormat;

import dataset.NgramExtractor;
import dataset.Texts;
import edu.cmu.lti.jawjaw.pobj.Synset;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IPointer;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.WordID;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class WordNet2 {
	
	private static DecimalFormat f = new DecimalFormat("#0.00");

	public static void main(String[] args) {
		WordNet2 w = WordNet2.fromFile(new File(Environment.resources(), "wordnet-dict").toString());
		w.go();
	}

	public void go() {
		Scanner s = new Scanner(System.in);
		boolean c = true;
		while (c) {
			System.out.print("INPUT: ");
			String input1 = s.nextLine();
			fromSentence(input1);
		}
		s.close();
	}

	public IDictionary dict;

	List<String> upPointers = new ArrayList<String>(Arrays.asList(new String[] {
			// nouns
			"+", "@", "@i", "#m", "#s", "#p", ";c", ";r", ";u",

			// additional for verbs
			"*", ">",

			// additional for adj
			"&", "<", "\\", "="

	// adverb no additional
			}));

	private HashMap<ISynsetID, HashMap<Integer, Integer>> shortestPaths = new HashMap<ISynsetID, HashMap<Integer, Integer>>();
	private Queue<ISynsetID> queue = new LinkedList<ISynsetID>();
	private HashMap<Integer, HashMap<ISynsetID, ISynset>> predecessors = new HashMap<Integer, HashMap<ISynsetID, ISynset>>();
	StanfordCoreNLP pipeline;
	ArrayList<String> phrases;
	HashMap<ISynset, Integer> depths = new HashMap<ISynset, Integer>();

	public static WordNet2 fromFile(String dictDir) {
		try {
			URL dictUrl = new URL("file", null, dictDir);
			IDictionary dict = new Dictionary(dictUrl);
			return new WordNet2(dict);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}

	public WordNet2(IDictionary dict) {
		try {
			this.dict = dict;
			dict.open();
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");// ,
																			// ner,
																			// parse");
			pipeline = new StanfordCoreNLP(props);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	List<IWordID>fromSentence(String sentenceText) {
		setup(sentenceText);
		System.out.println("size: " + queue.size());
		printPaths(shortestPaths);
		List<IWordID> foundSynsets = graphSearch();
		return foundSynsets;
	}

	void setup(String sentenceText) {
		shortestPaths.clear();
		queue.clear();
		predecessors.clear();
		phrases = new ArrayList<String>();
		Annotation document = new Annotation(sentenceText);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			int phraseIndex = 0;
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				if (!Texts.instance().isStopword(lemma)) {
					POS posTag;
					if (pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP")
							|| pos.equals("VBZ")) {
						posTag = POS.VERB;
					} else if (pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")) {
						posTag = POS.ADJECTIVE;
					} else if (pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS")) {
						posTag = POS.ADVERB;
					} else {
						System.out.println(lemma + ": " + pos);
						posTag = POS.NOUN;
					}
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

	boolean processPhrase(int phraseIndex, String phrase, POS pos) {
		IIndexWord indexWord = dict.getIndexWord(phrase, pos);
		if (indexWord != null) {
			System.out.println(phraseIndex + ": " + phrase + " (" + pos + ")");
			for (IWordID id : indexWord.getWordIDs()) {
				ISynsetID synsetId = id.getSynsetID();
				queue.add(synsetId);
				HashMap<Integer, Integer> pathsHere = new HashMap<Integer, Integer>();
				pathsHere.put(phraseIndex, 0);
				shortestPaths.put(synsetId, pathsHere);
				if (!predecessors.containsKey(phraseIndex)) {
					predecessors.put(phraseIndex, new HashMap<ISynsetID, ISynset>());
				}
				predecessors.get(phraseIndex).put(synsetId, null);
			}
			return true;
		} else {
			return false;
		}
	}

	List<IWordID> graphSearch() {
		for (int i = 0; i < 10; i++) {
			// System.out.println("ITERATION#" + i);
			int n = queue.size();
			for (int j = 0; j < n; j++) {
				ISynset synset = dict.getSynset(queue.remove());
				ISynsetID synsetId = synset.getID();
				if (!shortestPaths.containsKey(synsetId)) {
					shortestPaths.put(synsetId, new HashMap<Integer, Integer>());
				}
				HashMap<Integer, Integer> pathsHere = shortestPaths.get(synsetId);
				for (Entry<IPointer, List<ISynsetID>> pointer : synset.getRelatedMap().entrySet()) {
					if (!pointsUp(pointer.getKey())) {
						continue;
					}
					for (ISynsetID parent : pointer.getValue()) {
						if (!shortestPaths.containsKey(parent)) {
							shortestPaths.put(parent, new HashMap<Integer, Integer>());
						}
						HashMap<Integer, Integer> pathsToParent = shortestPaths.get(parent);
						boolean parentWasUpdated = false;
						for (Entry<Integer, Integer> path : pathsHere.entrySet()) {
							int wordIndex = path.getKey();
							int newLength = path.getValue() + 1;
							boolean existingShorter = pathsToParent.containsKey(wordIndex)
									&& pathsToParent.get(wordIndex) < newLength;
							if (!existingShorter) {
								pathsToParent.put(wordIndex, newLength);
								predecessors.get(wordIndex).put(parent, synset);
								parentWasUpdated = true;
							}
						}
						if (parentWasUpdated) {
							queue.add(parent);
						}
					}
				}
			}
		}
//		System.out.println("PATHS: ");
//		printPaths(shortestPaths);
//		removeRedundant(shortestPaths);
//		System.out.println("IMPORTANT PATHS: ");
//		printPaths(shortestPaths);
		List<FoundPath> sortedPaths = getSorted(shortestPaths);
		prettyPrintList(sortedPaths, 15);
		List<FoundPath> cover = extractCover(sortedPaths);
		System.out.println("EXTRACTED COVER:");
		prettyPrintList(cover, 0);
		return cover.stream().map(fp -> fp.firstWordId).collect(Collectors.toCollection(ArrayList::new));
	}

	<T> void prettyPrintList(List<T> list, int limit) {
		System.out.println("[");
		for (int i = 0; i < (limit == 0 ? list.size() : limit); i++) {
			T e = list.get(i);
			System.out.println("  " + e + ",");
		}
		System.out.println("]");
	}

	void removeRedundant(HashMap<ISynsetID, HashMap<Integer, Integer>> shortestPaths) {
		Iterator<Entry<ISynsetID, HashMap<Integer, Integer>>> it = shortestPaths.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ISynsetID, HashMap<Integer, Integer>> e = it.next();
			HashMap<Integer, Integer> paths = e.getValue();
			boolean isRedundant = false;
			for (ISynsetID otherId : shortestPaths.keySet()) {
				if (otherId.equals(e.getKey())) {
					continue; // avoid self comparison
				}
				HashMap<Integer, Integer> otherPaths = shortestPaths.get(otherId);
				if (strictlyBetterThan(otherPaths, paths)) {
					isRedundant = true;
					break;
				}
			}
			if (isRedundant) {
				it.remove();
			}
		}
	}

	boolean strictlyBetterThan(HashMap<Integer, Integer> a, HashMap<Integer, Integer> b) {
		boolean possiblyBetter = false;
		for (Entry<Integer, Integer> e : b.entrySet()) {
			int key = e.getKey();
			int val = e.getValue();
			if (!a.containsKey(key) || a.get(key) > val) {
				return false;
			}
			if (a.containsKey(key) && a.get(key) < val) {
				possiblyBetter = true;
			}
		}
		return possiblyBetter;
	}

	List<FoundPath> getSorted(HashMap<ISynsetID, HashMap<Integer, Integer>> shortestPaths) {
		ArrayList<FoundPath> sorted = new ArrayList<FoundPath>();
		for (ISynsetID id : shortestPaths.keySet()) {
			HashMap<Integer, Integer> paths = shortestPaths.get(id);
			ISynset synset = dict.getSynset(id);
			sorted.add(new FoundPath(synset, paths));
		}
		Collections.sort(sorted);
		return sorted;
	}

	class FoundPath implements Comparable<FoundPath> {
		IWordID firstWordId;
		ISynset goal;
		HashMap<Integer, Integer> paths;
		double score;
		int depth;
		
		private HashSet<Integer> ignoredWords = new HashSet<Integer>();
		
		FoundPath(ISynset goal, HashMap<Integer, Integer> paths) {
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
			int numWords = 0;
			for (Entry<Integer,Integer> e : paths.entrySet()) {
				int wordIndex = e.getKey();
				if(!ignoredWords.contains(wordIndex)){
					int length = e.getValue();
					score += 1.0 / Math.pow(1.5, length);
					numWords ++;
				}
			}
			score *= Math.pow(numWords, 1);
			score *= Math.pow(depth-0.5, 1);
		}

		public int compareTo(FoundPath other) {
			return (int) Math.signum(other.score - score);
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(goal.getPOS() + "#" + goal.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
			s.append(" [score: " + f.format(score) + ", depth: " + depth + "]");
			s.append(" ~ "
					+ paths.entrySet().stream().map(e -> e.getKey() + ":" + phrases.get(e.getKey()) + "(" + e.getValue() + ")")
							.collect(Collectors.toList()));
			return s.toString();
		}
	}

	List<FoundPath> extractCover(List<FoundPath> paths){
		int num = Math.min(paths.size() / 3, 5);
		List<FoundPath> extracted = new ArrayList<FoundPath>();
		while(extracted.size() < num){
			FoundPath best = paths.stream().filter(p -> ! extracted.contains(p)).min(FoundPath::compareTo).get();
			extracted.add(best);
			
			int numBestCoveredWords = Math.max(1, (int)(best.paths.size()/2.5));
			List<Integer> bestCoveredWords = best.paths.entrySet().stream().sorted(Comparator.comparing(Entry::getValue)).limit(numBestCoveredWords).map(e -> e.getKey()).collect(Collectors.toList());
			System.out.println("ignoring " + bestCoveredWords + ": " + bestCoveredWords.stream().map(i -> phrases.get(i)).collect(Collectors.toList()));
			for(FoundPath path : paths){
				path.ignoreWords(bestCoveredWords);
			}
		}
		return extracted;
	}

	void printPaths(HashMap<ISynsetID, HashMap<Integer, Integer>> paths) {
		System.out.println("{");
		for (Entry<ISynsetID, HashMap<Integer, Integer>> e : paths.entrySet()) {
			HashMap<Integer, Integer> pathsHere = e.getValue();
			if (pathsHere.size() > 1) {
				System.out.print("  ");
				ISynset synset = dict.getSynset(e.getKey());
				System.out.print(synset.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
				System.out.print(" :  " + pathsHere);
				System.out.println("    from:");
				// for(int wordIndex : pathsHere.keySet()){
				// ISynset predecessor =
				// predecessors.get(wordIndex).get(synset.getID());
				// System.out.print("    " + wordIndex + ":");
				// if(predecessor == null){
				// System.out.println("NULL");
				// }else{
				// System.out.println(predecessor.getWords().stream().map(w ->
				// w.getLemma()).collect(Collectors.toList()) + ", ");
				// }
				// }
			}
		}
		System.out.println("}");
	}

	private boolean pointsUp(IPointer pointer) {
		return upPointers.contains(pointer.getSymbol());
	}

	int getDepth(ISynset startSynset) {
		if (depths.containsKey(startSynset)) {
			return depths.get(startSynset);
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
			// System.out.println("up progress: " + progress);
			int n = queue.size();
			// System.out.println(n);
			for (int i = 0; i < n; i++) {
				ISynset topSynset = queue.remove();
				boolean hasParent = false;
				for (Entry<IPointer, List<ISynsetID>> e : topSynset.getRelatedMap().entrySet()) {
					if (pointsUp(e.getKey())) {
						for (ISynsetID parentId : e.getValue()) {
							ISynset parent = dict.getSynset(parentId);
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
						// System.out.println("still not empty");
						int backQueueSize = backQueue.size();
						// System.out.println("will loop through backqueue, size : "
						// + backQueueSize);
						for (int j = 0; j < backQueueSize; j++) {
							// System.out.print("popping..   ");
							ISynset syn = backQueue.remove();
							// System.out.println("popped: " + syn);
							if (backLinks.containsKey(syn)) {
								// System.out.println("#children: " +
								// backLinks.get(syn).size());
								for (ISynset child : backLinks.get(syn)) {
									// System.out.println("CHILD: " + child);
									// if(!depths.containsKey(child) ||
									// depths.get(child) > depth){
									if (!backVisited.contains(child)) {
										// System.out.println("Adding it!");
										backQueue.add(child);
										backVisited.add(child);
									} else {
										// System.out.println("Already visited!");
									}
								}
							} else {
								// System.out.println("NO CHILDREN!");
							}
							depths.put(syn, depth);
							// System.out.println("Added it at depth: " +
							// depth);
						}
						depth++;
						// System.out.println("increased depth to  " + depth);
					}
					// System.out.println("now it's empty!");
					return depths.get(startSynset);
				}
			}
		}
	}

}
