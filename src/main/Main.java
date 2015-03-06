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
import citationContextData.SingleCitedDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import citationContextData.SentenceClass;
import conceptGraph.PreBuiltWikiGraph;
import conceptGraph.QuickWikiGraph;
import conceptGraph.WikiGraph;
import conceptGraph.WikiGraphFactory;
import conceptGraph.WordNet;

public class Main {
	
	public static final File DATA_DIR = new File(System.getenv("RESOURCES_DIR"));// Paths.get("/home/jonathan/Documents/exjobb/data/").toFile();
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
		
	}
	
	public static  void testQuickWikiGraph(){
		QuickWikiGraph g = new QuickWikiGraph(
				new File(WIKI_DIR, "titles-sorted.txt"),
				new File(WIKI_DIR, "links-simple-sorted.txt"));
		
		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
		System.out.println(g.similarity(sentences[0].split("\\s+"), sentences[1].split("\\s+")));
	}
	
	public static void compareConceptGraphs(){
//		System.out.println("CREATING wikigraph from ser files");
//		PreBuiltWikiGraph wikiGraph = WikiGraphFactory.loadWikiGraph("links.ser", "phraseToIndex.ser", 0.01, false);
//		System.out.println("created wikigraph from ser-files");
//		wikiGraph.setSimilarityMultiplier(0.01);
//		MRF_WithConcepts wikiMrf = new MRF_WithConcepts(4, wikiGraph);
//		WordNet wordnet = WordNet.fromFile("/home/jonathan/Documents/exjobb/data/wordnet-dict");
//		MRF_WithConcepts wordnetMrf = new MRF_WithConcepts(4, wordnet);
//		String[] sentences = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements".split("\\.");
//		System.out.println(wikiGraph.similarity(sentences[0].trim().split(" "), sentences[1].trim().split(" ")));
//		System.out.println(wordnet.similarity(sentences[0].trim().split(" "), sentences[1].trim().split(" ")));
	}
	
	public static void printInfoFromAllHTML_Files(){
		for(File f : CITATION_DIR.listFiles()){
			if(f.getName().endsWith(".html")){
				SingleCitedDataSet dataset = SingleCitedDataSet.fromHTMLFile(f);
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
			SingleCitedDataSet dataset = ContextHTML_Parser.parseHTML(htmlFile);
			List<SentenceInstance> instances = InstanceHandler.createInstances(dataset, false, true);
			InstanceHandler.writeToArffFile(instances, Paths.get("arff/" + "balanced-" + htmlFile.getName() + ".arff").toFile());
		}
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
		SingleCitedDataSet dataset = new ContextHTML_Parser().parseHTML(inFile);
		Citer citer = dataset.citers.get(0);
		NonThrowingFileWriter writer = new NonThrowingFileWriter(Paths.get("SENTENCES-TEST.txt").toFile());
		for(Sentence s : citer.sentences){
			writer.write(s.unprocessedText + "\n");
		}
		writer.close(); 
	}

}
