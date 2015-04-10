package dataset;

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Element;

import edu.ucla.sspace.common.Similarity;

public class TextWithRI extends TextWithNgrams{
	
//	private SSpaceWrapper sspace;
	public double[] vector;
	
	public TextWithRI(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf, SSpaceWrapper sspace) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
//		this.sspace = sspace;
		this.vector = sspace.getVectorForDocument(lemmas);
//		System.out.println(Arrays.toString(vector));
//		System.out.println();
//		System.out.println();
		
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
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		return text;
	}
	
	public static TextWithRI fromXml(Element textTag, SSpaceWrapper sspace){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		return new TextWithRI(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf, sspace);
	}
}
