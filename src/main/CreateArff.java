package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import mrf.MRF_classifier;
import mrf.MRF_params;
import util.Environment;
import util.Printer;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SentenceInstance;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.TextWithNgrams;
import dataset.TextWithSspace;


public class CreateArff {
	
	public static void main(String[] args) {
		Printer.printBigHeader("Create .arff-datasets");
		
		String resourcesDir = Environment.resources();
		
		String[] labels = new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
		
//		labels = new String[]{"W06-1615"}; //TODO
		
		final boolean onlyText = false; //TODO
		
		MRF_classifier<TextWithSspace> mrfClassifier = 
				new MRF_classifier<TextWithSspace>(new MRF_params(3, 0.4, 10));
		
		for(int i = 0; i < labels.length; i++){
			String label = labels[i];
			final int MAX_CITERS = 0; //0 means unlimited
			Dataset<TextWithSspace> dataset = DatasetXml.parseXmlFile(
					TextWithSspace.class,
				new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml"), 
				MAX_CITERS);
			Printer.printBigProgressHeader(i, labels.length);
			System.out.println(dataset.datasetLabel);
			System.out.println("(" + dataset.citedMainAuthor + ")");
			
			//TODO
			List<Double> mrfProbabilities = mrfClassifier.classify(dataset).classificationProbabilities();
			mrfProbabilities = null;
			
			String outAfterLabel = "lsa";
			
			ArrayList<SentenceInstance> balancedInstances =  InstanceHandler.createInstances(dataset, onlyText, true, mrfProbabilities);
			InstanceHandler.writeToArffFile(balancedInstances, new File(Environment.resources(), 
					"arff/" + dataset.datasetLabel + "-" + outAfterLabel + ".arff"));	
			
			ArrayList<SentenceInstance> fullInstances =  InstanceHandler.createInstances(dataset, onlyText, false, mrfProbabilities);
			InstanceHandler.writeToArffFile(fullInstances, new File(Environment.resources(), 
					"arff/" + dataset.datasetLabel + "-" + outAfterLabel + "-full.arff"));	
		}
		
	}
}
