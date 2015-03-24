package dataset;

import java.util.List;

import org.jsoup.nodes.Element;

public class TextWithSkipgrams extends TextWithNgrams{
	
	protected static final String XML_TEXT_CLASS = "text-with-skipgrams";
	private static final String TAG_SKIPGRAMS_TF_IDF = "skipgrams-tf-idf";
	
	public Ngrams skipgramsTfIdf;

	public TextWithSkipgrams(String raw, List<String> rawWords, List<String> lemmas, 
			Ngrams ngramsTfIdf, Ngrams skipgramsTfIdf) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		this.skipgramsTfIdf = skipgramsTfIdf;
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
		double ngramSimiliarity = ngramsTfIdf.similarity(other.ngramsTfIdf);
		double skipgramSimilarity = skipgramsTfIdf.similarity(other.skipgramsTfIdf);
		return (ngramSimiliarity + skipgramSimilarity) / 2;
	}
}
