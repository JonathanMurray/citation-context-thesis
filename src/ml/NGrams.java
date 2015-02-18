package ml;

import java.util.Collection;

public class NGrams {
	Collection<String> unigrams;
	Collection<String> bigrams;
	Collection<String> trigrams;
	
	public NGrams(Collection<String> unigrams, Collection<String> bigrams, Collection<String> trigrams){
		this.unigrams = unigrams;
		this.bigrams = bigrams;
		this.trigrams = trigrams;
	}
}
