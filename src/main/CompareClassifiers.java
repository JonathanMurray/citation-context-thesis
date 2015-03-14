package main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import mrf.MRF_params;
import mrf.MRF_withConcepts;
import util.ClassificationResult;
import util.Environment;
import util.Printer;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;
import citationContextData.Dataset;
import citationContextData.DatasetFactory;
import citationContextData.DatasetParams;
import citationContextData.TextParams;
import citationContextData.TextWithNgrams;
import conceptGraph.WikiGraph;
import conceptGraph.WikiGraphFactory;
import conceptGraph.WordNet;

public class CompareClassifiers {
	
	private static Printer printer = new Printer(true);
	
	public static void main(String[] args){
		
		System.out.println("Compare classifiers");
		System.out.println("--------------------------------------------------");
		System.out.println();

		WordNet wordnetGraph = WordNet.fromFile(new File(Environment.resources(), "wordnet-dict").getPath());
		String resourcesDir = Environment.resources();
		
		//---------------------Datasets
		//Naive data for Weka
		Instances ngramsSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-ngrams-full-dataset.arff"));
		
		//Sophisticated data for Weka
		Instances fullSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-features-full-dataset.arff"));
		
		//Datasets for MRF
		ArrayList<Dataset<TextWithNgrams>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.basic(TextWithNgrams.class), 20, 5), 
				new File(resourcesDir, "teufel-citation-context-corpus"));
		
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();
		
		WikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph(new File(resourcesDir, "ser/linksSingleWords.ser"), new File(resourcesDir, "ser/toIndexSingleWords.ser"), false);
		
		
		double simMult = 0.01;
		int numFolds = 4;
		boolean balanceData = false; //dataset is already balanced
		List<String> testSentences = null;
		
		MRF_params params = new MRF_params();
		
		printResult("MRF-wordnet", new MRF_withConcepts(params, wordnetGraph, simMult).classify(datasets.get(0)), testSentences);
		printResult("MRF-wiki", new MRF_withConcepts(params, wikiGraph, simMult).classify(datasets.get(0)), testSentences);
		
		printResult("SMO+", wekaSMO.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("NB+", wekaNB.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("Tree+", wekaTree.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("KNN+", wekaKnn.crossValidate(fullSet, numFolds, balanceData), testSentences);
		
		printResult("SMO", wekaSMO.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("NB", wekaNB.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("Tree", wekaTree.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("KNN", wekaKnn.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
	}
	
	private static void printResult(String title, ClassificationResult result, List<String> testSentences){
		NumberFormat f = new DecimalFormat("#.000"); 
		System.out.println();
		System.out.println(title);
		System.out.println("-------------------------");
		System.out.println("Passed time: " + (int)(result.getPassedMillis()/1000.0) + "s");
		System.out.println(result.confusionMatrixToString());
		System.out.println("pos F: " + f.format(result.positiveFMeasure(1)));
		System.out.println("neg F: " + f.format(result.negativeFMeasure(1)));
		System.out.println("Micro avg. F: " + f.format(result.microAvgFMeasure(1)));
		System.out.println("Macro avg. F: " + f.format(result.macroAvgFMeasure(1)));
		
		if(testSentences != null){
			System.out.println("\nFalse negatives:");
			result.falseNegativeIndices().stream()
					.limit(2)
					.map(i -> testSentences.get(i))
					.forEach(System.out::println);
			System.out.println("\nFalse positives:");
			result.falsePositiveIndices().stream()
					.limit(2)
					.map(i -> testSentences.get(i))
					.forEach(System.out::println);
		}
		System.out.println();
	}
}
