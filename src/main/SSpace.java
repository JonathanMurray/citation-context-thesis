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

import util.Environment;
import util.Printer;
import util.Timer;
import dataset.NgramIdf;
import dataset.TextFactory;
import dataset.TextParams;
import dataset.TextWithRI;
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
import gnu.trove.map.hash.TIntObjectHashMap;

public class SSpace {
	
	static String sspaceDir = Environment.resources() + "/sspace";
	
	public static void main(String[] args) throws IOException {
		File txtDir = new File(Environment.resources() + "/corpus/lemmas");
		File sspaceFile = new File(sspaceDir + "/small-space.sspace");
//		createSpace(txtDir, sspaceFile);
		testSpace(sspaceFile);
	}
	
	private static void createSpace(File txtDir, File saveFile) throws FileNotFoundException, IOException{
		int vecLen = 200;
		int windowSize = 3;
		boolean permutations = false;
		Printer.printBigHeader("Creating S-Space {veclen: " + vecLen + ", windowsize: " + windowSize + ", permutations: " + permutations);
		SemanticSpace sspace = new RandomIndexing(vecLen, windowSize, permutations, new DefaultPermutationFunction(), true, 0, System.getProperties());
		File[] files= txtDir.listFiles();
		for(int i = 0; i < files.length; i++){
			File textFile = files[i];
			if(i % 100 == 0){
				System.out.print(i + "   ");
			}
			sspace.processDocument(new BufferedReader(new FileReader(textFile)));
		}
		System.out.println();
		sspace.processSpace(System.getProperties());
		
		System.out.println("name: " + sspace.getSpaceName());
		System.out.println("vector length: " + sspace.getVectorLength());
		
//		SemanticSpaceIO.save(sspace, saveFile);
		SemanticSpaceIO.save(sspace, saveFile, SSpaceFormat.BINARY);
	}
	
	private static void testSpace(File file) throws IOException{
		System.out.print("loading " + file.getName() + " ... ");
		SemanticSpace sspace = SemanticSpaceIO.load(file);
		System.out.println("[x]");
//		System.out.println(sspace.getWords().stream().limit(200).collect(Collectors.toList()));
		System.out.println("Num words: " + sspace.getWords().size());
		
		TIntObjectHashMap<String> wordMap = new TIntObjectHashMap<String>();
		
//		java.util.Properties javaProps = new java.util.Properties();
//		javaProps.setProperty(OnlineClustering.NUM_CLUSTERS_PROPERTY, "10");
//		Properties props = new Properties(javaProps);
//		System.out.println(props.getProperty(OnlineClustering.NUM_CLUSTERS_PROPERTY));
//		StreamingKMeans<DoubleVector> kMeans = new StreamingKMeans<DoubleVector>(props);
//		System.out.print("generating online clustering ... ");
//		OnlineClustering<DoubleVector> onlineClustering = kMeans.generate();
//		System.out.println("[x]");
		
		
		List<DoubleVector> rowVectors =new ArrayList<DoubleVector>();
		
		int i = 0;
		for(String word : sspace.getWords()){
			DoubleVector v = (DoubleVector) sspace.getVector(word);
			DenseVector vector = new DenseVector(v.toArray());
			rowVectors.add(vector);
//			System.out.print(".");
//			int vecId = onlineClustering.addVector(vector);
			wordMap.put(i, word);
			i++;
		}
		
		DirectClustering directClustering = new DirectClustering();
		Matrix matrix = Matrices.asMatrix(rowVectors);
		int numClusters = 100;
		System.out.print("Clustering ... ");
		Timer t = new Timer();
		Assignments assignments = directClustering.cluster(matrix, numClusters, new java.util.Properties());
		System.out.println("[x] " + t.getSecString());
		for(Set<Integer> cluster : assignments.clusters()){
			List<String> wordsInCluster = cluster.stream().limit(30).map(index -> wordMap.get(index)).collect(Collectors.toList());
			System.out.println("Words in cluster: ");
			System.out.println(wordsInCluster);
			System.out.println();
		}
		
		
//		for(Cluster<DoubleVector> cluster : onlineClustering.getClusters()){
//			System.out.println();
//			System.out.println(cluster + ":");
////			System.out.println(cluster.dataPointIds());
//			
//		}
		
		Scanner sc = new Scanner(System.in);
		
		while(true){
			System.out.print("Sentence A:  ");
			String sa = sc.nextLine();
			System.out.print("Sentence B:  ");
			String sb = sc.nextLine();
			NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(Environment.resources() + "/xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT); 
			TextWithRI a = TextFactory.createText(TextParams.withRI(ngramIdf), sa);
			TextWithRI b = TextFactory.createText(TextParams.withRI(ngramIdf), sb);	
			
			System.out.println("a: " + Arrays.toString(a.vector));
			System.out.println("b: " + Arrays.toString(b.vector));
			System.out.println("a magn: " + TextWithRI.magnitude(a.vector));
			System.out.println("b magn: " + TextWithRI.magnitude(b.vector));
			
			System.out.println("n-gram similarity: " + Printer.toString(a.similarity(b)));
			System.out.println("vector similarity: " + Printer.toString(a.vectorSim(b)));
		}
		
		
		
//		String[] words = new String[]{
//			"see", "show", "semantics", "be"
//		};
//		for(String w1 : words){
//			for(String w2 : words){
//				Vector v1 = sspace.getVector(w1);
//				Vector v2 = sspace.getVector(w2);
//				if(v1 != null && v2 != null){
//					System.out.println(w1 + " ~ " + w2 + " --> " + Similarity.cosineSimilarity(v1, v2));
//				}
//				
//			}
//		}
	}
}
