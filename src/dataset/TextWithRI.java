package dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.tika.detect.MagicDetector;
import org.jsoup.nodes.Element;

import util.Environment;
import weka.core.matrix.DoubleVector;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.Vector;

public class TextWithRI extends TextWithNgrams{
	
	
	private final static File sspaceFile = new File(Environment.resources() + "/sspace/small-space.sspace");
	
	static SemanticSpace sspace;
	static double[] meanVector;
	
	public double[] vector;
	

	public TextWithRI(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		ensureSspaceLoaded();
		setupVector();
	}
	
	private void setupVector(){
		vector = new double[sspace.getVectorLength()];
		double numVecsAdded = 0;
		for(String lemma : lemmas){
//			if(!Texts.instance().isStopword(lemma)){
				Vector vec = sspace.getVector(lemma);
				if(vec != null){
					for(int i = 0; i < sspace.getVectorLength(); i++){
						double termVectorVal = (double)vec.getValue(i) / vec.magnitude();
						double termMinusMean = termVectorVal - meanVector[i];
						vector[i] += termMinusMean;
					}
					numVecsAdded ++;
				}
//			}
		}
		
		
		for(int i = 0; i<sspace.getVectorLength(); i++){
			vector[i] /= numVecsAdded;
		}
	}
	
	public double vectorSim(TextWithRI other){
		return Similarity.cosineSimilarity(vector, other.vector);
	}

	public double similarity(Object o){
		TextWithRI other = (TextWithRI)o;
		double ngramSim = ngramsTfIdf.similarity(other.ngramsTfIdf);
		
		return ngramSim; //TODO
//		return (ngramSim + semSim) / 2;
	}
	
	
	
	void ensureSspaceLoaded(){
		if(sspace == null){
			try {
				System.out.print("Loading sspace ... ");
				sspace = SemanticSpaceIO.load(sspaceFile);
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
		double numVectors = 0;
		for(String w : sspace.getWords()){
			Vector termVector = sspace.getVector(w);
			if(termVector != null){
				for(int i = 0; i < sspace.getVectorLength(); i++){
					meanVector[i] += (double) termVector.getValue(i)/termVector.magnitude();
				}
				numVectors ++;
			}
		}
		double invNumVectors = 1.0/numVectors;
		for(int i = 0; i < sspace.getVectorLength(); i++){
			meanVector[i] *= invNumVectors;
		}
		
		System.out.println("mean vec magn: " + magnitude(meanVector));
		System.out.println("[x]  (" + numVectors + " vectors)");
	}
	
	public static double magnitude(double[] v){
		double magnitude = 0;
		for(int i = 0; i < v.length; i++){
			magnitude += Math.pow(v[i], 2);
		}
		magnitude = Math.sqrt(magnitude);
		return magnitude;
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
