package citationContextData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import util.Lemmatizer;
import conceptGraph.Concept;

public class TextFactory {
	
	private final static int MAX_NGRAM_N = 3;
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T createText(TextParams<T> params, String raw){
		
		List<String> lemmatizedWords = Lemmatizer.instance().lemmatize(raw);
		ArrayList<String> rawWords = Texts.split(raw).collect(Collectors.toCollection(ArrayList::new));
		
		if(params.textClass.equals(TextWithNgrams.class)){
			List<String> lowercaseLemmas = new ArrayList<String>();
			for(String lemma : lemmatizedWords){
				lowercaseLemmas.add(lemma.toLowerCase());
			}
			Ngrams ngramsTfIdf = Texts.instance().getAllNgramsTfIdf(MAX_NGRAM_N, lowercaseLemmas, params.ngramIdf);
			return (T) new TextWithNgrams(raw, rawWords, lemmatizedWords, ngramsTfIdf);
		}
		
		else if(params.textClass.equals(TextWithConcepts.class)){
			List<Concept> concepts = params.wikiGraph.sentenceToConcepts(lemmatizedWords);
			List<String> lowercaseLemmas = new ArrayList<String>();
			for(String lemma : lemmatizedWords){
				lowercaseLemmas.add(lemma.toLowerCase());
			}
			Ngrams ngramsTfIdf = Texts.instance().getAllNgramsTfIdf(MAX_NGRAM_N, lowercaseLemmas, params.ngramIdf);
			return (T) new TextWithConcepts(raw, rawWords, lemmatizedWords, 
					ngramsTfIdf, concepts);
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
