package main;

import java.io.File;
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
import sentenceFeaturesToWeka.WekaClassifier;
import sentenceFeaturesToWeka.Instance;
import sentenceFeaturesToWeka.InstanceHandler;
import util.ClassificationResult;
import util.ClassificationResultImpl;
import util.NonThrowingFileWriter;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import citationContextData.SentenceClass;
import conceptGraph.ConceptGraph;

public class Main {
	
	public static final String DATA_DIR = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = DATA_DIR + "CFC_distribution/2006_paper_training/";
	public static final String SENTIMENT_CORPUS_DIR = DATA_DIR + "teufel-citation-context-corpus/";
	
	
	public static void main(String[] args) {
//		convertDataToArff("A92-1018");
		compareClassifiers("A92-1018");
	}
	
	public static void convertDataToArff(String filename){
		ContextDataSet dataset = ContextHTML_Parser.parseHTML(Paths.get(SENTIMENT_CORPUS_DIR + filename + ".html").toFile());
		List<Instance> instances = InstanceHandler.createInstances(dataset);
		InstanceHandler.writeToArffFile(instances, Paths.get(filename + ".arff"));
	}
	

	public static void compareClassifiers(String filename){
		
		System.out.println("Comparing classifiers on [" + filename + "]");
		System.out.println("--------------------------------------------------");
		System.out.println();
		
		ContextDataSet dataset = ContextHTML_Parser.parseHTML(Paths.get(SENTIMENT_CORPUS_DIR + filename + ".html").toFile());
		
//		System.out.println(dataset.citedTitle);
//		System.out.println("main author: " + dataset.citedMainAuthor);
		
		String citedAbstract = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements. Only a lexicon and some unlabeled training text are required. Accuracy exceeds 96%. We describe implementation strategies and optimizations which result in high-speed operation. Three applications for tagging are described: phrase recognition; word sense disambiguation; and grammatical function assignment.";
		
		List<Citer> citers = dataset.citers;
		
		ClassificationResult res;
		
		res = WekaClassifier.trainOnArffFile(Paths.get(filename + ".arff"));
		print("WEKA", res);
		
		res = new MRF(4).runMany(citers, dataset.citedMainAuthor, citedAbstract, dataset);
		print("MRF - Normal", res);
		ConceptGraph conceptGraph = ConceptGraph.fromFiles("links.ser", "phraseToIndex.ser");
		conceptGraph.setSimilarityMultiplier(0.01);
		res = new MRF_WithConcepts(4, conceptGraph).runMany(citers, dataset.citedMainAuthor, citedAbstract, dataset);
		print("MRF - Concepts", res);
	}
	
	public static void print(String title, ClassificationResult result){
		NumberFormat f = new DecimalFormat("#.000"); 
		System.out.println();
		System.out.println(title);
		System.out.println("-------------------------");
		System.out.println("precision: " + f.format(result.precision()));
		System.out.println("recall: " + f.format(result.recall()));
		System.out.println("F: " + f.format(result.fMeasure()));
		System.out.println();
	}
	
	public static List<Instance> createInstancesFromFiles(File[] files){
		List<ContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> ContextHTML_Parser.parseHTML(f))
				.collect(Collectors.toList());
		ArrayList<Instance> instances = datasets.stream()
				.flatMap(dataset -> InstanceHandler.createInstances(dataset).stream())
				.collect(Collectors.toCollection(ArrayList::new));
				
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE).limit(10000),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toCollection(ArrayList::new));
		
		return instances;
	}
	
	public static void conceptSimilarity(){
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
