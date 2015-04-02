package dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.nodes.Element;

import util.Environment;
import weka.core.matrix.DoubleVector;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.Vector;

public class TextWithRI extends TextWithNgrams{
	
	static SemanticSpace sspace;
	static double[] meanVector;
	
	private double[] vector;
	

	public TextWithRI(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		ensureSspaceLoaded();
		setupVector();
	}
	
	private void setupVector(){
		vector = new double[sspace.getVectorLength()];
		for(String lemma : lemmas){
//			if(!Texts.instance().isStopword(lemma)){
				Vector vec = sspace.getVector(lemma);
				if(vec != null){
					for(int i = 0; i < sspace.getVectorLength(); i++){
						vector[i] += (double)vec.getValue(i) / vec.magnitude();
					}
				}
//			}
		}
		
		//subtract mean
		for(int i = 0; i<sspace.getVectorLength(); i++){
			vector[i] -= meanVector[i];
		}
	}

	public double similarity(Object o){
		TextWithRI other = (TextWithRI)o;
		double ngramSim = ngramsTfIdf.similarity(other.ngramsTfIdf);
		
		double semSim = Similarity.cosineSimilarity(vector, other.vector);
		
		
//		int n = 0;
//		double semSim = 0;
//		for(String word : lemmas){
//			if(Texts.instance().isStopword(word)){
//				continue;
//			}
////			System.out.println(word + ": " + tfidf); //TODO
//			for(String otherWord : other.lemmas){
//				if(Texts.instance().isStopword(otherWord)){
//					continue;
//				}
//				Vector v1 = sspace.getVector(word);
//				Vector v2 = sspace.getVector(otherWord);
//				if(v1 != null && v2 != null){
//					double tfidf1 = ngramsTfIdf.getNgram(1, word);
//					double tfidf2 = other.ngramsTfIdf.getNgram(1, otherWord);
//					n++;
//					semSim += tfidf1 * tfidf2 * Math.max(0, Similarity.cosineSimilarity(v1, v2));
//				}
//			}
//		}
//		if(n > 0){
//			semSim /= n;
//		}
//		semSim /= 2; //TODO
//		System.out.println("sspace sim: " + semSim);
//		if(raw.length() < 300 && other.raw.length() < 300){
//			System.out.println(raw);
//			System.out.println("\n" + other.raw);
//			System.out.println(semSim + " ~ " + ngramSim);
//			System.out.println("-----------------------------------------------");	
//		}
		return semSim / 1.5;
//		return (ngramSim + semSim) / 2;
	}
	
	void ensureSspaceLoaded(){
		if(sspace == null){
			File file = new File(Environment.resources() + "/sspace/small-space.sspace");
			try {
				System.out.print("Loading sspace ... ");
				sspace = SemanticSpaceIO.load(file);
				System.out.println("[x]");
				computeMeanVector();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	void computeMeanVector(){
		System.out.print("Computing mean vector ... ");
		meanVector = new double[sspace.getVectorLength()];
		int numVectors = 0;
		for(String w : sspace.getWords()){
			Vector vector = sspace.getVector(w);
			if(vector != null){
				for(int i = 0; i < sspace.getVectorLength(); i++){
					meanVector[i] += (double) vector.getValue(i)/vector.magnitude();
				}
				numVectors ++;
			}
		}
		double invNumVectors = 1/numVectors;
		for(int i = 0; i < sspace.getVectorLength(); i++){
			meanVector[i] *= invNumVectors;
		}
		System.out.println("[x]  (" + numVectors + " vectors)");
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		return text;
	}
	
	public static TextWithRI fromXml(Element textTag){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		return new TextWithRI(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf);
	}
	
	
	
}
