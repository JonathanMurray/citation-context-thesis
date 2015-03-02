package jwi_test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

public class JWIMain {

	public static void main(String[] args) throws IOException {
		JWIMain jwi = JWIMain.fromFile("/home/jonathan/Documents/exjobb/data/wordnet-dict");
		System.out.println(jwi.getRelated("part_of_speech"));
	}
	
	private IDictionary dict;
	
	public JWIMain(IDictionary dict){
		try {
			this.dict = dict;
			dict.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static JWIMain fromFile(String dictDir){
		try{
			URL dictUrl = new URL("file", null, dictDir);
			IDictionary dict = new Dictionary(dictUrl);
			return new JWIMain(dict);
		}catch(IOException e){
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public Set<String> getRelated(String wordString){
		IIndexWord indexWord = dict.getIndexWord(wordString, POS.NOUN);
		if(indexWord == null){
//			System.out.println("Unknown word");
			return new HashSet<String>();
		}
		
		ArrayList<String> result = new ArrayList<String>();
		
		for(IWordID wordID : indexWord.getWordIDs()){
			IWord word = dict.getWord(wordID);
			ISynset synset = word.getSynset();
			
			List<String> words = synset.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList());
			result.addAll(words);
			
			for(ISynsetID relatedID : synset.getRelatedSynsets()){
				ISynset related = dict.getSynset(relatedID);
//				System.out.println(related.getWords() + " " +  related.getGloss());
				List<String> relatedWords = related.getWords().stream().map(w -> w.getLemma()).collect(Collectors.toList());
				result.addAll(relatedWords);
			}
		}
		return new HashSet<String>(result);
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
