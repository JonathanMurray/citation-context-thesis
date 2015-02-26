package main;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import markovRandomField.MRF;
import markovRandomField.MRF_WithConcepts;
import util.NonThrowingFileWriter;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import conceptGraph.ConceptGraph;

public class Main {
	
	public static final String DATA_DIR = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = DATA_DIR + "CFC_distribution/2006_paper_training/";
	public static final String SENTIMENT_CORPUS_DIR = DATA_DIR + "teufel-citation-context-corpus/";
	
	
	public static void main(String[] args) {
		
	}
	

	private static void compare(){
		ContextDataSet dataset = ContextHTML_Parser.parseHTML(Paths.get(SENTIMENT_CORPUS_DIR + "A92-1018.html").toFile());
		
		System.out.println(dataset.citedTitle);
		System.out.println("main author: " + dataset.citedMainAuthor);
		
		String citedAbstract = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements. Only a lexicon and some unlabeled training text are required. Accuracy exceeds 96%. We describe implementation strategies and optimizations which result in high-speed operation. Three applications for tagging are described: phrase recognition; word sense disambiguation; and grammatical function assignment.";
		
		List<Citer> citers = dataset.citers;
		
		new MRF().runManyAndPrintResults(citers, dataset.citedMainAuthor, citedAbstract, dataset);
		ConceptGraph conceptGraph = ConceptGraph.fromFiles("links.ser", "phraseToIndex.ser");
		conceptGraph.setSimilarityMultiplier(0.01);
		new MRF_WithConcepts(conceptGraph).runManyAndPrintResults(citers, dataset.citedMainAuthor, citedAbstract, dataset);
		
	}
	
	private static void conceptSimilarity(){
		ConceptGraph graph = ConceptGraph.fromFiles("", "");
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
	
	private static void readDatasetWriteSentences(){
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
