package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Scanner;

import util.ClassificationResult;
import util.Dirs;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;

public class CompareClassifiers {
	
	public static void main(String[] args){
		
		System.out.println("Compare classifiers");
		System.out.println("--------------------------------------------------");
		System.out.println();
		
//		ContextDataSet contextDataset = ContextHTML_Parser.parseHTML(new File(CITATION_DIR, filename + ".html"));
//		String citedContent = readTextfile(new File(CITATION_DIR, filename + ".txt"));
//		File testFile = new File("arff/" + filename + ".html.arff");
		
		
//		File[] htmlArffFiles = Arrays.asList(new File("arff").listFiles()).stream()
//			.filter(f -> f.getName().endsWith(".html.arff"))
//			.collect(Collectors.toList())
//			.toArray(new File[0]);
//		Instances wekaSet = WekaClassifier.fromFiles(htmlArffFiles);
		
		Instances ngramsSet = WekaClassifier.fromFiles(new File(Dirs.resources(), "arff/balanced-ngrams-full-dataset.arff"));
		Instances fullSet = WekaClassifier.fromFiles(new File(Dirs.resources(), "arff/balanced-features-full-dataset.arff"));

		//		DataSet dataset = new DataSet(contextDataset, citedContent, wekaTestSet);
		
//		ArrayList<String> testSentences = contextDataset.citers.stream()
//				.flatMap(citer -> citer.sentences.stream())
//				.map(sentence -> sentence.unprocessedText)
//				.collect(Collectors.toCollection(ArrayList::new));
		
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();
		
//		MRF mrf = new MRF(4);
//		double simMult = 0.01;
//		WikiGraph conceptGraph = WikiGraphFactory.loadWikiGraph("linksSingleWords.ser", "toIndexSingleWords.ser", simMult, false);
//		MRF_WithConcepts mrfConcepts = new MRF_WithConcepts(4, conceptGraph);
		
//		testClassifierPrintResults(dataset, new Classifier("MRF", mrf));
//		testClassifierPrintResults(dataset, new Classifier("MRF - Concepts", mrfConcepts));
//		wekaSMO.trainOnData(wekaTrain);
		
		int numFolds = 4;
		
		boolean balanceData = false; //dataset is already balanced
		List<String> testSentences = null;
		
		printResult("SMO+", wekaSMO.trainAndCrossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("NB+", wekaNB.trainAndCrossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("Tree+", wekaTree.trainAndCrossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("KNN+", wekaKnn.trainAndCrossValidate(fullSet, numFolds, balanceData), testSentences);
		
		printResult("SMO", wekaSMO.trainAndCrossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("NB", wekaNB.trainAndCrossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("Tree", wekaTree.trainAndCrossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("KNN", wekaKnn.trainAndCrossValidate(ngramsSet, numFolds, balanceData), testSentences);
	}
	
	private static String readTextfile(File f){
		try(Scanner sc = new Scanner(new BufferedReader(new FileReader(f)))) {
			StringBuilder s = new StringBuilder();
			while(sc.hasNextLine()){
				s.append(sc.nextLine() + "\n");
			}
			return s.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
//	public static void testClassifierPrintResults(DataSet dataset, Evaluator classifier, List<String> testSentences){
//		System.out.print("Testing " + classifier + "...    ");
//		ClassificationResult res = classifier.evaluate(dataset);
//		printResult(classifier.toString(), res, testSentences);
//	}
	
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
