package sentenceFeaturesToWeka;

import java.util.Map;

import citationContextData.SentenceClass;

class Instance{
	Map<String, Comparable> features;
	SentenceClass instanceClass;
	
	Instance(Map<String,Comparable> features, SentenceClass instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}

}