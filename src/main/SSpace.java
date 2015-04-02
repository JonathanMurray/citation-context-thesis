package main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

import util.Environment;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.index.DefaultPermutationFunction;
import edu.ucla.sspace.ri.RandomIndexing;
import edu.ucla.sspace.vector.Vector;

public class SSpace {
	
	static String sspaceDir = Environment.resources() + "/sspace";
	
	public static void main(String[] args) throws IOException {
		File txtDir = new File(Environment.resources() + "/corpus/lemmas");
		File sspaceFile = new File(sspaceDir + "/small-space.sspace");
//		createSpace(txtDir, sspaceFile);
		testSpace(sspaceFile);
	}
	
	private static void createSpace(File txtDir, File saveFile) throws FileNotFoundException, IOException{
		SemanticSpace sspace = new RandomIndexing(200, 2, true, new DefaultPermutationFunction(), true, 0, System.getProperties());
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
		System.out.println(sspace.getWords().stream().limit(500).collect(Collectors.toList()));
		System.out.println(sspace.getWords().size());
		
		String[] words = new String[]{
			"see", "show", "semantics", "be"
		};
		for(String w1 : words){
			for(String w2 : words){
				Vector v1 = sspace.getVector(w1);
				Vector v2 = sspace.getVector(w2);
				if(v1 != null && v2 != null){
					System.out.println(w1 + " ~ " + w2 + " --> " + Similarity.cosineSimilarity(v1, v2));
				}
				
			}
		}
	}
}
