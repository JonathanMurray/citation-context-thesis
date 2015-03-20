package dataset;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import util.CosineSimilarity;

public class Ngrams {
	
	private static final String[] TAG_MAP = new String[]{
		"unigrams", "bigrams", "trigrams"
	};
	private static final String[] TAG_ENTRY = new String[]{
		"unigram", "bigram", "trigram"
	};
	private static final int MIN_COUNT = 4;
	
	private List<TObjectDoubleHashMap<String>> maps;
	
	public static Ngrams empty(int maxN){
		List<TObjectDoubleHashMap<String>> maps = new ArrayList<TObjectDoubleHashMap<String>>();
		for(int i = 0; i < maxN; i ++){
			maps.add(new TObjectDoubleHashMap<String>());
		}
		return new Ngrams(maps);
	}
	
	public Ngrams(List<TObjectDoubleHashMap<String>> maps){
		this.maps = maps;
	}
	
	public double getNgram(int n, String ngram){
		if(maps.size() < n){
			throw new IllegalArgumentException("Don't have " + n + "-grams. Only up to " + maps.size());
		}
		TObjectDoubleHashMap<String> map = maps.get(n - 1);
		if(map.containsKey(ngram) && map.get(ngram) >= MIN_COUNT){
			return map.get(ngram);
		}
		return 0;
	}
	
	public int getMaxN(){
		return maps.size();
	}
	
	public double similarity(Ngrams other){
		if(getMaxN() != other.getMaxN()){
			throw new IllegalArgumentException("this: " + getMaxN() + "-grams. Other: " + other.getMaxN() + "-grams.");
		}
		double sum = 0; 
		for(int i = 0; i < maps.size(); i++){
			TObjectDoubleHashMap<String> mine = maps.get(i);
			TObjectDoubleHashMap<String> others = other.maps.get(i);
			
			sum += CosineSimilarity.calculateCosineSimilarity(mine, others);
		}
		return sum / (double)maps.size();
		//I suppose similarity might in general be higher when sticking to low n-grams. 
		//Shouldn't be a problem though since similarities should be normalized against each other
	}
	
	public void add(Ngrams other, boolean onlyIncrementOne){
		if(getMaxN() != other.getMaxN()){
			throw new IllegalArgumentException(getMaxN() + " != " + other.getMaxN());
		}
		for(int i = 0; i < getMaxN(); i++){
			TObjectDoubleIterator<String> it = other.maps.get(i).iterator();
			while(it.hasNext()){
				it.advance();
				double increment = onlyIncrementOne ? 1 : it.value();
				maps.get(i).adjustOrPutValue(it.key(), increment, increment);
			}
		}
	}
	
	public Element toXml(String tagName){
		Element ngramsTag = new Element(Tag.valueOf(tagName), "");
		for(int i = 0; i < maps.size(); i++){
			TObjectDoubleHashMap<String> map = maps.get(i);
			ngramsTag.appendChild(mapToElement(map, TAG_MAP[i], TAG_ENTRY[i]));
		}
		return ngramsTag;
	}
	
	private Element mapToElement(TObjectDoubleHashMap<String> map, String mapName, String entryName){
		Element mapTag = new Element(Tag.valueOf(mapName), "");
		TObjectDoubleIterator<String> it = map.iterator();
		while(it.hasNext()){
			it.advance();
			if(it.value() >= MIN_COUNT){
				Element entryTag = mapTag.appendElement(entryName);
				entryTag.text(it.key());
				entryTag.attr("count", ""+it.value());
			}
		}
		return mapTag;
	}
	
	public static Ngrams fromXml(Element ngramsTag){
		List<TObjectDoubleHashMap<String>> maps = new ArrayList<TObjectDoubleHashMap<String>>();
		for(int i = 0; i < 3; i++){
			if(ngramsTag.select(TAG_MAP[i]).size() > 0){
				maps.add(map(ngramsTag.select(TAG_MAP[i]).first(), TAG_ENTRY[i]));
			}
		}
		return new Ngrams(maps);
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
}
