package main;

import java.io.File;
import java.util.ArrayList;

import util.Environment;
import util.Lemmatizer;
import util.Printer;
import dataset.Dataset;
import dataset.DatasetFactory;
import dataset.DatasetParams;
import dataset.DatasetXml;
import dataset.NgramIdf;
import dataset.Text;
import dataset.TextParams;
import dataset.TextWithSkipgrams;
import dataset.TextWithNgrams;

public class CreateDatasetsSaveXml {
	public static void main(String[] args) {
		Lemmatizer.instance(); //Want the lemma debug prints to appear first
		withSkipgrams();
//		withNgrams();
	}
	
	private final static int BOUNDARY = 80;
	private final static int NUM_HOOKS = 2;
	private final static int NUM_ACRONYMS = 2;
	
	private final static String[] LABELS = new String[]{
			"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
			"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
			"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
	
	private static void basic(){
		File resourcesDir = new File(Environment.resources());
		ArrayList<Dataset<Text>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.basic(), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				new File(resourcesDir, "teufel-citation-context-corpus"));
		for(Dataset<Text> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(resourcesDir, "xml-datasets/" + dataset.datasetLabel + ".xml"));
		}
	}
	
	private static void withNgrams(){
		File resourcesDir = new File(Environment.resources());
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		ArrayList<Dataset<TextWithNgrams>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.withNgrams(ngramIdf), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				new File(resourcesDir, "teufel-citation-context-corpus"));
		for(Dataset<TextWithNgrams> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(resourcesDir, "xml-datasets/" + dataset.datasetLabel + "-with-ngrams.xml"));
		}
	}
	
	private static void withSkipgrams(){
		File resourcesDir = new File(Environment.resources());
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		NgramIdf skipgramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/skipgram-frequencies.xml"), NgramIdf.DEFAULT_SKIPGRAM_MIN_COUNT);
//		ArrayList<Dataset<TextWithSkipgrams>> datasets = DatasetFactory.fromHtmlDir(
//				DatasetParams.enhanced(TextParams.withSkipgrams(ngramIdf, skipgramIdf), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
//				new File(resourcesDir, "teufel-citation-context-corpus"));
		ArrayList<Dataset<TextWithSkipgrams>> datasets = new ArrayList<Dataset<TextWithSkipgrams>>();
		for(int i = 0; i < LABELS.length; i++){
			String label = LABELS[i];
			Printer.printBigProgressHeader(i, LABELS.length);
			Dataset<Text> other = DatasetXml.parseXmlFile(
					Text.class, 
					new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml")
					, 0);
			Dataset<TextWithSkipgrams> dataset = DatasetFactory.fromOtherRaw(TextParams.withSkipgrams(ngramIdf, skipgramIdf), other);
			dataset.findExtra(BOUNDARY, NUM_HOOKS, NUM_ACRONYMS);
			datasets.add(dataset);
		}
		for(Dataset<TextWithSkipgrams> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(resourcesDir, "xml-datasets/" + dataset.datasetLabel + "-with-skipgrams.xml"));
		}
	}
}
