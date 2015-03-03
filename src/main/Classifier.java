package main;

import markovRandomField.MRF;
import util.ClassificationResult;
import wekaWrapper.WekaClassifier;

public class Classifier {
	String name;
	WekaClassifier wekaClassifier;
	MRF mrf;
	
	Classifier(String name, WekaClassifier w){
		this.name = name;
		wekaClassifier = w;
	}
	
	Classifier(String name, MRF m){
		this.name = name;
		mrf = m;
	}
	
	ClassificationResult testOn(DataSet dataset){
		if(wekaClassifier != null){
			return wekaClassifier.testOnData(dataset.wekaInstances);
		}else{
			return mrf.runMany(dataset.contextDataset.citers, dataset.citedArticleContent, dataset.contextDataset);
		}
	}
	
	public String toString(){
		return name;
	}
}
