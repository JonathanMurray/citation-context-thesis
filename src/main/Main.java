package main;

import java.io.File;
import java.nio.file.Paths;
import java.util.Scanner;

import util.NonThrowingFileWriter;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.Sentence;
import conceptGraph.ConceptGraph;

public class Main {
	public static void main(String[] args) {
		
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
