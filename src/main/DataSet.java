package main;

import weka.core.Instances;
import citationContextData.ContextDataSet;

public class DataSet {
	ContextDataSet contextDataset;
	String citedArticleContent;
	Instances wekaTestSet;
	
	
	public DataSet(ContextDataSet contextDataset, String citedArticleContent, Instances wekaTestSet) {
		this.contextDataset = contextDataset;
		this.citedArticleContent = citedArticleContent;
		this.wekaTestSet = wekaTestSet;
	}
}
