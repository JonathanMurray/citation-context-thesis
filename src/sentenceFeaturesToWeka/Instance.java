package sentenceFeaturesToWeka;

import java.util.Map;

public class Instance{
	Map<String, Comparable> features;
	SentenceClass instanceClass;
	
	Instance(Map<String,Comparable> features, SentenceClass instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}

}