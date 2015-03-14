package citationContextData;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import util.Texts;

public class TextWithNgrams extends Text{
	
	public TObjectIntHashMap<String> unigrams;
	public TObjectIntHashMap<String> bigrams;

	public TextWithNgrams(String raw, String lemmatized, List<String> lemmatizedWords, TObjectIntHashMap<String> unigrams, TObjectIntHashMap<String> bigrams) {
		super(raw, lemmatized, lemmatizedWords);
		this.unigrams = unigrams;
		this.bigrams = bigrams;
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", "text-with-ngrams");
		text.appendChild(map(unigrams, "unigrams", "unigram"));
		text.appendChild(map(bigrams, "bigrams", "bigram"));
		return text;
	}
	
	protected Element map(TObjectIntHashMap<String> map, String mapName, String entryName){
		Element mapTag = new Element(Tag.valueOf(mapName), "");
		TObjectIntIterator<String> it = map.iterator();
		while(it.hasNext()){
			it.advance();
			Element entryTag = mapTag.appendElement(entryName);
			entryTag.text(it.key());
			entryTag.attr("count", ""+it.value());
		}
		return mapTag;
	}
	
	protected static TextWithNgrams fromXml(Element textTag){
		String raw = textTag.select("raw").text();
		String lemmatized = textTag.select("lemmatized").text();
		List<String> lemmatizedWords = Texts.split(lemmatized);
		
		return new TextWithNgrams(raw, lemmatized, lemmatizedWords, 
				map(textTag.select("unigrams").first(), "unigram"),
				map(textTag.select("bigrams").first(), "bigram"));
	}
	
	protected static TObjectIntHashMap<String> map(Element mapTag, String entryName){
		TObjectIntHashMap<String> mapObj = new TObjectIntHashMap<String>();
		for(Element entryTag : mapTag.select(entryName)){
			int count = Integer.parseInt(entryTag.attr("count"));
			String text = entryTag.text();
			mapObj.put(text, count);
		}
		return mapObj;
	}
}
