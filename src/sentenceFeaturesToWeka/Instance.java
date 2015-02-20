package sentenceFeaturesToWeka;

import java.util.Map;

public class Instance{
	Map<String, Comparable> features;
	SentenceType instanceClass;
	
	Instance(Map<String,Comparable> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}

}