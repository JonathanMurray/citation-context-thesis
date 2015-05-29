package main;

import java.io.File;
import java.util.ArrayList;

import concepts.SynsetExtractor;
import concepts.WikiGraph;
import concepts.WikiGraphFactory;
import util.Environment;
import util.Lemmatizer;
import util.Printer;
import dataset.Dataset;
import dataset.DatasetFactory;
import dataset.DatasetParams;
import dataset.DatasetXml;
import dataset.NgramIdf;
import dataset.SSpaceWrapper;
import dataset.Text;
import dataset.TextParams;
import dataset.TextWithWiki;
import dataset.TextWithSkipgrams;
import dataset.TextWithNgrams;
import dataset.TextWithSspace;
import dataset.TextWithSynsets;
import edu.mit.jwi.IDictionary;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class CreateDatasetsSaveXml {
	public static void main(String[] args) throws ClassNotFoundException {
		Class textClass = TextWithWiki.class;
		int numDatasets = -1;
		
		if(args.length == 1){
			textClass = Class.forName(args[0]);
		}else if(args.length == 2){
			textClass = Class.forName(args[0]);
			numDatasets = Integer.parseInt(args[1]);
		}else if(args.length != 0){
			System.out.println("Usage:");
			System.out.println("0 args or");
			System.out.println("1 arg: 'text_class' or");
			System.out.println("2 args: 'text_class' 'num_datasets'");
			return;
		}
		
		Printer.printBigHeader("Create XML-datasets (" + textClass + ")");
		Lemmatizer.instance(); //Want the lemma debug prints to appear first
		
		if(textClass == Text.class){
			basic();
		}else if(textClass == TextWithSkipgrams.class){
			withSkipgrams();
		}else if(textClass ==TextWithNgrams.class){
			withNgrams();
		}else if(textClass == TextWithSynsets.class){
			withSynsets(numDatasets); //TODO numdatasets for others too
		}else if(textClass == TextWithSspace.class){
			withSspace();
		}else if(textClass == TextWithWiki.class){
			withWiki();
		}
	}
	
	private final static int BOUNDARY = 80;
	private final static int NUM_HOOKS = 2;
	private final static int NUM_ACRONYMS = 2;
	
//	private final static File HTML_DIR = new File(Environment.resources() + "/my-citation-context-corpus");
//	private final static File XML_DIR = new File(Environment.resources() + "/my-xml-datasets"); 
	
	private final static File XML_DIR = new File(Environment.resources() + "/xml-datasets"); 		
	private final static File HTML_DIR = new File(Environment.resources() + "/teufel-citation-context-corpus");
	
	
	private static String[] LABELS = new String[]{
			"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
			"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
			"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
	
	private static void basic(){
		File resourcesDir = new File(Environment.resources());
		ArrayList<Dataset<Text>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.basic(), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				HTML_DIR);
		for(Dataset<Text> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + ".xml"));
		}
	}
	
	private static void withNgrams(){
		File resourcesDir = new File(Environment.resources());
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		ArrayList<Dataset<TextWithNgrams>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.withNgrams(ngramIdf), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				HTML_DIR);
		for(Dataset<TextWithNgrams> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + "-with-ngrams.xml"));
		}
	}
	
	private static void withSspace(){
		File resourcesDir = new File(Environment.resources());
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		SSpaceWrapper sspace = SSpaceWrapper.load(new File(resourcesDir, "sspace/space-lsa-500.sspace"), new File(resourcesDir, "sspace/wordfrequencies.ser"));
		ArrayList<Dataset<TextWithSspace>> datasets = DatasetFactory.fromHtmlDir(
				DatasetParams.enhanced(TextParams.withSSpace(ngramIdf, sspace), BOUNDARY, NUM_HOOKS, NUM_ACRONYMS), 
				HTML_DIR);
		for(Dataset<TextWithSspace> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + "-with-sspace.xml"));
		}
	}
	
	private static void withSynsets(int numDatasets){
		File resourcesDir = new File(Environment.resources());
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		StanfordCoreNLP pipeline = SynsetExtractor.createPipeline();
		String dictDir = new File(Environment.resources(), "wordnet-dict").toString();
		IDictionary dict = SynsetExtractor.dictFromDir(dictDir);
		TextParams<TextWithSynsets> textParams = TextParams.withSynsets(ngramIdf, pipeline, dict);
		int n = numDatasets > -1 ? numDatasets : LABELS.length;
		for(int i = 0; i < n; i++){
			String label = LABELS[i];
			Printer.printBigProgressHeader(i, n);
			Dataset<Text> other = DatasetXml.parseXmlFile(
					Text.class, 
					new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml")
					, 0);
			Dataset<TextWithSynsets> dataset = DatasetFactory.fromOtherRaw(textParams, other);
			dataset.findAcronymsHooks(BOUNDARY, NUM_HOOKS, NUM_ACRONYMS);
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + "-with-synsets-small.xml"));
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
			dataset.findAcronymsHooks(BOUNDARY, NUM_HOOKS, NUM_ACRONYMS);
			datasets.add(dataset);
		}
		for(Dataset<TextWithSkipgrams> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + "-with-skipgrams.xml"));
		}
	}
	
	private static void withWiki(){
		File resourcesDir = new File(Environment.resources());
		File serDir = new File(resourcesDir, "ser");
		ArrayList<Dataset<TextWithWiki>> datasets = new ArrayList<Dataset<TextWithWiki>>();
		WikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph(new File(serDir, "linksSingleWords.ser"), new File(serDir, "toIndexSingleWords.ser"), false);
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(resourcesDir, "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		for(int i = 0; i < LABELS.length; i++){
			String label = LABELS[i];
			Printer.printBigProgressHeader(i, LABELS.length);
			Dataset<Text> other = DatasetXml.parseXmlFile(
					Text.class, 
					new File(resourcesDir, "xml-datasets/" + label + "-with-ngrams.xml")
					, 0);
			
			Dataset<TextWithWiki> dataset = DatasetFactory.fromOtherRaw(TextParams.withWikiConcepts(ngramIdf, wikiGraph), other);
			dataset.findAcronymsHooks(BOUNDARY, NUM_HOOKS, NUM_ACRONYMS);
			datasets.add(dataset);
		}
		for(Dataset<TextWithWiki> dataset : datasets){
			DatasetXml.writeToXml(dataset, new File(XML_DIR, dataset.datasetLabel + "-with-wiki-concepts.xml"));
		}
	}
}
