package mrf;

import java.util.HashMap;
import java.util.List;

public class MRF_citerNgrams {
	List<HashMap<String,Double>> sentencesUnigrams;
	List<HashMap<String,Double>> sentencesBigrams;
	
	public MRF_citerNgrams(List<HashMap<String,Double>> sentencesUnigrams, List<HashMap<String,Double>> sentencesBigrams){
		this.sentencesUnigrams = sentencesUnigrams;
		this.sentencesBigrams = sentencesBigrams;
	}
}
