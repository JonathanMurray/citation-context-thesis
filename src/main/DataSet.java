package main;

import weka.core.Instances;
import citationContextData.ContextDataSet;

public class DataSet {
	ContextDataSet contextDataset;
	String citedArticleContent;
	Instances wekaInstances;
	
	
	public DataSet(ContextDataSet contextDataset, String citedArticleContent, Instances wekaInstances) {
		this.contextDataset = contextDataset;
		this.citedArticleContent = citedArticleContent;
		this.wekaInstances = wekaInstances;
	}
}
