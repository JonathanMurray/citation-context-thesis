package weka;

import java.util.Map;

import dataset.SentenceType;

/**
 * Represents a sentence in the context of WEKA-classification.
 * @author jonathan
 *
 */
public class SentenceInstance{
	public Map<String, Comparable<?>> features;
	public SentenceType instanceClass;
	
	SentenceInstance(Map<String,Comparable<?>> features, SentenceType instanceClass){
		this.features = features;
		this.instanceClass = instanceClass;
	}
}