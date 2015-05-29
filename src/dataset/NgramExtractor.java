package dataset;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import util.Printer;

/**
 * Handles extracting n-grams and skip-grams, as well as tf-idf scores of 
 * these grams. 
 * @author jonathan
 *
 */
public class NgramExtractor {

	private static final Pattern NUM_OR_CHAR = Pattern.compile("\\d+|.");
	
	//Tries to optimize the process by not creating the same n-grams twice for Ngram and Skipgram
	public static Ngrams[] nAndSkipgramsTfIdf(int maxN, List<String> words, NgramIdf ngramIdf, NgramIdf skipgramIdf){
		List<TObjectDoubleHashMap<String>> ngrams = allNgrams(maxN, words);
		final int minSkip = 1;
		final int maxSkip = 2;
		List<TObjectDoubleHashMap<String>> skipgrams = allSkipgrams(maxN, minSkip, maxSkip, words);
		for(int n = 2; n <= maxN; n++){ //Don't add unigrams!
			addTo(ngrams.get(n-1), skipgrams.get(n-1));
		}
		for(int n = 1; n <= maxN; n++){
			countsToTfIdf(n, ngrams.get(n-1), ngramIdf);
			countsToTfIdf(n, skipgrams.get(n-1), skipgramIdf);
		}
		return new Ngrams[]{new Ngrams(ngrams), new Ngrams(skipgrams)};
	}
	
	public static Ngrams ngramsTfIdf(int maxN , List<String> words, NgramIdf ngramIdf){
		List<TObjectDoubleHashMap<String>> ngrams = allNgrams(maxN, words);
		for(int n = 1; n <= ngrams.size(); n++){
			countsToTfIdf(n, ngrams.get(n-1), ngramIdf);
		}
		return new Ngrams(ngrams);
	}
	
	//Optimized to not extract skip0-ngrams twice
	public static Ngrams[] nAndSkipgrams(int maxN, int maxSkip, List<String> words){
		List<TObjectDoubleHashMap<String>> ngrams = allNgrams(maxN, words);
		List<TObjectDoubleHashMap<String>> skipgrams = allSkipgrams(maxN, 1, maxSkip, words);
		for(int i = 0; i < ngrams.size(); i++){
			addTo(ngrams.get(i), skipgrams.get(i));
		}
		return new Ngrams[]{
				new Ngrams(ngrams),
				new Ngrams(skipgrams)
		};
	}
	
	public static List<String> allNgramPhrases(int minN, int maxN, List<String> words){
		List<TObjectDoubleHashMap<String>> allNgrams = allNgrams(maxN, words);
		List<String> phrases = new ArrayList<String>();
		for(int i = minN - 1; i < maxN; i++){
			phrases.addAll(allNgrams.get(i).keySet());
		}
		return phrases;
	}
	
	public static List<TObjectDoubleHashMap<String>> allNgrams(int maxN, List<String> words){
		return IntStream.range(1, maxN + 1)
					.mapToObj(n -> ngrams(n, words))
					.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public static List<TObjectDoubleHashMap<String>> allSkipgrams(int maxN, int minSkip, int maxSkip, List<String> words){
		return IntStream.range(1, maxN + 1)
				.mapToObj(n -> skipgrams(n, minSkip, maxSkip, words))
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public static TObjectDoubleHashMap<String> ngrams(int n, List<String> words){
		TObjectDoubleHashMap<String> ngramCounts = new TObjectDoubleHashMap<String>();
		final boolean SKIP_STOPWORDS = true;
		for(int word0 = 0; word0 <= words.size() - n; word0++){
			maybeAddNgram(ngramCounts, words.subList(word0, word0 + n), SKIP_STOPWORDS);
		}
		return ngramCounts;
	}
	
	private static TObjectDoubleHashMap<String> skipgrams(int n, int minSkip, int maxSkip, List<String> words){
		TObjectDoubleHashMap<String> counts = new TObjectDoubleHashMap<String>();
		if(n == 1){
			return counts; //actually, no skipgrams are added when n == 1, so this just saves computation time
		}
		for(int skip = minSkip; skip <= maxSkip; skip++){
			addSkipgrams(counts, n, skip, words);
		}
		return counts;
	}
	
	private static void addSkipgrams(TObjectDoubleHashMap<String> counts, int n, int numSkip, List<String> words){
		final boolean SKIP_STOPWORDS = true;
		Printer p = new Printer(false);
		for(int word0 = 0; word0 <= words.size() - n - numSkip; word0++){
			p.println("w0: " + word0);
			for(int skip0 = word0 + 1; skip0 < word0 + n; skip0++){
				p.println(" s0: " + skip0);
				List<String> ngramWords = new ArrayList<String>(n);
				for(int word = word0; word < word0 + n + numSkip; word++){
					p.println("   w: " + word);
					boolean shouldSkip = word >= skip0 && word < skip0 + numSkip;
					if(!shouldSkip){
						ngramWords.add(words.get(word));
					}
				}
				maybeAddNgram(counts, ngramWords, SKIP_STOPWORDS);
			}
		}
	}
	
	private static void maybeAddNgram(TObjectDoubleHashMap<String> ngramCounts, List<String> ngramWords, boolean skipStopwords){
		if(skipStopwords){
			if(ngramWords.stream().anyMatch(w -> TextUtil.instance().isStopword(w))){ //TODO number-tag
				return;
			}
		}
		if(NUM_OR_CHAR != null){
			if(ngramWords.stream().anyMatch(w -> NUM_OR_CHAR.matcher(w).matches())){
				return;
			}
		}
		Stream<String> ngramWordsStream = ngramWords.stream();
		String ngram = ngramWordsStream.reduce((s1,s2) -> s1 + " " + s2).get();
		if(ngram.length() > 0){
			ngramCounts.adjustOrPutValue(ngram, 1, 1);
		}
	}
	
	public static void countsToTfIdf(int n, TObjectDoubleHashMap<String> counts, NgramIdf ngramIdf){
		for(Object k : counts.keys()){
			String ngram = (String) k;
			double tf = 1 + Math.log(counts.get(ngram));
			double idfCount = ngramIdf.idf.getNgram(n, ngram);
			if(idfCount > 0){
				double idf = Math.log(1 + ngramIdf.numDocuments/idfCount);
				counts.put(ngram, tf*idf);
			}else{
				counts.remove(ngram);
			}
		}
	}
	
	private static <K> void addTo(TObjectDoubleHashMap<K> added, TObjectDoubleHashMap<K> receiver){
		TObjectDoubleIterator<K> it = added.iterator();
		while(it.hasNext()){
			it.advance();
			receiver.adjustOrPutValue(it.key(), it.value(), it.value());
		}
	}
	
	public static void main(String[] args) {
		TObjectDoubleHashMap<String> counts = skipgrams(1, 0, 2, Arrays.asList(new String[]{"Peter", "lies" ,"above", "his", "bed", "sleeping"}));
		System.out.println(counts);
	}
}
