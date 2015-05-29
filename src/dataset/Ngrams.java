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
	
	private List<TObjectDoubleHashMap<String>> ngramMaps;
	
	public int size(int n){
		return ngramMaps.get(n-1).size();
	}
	
	public static Ngrams empty(int maxN){
		List<TObjectDoubleHashMap<String>> ngramMaps = new ArrayList<TObjectDoubleHashMap<String>>();
		for(int i = 1; i <= maxN; i ++){
			ngramMaps.add(new TObjectDoubleHashMap<String>());
		}
		return new Ngrams(ngramMaps);
	}
	
	public Ngrams(List<TObjectDoubleHashMap<String>> ngramMaps){
		this.ngramMaps = ngramMaps;
	}
	
	public double getNgram(int n, String ngram){
		TObjectDoubleHashMap<String> map = ngramMaps.get(n - 1);
		if(map.containsKey(ngram)){// && map.get(ngram) >= minCount){
			return map.get(ngram);
		}
		return 0;
	}
	
	
	//TODO this version uses bigrams and trigrams too
	public double similarity(Ngrams other, int minN, int maxN){
		if(ngramMaps.size() != other.ngramMaps.size()){
			throw new IllegalArgumentException("this: " + ngramMaps.size() + "-grams. Other: " + other.ngramMaps.size() + "-grams.");
		}
		double sum = 0; 
		int max = Math.min(maxN, ngramMaps.size());
		for(int i = minN-1; i < max; i++){
			TObjectDoubleHashMap<String> mine = ngramMaps.get(i);
			TObjectDoubleHashMap<String> others = other.ngramMaps.get(i);
			sum += CosineSimilarity.calculateCosineSimilarity(mine, others);
		}
		return sum / ((double)max-minN+1);
		//I suppose similarity might in general be higher when sticking to low n-grams. 
		//Shouldn't be a problem though since similarities should be normalized against each other
	}
	
//	public double similarity(Ngrams other){
//		TObjectDoubleHashMap<String> myUnigrams = ngramMaps.get(0);
//		TObjectDoubleHashMap<String> othersUnigrams = other.ngramMaps.get(0);
//		return CosineSimilarity.calculateCosineSimilarity(myUnigrams, othersUnigrams);
//	}
	
	
	
	
	public void add(Ngrams other, boolean onlyIncrementOne){
		if(ngramMaps.size() != other.ngramMaps.size()){
			throw new IllegalArgumentException(ngramMaps.size() + " != " + other.ngramMaps.size());
		}
		for(int i = 0; i < ngramMaps.size(); i++){
			TObjectDoubleIterator<String> it = other.ngramMaps.get(i).iterator();
			while(it.hasNext()){
				it.advance();
				double increment = onlyIncrementOne ? 1 : it.value();
				ngramMaps.get(i).adjustOrPutValue(it.key(), increment, increment);
			}
		}
	}
	
	public Element toXml(String tagName){
		return toXml(tagName, 0);
	}
	
	public Element toXml(String tagName, int minCount){
		Element ngramsTag = new Element(Tag.valueOf(tagName), "");
		for(int i = 0; i < ngramMaps.size(); i++){
			TObjectDoubleHashMap<String> map = ngramMaps.get(i);
			ngramsTag.appendChild(mapToElement(map, TAG_MAP[i], TAG_ENTRY[i], minCount));
		}
		return ngramsTag;
	}
	
	private Element mapToElement(TObjectDoubleHashMap<String> map, String mapName, String entryName, int minCount){
		Element mapTag = new Element(Tag.valueOf(mapName), "");
		TObjectDoubleIterator<String> it = map.iterator();
		while(it.hasNext()){
			it.advance();
			if(it.value() >= minCount){
				Element entryTag = mapTag.appendElement(entryName);
				entryTag.text(it.key());
				entryTag.attr("count", ""+it.value());
			}
		}
		return mapTag;
	}
	
	public static Ngrams fromXml(Element ngramsTag){
		return fromXml(ngramsTag, 0);
	}
	
	public static Ngrams fromXml(Element ngramsTag, int minCount){
		List<TObjectDoubleHashMap<String>> maps = new ArrayList<TObjectDoubleHashMap<String>>();
		for(int i = 0; i < 3; i++){
			if(ngramsTag.select(TAG_MAP[i]).size() > 0){
				maps.add(map(ngramsTag.select(TAG_MAP[i]).first(), TAG_ENTRY[i], minCount));
			}
		}
		return new Ngrams(maps);
	}
	
	protected static TObjectDoubleHashMap<String> map(Element mapTag, String entryName, int minCount){
		TObjectDoubleHashMap<String> mapObj = new TObjectDoubleHashMap<String>();
		for(Element entryTag : mapTag.select(entryName)){
			double count = Double.parseDouble(entryTag.attr("count"));
			String text = entryTag.text();
			if(count >= minCount){
				mapObj.put(text, count);
			}
		}
		return mapObj;
	}
	
	public String toString(){
		return ngramMaps.toString();
	}
}
