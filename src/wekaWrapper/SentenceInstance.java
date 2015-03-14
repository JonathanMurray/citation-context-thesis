package wekaWrapper;

import java.util.Map;

import citationContextData.SentenceType;

public class SentenceInstance{
	@SuppressWarnings("rawtypes")
	public Map<String, Comparable> features;
	public SentenceType instanceClass;
	
	@SuppressWarnings("rawtypes")
	SentenceInstance(Map<String,Comparable> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}