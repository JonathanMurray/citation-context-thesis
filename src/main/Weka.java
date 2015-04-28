package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.Environment;
import util.Printer;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;
import dataset.Result;

public class Weka {
	
	public static void main(String[] args){
		
		int numDatasets = -1;
		if(args.length >= 1){
			numDatasets = Integer.parseInt(args[0]);
		}
		String label = "";
		if(args.length >= 2){
			label = args[1];
		}
		
		Printer.printBigHeader("Weka-classifier");

		List<String> labels = Arrays.asList(new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"});
		
		if(numDatasets > -1){
			labels = labels.subList(0, numDatasets);
		}

		testWeka(labels, label);
	}
	
	private static void testWeka(List<String> labels, String afterLabel){
		System.out.println("after label: " + afterLabel);
		WekaClassifier smo = WekaClassifier.SMO();
		WekaClassifier nb = WekaClassifier.NaiveBayes();
		WekaClassifier j48 = WekaClassifier.J48();
		WekaClassifier knn = WekaClassifier.KNN();
		WekaClassifier adaboost = WekaClassifier.Adaboost();
		WekaClassifier vote = WekaClassifier.Vote();

		String resourcesDir = Environment.resources();
		
		List<Instances> wekaBalancedDatasets = new ArrayList<Instances>();
		List<Instances> wekaFullDatasets = new ArrayList<Instances>();
		for(String label : labels){
			Instances balancedDataset = WekaClassifier.fromFiles(new File(resourcesDir, "arff/" + label + afterLabel + ".arff"));
			wekaBalancedDatasets.add(balancedDataset);
			Instances fullDataset = WekaClassifier.fromFiles(new File(resourcesDir, "arff/" + label + afterLabel + "-full.arff"));
			wekaFullDatasets.add(fullDataset);
		}
		
		
		//TODO Run infogain on full dataset (on server?)
//		HashMap<String,Double> infogains = smo.evaluateAttributes(WekaClassifier.mergeDatasets(wekaFullDatasets, -1));
//		System.out.println("INFO-GAIN:");
//		System.out.println(Printer.valueSortedMap(infogains, 50));
		
//		smo.ROC(wekaBalancedDatasets.get(0));
		
		
//		System.out.println("FULL RESULTS:");
//		List<Result> results = smo.manualCrossValidation(labels, wekaBalancedDatasets, wekaFullDatasets);
//		Printer.printMultipleResults("SMO", results, null, true);
//		System.out.println("COMPACT RESULTS:");
//		Printer.printMultipleResults("SMO", results, null, false);
		
		System.out.println("RESULT FOR MERGED X-VALIDATION:");
		Result result = smo.crossValidateMerged("Merged full datasets", wekaFullDatasets, 4);
		Printer.printResult(result, null, true, null);
	}
}
