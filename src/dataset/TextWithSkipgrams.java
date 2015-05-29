package dataset;

import java.util.List;

import org.jsoup.nodes.Element;

import util.Printer;

public class TextWithSkipgrams extends TextWithNgrams{
	
	protected static final String XML_TEXT_CLASS = "text-with-skipgrams";
	private static final String TAG_SKIPGRAMS_TF_IDF = "skipgrams-tf-idf";
	
	public Ngrams skipgramsTfIdf;

	public TextWithSkipgrams(String raw, List<String> rawWords, List<String> lemmas, 
			Ngrams ngramsTfIdf, Ngrams skipgramsTfIdf) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		this.skipgramsTfIdf = skipgramsTfIdf;
//		System.out.println(this); //TODO
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		text.appendChild(skipgramsTfIdf.toXml(TAG_SKIPGRAMS_TF_IDF));
		return text;
	}
	
	public static TextWithSkipgrams fromXml(Element textTag){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		Ngrams skipgramsTfIdf = Ngrams.fromXml(textTag.select(TAG_SKIPGRAMS_TF_IDF).first());
		return new TextWithSkipgrams(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf, skipgramsTfIdf);
	}
	
	@Override
	public double similarity(Object o){
		TextWithSkipgrams other = (TextWithSkipgrams)o;
		double ngramSimiliarity = ngramsTfIdf.similarity(other.ngramsTfIdf, 1,1);
		double skipgramSimilarity = skipgramsTfIdf.similarity(other.skipgramsTfIdf, 2,2);
//		System.out.println(Printer.toString(ngramSimiliarity) + "   ;    " + skipgramSimilarity); //TODO
//		return ngramSimiliarity + skipgramSimilarity;
		return ngramsTfIdf.similarity(other.ngramsTfIdf, 1, 1) + ngramsTfIdf.similarity(other.ngramsTfIdf, 2,2);
	}
	
	public String toString(){
		return "skipgrams: " + skipgramsTfIdf;
	}
}
