package ml;

import java.util.Map;

public class Instance{
	Map<Feature, Boolean> features;
	SentenceType instanceClass;
	
	Instance(Map<Feature,Boolean> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder();
		for(Feature feature : Feature.values()){
			s.append(features.get(feature) ? "1" : "0");
		}
		return instanceClass + ": " + s;
	}
}