package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import mrf.MRF_params;
import mrf.MRF_classifier;
import util.Environment;
import util.Printer;
import weka.InstanceHandler;
import weka.SentenceInstance;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.SentenceKey;
import dataset.Text;
import dataset.TextWithNgrams;
import dataset.TextWithSspace;

/**
 * Create .arff-files used as input in WEKA.
 * @author jonathan
 *
 */
public class CreateArff {
	
	public static void main(String[] args) throws ClassNotFoundException {
		Class textClass = TextWithSspace.class;
		String outAfterLabel = "-my-features-mrf-test";
		
		if(args.length == 2){
			textClass = Class.forName(args[0]);
			outAfterLabel = args[1];
		}else if(args.length != 0){
			System.out.println("Usage: ");
			System.out.println("0 arguments or");
			System.out.println("2 arguments: 'text_class' 'output_label'");
			return;
		}
		createArff(textClass, outAfterLabel);
	}
	
	public static <T extends Text> void createArff(Class<T> textClass, String outAfterLabel){
		Printer.printBigHeader("Create .arff-datasets");
		String resourcesDir = Environment.resources();
		String[] labels = new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
		
//		labels = new String[]{"W06-1615"}; //TODO
		
		final boolean onlyText = false; //TODO
		
		MRF_classifier<TextWithNgrams> mrfClassifier = 
				new MRF_classifier<TextWithNgrams>(new MRF_params(4, 0.5, 100));
		
		for(int i = 0; i < labels.length; i++){
			String label = labels[i];
			final int MAX_CITERS = 0; //0 means unlimited
			Dataset<TextWithNgrams> dataset = DatasetXml.parseXmlFile(
					TextWithNgrams.class,
				new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml"), 
				MAX_CITERS);
			Printer.printBigProgressHeader(i, labels.length);
			System.out.println(dataset.datasetLabel);
			System.out.println("(" + dataset.citedMainAuthor + ")");
			
			HashMap<SentenceKey<TextWithNgrams>, Double> mrfProbabilities =  mrfClassifier.classify(dataset).classificationProbabilities();
			
			ArrayList<SentenceInstance> balancedInstances =  InstanceHandler.createInstances(dataset, onlyText, true, mrfProbabilities);
			InstanceHandler.writeToArffFile(balancedInstances, new File(Environment.resources(), 
					"arff/" + dataset.datasetLabel + outAfterLabel + ".arff"));	
			
			ArrayList<SentenceInstance> fullInstances =  InstanceHandler.createInstances(dataset, onlyText, false, mrfProbabilities);
			InstanceHandler.writeToArffFile(fullInstances, new File(Environment.resources(), 
					"arff/" + dataset.datasetLabel + outAfterLabel + "-full.arff"));	
		}
	}
}
