package dataset;

import concepts.WikiGraph;
import concepts.WordNet;

public class TextParams<T extends Text>{
	public Class<T> textClass;
	public NgramIdf ngramIdf;
	public WikiGraph wikiGraph;
	public WordNet wordnet;
	
	public static TextParams<Text> basic(){
		TextParams<Text> p = new TextParams<Text>(Text.class);
		return p;
	}
	
	public static TextParams<TextWithNgrams> withNgrams(NgramIdf wordIdf){
		TextParams<TextWithNgrams> p = new TextParams<TextWithNgrams>(TextWithNgrams.class);
		p.ngramIdf = wordIdf;
		return p;
	}
	
	public static TextParams<TextWithConcepts> withWikiConcepts(NgramIdf wordIdf, WikiGraph wikiGraph){
		TextParams<TextWithConcepts> p = new TextParams<TextWithConcepts>(TextWithConcepts.class);
		p.ngramIdf = wordIdf;
		p.wikiGraph = wikiGraph;
		return p;
	}
	
	public static TextParams<TextWithWordnet> withWordnet(WordNet wordnet){
		TextParams<TextWithWordnet> p = new TextParams<TextWithWordnet>(TextWithWordnet.class);
		p.wordnet = wordnet;
		return p;
	}
	
	private TextParams(Class<T> textClass){
		this.textClass = textClass;
	}
}