package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import util.Environment;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SentenceInstance;
import citationContextData.Dataset;
import citationContextData.TextWithNgrams;
import citationContextData.DatasetXml;


public class CreateArff {
	
	public static void main(String[] args) {
		System.out.println("Create ARFF-file");
		System.out.println("--------------------------");
		
		String resourcesDir = Environment.resources();
		
		String[] labels = new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
		
		List<Dataset<TextWithNgrams>> datasets = new ArrayList<Dataset<TextWithNgrams>>();
		for(String label : labels){
			final int MAX_CITERS = 0;
			Dataset<TextWithNgrams> dataset = DatasetXml.parseXmlFile(
				TextWithNgrams.class,
				new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml"), 
				MAX_CITERS);
			System.out.println(dataset.datasetLabel);
			System.out.println("(" + dataset.citedMainAuthor + ")");
			datasets.add(dataset);
		}
		
		
		
		HashMap<String, ArrayList<SentenceInstance>> balancedDatasets = 
				InstanceHandler.createInstanceSets(datasets, false, true);
		for(Entry<String, ArrayList<SentenceInstance>> e : balancedDatasets.entrySet()){
			String label = e.getKey();
			ArrayList<SentenceInstance> instances = e.getValue();
				InstanceHandler.writeToArffFile(instances, new File(Environment.resources(), 
						"arff/" + label + "-enhanced.arff"));	
		}
		
		HashMap<String, ArrayList<SentenceInstance>> fullDatasets = 
				InstanceHandler.createInstanceSets(datasets, false, false);
		for(Entry<String, ArrayList<SentenceInstance>> e : fullDatasets.entrySet()){
			String label = e.getKey();
			ArrayList<SentenceInstance> fullDataset = e.getValue();
				InstanceHandler.writeToArffFile(fullDataset, new File(Environment.resources(), 
						"arff/" + label + "-enhanced-full.arff"));	
		}
		
	}
}
