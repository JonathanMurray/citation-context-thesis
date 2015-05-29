package dataset;

import java.util.List;

import org.jsoup.nodes.Element;

import edu.ucla.sspace.common.Similarity;

/**
 * Represents a piece of text with a corresponding vector from S-Space
 * that can be used for semantic comparison
 * @author jonathan
 *
 */
public class TextWithSspace extends TextWithNgrams{
	
//	private SSpaceWrapper sspace;
	public double[] vector;
	
	public TextWithSspace(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf, SSpaceWrapper sspace) {
		super(raw, rawWords, lemmas, ngramsTfIdf);

		//		this.sspace = sspace;
		this.vector = sspace.getVectorForDocument(lemmas);
//		this.vector = sspace.getVectorForDocumentLSASpecial(lemmas);
	}
	
	public double vectorSim(TextWithSspace other){
//		double sim = Similarity.euclideanDistance(vector, other.vector);
//		double sim = Similarity.linSimilarity(vector, other.vector);
		double sim = Similarity.cosineSimilarity(vector, other.vector);
		if(Double.isNaN(sim)){
//			System.out.println("this: " + Arrays.toString(vector));
//			System.out.println("\nother: " + Arrays.toString(other.vector));
//			throw new RuntimeException("sim is NaN");
//			System.out.println("sim is NaN");
			sim = 0;
		}
		return sim;
	}

	public double similarity(Object o){
		TextWithSspace other = (TextWithSspace)o;
		return vectorSim(other);
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		return text;
	}
	
	public static TextWithSspace fromXml(Element textTag, SSpaceWrapper sspace){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		return new TextWithSspace(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf, sspace);
	}
}
