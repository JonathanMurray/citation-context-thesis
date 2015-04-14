package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mrf.MRF_classifier;
import mrf.MRF_params;
import util.Environment;
import util.Printer;
import wekaWrapper.WekaClassifier;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.ResultImpl;
import dataset.Text;
import dataset.TextWithNgrams;
import dataset.TextWithSspace;

public class MRF {

	public static void main(String[] args) {
		
		int numDatasets = -1;
		if(args.length == 1){
			numDatasets = Integer.parseInt(args[0]);
		}
		
		Printer.printBigHeader("MRF-classifier");

		List<String> labels = Arrays.asList(new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"});
		if(numDatasets > -1){
			labels = labels.subList(0, numDatasets);
		}
		testMRF(TextWithSspace.class, "-with-ngrams", labels);
	}
	
	private static <T extends Text> void testMRF(Class<T> textClass, String afterLabelInFileName, List<String> labels){
		String resourcesDir = Environment.resources();
		List<Dataset<T>> datasets = new ArrayList<Dataset<T>>();
//		labels = labels.subList(0, 1); //TODO
		for(String label : labels){
			final int MAX_CITERS = 0;
			Dataset<T> dataset = DatasetXml.parseXmlFile(
					textClass,
					new File(resourcesDir, "xml-datasets/" + label + afterLabelInFileName + ".xml"), 
					MAX_CITERS);
//			dataset.findExtra(80, 2, 2);
//			System.out.println(dataset.datasetLabel);
//			System.out.println("(" + dataset.citedMainAuthor + ")");
//			System.out.println(dataset.getAcronyms());
//			System.out.println(dataset.getLexicalHooks()); //TODO
//			System.out.println();
			datasets.add(dataset);
		}
		
		final int neighbourhood = 4;
		final double beliefThreshold = 0.6;
		final int maxRuns = 0;
		MRF_params params = new MRF_params(neighbourhood, beliefThreshold, maxRuns);
		
		List<ResultImpl> results = new MRF_classifier<T>(params).classify(datasets);
		
		ResultImpl mergedResults = ResultImpl.mergeMany(results);
		int classIndex = 1;
		WekaClassifier.plotROC_curve(mergedResults.predictions(), classIndex);
		
		System.out.println("FULL RESULTS:");
		Printer.printMultipleResults("MRF-wiki", results, datasets, true);
		System.out.println("COMPACT RESULTS:");
		Printer.printMultipleResults("MRF-wiki", results, datasets, false);
	}
}
