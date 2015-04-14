package dataset;

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Element;

import edu.ucla.sspace.common.Similarity;

public class TextWithSspace extends TextWithNgrams{
	
//	private SSpaceWrapper sspace;
	public double[] vector;
	
	public TextWithSspace(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf, SSpaceWrapper sspace) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
//		this.sspace = sspace;
		this.vector = sspace.getVectorForDocument(lemmas);
//		System.out.println(Arrays.toString(vector));
//		System.out.println();
//		System.out.println();
		
	}
	
	public double vectorSim(TextWithSspace other){
		double sim = Similarity.cosineSimilarity(vector, other.vector);
		if(Double.isNaN(sim)){
			System.out.println("this: " + Arrays.toString(vector));
			System.out.println("\nother: " + Arrays.toString(other.vector));
			throw new RuntimeException("sim is NaN");
		}
		return sim;
	}

	public double similarity(Object o){
		TextWithSspace other = (TextWithSspace)o;
		double ngramSim = ngramsTfIdf.similarity(other.ngramsTfIdf);
		return ngramSim; //TODO
//		return vectorSim(other); //TODO
//		return (ngramSim + semSim) / 2;
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
