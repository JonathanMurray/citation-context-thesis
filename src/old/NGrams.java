package old;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.IncrementableMap;

public class NGrams {
//	Collection<String> unigrams;
//	Collection<String> bigrams;
//	Collection<String> trigrams;
//	
//	public static final String NUMBER = "NUMBER";
//	
//	public NGrams(Collection<String> unigrams, Collection<String> bigrams, Collection<String> trigrams){
//		this.unigrams = unigrams;
//		this.bigrams = bigrams;
//		this.trigrams = trigrams;
//	}
//	
//	public static String cleanString(String before){
//		String after = before.replaceAll("[',:;%\\.\\(\\)]", "");
//		after = after.trim();
//		after = after.toLowerCase();
//		after = after.replaceAll("\\d+", "<NUMBER>");
////		after = stem(after);
//		return after;
//	}
//	
//	public static String stem(String before){
//		//TODO Extremely naive stemming
//		return before.replaceAll("s ", " ");
//	}
//	
//	public static NGrams fromDatasets(List<ContextDataSet> datasets){
//		
//		
////		IncrementableMap<String> newUni = IncrementableMap.merge(datasets.stream().map(dataset -> dataset.newUniNon()));
////		System.out.println(".");
////		IncrementableMap<String> newImpl = IncrementableMap.merge(datasets.stream().map(dataset -> dataset.newUniImpl()));
////		System.out.println("-");
////		Set<String> unigrams = pickMostDiffering(newUni, newImpl, 20);
////		System.out.println(unigrams);
////		
////		
//		
//		
////		
////		
////		IncrementableMap<String> implicitUnigrams = countAndMergeTopOccurences(10, 
////				datasets.stream().map(dataset -> dataset.getUnigramsInImplicitReferences()));
////		implicitUnigrams.removeIfValue(count -> count < 3);
////		System.out.println(implicitUnigrams);
////		IncrementableMap<String> nonReferenceUnigrams = countAndMergeTopOccurences(10, 
////				datasets.stream().map(dataset -> dataset.getUnigramsInNonReferences()));
////		nonReferenceUnigrams.removeIfValue(count -> count < 5);
////		System.out.println(nonReferenceUnigrams);
////		Set<String> unigrams = mergeKeys(implicitUnigrams, nonReferenceUnigrams);
////		System.out.println(unigrams);
////		
////		
////		System.out.println("\n\n\n");
//		
//		
//		
//		
//		IncrementableMap<String> implicitBigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getBigramsInImplicitReferences()));
//		implicitBigrams.removeIfValue(count -> count < 2);
//		System.out.println(implicitBigrams);
//		IncrementableMap<String> nonReferenceBigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getBigramsInNonReferences()));
//		nonReferenceBigrams.removeIfValue(count -> count < 4);
//		System.out.println(nonReferenceBigrams);
//		Set<String> bigrams = mergeKeys(implicitBigrams, nonReferenceBigrams);
//		
//		IncrementableMap<String> implicitTrigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getTrigramsInImplicitReferences()));
//		implicitTrigrams.removeIfValue(count -> count < 2);
//		System.out.println(implicitTrigrams);
//		IncrementableMap<String> nonReferenceTrigrams = countAndMergeTopOccurences(10, 
//				datasets.stream().map(dataset -> dataset.getTrigramsInNonReferences()));
//		nonReferenceTrigrams.removeIfValue(count -> count < 4);
//		System.out.println(nonReferenceTrigrams);
//		Set<String> trigrams = mergeKeys(implicitTrigrams, nonReferenceTrigrams);
//		
//		return new NGrams(unigrams, bigrams, trigrams);
//	}
//	
//	private static <T> IncrementableMap<T> countAndMergeTopOccurences(int topN, Stream<IncrementableMap<T>> maps){
//		IncrementableMap<T> merged = new IncrementableMap<T>();
//		maps.forEach(map -> {
//			for(Entry<T,Integer> e : map.getTopN(topN)){
//				merged.increment(e.getKey(), 1); //note value
//			}
//		});
//		return merged;
//	}
//	
//	private static <T> Set<T> mergeKeys(IncrementableMap<T> map1, IncrementableMap<T> map2){
//		Set<T> set = new HashSet<T>();
//		map1.forEach((k,v) -> set.add(k));
//		map2.forEach((k,v) -> set.add(k));
//		return set;
//	}
//	
//	private static <T> Set<T> pickMostDiffering(IncrementableMap<T> map1, IncrementableMap<T> map2, int maxPicks){
//		HashMap<T, Float> diffs = new HashMap<T, Float>();
//		map1.entrySet().stream()
//			.forEach(e -> {
//				diffs.put(e.getKey(), differing(e, map2));
//			});
//		map2.entrySet().stream()
//			.forEach(e -> {
//				diffs.put(e.getKey(), differing(e, map1));
//			});
//		
//		
//		diffs.entrySet().stream()
//				.sorted((e1,e2) -> (int)Math.signum(e2.getValue().floatValue() - e1.getValue().floatValue()))
//				.limit(maxPicks)
//				.forEach(System.out::println);
//		
//		
//		return diffs.entrySet().stream()
//			.sorted((e1,e2) -> (int)Math.signum(e2.getValue().floatValue() - e1.getValue().floatValue()))
//			.limit(maxPicks)
//			.map(e -> e.getKey())
//			.collect(Collectors.toCollection(HashSet::new));
//	}
//	
//	private static <T> Float differing(Entry<T, Integer> e, IncrementableMap<T> otherMap){
//		int val = e.getValue();
//		if(!otherMap.containsKey(e.getKey())){
//			return val*1.5f;
//		}
//		int otherVal = otherMap.get(e.getKey());
//		
//		if(val > otherVal){
//			return val / (float)otherVal;
//		}
//		return otherVal / (float)val;
//	}
}
