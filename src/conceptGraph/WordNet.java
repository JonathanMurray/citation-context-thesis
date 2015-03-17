package conceptGraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import util.Environment;
import util.Lemmatizer;
import util.Printer;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IPointer;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordNet implements ConceptGraph{

	private static Printer printer = new Printer(true);
	
	public IDictionary dict;
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		WordNet w = WordNet.fromFile(new File(Environment.resources(), "wordnet-dict").toString());
		Scanner s = new Scanner(System.in);
		
		boolean c = true;
		while(c){
			String input = s.nextLine();
			IIndexWord iw = w.dict.getIndexWord(input, POS.NOUN);
			
			
			
			System.out.println(iw);
			if(iw != null){
				for(IWordID id  : iw.getWordIDs()){
					System.out.println();
					System.out.println("lemma: " + id.getLemma());
					IWord word = w.dict.getWord(id);
					System.out.println("word: " + word);
					System.out.println("synset: " + word.getSynset());
					Map<IPointer, List<ISynsetID>> relatedMap = word.getSynset().getRelatedMap();
					if(!relatedMap.isEmpty()){
						System.out.println("RELATED:");
						Entry<IPointer, List<ISynsetID>> e = relatedMap.entrySet().iterator().next();
						System.out.println(e.getKey());
						for(ISynsetID rel : e.getValue()){
							System.out.println(w.dict.getSynset(rel));
						}
						System.out.println();
					}
					System.out.println("gloss: " + word.getSynset().getGloss());
				}
			}
			
		}
		
		
		
		
		while(true){
			String a = s.nextLine();
			String b = s.nextLine();
			
			if(w.dict.getIndexWord(a, POS.NOUN) != null){
				System.out.println("a: " + w.dict.getIndexWord(a, POS.NOUN).getLemma());
			}
			
			Collection<String> aWords = Lemmatizer.instance().lemmatize(a);
			Collection<String> bWords = Lemmatizer.instance().lemmatize(b);
			System.out.println(w.similarity(aWords, bWords));
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
