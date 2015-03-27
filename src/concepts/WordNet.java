package concepts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import util.Environment;
import util.Printer;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;

import dataset.NgramExtractor;
import dataset.Texts;
import edu.cmu.lti.jawjaw.pobj.Synset;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IPointer;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.IndexWordID;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class WordNet implements ConceptGraph{

	private static Printer printer = new Printer(true);
	
	public IDictionary dict;
	
	public static void ws4j(){
		ILexicalDatabase db = new NictWordNet();
        RelatednessCalculator[] rcs = {
                        new HirstStOnge(db), new LeacockChodorow(db), new Lesk(db),  new WuPalmer(db),
                        new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db)
                        };
       
        Scanner s = new Scanner(System.in);
		
		boolean c = true;
		while(c){
			System.out.print("INPUT: ");
			String input1 = s.nextLine();
			String input2 = s.nextLine();
			List<String> words1 = new ArrayList(Arrays.asList(input1.split(" ")));
			List<String> words2 = new ArrayList(Arrays.asList(input2.split(" ")));
			List<String> allNgrams1 = NgramExtractor.allNgramPhrases(1,3, words1);
			List<String> allNgrams2 = NgramExtractor.allNgramPhrases(1,3, words2);

//			System.out.println("PATH: \n--------");
//			for(int i = 0; i < allNgrams1.size(); i++){
//				for(int j = i + 1; j < allNgrams2.size(); j++){
//					String a = allNgrams1.get(i);
//					String b = allNgrams2.get(j);
//					System.out.println("comparing " + a + " and " + b + ": ");
//					run(a, b, rcs);
//				}
//			}
			
//			run(rcs[0], allNgrams1, allNgrams2);
			run(rcs[1], allNgrams1, allNgrams2);
//			run(rcs[2], allNgrams1, allNgrams2);
//			run(rcs[3], allNgrams1, allNgrams2);
//			run(rcs[4], allNgrams1, allNgrams2);
//			run(rcs[5], allNgrams1, allNgrams2);
//			run(rcs[6], allNgrams1, allNgrams2);
			
			Sentence sentence;
			
//			sentence.
//			
//			Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
//			headFinder.determineHead(tree).pennPrint(out);
		}
	}
	
	static void run(RelatednessCalculator c, List<String> a, List<String> b){
		double[][] simMatrix = c.getNormalizedSimilarityMatrix(
				a.toArray(new String[0]), b.toArray(new String[0]));
		System.out.println(Arrays.deepToString(simMatrix));
	}
	
	static void run( String word1, String word2, RelatednessCalculator[] rcs ) {
		
        WS4JConfiguration.getInstance().setMFS(true);
        for ( RelatednessCalculator rc : rcs ) {
                double s = rc.calcRelatednessOfWords(word1, word2);
                System.out.println( rc.getClass().getName()+"\t"+s );
        }
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		
		WordNet w = WordNet.fromFile(new File(Environment.resources(), "wordnet-dict").toString());
		Scanner s = new Scanner(System.in);
		
		boolean c = true;
		while(c){
			
//			NgramIdf idf = NgramIdf.fromXmlFile(new File("/home/jonathan/Documents/eclipse-workspace/exjobb/resources/xml-datasets/ngram-frequencies.xml"), 0);
			
			System.out.print("INPUT: ");
			String input1 = s.nextLine();
			String input2 = null;
			
			
			List<String> words1 = new ArrayList(Arrays.asList(input1.split(" ")));
			List<String> words2 = new ArrayList(Arrays.asList(input2.split(" ")));
			
			List<TObjectDoubleHashMap<String>> ngrams = NgramExtractor.allNgrams(3, words1);
			TObjectDoubleHashMap<String> unigrams = ngrams.get(0);
			TObjectDoubleHashMap<String> bigrams = ngrams.get(1);
			TObjectDoubleHashMap<String> trigrams = ngrams.get(2);
			List<String> allNgrams1 = NgramExtractor.allNgramPhrases(1,3, words1);
			List<String> allNgrams2 = NgramExtractor.allNgramPhrases(1,3, words2);
			
			RelatednessCalculator calc;
			
			
			
			List<String> unigramWords = new ArrayList<String>();
			List<String> bigramWords = new ArrayList<String>();
			List<String> trigramWords = new ArrayList<String>();
			
//			System.out.println("\nUNIGRAMS:");
			for(String ngram : unigrams.keySet()){
//				System.out.println(ngram);
				IIndexWord indexWord = w.dict.getIndexWord(ngram, POS.NOUN);
				if(indexWord != null){
					for(IWordID wordId : indexWord.getWordIDs()){
						IWord foundWord = w.dict.getWord(wordId);
						ISynset synset = foundWord.getSynset();
						List<String> lemmas = synset.getWords().stream().map(IWord::getLemma).collect(Collectors.toCollection(ArrayList::new));
						String gloss = synset.getGloss();
						System.out.println(lemmas);
//						System.out.println(lemmas + " :  " + gloss);
						unigramWords.addAll(lemmas);
						words1.addAll(lemmas);
					}
				}
				
				
				
			}
			
//			System.out.println("\nBIGRAMS:");
			for(String ngram : bigrams.keySet()){
				ngram = ngram.replaceAll("\\s+", "_");
				IIndexWord indexWord = w.dict.getIndexWord(ngram, POS.NOUN);
				if(indexWord != null){
					for(IWordID wordId : indexWord.getWordIDs()){
						IWord foundWord = w.dict.getWord(wordId);
						ISynset synset = foundWord.getSynset();
						List<String> lemmas = synset.getWords().stream().map(IWord::getLemma).collect(Collectors.toCollection(ArrayList::new));
						String gloss = synset.getGloss();
//						System.out.println(lemmas + " :  " + gloss);
						bigramWords.addAll(lemmas);
						words1.addAll(lemmas);
					}
				}
			}
			
			
//			System.out.println("\nTRIGRAMS:");
			for(String ngram : trigrams.keySet()){
				ngram = ngram.replaceAll("\\s+", "_");
//				System.out.println(ngram);
				IIndexWord indexWord = w.dict.getIndexWord(ngram, POS.NOUN);
				if(indexWord != null){
					for(IWordID wordId : indexWord.getWordIDs()){
						IWord foundWord = w.dict.getWord(wordId);
						ISynset synset = foundWord.getSynset();
						List<String> lemmas = synset.getWords().stream().map(IWord::getLemma).collect(Collectors.toCollection(ArrayList::new));
						String gloss = synset.getGloss();
//						System.out.println(lemmas + " :  " + gloss);
						trigramWords.addAll(lemmas);
						words1.addAll(lemmas);
					}
				}
			}
			
			System.out.println(words1);
			System.out.println(words2);
			
//			System.out.println("JCN: \n--------");
//			for(int i = 0; i < words.size(); i++){
//				for(int j = i + 1; j < words.size(); j++){
//					String a = words.get(i);
//					String b = words.get(j);
//					double sim =  WS4J.runJCN(a,b);
////					if(sim > 0){
//						System.out.print("Comparing " + a + " and " + b + ": ");
//						System.out.println(sim);
////					}
//				}
//			}
//			
//			System.out.println("WUP: \n--------");
//			for(int i = 0; i < words.size(); i++){
//				for(int j = i + 1; j < words.size(); j++){
//					String a = words.get(i);
//					String b = words.get(j);
//					double sim =  WS4J.runWUP(a,b);
////					if(sim > 0){
//						System.out.print("Comparing " + a + " and " + b + ": ");
//						System.out.println(sim);
////					}
//				}
//			}
//			
//			
//			
//			System.out.println("PATH: \n--------");
//			for(int i = 0; i < words.size(); i++){
//				for(int j = i + 1; j < words.size(); j++){
//					String a = words.get(i);
//					String b = words.get(j);
//					double sim =  WS4J.runPATH(a,b);
////					if(sim > 0){
//						System.out.print("Comparing " + a + " and " + b + ": ");
//						System.out.println(sim);
////					}
//				}
//			}
			
			
//			StringBuilder allGlosses = new StringBuilder();
//			TObjectDoubleHashMap<String> wordsMap = new TObjectDoubleHashMap<String>();
//			
//			for(String word : words){
//				IIndexWord indexWord = w.dict.getIndexWord(word, POS.NOUN);
//				if(indexWord != null){
//					for(IWordID wordId : indexWord.getWordIDs()){
//						IWord foundWord = w.dict.getWord(wordId);
//						ISynset synset = foundWord.getSynset();
//						String gloss = synset.getGloss();
//						System.out.println(gloss + "\n");
//						allGlosses.append(synset.getWords().stream().map(IWord::getLemma).reduce("", (x,y) -> x + " " + y));
//						synset.getRelatedMap().entrySet().stream()
//							.forEach(entry -> {
//								entry.getValue().forEach(relatedSynset -> {
////									allGlosses.append(" " + w.dict.getSynset(relatedSynset).getGloss());
//									allGlosses.append(" " + w.dict.getSynset(relatedSynset).getWords().stream().map(IWord::getLemma).reduce("", (x,y) -> x + " " + y));
//								});
//							});
//					}
//				}
//			}
			
			
//			
//			List<String> allGlossesWords = Lemmatizer.instance().lemmatize(allGlosses.toString());
//			
//			allGlossesWords.stream().filter(lemma -> ! Texts.instance().isStopword(lemma)).forEach(lemma -> {
//				wordsMap.adjustOrPutValue(lemma, 1, 1);
//			});
//			
////			NgramExtractor.countsToTfIdf(1, wordsMap, idf);
//			TObjectDoubleIterator<String> it = wordsMap.iterator();
//			List<X> list = new ArrayList<X>();
//			while(it.hasNext()){
//				it.advance();
//				list.add(new X(it.key(), it.value()));
//			}
//			Collections.sort(list);
//			System.out.println(list);
			
			
		}
	}

	
	
	
	
	static class X implements Comparable<X>{
		String s;
		double c;
		NumberFormat f = new DecimalFormat("#0.00");
		public X(String s, double c){
			this.s = s;this.c=c;
		}
		public int compareTo(X other){
			return (int)Math.signum(c - other.c);
		}
		public String toString(){
			return s + " (" + f.format(c) + ")";
		}
	}
	
	public WordNet(IDictionary dict){
		try {
			this.dict = dict;
			dict.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static WordNet fromFile(String dictDir){
		try{
			printer.print("Creating wordnet graph from dict-dir: " + dictDir + " ... ");
			URL dictUrl = new URL("file", null, dictDir);
			IDictionary dict = new Dictionary(dictUrl);
			printer.println("[x]");
			return new WordNet(dict);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public List<IIndexWord> sentenceToIndexWords(Collection<String> words){
		ArrayList<IIndexWord> indexWords = new ArrayList<IIndexWord>();
		for(String word : words){
			IIndexWord indexWord = dict.getIndexWord(word, POS.NOUN);
			if(indexWord != null){
				indexWords.add(indexWord);
			}
		}
		return indexWords;
	}
	
//	@Override
	public double similarity(Collection<String> sentence1, Collection<String> sentence2) {
//		simpleSimilarity(sentence1, sentence2);
		return similarity2(sentence1, sentence2);
	}
	
	private double simpleSimilarity(Collection<String> sentence1, Collection<String> sentence2) {
		double sum = 0;
		for(String word1 : sentence1){
			if(anyRelatedIn(word1, sentence2)){
				sum += 1.0;
			}
		}
		return sum / (double)sentence1.size();
	}
	
	private double similarity2(Collection<String> sentence1, Collection<String> sentence2) {
		ArrayList<IWordID> words1 = sentence1.stream()
				.map(w -> dict.getIndexWord(w, POS.NOUN))
				.filter(iw -> iw != null)
				.flatMap(iw -> iw.getWordIDs().stream())
				.collect(Collectors.toCollection(ArrayList::new));
		ArrayList<IWordID> words2 = sentence2.stream()
				.map(w -> dict.getIndexWord(w, POS.NOUN))
				.filter(iw -> iw != null)
				.flatMap(iw -> iw.getWordIDs().stream())
				.collect(Collectors.toCollection(ArrayList::new));
		
		System.out.println(words1 + "\n");
		System.out.println(words2 + "\n");
		
		double sum = 0;
		for(IWordID word1 : words1){
			for(IWordID related1 : dict.getWord(word1).getRelatedWords()){
				if(words2.contains(related1) || words2.contains(word1)){
					sum += 1.0;
					System.out.println("MATCH " + related1 + " or " + word1);
					break; //don't count many matches for the same lemma
				}
			}
		}
		return sum / (double) words1.size();
	}
	
	public boolean anyRelatedIn(String wordString, Collection<String> collection){
		
		IIndexWord indexWord = dict.getIndexWord(wordString, POS.NOUN);
		if(indexWord == null){
			return false;
		}
		
	
		
		for(IWordID wordID : indexWord.getWordIDs()){
			IWord word = dict.getWord(wordID);
			
			ISynset synset = word.getSynset();
			for(IWord w : synset.getWords()){
				if(collection.contains(w.getLemma())){
					return true;
				}
			}
		}
		return false;
	}
	
	public Set<String> getRelated(String wordString, POS posTag){
		IIndexWord indexWord = dict.getIndexWord(wordString, posTag);
		if(indexWord == null){
			return new HashSet<String>();
		}
		
		HashSet<String> result = new HashSet<String>();
		
		for(IWordID wordID : indexWord.getWordIDs()){
			IWord word = dict.getWord(wordID);
			ISynset synset = word.getSynset();
			
			for(IWord w : synset.getWords()){
				result.add(w.getLemma());
			}
			
			for(ISynsetID relatedID : synset.getRelatedSynsets()){
				ISynset related = dict.getSynset(relatedID);
				for(IWord w : related.getWords()){
					result.add(w.getLemma());
				}
			}
		}
		return result;
	}
	
	public void printSynsetAndRelated(String searchWord){
		IIndexWord indexWord = dict.getIndexWord(searchWord, POS.NOUN);
		if(indexWord == null){
			System.out.println("Unknown word");
			return;
		}
		for(IWordID wordID : indexWord.getWordIDs()){
			IWord word = dict.getWord(wordID);
			ISynset synset = word.getSynset();
			
			System.out.println(synset.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
			
			
			
			System.out.println("Gloss: " + synset.getGloss());
			for(ISynsetID relatedID : synset.getRelatedSynsets()){
				ISynset related = dict.getSynset(relatedID);
				System.out.println("\n\t" + related.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList()));
				System.out.println("\t" + related.getGloss());
			}
			System.out.println();
		}
	}

}
