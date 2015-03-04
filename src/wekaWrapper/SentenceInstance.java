package wekaWrapper;

import java.util.Map;

import citationContextData.SentenceClass;

public class SentenceInstance{
	@SuppressWarnings("rawtypes")
	public Map<String, Comparable> features;
	public SentenceClass instanceClass;
	
	@SuppressWarnings("rawtypes")
	SentenceInstance(Map<String,Comparable> features, SentenceClass instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}