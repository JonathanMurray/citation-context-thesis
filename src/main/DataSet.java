package main;

import weka.core.Instances;
import citationContextData.ContextDataSet;

public class DataSet {
	ContextDataSet contextDataset;
	String citedAbstract;
	Instances wekaInstances;
	
	
	public DataSet(ContextDataSet contextDataset, String citedAbstract, Instances wekaInstances) {
		this.contextDataset = contextDataset;
		this.citedAbstract = citedAbstract;
		this.wekaInstances = wekaInstances;
	}
}
