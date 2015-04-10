package dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import util.Environment;
import util.Printer;
import util.Timer;
import edu.stanford.nlp.ling.tokensregex.SequencePattern.SequenceStartPatternExpr;
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
import gnu.trove.map.hash.TObjectIntHashMap;

public class SSpaceWrapper {

	private SemanticSpace sspace;
	// private NgramIdf ngramIdf;
	private TObjectIntHashMap<String> wordFrequencies;
	private double[] meanVector;
	
	private static SSpaceWrapper instance;
	
	public static SSpaceWrapper instance(){
		if(instance == null){
			String sspaceDir = Environment.resources() + "/sspace";
			File sspaceFile = new File(sspaceDir + "/small-space.sspace");
			File wordFrequenciesFile = new File(sspaceDir + "/wordfrequencies.ser");
			instance = SSpaceWrapper.load(sspaceFile, wordFrequenciesFile);
		}
		return instance;
	}

	public SSpaceWrapper(SemanticSpace sspace, TObjectIntHashMap<String> wordFrequencies) {
		this.sspace = sspace;
		// this.ngramIdf = ngramIdf;
		this.wordFrequencies = wordFrequencies;
//		System.out.println(wordFre);
		computeMeanVector();
	}

	public static SSpaceWrapper fromTextFiles(File txtDir, int vecLen, int windowSize) {
		try {
			// int vecLen = 500;
			// int windowSize = 6;
			boolean permutations = false;
			Printer.printBigHeader("Creating S-Space {veclen: " + vecLen + ", windowsize: " + windowSize + ", permutations: "
					+ permutations);
			SemanticSpace sspace = new RandomIndexing(vecLen, windowSize, permutations, new DefaultPermutationFunction(), true,
					0, System.getProperties());
			File[] files = txtDir.listFiles();
			TObjectIntHashMap<String> wordFrequencies = new TObjectIntHashMap<String>();
			for (int i = 0; i < files.length; i++) {
				File textFile = files[i];
				if (i % 100 == 0) {
					System.out.print(i + "   ");
				}
				sspace.processDocument(new BufferedReader(new FileReader(textFile)));
				Scanner scanner = new Scanner(new BufferedReader(new FileReader(textFile)));
				while (scanner.hasNext()) {
					String lemma = scanner.next();
					wordFrequencies.adjustOrPutValue(lemma, 1, 1);
				}
			}
			System.out.println();
			sspace.processSpace(System.getProperties());

			System.out.println("name: " + sspace.getSpaceName());
			System.out.println("vector length: " + sspace.getVectorLength());
			System.out.println("num unique words: " + wordFrequencies.size());
			int numTokens = 0;
			for (int freq : wordFrequencies.values()) {
				numTokens += freq;
			}
			System.out.println("num tokens: " + numTokens);
			return new SSpaceWrapper(sspace, wordFrequencies);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static NgramIdf loadNgramIdf() {
		return NgramIdf.fromXmlFile(new File(Environment.resources() + "/xml-datasets/ngram-frequencies.xml"),
				NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
	}

	public void save(File sspaceFile, File wordFrequencyFile) {
		try {
			SemanticSpaceIO.save(sspace, sspaceFile, SSpaceFormat.BINARY);
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(wordFrequencyFile));
			wordFrequencies.writeExternal(out);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static SSpaceWrapper load(File sspaceFile, File wordFrequenciesFile) {
		try {
			SemanticSpace sspace = SemanticSpaceIO.load(sspaceFile);
			// TObjectIntHashMap<String> wordFrequencies =
			// (TObjectIntHashMap<String>) new ObjectInputStream(new
			// FileInputStream(wordFrequenciesFile)).readObject();
			TObjectIntHashMap<String> wordFrequencies = new TObjectIntHashMap<String>();
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(wordFrequenciesFile));
			wordFrequencies.readExternal(in);
			return new SSpaceWrapper(sspace, wordFrequencies);
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public int getVectorLength() {
		return sspace.getVectorLength();
	}

	/**
	 * Normalized with mean subtracted
	 * @param word
	 * @return
	 */
	public double[] getVector(String word) {
		Vector v = sspace.getVector(word);
		if(v == null){
			return null;
		}
		double[] vector = new double[v.length()];
		for (int i = 0; i < sspace.getVectorLength(); i++) {
			double termVectorVal = v.getValue(i).doubleValue() / v.magnitude();
			double termMinusMean = termVectorVal - meanVector[i];
			vector[i] = termMinusMean;
		}
		return vector;
	}

	private void computeMeanVector() {
		System.out.print("Computing mean vector ... ");
		meanVector = new double[sspace.getVectorLength()];
		double numVectors = 0;
		for (String w : sspace.getWords()) {
			Vector termVector = sspace.getVector(w);
			if (termVector != null && wordFrequencies.contains(w)) {
				int wordFrequency = wordFrequencies.get(w);
				if(wordFrequency == 0){
					System.out.println();
					System.out.println("----------------word freq: " + wordFrequency);
					System.out.println();
				}
				for (int i = 0; i < sspace.getVectorLength(); i++) {
					double vecValue = termVector.getValue(i).doubleValue() / termVector.magnitude();
					if(!Double.isFinite(vecValue)){
						vecValue = 0;
					}
					meanVector[i] += vecValue * wordFrequency;
				}
				numVectors+=wordFrequency;
			}
		}
		double invNumVectors = 1.0 / numVectors;
		System.out.println("numV: " + numVectors);
		for (int i = 0; i < sspace.getVectorLength(); i++) {
			meanVector[i] *= invNumVectors;
			System.out.println(meanVector[i]); //TODO
		}

		System.out.println("mean vec magn: " + magnitude(meanVector));
		System.out.println("[x]  (" + numVectors + " vectors)");
	}

	public double[] getVectorForDocument(List<String> words) {
		double[] vector = new double[sspace.getVectorLength()];
		double numVecsAdded = 0;
		for (String word : words) {
			Vector vec = sspace.getVector(word);
			if (vec != null) {
				
				for (int i = 0; i < sspace.getVectorLength(); i++) {
					double termVectorVal = (double) vec.getValue(i) / vec.magnitude();
					double termMinusMean = termVectorVal - meanVector[i];
					vector[i] += termMinusMean;
				}
				numVecsAdded += 1;
			}
		}

		for (int i = 0; i < sspace.getVectorLength(); i++) {
			vector[i] /= numVecsAdded;
		}

		return vector;
	}

	public List<ArrayList<String>> getClusters(int numClusters, int numWordsPerCluster) {
		List<DoubleVector> rowVectors = new ArrayList<DoubleVector>();
		HashMap<Integer, String> wordMap = new HashMap<Integer, String>();
		int i = 0;
		for (String word : sspace.getWords()) {
			if(!wordFrequencies.containsKey(word) || wordFrequencies.get(word) <= 15 || word.length() <= 1){
				continue; //skip weird words
			}
			DoubleVector v = (DoubleVector) sspace.getVector(word);
			DenseVector vector = new DenseVector(v.toArray());
			rowVectors.add(vector);
			// System.out.print(".");
			// int vecId = onlineClustering.addVector(vector);
			wordMap.put(i, word);
			i++;
		}
		System.out.println("Clustering " + wordMap.size() + " / " + sspace.getWords().size() + " words");
		
		DirectClustering directClustering = new DirectClustering();
		Matrix matrix = Matrices.asMatrix(rowVectors);
		System.out.print("Clustering (" + numClusters + " clusters) ... ");
		Timer t = new Timer();
		Assignments assignments = directClustering.cluster(matrix, numClusters, new java.util.Properties());
		System.out.println("[x] " + t.getSecString());
		List<ArrayList<String>> clusters = new ArrayList<ArrayList<String>>();
		for (Set<Integer> cluster : assignments.clusters()) {
			ArrayList<String> wordsInCluster = cluster.stream().limit(numWordsPerCluster).map(index -> wordMap.get(index))
					.collect(Collectors.toCollection(ArrayList::new));
			clusters.add(wordsInCluster);
		}
		return clusters;
	}

	public static double magnitude(double[] v) {
		double magnitude = 0;
		for (int i = 0; i < v.length; i++) {
			magnitude += v[i] * v[i];
		}
		magnitude = Math.sqrt(magnitude);
		return magnitude;
	}

}
