package main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import mrf.MRF_classifier;
import mrf.MRF_params;
import mrf.MRF_params.Relatedness;
import mrf.MRF_params.SelfBelief;
import util.ClassificationResult;
import util.Environment;
import util.Printer;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.Text;
import citationContextData.TextWithConcepts;
import citationContextData.Xml;

public class CompareClassifiers {
	
	private static Printer printer = new Printer(true);
	
	public static void main(String[] args){
		
		System.out.println("Compare classifiers");
		System.out.println("--------------------------------------------------");
		System.out.println();

//		WordNet wordnet = WordNet.fromFile(new File(Environment.resources(), "wordnet-dict").getPath());
		String resourcesDir = Environment.resources();
		
		//---------------------Datasets
		//Naive data for Weka
		Instances ngramsSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-ngrams-full-dataset.arff"));
		
		//Sophisticated data for Weka
		Instances fullSet = WekaClassifier.fromFiles(new File(resourcesDir, "arff/balanced-features-full-dataset.arff"));
		
		//Datasets for MRF
//		ArrayList<Dataset<TextWithNgrams>> datasets = DatasetFactory.fromHtmlDir(
//				DatasetParams.enhanced(TextParams.withNgrams(), 20, 5), 
//				new File(resourcesDir, "teufel-citation-context-corpus"));
		final int MAX_CITERS = 0;
		Dataset<TextWithConcepts> datasetWithConcepts = Xml.parseXmlFile(
				new File(resourcesDir, "xml-datasets/A92-1018-with-concepts.xml"), MAX_CITERS);
		
//		Xml.parseFromXml(new File(resourcesDir, ))
		
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();
		
//		WikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph(
//				new File(resourcesDir, "ser/linksSingleWords.ser"), 
//				new File(resourcesDir, "ser/toIndexSingleWords.ser"), 
//				false);
		
		
		int numFolds = 4;
		boolean balanceData = false; //dataset is already balanced
		List<Sentence<TextWithConcepts>> testSentences = datasetWithConcepts.getSentences();
		
		MRF_params params = new MRF_params(3, 0.5, new SelfBelief(), new Relatedness());
		printResult("MRF-wiki", new MRF_classifier<TextWithConcepts>(params).classify(datasetWithConcepts), testSentences);
//		params = new MRF_params(3, 0.6, new SelfBelief(), new Relatedness());
//		printResult("MRF-wiki-0.6", new MRF_classifier<TextWithConcepts>(params).classify(datasetWithConcepts), testSentences);
//		params = new MRF_params(3, 0.4, new SelfBelief(), new Relatedness());
//		printResult("MRF-wiki-0.4", new MRF_classifier<TextWithConcepts>(params).classify(datasetWithConcepts), testSentences);
		
		printResult("SMO+", wekaSMO.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("NB+", wekaNB.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("Tree+", wekaTree.crossValidate(fullSet, numFolds, balanceData), testSentences);
		printResult("KNN+", wekaKnn.crossValidate(fullSet, numFolds, balanceData), testSentences);
		
		printResult("SMO", wekaSMO.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("NB", wekaNB.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("Tree", wekaTree.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
		printResult("KNN", wekaKnn.crossValidate(ngramsSet, numFolds, balanceData), testSentences);
	}
	
	private static <T extends Text> void printResult(String title, ClassificationResult result, List<Sentence<T>> testSentences){
		NumberFormat f = new DecimalFormat("#.000"); 
		System.out.println();
		System.out.println(title);
		System.out.println("-------------------------");
		System.out.println("Passed time: " + (int)(result.getPassedMillis()/1000.0) + "s");
		System.out.println(result.confusionMatrixToString());
		System.out.println("pos F: " + f.format(result.positiveFMeasure(1)));
		System.out.println("pos F3: " + f.format(result.positiveFMeasure(3)));
		System.out.println("neg F: " + f.format(result.negativeFMeasure(1)));
		System.out.println("Micro avg. F: " + f.format(result.microAvgFMeasure(1)));
		System.out.println("Macro avg. F: " + f.format(result.macroAvgFMeasure(1)));
		
//		if(testSentences != null){
//			System.out.println("\nFalse negatives:");
//			result.falseNegativeIndices().stream()
//					.limit(2)
//					.map(i -> testSentences.get(i))
//					.forEach(sentence -> System.out.println(sentence.type + "  -   " + sentence.text.raw));
//			System.out.println("\nFalse positives:");
//			result.falsePositiveIndices().stream()
//					.limit(2)
//					.map(i -> testSentences.get(i))
//					.forEach(sentence -> System.out.println(sentence.type + "  -   " + sentence.text.raw));
//		}
		System.out.println();
	}
}
