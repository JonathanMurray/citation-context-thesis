package sentenceFeaturesToWeka;

import java.util.Map;

import citationContextData.SentenceClass;

public class Instance{
	@SuppressWarnings("rawtypes")
	public Map<String, Comparable> features;
	public SentenceClass instanceClass;
	
	@SuppressWarnings("rawtypes")
	Instance(Map<String,Comparable> features, SentenceClass instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}