package ml;

import java.util.Map;

public class Instance{
	Map<String, Comparable> features;
	SentenceType instanceClass;
	
	Instance(Map<String,Comparable> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
	
//	public String toString(){
//		StringBuilder s = new StringBuilder();
//		for(Feature feature : Feature.values()){
//			s.append(features.get(feature.toString()) ? "1" : "0");
//		}
//		return instanceClass + ": " + s;
//	}
}