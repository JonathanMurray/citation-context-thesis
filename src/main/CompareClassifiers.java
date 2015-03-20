package main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import mrf.MRF_classifier;
import mrf.MRF_params;
import util.Environment;
import util.Printer;
import weka.core.Instances;
import wekaWrapper.WekaClassifier;
import citationContextData.ClassificationResult;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.Text;
import citationContextData.TextWithNgrams;
import citationContextData.Xml;

public class CompareClassifiers {
	
	private static Printer printer = new Printer(true);
	
	public static void main(String[] args){
		
		System.out.println("Compare classifiers");
		System.out.println("--------------------------------------------------");
		System.out.println();

		List<String> labels = Arrays.asList(new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"});
		
//		labels = labels.subList(0, 5); //TODO
		
		testMRF(labels);
//		testWeka(labels);
		
//		WikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph(
//				new File(resourcesDir, "ser/linksSingleWords.ser"), 
//				new File(resourcesDir, "ser/toIndexSingleWords.ser"), 
//				false);

	}
	
	private static void testWeka(List<String> labels){
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();

		String resourcesDir = Environment.resources();
		
		List<Instances> wekaBalancedDatasets = new ArrayList<Instances>();
		List<Instances> wekaFullDatasets = new ArrayList<Instances>();
		for(String label : labels){
			Instances balancedDataset = WekaClassifier.fromFiles(new File(resourcesDir, "arff/" + label + "-enhanced.arff"));
			wekaBalancedDatasets.add(balancedDataset);
			Instances fullDataset = WekaClassifier.fromFiles(new File(resourcesDir, "arff/" + label + "-enhanced-full.arff"));
			wekaFullDatasets.add(fullDataset);
		}
		
		List<ClassificationResult> results = wekaSMO.manualCrossValidation(labels, wekaBalancedDatasets, wekaFullDatasets);
		printMultipleResults("SMO", results, null, true);
		
	}
	
	private static void testMRF(List<String> labels){
		String resourcesDir = Environment.resources();
		List<Dataset<TextWithNgrams>> datasets = new ArrayList<Dataset<TextWithNgrams>>();
		for(String label : labels){
			final int MAX_CITERS = 0;
			Dataset<TextWithNgrams> dataset = Xml.parseXmlFile(
					TextWithNgrams.class,
					new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml"), 
					MAX_CITERS);
			System.out.println(dataset.datasetLabel);
			System.out.println("(" + dataset.citedMainAuthor + ")");
//			dataset.findExtra(80, 2, 2);
			System.out.println(dataset.getAcronyms());
			System.out.println(dataset.getLexicalHooks()); //TODO
			System.out.println();
			datasets.add(dataset);
		}
		
		MRF_params params = new MRF_params(3, 0.4);
		List<ClassificationResult> results = new MRF_classifier<TextWithNgrams>(params).classify(datasets);
		System.out.println("FULL RESULTS:");
		printMultipleResults("MRF-wiki", results, datasets, true);
		System.out.println("COMPACT RESULTS:");
		printMultipleResults("MRF-wiki", results, datasets, false);
	}
	
	private static <T extends Text> void printMultipleResults(String title, Collection<ClassificationResult> results, List<Dataset<T>> datasets, boolean verbose){
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		System.out.println("                 MULTIPLE RESULTS (" + title + "): ");
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		int i = 0;
		for(ClassificationResult result : results){
			Dataset<T> dataset = null;
			if(datasets != null){
				dataset = datasets.get(i);
			}
			printResult(result, null, verbose, dataset);
			i ++;
		}
		System.out.println();
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		System.out.println("\n\n\n");
	}
	
	private static <T extends Text> void printResult(ClassificationResult result, List<Sentence<T>> testSentences, boolean verbose, Dataset<T> dataset){
		NumberFormat f = new DecimalFormat("#.000"); 
		if(verbose){
			System.out.println();
			System.out.println(result.label());
			System.out.println("-------------------------");
			System.out.println("Passed time: " + (int)(result.getPassedMillis()/1000.0) + "s");
			System.out.println(result.confusionMatrixToString());
			System.out.println("pos F: " + f.format(result.positiveFMeasure(1)));
			System.out.println("pos F3: " + f.format(result.positiveFMeasure(3)));
			System.out.println("neg F: " + f.format(result.negativeFMeasure(1)));
			System.out.println("Micro avg. F: " + f.format(result.microAvgFMeasure(1)));
			System.out.println("Macro avg. F: " + f.format(result.macroAvgFMeasure(1)));
			System.out.println();
		}else{
			System.out.print("pos F: " + f.format(result.positiveFMeasure(1)));
			System.out.print("    pos F3: " + f.format(result.positiveFMeasure(3)));
			if(dataset != null){
				System.out.print("  (" + dataset.citedMainAuthor + ")");
				if(dataset != null && dataset.hasExtra){
					System.out.print("    " + dataset.getLexicalHooks() + " " + dataset.getAcronyms() + " ");
				}
			}
			System.out.println();
		}
	}
}
