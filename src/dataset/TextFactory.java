package dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import concepts.SynsetExtractor;
import util.Lemmatizer;
import edu.mit.jwi.item.ISynset;

public class TextFactory {
	
	private final static int MAX_NGRAM_N = 3;
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T createText(TextParams<T> params, String raw){
		
		List<String> lemmas = Lemmatizer.instance().lemmatize(raw);
		ArrayList<String> rawWords = Texts.split(raw).collect(Collectors.toCollection(ArrayList::new));
		
		if(params.textClass.equals(TextWithNgrams.class)){
			Ngrams ngramsTfIdf = NgramExtractor.ngramsTfIdf(MAX_NGRAM_N, lemmas, params.ngramIdf);
			return (T) new TextWithNgrams(raw, rawWords, lemmas, ngramsTfIdf);
		}
		
		else if(params.textClass.equals(TextWithSkipgrams.class)){
			Ngrams[] nAndSkipgrams = NgramExtractor.nAndSkipgramsTfIdf(MAX_NGRAM_N, lemmas, params.ngramIdf, params.skipgramIdf);
			Ngrams ngramsTfIdf = nAndSkipgrams[0];
			Ngrams skipgramsTfIdf = nAndSkipgrams[1];
			return (T) new TextWithSkipgrams(raw, rawWords, lemmas, ngramsTfIdf, skipgramsTfIdf);
		}
		
		else if(params.textClass.equals(TextWithSynsets.class)){
			Ngrams ngramsTfIdf = NgramExtractor.ngramsTfIdf(MAX_NGRAM_N, lemmas, params.ngramIdf);
			SynsetExtractor wordnet = new SynsetExtractor(params.nlpPipeline, params.wordnetDict, params.synsetDepths);
			List<ISynset> synsets = wordnet.fromSentence(lemmas);
			return (T) new TextWithSynsets(raw, rawWords, lemmas, ngramsTfIdf, synsets);
		}
		
//		else if(params.textClass.equals(TextWithConcepts.class)){
//			List<Concept> concepts = params.wikiGraph.sentenceToConcepts(lemmatizedWords);
//			List<String> lowercaseLemmas = new ArrayList<String>();
//			for(String lemma : lemmatizedWords){
//				lowercaseLemmas.add(lemma.toLowerCase());
//			}
//			Ngrams ngramsTfIdf = Texts.instance().getAllNgramsTfIdf(MAX_NGRAM_N, lowercaseLemmas, params.ngramIdf);
//			return (T) new TextWithConcepts(raw, rawWords, lemmatizedWords, 
//					ngramsTfIdf, concepts);
//		}
		
//		else if(params.textClass.equals(TextWithWordnet.class)){
//			return (T) new TextWithWordnet(raw, rawWords, lemmatizedWords, params.wordnet);
//		}

		else if(params.textClass.equals(Text.class)){
			return (T) new Text(raw, rawWords, lemmas);
		}
		
		else{
			throw new IllegalArgumentException("Unknown class " + params.textClass);
		}
	}
}
