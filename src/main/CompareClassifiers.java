package main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import markovRandomField.MRF;
import markovRandomField.MRF_WithConcepts;
import util.ClassificationResult;
import util.Environment;
import util.Printer;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;
import citationContextData.SingleCitedDataSet;
import conceptGraph.WikiGraph;
import conceptGraph.WikiGraphFactory;

public class CompareClassifiers {
	
	private static Printer printer = new Printer(true);
	
	public static void main(String[] args){
		
		System.out.println("Compare classifiers");
		System.out.println("--------------------------------------------------");
		System.out.println();
		
//		File[] htmlArffFiles = Arrays.asList(new File("arff").listFiles()).stream()
//			.filter(f -> f.getName().endsWith(".html.arff"))
//			.collect(Collectors.toList())
//			.toArray(new File[0]);
//		Instances wekaSet = WekaClassifier.fromFiles(htmlArffFiles);
		
		String resourcesDir = Environment.resources();
		
		Instances ngramsSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-ngrams-full-dataset.arff"));
		Instances fullSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-features-full-dataset.arff"));
		ArrayList<SingleCitedDataSet> datasets = datasetsFromDir(new File(resourcesDir, "teufel-citation-context-corpus"));
		
//		DataSet dataset = new DataSet(contextDataset, citedContent, wekaTestSet);
		
//		ArrayList<String> testSentences = contextDataset.citers.stream()
//				.flatMap(citer -> citer.sentences.stream())
//				.map(sentence -> sentence.unprocessedText)
//				.collect(Collectors.toCollection(ArrayList::new));
		
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();
		
		MRF mrf = new MRF(4);
		double simMult = 0.01;
		WikiGraph conceptGraph = WikiGraphFactory.loadWikiGraph(new File(resourcesDir, "ser/linksSingleWords.ser"), new File(resourcesDir, "ser/toIndexSingleWords.ser"), simMult, false);
		MRF_WithConcepts mrfConcepts = new MRF_WithConcepts(4, conceptGraph);
		
//		testClassifierPrintResults(dataset, new Classifier("MRF", mrf));
//		testClassifierPrintResults(dataset, new Classifier("MRF - Concepts", mrfConcepts));
//		wekaSMO.trainOnData(wekaTrain);
		
		int numFolds = 4;
		
		boolean balanceData = false; //dataset is already balanced
		List<String> testSentences = null;
		
		printResult("MRF", mrf.classify(datasets.get(0)), testSentences);
		printResult("MRF+", mrfConcepts.classify(datasets.get(0)), testSentences);
		
		printResult("SMO+", wekaSMO.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("NB+", wekaNB.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("Tree+", wekaTree.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("KNN+", wekaKnn.crossValidate(fullSet, numFolds, balanceData), testSentences);
		
		printResult("SMO", wekaSMO.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("NB", wekaNB.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("Tree", wekaTree.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("KNN", wekaKnn.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
	}
	
//	public static void testClassifierPrintResults(DataSet dataset, Evaluator classifier, List<String> testSentences){
//		System.out.print("Testing " + classifier + "...    ");
//		ClassificationResult res = classifier.evaluate(dataset);
//		printResult(classifier.toString(), res, testSentences);
//	}
	
	private static ArrayList<SingleCitedDataSet> datasetsFromDir(File dir){
		ArrayList<SingleCitedDataSet> datasets = new ArrayList<SingleCitedDataSet>();
		File[] files = dir.listFiles();
		System.out.print("Creating citation data set from files: ");
		for(int i = 0; i < files.length; i++){
			printer.progress(i, 1);
			File htmlFile = files[i];
			if(!htmlFile.getName().endsWith(".html")){
				continue;
			}
			String baseName = htmlFile.getName().substring(0, htmlFile.getName().length()-5);
			File textFile = new File(dir, baseName + ".txt");
			datasets.add(SingleCitedDataSet.fromFiles(htmlFile, textFile));
		}
		System.out.println(" [x]");
		return datasets;
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
