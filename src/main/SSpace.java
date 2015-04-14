package main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tika.sax.OfflineContentHandler;

import util.Environment;
import util.Printer;
import util.Timer;
import dataset.NgramIdf;
import dataset.SSpaceWrapper;
import dataset.TextFactory;
import dataset.TextParams;
import dataset.TextWithSspace;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.DirectClustering;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;
import edu.ucla.sspace.index.DefaultPermutationFunction;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.ri.RandomIndexing;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SSpace {
	
	static String sspaceDir = Environment.resources() + "/sspace";
	
	public static void main(String[] args) throws IOException {
		File txtDir = new File(Environment.resources() + "/corpus/lemmas-sentences");
		File sspaceFile = new File(sspaceDir + "/space-lsa-1000.sspace");
		File wordFrequenciesFile = new File(sspaceDir + "/wordfrequencies.ser");
		createSpace(txtDir, sspaceFile, wordFrequenciesFile);
//		testSimilarity(sspaceFile, wordFrequenciesFile);
//		testClustering(sspaceFile, wordFrequenciesFile, 20, 30);
//		testWeights(sspaceFile, wordFrequenciesFile);
	}
	
	private static void createSpace(File txtDir, File sspaceFile, File wordFrequenciesFile) throws FileNotFoundException, IOException{
		int vecLen = 1000;
		int windowSize = 5;
		SSpaceWrapper sspace = SSpaceWrapper.fromTextFiles(txtDir, vecLen, windowSize);
		sspace.save(sspaceFile, wordFrequenciesFile);
	}
	
	private static void testClustering(File sspaceFile, File wordFrequenciesFile, int numClusters, int wordsPerCluster) throws IOException{
		SSpaceWrapper sspace = SSpaceWrapper.load(sspaceFile, wordFrequenciesFile);
		for(List<String> cluster : sspace.getClusters(numClusters, wordsPerCluster)){
			System.out.println(cluster);
			System.out.println();
		}
	}
	
	
	
	private static void testWeights(File sspaceFile, File wordFrequenciesFile) throws IOException{
		SSpaceWrapper sspace = SSpaceWrapper.load(sspaceFile, wordFrequenciesFile);
		NgramIdf idf = SSpaceWrapper.loadNgramIdf();
		TextParams<TextWithSspace> params = TextParams.withRI(idf, sspace);
		Scanner sc = new Scanner(System.in);
		while(true){
			System.out.print("Sentence:  ");
			String sa = sc.nextLine();
			System.out.println();
			TextWithSspace a = TextFactory.createText(params, sa);
			for(String lemma : a.lemmas){
				double[] lemmaVector = sspace.getVector(lemma);
				if(lemmaVector != null){
					double lemmaMagnitude = SSpaceWrapper.magnitude(lemmaVector);
					System.out.println("|" + lemma + "| == " + lemmaMagnitude);	
				}else{
					System.out.println("(" + lemma + ")");
				}
			}
			System.out.println("|Sentence| == " + SSpaceWrapper.magnitude(sspace.getVectorForDocument(a.lemmas)));
			System.out.println();
		}
		
	}
	
	private static void testSimilarity(File sspaceFile, File wordFrequenciesFile) throws IOException{
		SSpaceWrapper sspace = SSpaceWrapper.load(sspaceFile, wordFrequenciesFile);
		NgramIdf idf = SSpaceWrapper.loadNgramIdf();
		TextParams<TextWithSspace> params = TextParams.withRI(idf, sspace);
		String[] sentences = new String[]{
				"This paper investigates why the HMMs estimated by Expectation-Maximization (EM) produce such poor results as Part-of-Speech (POS) taggers.",
				"We find that the HMMs estimated by EM generally assign a roughly equal number of word tokens to each hidden state, while the empirical distribution of tokens to POS tags is highly skewed",
				"However, as Banko and Moore (2004) point out, the accuracy achieved by these unsupervised methods depends strongly on the precise nature of the supervised training data (in their case, the ambiguity of the tag lexicon available to the system), which makes it more difficult to understand the behaviour of such systems.",
				"This section describes how we evaluate how well these sequences of hidden states correspond to the gold- standard POS tags for the training corpus (here, the PTB POS tags)",
				"The polar bear (Ursus maritimus) is a carnivorous bear whose native range lies largely within the Arctic Circle, encompassing the Arctic Ocean, its surrounding seas and surrounding land masses",
				"Although it is the sister species of the brown bear,[5] it has evolved to occupy a narrower ecological niche, with many body characteristics adapted for cold temperatures, for moving across snow, ice, and open water, and for hunting the seals which make up most of its diet."
		};
		for(int i = 0; i < sentences.length; i++){
			for(int j = i + 1; j < sentences.length; j++){
				TextWithSspace a = TextFactory.createText(params, sentences[i]);
				TextWithSspace b = TextFactory.createText(params, sentences[j]);
				System.out.println();
				System.out.println(i + ": " + sentences[i]);
				System.out.println(j + ": " + sentences[j]);
				System.out.println("n-gram similarity: " + Printer.toString(a.similarity(b)));
				System.out.println("vector similarity: " + Printer.toString(a.vectorSim(b)));
			}
		}
	}
	
	private static void testSimilarityInteractive(File sspaceFile, File wordFrequenciesFile) throws IOException{
		SSpaceWrapper sspace = SSpaceWrapper.load(sspaceFile, wordFrequenciesFile);
		NgramIdf idf = SSpaceWrapper.loadNgramIdf();
		TextParams<TextWithSspace> params = TextParams.withRI(idf, sspace);
		Scanner sc = new Scanner(System.in);
		while(true){
			System.out.print("Sentence A:  ");
			String sa = sc.nextLine();
			System.out.print("Sentence B:  ");
			String sb = sc.nextLine();
			 
			TextWithSspace a = TextFactory.createText(params, sa);
			TextWithSspace b = TextFactory.createText(params, sb);	
			
//			System.out.println("a: " + Arrays.toString(a.vector));
//			System.out.println("b: " + Arrays.toString(b.vector));
			
			System.out.println("n-gram similarity: " + Printer.toString(a.similarity(b)));
			System.out.println("vector similarity: " + Printer.toString(a.vectorSim(b)));
		}
	}
}
