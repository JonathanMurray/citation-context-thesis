package sentenceFeaturesToWeka;

import java.util.Map;

import citationContextData.SentenceClass;

public class SimpleInstance{
	@SuppressWarnings("rawtypes")
	public Map<String, Comparable> features;
	public SentenceClass instanceClass;
	
	@SuppressWarnings("rawtypes")
	SimpleInstance(Map<String,Comparable> features, SentenceClass instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}