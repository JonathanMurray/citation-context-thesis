package ml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import util.IncrementableMap;

public class NGrams {
	Collection<String> unigrams;
	Collection<String> bigrams;
	Collection<String> trigrams;
	
	public static final String NUMBER = "NUMBER";
	
	public NGrams(Collection<String> unigrams, Collection<String> bigrams, Collection<String> trigrams){
		this.unigrams = unigrams;
		this.bigrams = bigrams;
		this.trigrams = trigrams;
	}
	
	public static String cleanString(String before){
		String after = before.replaceAll("[,:;%\\.\\(\\)]", "");
		after = after.trim();
		after = after.toLowerCase();
		after = after.replaceAll("\\d+", NUMBER);
		after = stem(after);
		return after;
	}
	
	public static String stem(String before){
		//TODO Extremely naive stemming
		return before.replaceAll("s ", " ");
	}
	
	public static NGrams fromDatasets(List<ContextDataSet> datasets){
		IncrementableMap<String> implicitUnigrams = countAndMergeTopOccurences(10, 
				datasets.stream().map(dataset -> dataset.getUnigramsInImplicitReferences()));
		implicitUnigrams.removeIfValue(count -> count < 3);
		System.out.println(implicitUnigrams);
		IncrementableMap<String> nonReferenceUnigrams = countAndMergeTopOccurences(10, 
				datasets.stream().map(dataset -> dataset.getUnigramsInNonReferences()));
		nonReferenceUnigrams.removeIfValue(count -> count < 5);
		System.out.println(nonReferenceUnigrams);
		Set<String> unigrams = mergeKeys(implicitUnigrams, nonReferenceUnigrams);
		System.out.println(unigrams);
		
		IncrementableMap<String> implicitBigrams = countAndMergeTopOccurences(10, 
				datasets.stream().map(dataset -> dataset.getBigramsInImplicitReferences()));
		implicitBigrams.removeIfValue(count -> count < 2);
		System.out.println(implicitBigrams);
		IncrementableMap<String> nonReferenceBigrams = countAndMergeTopOccurences(10, 
				datasets.stream().map(dataset -> dataset.getBigramsInNonReferences()));
		nonReferenceBigrams.removeIfValue(count -> count < 4);
		System.out.println(nonReferenceBigrams);
		
		Set<String> bigrams = mergeKeys(implicitBigrams, nonReferenceBigrams);
		
//		IncrementableMap<String> implicitTrigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getTrigramsInImplicitReferences()));
//		
//		IncrementableMap<String> nonReferenceTrigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getTrigramsInNonReferences()));
//		
//		Set<String> trigrams = mergeKeys(implicitTrigrams, nonReferenceTrigrams);
		
		
		
		return new NGrams(unigrams, bigrams, new ArrayList<String>());
	}
	
	private static <T> IncrementableMap<T> countAndMergeTopOccurences(int topN, Stream<IncrementableMap<T>> maps){
		IncrementableMap<T> merged = new IncrementableMap<T>();
		maps.forEach(map -> {
			for(Entry<T,Integer> e : map.getTopN(topN)){
				merged.increment(e.getKey(), 1); //note value
			}
		});
		return merged;
	}
	
	private static <T> Set<T> mergeKeys(IncrementableMap<T> map1, IncrementableMap<T> map2){
		Set<T> set = new HashSet<T>();
		map1.forEach((k,v) -> set.add(k));
		map2.forEach((k,v) -> set.add(k));
		return set;
	}
}
