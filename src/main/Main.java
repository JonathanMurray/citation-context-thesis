package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import markovRandomField.MRF_WithConcepts;

import org.apache.pdfbox.ExtractText;

import util.ClassificationResult;
import util.NonThrowingFileWriter;
import weka.core.Instances;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SentenceInstance;
import wekaWrapper.WekaClassifier;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import citationContextData.SentenceClass;
import conceptGraph.PreBuiltWikiGraph;
import conceptGraph.QuickWikiGraph;
import conceptGraph.WikiGraph;
import conceptGraph.WikiGraphFactory;
import conceptGraph.WordNet;

public class Main {
	
	public static final File DATA_DIR = Paths.get("/home/jonathan/Documents/exjobb/data/").toFile();
	public static final File CFC_DIR = new File(DATA_DIR, "CFC_distribution/2006_paper_training/");
	public static final File CITATION_DIR = new File(DATA_DIR, "teufel-citation-context-corpus/");
	public static final File WIKI_DIR = new File(DATA_DIR, "wikipedia");
	public static final String WORDNET_DICT = "/home/jonathan/Documents/exjobb/data/wordnet-dict"; 
	
	public static void main(String[] args) throws IOException {
		
//		WikiGraphFactory.buildLinksAndSaveToFile("toIndexSingleWords.ser", "linksSingleWords.ser", true);
		
//		PreBuiltWikiGraph graph = PreBuiltWikiGraph.fromFiles("linksSingleWords.ser", "toIndexSingleWords.ser");
//		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
//		
//		conceptSimilarity(graph);
		
		compareClassifiers();
	}
	
	public static  void testQuickWikiGraph(){
		QuickWikiGraph g = new QuickWikiGraph(
				new File(WIKI_DIR, "titles-sorted.txt"),
				new File(WIKI_DIR, "links-simple-sorted.txt"));
		
		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
		System.out.println(g.similarity(sentences[0].split(" +"), sentences[1].split(" +")));
	}
	
	public static void convertAllPDFsToText(File dir){
		for(File pdfFile : dir.listFiles()){
			if(pdfFile.getName().endsWith(".pdf")){
				String baseName = pdfFile.getName().split("\\.pdf")[0];
				File textFile = new File(dir, baseName + ".txt");
				pdfToText(pdfFile, textFile);
			}
		}
	}
	
	public static void pdfToText(File pdfFile, File textFile){
		try {
			System.out.print("Converting " + pdfFile.getName() + " to " + textFile.getName() + "...  ");
			ExtractText.main(new String[]{pdfFile.getAbsolutePath(), textFile.getAbsolutePath()});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(textFile.exists()? "SUCCESS" : "FAIL");
	}
	
	public static void compareConceptGraphs(){
		System.out.println("CREATING wikigraph from ser files");
		PreBuiltWikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph("links.ser", "phraseToIndex.ser", 0.01, false);
		System.out.println("created wikigraph from ser-files");
		wikiGraph.setSimilarityMultiplier(0.01);
		MRF_WithConcepts wikiMrf = new MRF_WithConcepts(4, wikiGraph);
		WordNet wordnet = WordNet.fromFile("/home/jonathan/Documents/exjobb/data/wordnet-dict");
		MRF_WithConcepts wordnetMrf = new MRF_WithConcepts(4, wordnet);
		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
		System.out.println(wikiGraph.similarity(sentences[0].trim().split(" "), sentences[1].trim().split(" ")));
		System.out.println(wordnet.similarity(sentences[0].trim().split(" "), sentences[1].trim().split(" ")));
	}
	
	public static void downloadPDFsForHTML_Files() throws IOException{
		for(File f : CITATION_DIR.listFiles()){
			if(f.getName().endsWith(".html")){
				ContextDataSet dataset = ContextDataSet.fromHTML_File(f);
				String name = dataset.datasetLabel;
				URL url = new URL("http://www.aclweb.org/anthology/" + name);
				BufferedInputStream in = new BufferedInputStream(url.openStream());
				File newFile = new File(CITATION_DIR, name + ".pdf");
				System.out.println("new file: " + newFile.getAbsolutePath());
				BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile));
				int b;
				while((b = in.read()) != -1){
					fos.write(b);
				}
				in.close();
				fos.flush();
				fos.close();
			}
		}
	}
	
	public static void printInfoFromAllHTML_Files(){
		for(File f : CITATION_DIR.listFiles()){
			if(f.getName().endsWith(".html")){
				ContextDataSet dataset = ContextDataSet.fromHTML_File(f);
				System.out.println(dataset.datasetLabel);
				System.out.println(dataset.citedMainAuthor);
				System.out.println(dataset.citedTitle);
				System.out.println();
			}
		}
	}
	
	public static void convertAllDataToArff(File dir){
		for(File htmlFile : dir.listFiles()){
			convertDataToArff(htmlFile);
		}
	}

	private static void convertDataToArff(File... htmlFiles){
		for(File htmlFile : htmlFiles){
			ContextDataSet dataset = ContextHTML_Parser.parseHTML(htmlFile);
			List<SentenceInstance> instances = InstanceHandler.createInstances(dataset);
			InstanceHandler.writeToArffFile(instances, Paths.get("arff/" + htmlFile.getName() + ".arff"));
		}
	}

	public static void compareClassifiers(){
		
//		System.out.println("Comparing classifiers on [" + filename + "]");
		System.out.println("--------------------------------------------------");
		System.out.println();
		
//		ContextDataSet contextDataset = ContextHTML_Parser.parseHTML(new File(CITATION_DIR, filename + ".html"));
//		String citedContent = readTextfile(new File(CITATION_DIR, filename + ".txt"));
//		File testFile = new File("arff/" + filename + ".html.arff");
		Instances wekaSet = WekaClassifier.fromFiles(new File("arff").listFiles());

		//		DataSet dataset = new DataSet(contextDataset, citedContent, wekaTestSet);
		
//		ArrayList<String> testSentences = contextDataset.citers.stream()
//				.flatMap(citer -> citer.sentences.stream())
//				.map(sentence -> sentence.unprocessedText)
//				.collect(Collectors.toCollection(ArrayList::new));
		
		WekaClassifier wekaSMO = WekaClassifier.SMO();
		WekaClassifier wekaNB = WekaClassifier.NaiveBayes();
		WekaClassifier wekaTree = WekaClassifier.J48();
		WekaClassifier wekaKnn = WekaClassifier.KNN();
		
//		Instances wekaTrain = WekaClassifier.fromDirExcept(new File("arff/"), testFile);
//		Instances wekaTrain2 = new Instances(wekaTrain);
//		Instances wekaTrain3 = new Instances(wekaTrain);
//		Instances wekaTrain4 = new Instances(wekaTrain);
		
//		MRF mrf = new MRF(4);
//		double simMult = 0.01;
//		WikiGraph conceptGraph = WikiGraphFactory.loadWikiGraph("linksSingleWords.ser", "toIndexSingleWords.ser", simMult, false);
//		MRF_WithConcepts mrfConcepts = new MRF_WithConcepts(4, conceptGraph);
		
//		testClassifierPrintResults(dataset, new Classifier("MRF", mrf));
//		testClassifierPrintResults(dataset, new Classifier("MRF - Concepts", mrfConcepts));
//		wekaSMO.trainOnData(wekaTrain);
		int numFolds = 10;
		boolean balanceData = true;
		
		printResult("SMO", wekaSMO.trainAndCrossValidate(wekaSet, numFolds, balanceData));
		printResult("NB", wekaNB.trainAndCrossValidate(wekaSet, numFolds, balanceData));
		printResult("Tree", wekaTree.trainAndCrossValidate(wekaSet, numFolds, balanceData));
		printResult("KNN", wekaKnn.trainAndCrossValidate(wekaSet, numFolds, balanceData));
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
	
	public static void printResult(String title, ClassificationResult result, List<String> testSentences){
		printResult(title, result);;
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
	
	public static void printResult(String title, ClassificationResult result){
		NumberFormat f = new DecimalFormat("#.000"); 
		System.out.println("\n\n");
		System.out.println(title);
		System.out.println("-------------------------");
		System.out.println(result.confusionMatrixToString());
		System.out.println("pos F: " + f.format(result.positiveFMeasure(1)));
		System.out.println("neg F: " + f.format(result.negativeFMeasure(1)));
		System.out.println("Micro avg. F: " + f.format(result.microAvgFMeasure(1)));
		System.out.println("Macro avg. F: " + f.format(result.macroAvgFMeasure(1)));
	}
	
	public static List<SentenceInstance> createInstancesFromFiles(File[] files){
		List<ContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> ContextHTML_Parser.parseHTML(f))
				.collect(Collectors.toList());
		ArrayList<SentenceInstance> instances = datasets.stream()
				.flatMap(dataset -> InstanceHandler.createInstances(dataset).stream())
				.collect(Collectors.toCollection(ArrayList::new));
				
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE).limit(10000),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toCollection(ArrayList::new));
		
		return instances;
	}
	
	public static void conceptSimilarity(WikiGraph graph){
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter 2 sentences to compare: ");
		while(true){
			System.out.println("Enter first: ");
			String[] s1 = sc.nextLine().split("\\s+");
			if(s1.length > 0 && s1[0].equals("bye")){
				break;
			}
			System.out.println("Enter second: ");
			String[] s2 = sc.nextLine().split("\\s+");
			System.out.println(graph.similarity(s1, s2));
		}
		sc.close();
	}
	
	public static void readDatasetWriteSentences(){
		File inFile = Paths.get("/home/jonathan/Documents/exjobb/data/teufel-citation-context-corpus/A92-1018.html").toFile();
		ContextDataSet dataset = new ContextHTML_Parser().parseHTML(inFile);
		Citer citer = dataset.citers.get(0);
		NonThrowingFileWriter writer = new NonThrowingFileWriter(Paths.get("SENTENCES-TEST.txt").toFile());
		for(Sentence s : citer.sentences){
			writer.write(s.unprocessedText + "\n");
		}
		writer.close(); 
	}

}
