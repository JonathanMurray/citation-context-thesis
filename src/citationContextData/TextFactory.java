package citationContextData;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.List;

import util.Lemmatizer;
import util.Texts;
import conceptGraph.TextWithConcepts;

public class TextFactory {
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T getText(TextParams<T> params, String raw){
		
		if(params.textClass.equals(TextWithNgrams.class)){
			List<String> lemmatizedWords = Lemmatizer.instance().lemmatize(raw);
			String lemmatized = Texts.merge(lemmatizedWords);
			TObjectIntHashMap<String> unigrams = Texts.instance().getNgrams(1, lemmatizedWords, true);
			TObjectIntHashMap<String> bigrams = Texts.instance().getNgrams(2, lemmatizedWords, true);
			return (T) new TextWithNgrams(raw, lemmatized, lemmatizedWords, unigrams, bigrams);
		}
		
		else if(params.textClass.equals(TextWithConcepts.class)){
			List<String> lemmatizedWords = Lemmatizer.instance().lemmatize(raw);
			String lemmatized = Texts.merge(lemmatizedWords);
			return (T) new TextWithConcepts(raw, lemmatized, lemmatizedWords, params.wikiGraph.sentenceToConcepts(lemmatizedWords));
		}

		else if(params.textClass.equals(Text.class)){
			return (T) new Text(raw);
		}
		
		else{
			throw new IllegalArgumentException("Unknown class " + params.textClass);
		}
	}
}
