package main;

import java.io.File;
import java.util.ArrayList;

import util.Environment;
import util.NgramIdf;
import citationContextData.Dataset;
import citationContextData.DatasetFactory;
import citationContextData.DatasetParams;
import citationContextData.Text;
import citationContextData.TextParams;
import citationContextData.TextWithNgrams;
import citationContextData.Xml;

public class CreateDatasetsSaveXml {
	public static void main(String[] args) {
		withNgrams();
	}
	
	private final static int BOUNDARY = 80;
	private final static int NUM_HOOKS = 2;
	private final static int NUM_ACRONYMS = 2;
	
	private static void basic(){
		File resourcesDir = new File(Environment.resources());
		ArrayList<Dataset<Text>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.basic(), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				new File(resourcesDir, "teufel-citation-context-corpus"));
		for(Dataset<Text> dataset : datasets){
			Xml.writeToXml(dataset, new File(resourcesDir, "xml-datasets/" + dataset.datasetLabel + ".xml"));
		}
	}
	
	private static void withNgrams(){
		File resourcesDir = new File(Environment.resources());
		NgramIdf wordIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"));
		ArrayList<Dataset<TextWithNgrams>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.withNgrams(wordIdf), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				new File(resourcesDir, "teufel-citation-context-corpus"));
		for(Dataset<TextWithNgrams> dataset : datasets){
			Xml.writeToXml(dataset, new File(resourcesDir, "xml-datasets/" + dataset.datasetLabel + "-with-ngrams.xml"));
		}
	}
}
