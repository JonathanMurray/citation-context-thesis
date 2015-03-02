package conceptGraph;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordNet implements ConceptGraph{

	public static void main(String[] args) throws IOException {
		WordNet jwi = WordNet.fromFile("/home/jonathan/Documents/exjobb/data/wordnet-dict");
		System.out.println(jwi.getRelated("part_of_speech", POS.NOUN));
	}
	
	private IDictionary dict;
	
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
			URL dictUrl = new URL("file", null, dictDir);
			IDictionary dict = new Dictionary(dictUrl);
			return new WordNet(dict);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(0);
			return null;
		}
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

	@Override
	public double similarity(String[] sentence1, String[] sentence2) {
		double sum = 0;
		for(String word1 : sentence1){
			for(String related1 : getRelated(word1, POS.NOUN)){
				if(contains(related1, sentence2)){
					sum += 1;
				}
			}
		}
		return sum / (double)sentence1.length;
	}
	
	private <T> boolean contains(T element, T[] array){
		for(int i = 0; i < array.length; i++){
			if(array[i].equals(element)){
				return true;
			}
		}
		return false;
	}
}
