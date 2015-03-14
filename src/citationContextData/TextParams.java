package citationContextData;

import conceptGraph.TextWithConcepts;
import conceptGraph.WikiGraph;

public class TextParams<T extends Text>{
	public Class<T> textClass;
	public WikiGraph wikiGraph;
	
	public static <T extends Text> TextParams<T> basic(Class<T> textClass){
		return new TextParams(textClass, null);
	}
	
	public static <T extends Text> TextParams<T> withConcepts(WikiGraph wikiGraph){
		return new TextParams(TextWithConcepts.class, wikiGraph);
	}
	
	private TextParams(Class<T> textClass, WikiGraph wikiGraph){
		this.textClass = textClass;
		this.wikiGraph = wikiGraph;
	}
}