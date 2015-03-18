package citationContextData;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import util.CosineSimilarity;

public class TextWithNgrams extends Text{
	
	protected static final String XML_TEXT_CLASS = "text-with-ngrams";
	
	public TObjectDoubleHashMap<String> unigramsTfIdf;
	public TObjectDoubleHashMap<String> bigramsTfIdf;

	public TextWithNgrams(String raw, List<String> rawWords, List<String> lemmatizedWords, TObjectDoubleHashMap<String> unigramsTfIdf, TObjectDoubleHashMap<String> bigramsTfIdf) {
		super(raw, rawWords, lemmatizedWords);
		this.unigramsTfIdf = unigramsTfIdf;
		this.bigramsTfIdf = bigramsTfIdf;
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		text.appendChild(map(unigramsTfIdf, "unigrams", "unigram"));
		text.appendChild(map(bigramsTfIdf, "bigrams", "bigram"));
		return text;
	}
	
	protected Element map(TObjectDoubleHashMap<String> map, String mapName, String entryName){
		Element mapTag = new Element(Tag.valueOf(mapName), "");
		TObjectDoubleIterator<String> it = map.iterator();
		while(it.hasNext()){
			it.advance();
			Element entryTag = mapTag.appendElement(entryName);
			entryTag.text(it.key());
			entryTag.attr("count", ""+it.value());
		}
		return mapTag;
	}
	
	protected static TextWithNgrams fromXml(Element textTag){
		Text text = Text.fromXml(textTag);
		return new TextWithNgrams(text.raw, text.rawWords, text.lemmas, 
				map(textTag.select("unigrams").first(), "unigram"),
				map(textTag.select("bigrams").first(), "bigram"));
	}
	
	protected static TObjectDoubleHashMap<String> map(Element mapTag, String entryName){
		TObjectDoubleHashMap<String> mapObj = new TObjectDoubleHashMap<String>();
		for(Element entryTag : mapTag.select(entryName)){
			double count = Double.parseDouble(entryTag.attr("count"));
			String text = entryTag.text();
			mapObj.put(text, count);
		}
		return mapObj;
	}
	
	@Override
	public double similarity(Object o){
		TextWithNgrams other = (TextWithNgrams)o;
		double unigramSim = 0;
		if(unigramsTfIdf.size() > 0 && other.unigramsTfIdf.size() > 0){
			unigramSim = CosineSimilarity.calculateCosineSimilarity(unigramsTfIdf, other.unigramsTfIdf);
		}
		double bigramSim = 0;
		if(bigramsTfIdf.size() > 0 && other.bigramsTfIdf.size() > 0){
			bigramSim = CosineSimilarity.calculateCosineSimilarity(bigramsTfIdf, other.bigramsTfIdf);
		}
		return (unigramSim + bigramSim)/2; //Originally only cosSim
	}
}
