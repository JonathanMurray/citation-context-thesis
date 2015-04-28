package dataset;

import concepts.WikiGraph;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TextParams<T extends Text>{
	public Class<T> textClass;
	public NgramIdf ngramIdf;
	public NgramIdf skipgramIdf;
	public WikiGraph wikiGraph;
	public StanfordCoreNLP nlpPipeline;
	public IDictionary wordnetDict;
	public TObjectIntHashMap<ISynset> synsetDepths;
	public SSpaceWrapper sspace;
	
	public static TextParams<Text> basic(){
		TextParams<Text> p = new TextParams<Text>(Text.class);
		return p;
	}
	
	public static TextParams<TextWithNgrams> withNgrams(NgramIdf ngramIdf){
		TextParams<TextWithNgrams> p = new TextParams<TextWithNgrams>(TextWithNgrams.class);
		p.ngramIdf = ngramIdf;
		return p;
	}
	
	public static TextParams<TextWithSspace> withSSpace(NgramIdf ngramIdf, SSpaceWrapper sspace){
		TextParams<TextWithSspace> p = new TextParams<TextWithSspace>(TextWithSspace.class);
		p.ngramIdf = ngramIdf;
		p.sspace = sspace;
		return p;
	}

	public static TextParams<TextWithSynsets> withSynsets(NgramIdf ngramIdf, StanfordCoreNLP nlpPipeline, IDictionary wordnetDict){
		TextParams<TextWithSynsets> p = new TextParams<TextWithSynsets>(TextWithSynsets.class);
		p.ngramIdf = ngramIdf;
		p.nlpPipeline = nlpPipeline;
		p.wordnetDict = wordnetDict;
		p.synsetDepths = new TObjectIntHashMap<ISynset>();
		return p;
	}
	
	public static TextParams<TextWithSkipgrams> withSkipgrams(NgramIdf ngramIdf, NgramIdf skipgramIdf){
		TextParams<TextWithSkipgrams> p = new TextParams<TextWithSkipgrams>(TextWithSkipgrams.class);
		p.ngramIdf = ngramIdf;
		p.skipgramIdf = skipgramIdf;
		return p;
	}
	
//	public static TextParams<TextWithConcepts> withWikiConcepts(NgramIdf wordIdf, WikiGraph wikiGraph){
//		TextParams<TextWithConcepts> p = new TextParams<TextWithConcepts>(TextWithConcepts.class);
//		p.ngramIdf = wordIdf;
//		p.wikiGraph = wikiGraph;
//		return p;
//	}
//	
//	public static TextParams<TextWithWordnet> withWordnet(WordNet wordnet){
//		TextParams<TextWithWordnet> p = new TextParams<TextWithWordnet>(TextWithWordnet.class);
//		p.wordnet = wordnet;
//		return p;
//	}
	
	private TextParams(Class<T> textClass){
		this.textClass = textClass;
	}
}