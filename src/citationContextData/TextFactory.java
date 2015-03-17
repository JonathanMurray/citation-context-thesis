package citationContextData;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import util.Lemmatizer;
import util.Texts;
import conceptGraph.Concept;

public class TextFactory {
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T getText(TextParams<T> params, String raw){
		
		List<String> lemmatizedWords = Lemmatizer.instance().lemmatize(raw);
		ArrayList<String> rawWords = Texts.split(raw).collect(Collectors.toCollection(ArrayList::new));
		
		if(params.textClass.equals(TextWithNgrams.class)){
			List<String> lowercaseLemmas = new ArrayList<String>();
			for(String lemma : lemmatizedWords){
				lowercaseLemmas.add(lemma.toLowerCase());
			}
			
			TObjectDoubleHashMap<String> unigramsTfIdf = Texts.instance().getNgramsTfIdf(1, lowercaseLemmas, params.ngramIdf);
			TObjectDoubleHashMap<String> bigramsTfIdf = Texts.instance().getNgramsTfIdf(2, lowercaseLemmas, params.ngramIdf);
			return (T) new TextWithNgrams(raw, rawWords, lemmatizedWords, unigramsTfIdf, bigramsTfIdf);
		}
		
		else if(params.textClass.equals(TextWithConcepts.class)){
			List<Concept> concepts = params.wikiGraph.sentenceToConcepts(lemmatizedWords);
			List<String> lowercaseLemmas = new ArrayList<String>();
			for(String lemma : lemmatizedWords){
				lowercaseLemmas.add(lemma.toLowerCase());
			}
			TObjectDoubleHashMap<String> unigramsTfIdf = Texts.instance().getNgramsTfIdf(1, lowercaseLemmas, params.ngramIdf);
			TObjectDoubleHashMap<String> bigrams = Texts.instance().getNgramsTfIdf(2, lowercaseLemmas, params.ngramIdf);
			return (T) new TextWithConcepts(raw, rawWords, lemmatizedWords, 
					unigramsTfIdf, bigrams, concepts);
		}
		
		else if(params.textClass.equals(TextWithWordnet.class)){
			return (T) new TextWithWordnet(raw, rawWords, lemmatizedWords, params.wordnet);
		}

		else if(params.textClass.equals(Text.class)){
			return (T) new Text(raw, rawWords, lemmatizedWords);
		}
		
		else{
			throw new IllegalArgumentException("Unknown class " + params.textClass);
		}
	}
}
