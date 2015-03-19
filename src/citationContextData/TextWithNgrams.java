package citationContextData;

import java.util.List;

import org.jsoup.nodes.Element;

public class TextWithNgrams extends Text{
	
	protected static final String XML_TEXT_CLASS = "text-with-ngrams";
	private static final String TAG_NGRAMS_TF_IDF = "ngrams-tf-idf";
	
	public Ngrams ngramsTfIdf;

	public TextWithNgrams(String raw, List<String> rawWords, List<String> lemmas, 
			Ngrams ngramsTfIdf) {
		super(raw, rawWords, lemmas);
		this.ngramsTfIdf = ngramsTfIdf;
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		text.appendChild(ngramsTfIdf.toXml(TAG_NGRAMS_TF_IDF));
		return text;
	}
	
	protected static TextWithNgrams fromXml(Element textTag){
		Text text = Text.fromXml(textTag);
		Ngrams ngramsTfIdf = Ngrams.fromXml(textTag.select(TAG_NGRAMS_TF_IDF).first());
		return new TextWithNgrams(text.raw, text.rawWords, text.lemmas, ngramsTfIdf);
	}
	
	@Override
	public double similarity(Object o){
		TextWithNgrams other = (TextWithNgrams)o;
		return ngramsTfIdf.similarity(other.ngramsTfIdf);
	}
}
