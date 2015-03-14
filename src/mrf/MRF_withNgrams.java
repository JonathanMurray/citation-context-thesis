package mrf;

import util.CosineSimilarity;
import citationContextData.TextWithNgrams;

public class MRF_withNgrams extends MRF_classifier<TextWithNgrams>{

	public MRF_withNgrams(MRF_params params) {
		super(params);
	}

	@Override
	protected double similarity(TextWithNgrams s1, TextWithNgrams s2){
		double cosSim = 0;
		if(s1.unigrams.size() > 0 && s2.unigrams.size() > 0){
			cosSim = CosineSimilarity.calculateCosineSimilarity(s1.unigrams, s2.unigrams);
		}
		double bigramSim = 0;
		if(s1.bigrams.size() > 0 && s2.bigrams.size() > 0){
			bigramSim = CosineSimilarity.calculateCosineSimilarity(s1.bigrams, s2.bigrams);
		}
		return cosSim + bigramSim; //Originally only cosSim
	}

}
