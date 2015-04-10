package wekaWrapper;

import java.util.Map;

import dataset.SentenceType;

public class SentenceInstance{
	public Map<String, Comparable<?>> features;
	public SentenceType instanceClass;
	
	SentenceInstance(Map<String,Comparable<?>> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}