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

import markovRandomField.MRF;
import markovRandomField.MRF_WithConcepts;

import org.apache.pdfbox.ExtractText;

import util.ClassificationResult;
import util.NonThrowingFileWriter;
import weka.core.Instances;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SimpleInstance;
import wekaWrapper.WekaClassifier;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import citationContextData.SentenceClass;
import conceptGraph.PreBuiltWikiGraph;
import conceptGraph.QuickWikiGraph;
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
		
		PreBuiltWikiGraph graph = PreBuiltWikiGraph.fromFiles("linksSingleWords.ser", "toIndexSingleWords.ser");
		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
		System.out.println(graph.similarity(sentences[0].split(" +"), sentences[1].split(" +")));
		
//		compareConceptGraphs();
//		compareClassifiers("A92-1018");
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
		PreBuiltWikiGraph wikiGraph = PreBuiltWikiGraph.fromFiles("links.ser", "phraseToIndex.ser");
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
			List<SimpleInstance> instances = InstanceHandler.createInstances(dataset);
			InstanceHandler.writeToArffFile(instances, Paths.get("arff/" + htmlFile.getName() + ".arff"));
		}
	}

	public static void compareClassifiers(String filename){
		
		System.out.println("Comparing classifiers on [" + filename + "]");
		System.out.println("--------------------------------------------------");
		System.out.println();
		
		ContextDataSet contextDataset = ContextHTML_Parser.parseHTML(new File(CITATION_DIR, filename + ".html"));
		
		String citedContent = readTextfile(new File(CITATION_DIR, filename + ".txt"));
		
		
		Instances wekaInstances = WekaClassifier.fromFiles(new File("arff/" + filename + ".html.arff"));
		DataSet dataset = new DataSet(contextDataset, citedContent, wekaInstances);
		
		
		WekaClassifier classifier = WekaClassifier.SMO();
		classifier.trainOnData(WekaClassifier.fromDirExcept(
				new File("arff/"), new File("arff/" + filename + ".html.arff")));
		
		MRF mrf = new MRF(4);
//		WikiGraph conceptGraph = WikiGraph.fromFiles("links.ser", "phraseToIndex.ser");
//		conceptGraph.setSimilarityMultiplier(0.01);
//		MRF_WithConcepts mrfWithConcepts = new MRF_WithConcepts(4, conceptGraph);
		
		compareClassifiers(dataset, 
				new Classifier("MRF", mrf) ,
				new Classifier("Weka", classifier) 
				
//				new Classifier("MRF - Concepts", mrfWithConcepts)
		);
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
	
	public static void compareClassifiers(DataSet dataset, Classifier... classifiers){
		for(Classifier classifier : classifiers){
			System.out.println("Testing " + classifier + "...");
			ClassificationResult res = classifier.testOn(dataset);
			printResult(classifier.toString(), res);
		}
	}
	
	public static void printResult(String title, ClassificationResult result){
		NumberFormat f = new DecimalFormat("#.000"); 
		System.out.println();
		System.out.println(title);
		System.out.println("-------------------------");
		System.out.println("precision: " + f.format(result.precision()));
		System.out.println("recall: " + f.format(result.recall()));
		System.out.println("F: " + f.format(result.fMeasure()));
		System.out.println();
	}
	
	public static List<SimpleInstance> createInstancesFromFiles(File[] files){
		List<ContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> ContextHTML_Parser.parseHTML(f))
				.collect(Collectors.toList());
		ArrayList<SimpleInstance> instances = datasets.stream()
				.flatMap(dataset -> InstanceHandler.createInstances(dataset).stream())
				.collect(Collectors.toCollection(ArrayList::new));
				
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE).limit(10000),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toCollection(ArrayList::new));
		
		return instances;
	}
	
	public static void conceptSimilarity(){
		PreBuiltWikiGraph graph = PreBuiltWikiGraph.fromFiles("", "");
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
