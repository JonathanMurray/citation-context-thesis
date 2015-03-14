package mrf;

import citationContextData.TextWithNgrams;
import conceptGraph.ConceptGraph;

public class MRF_withConcepts extends MRF_classifier<TextWithNgrams>{

	private ConceptGraph conceptGraph;
	private double multiplier;
	
	public MRF_withConcepts(MRF_params params, ConceptGraph conceptGraph, double similarityMultiplier) {
		super(params);
		this.conceptGraph = conceptGraph;
		this.multiplier = similarityMultiplier;
	}
	
	@Override
	protected double similarity(TextWithNgrams t1, TextWithNgrams t2){
		double similarity = conceptGraph.similarity(t1.unigrams.keySet(), t2.unigrams.keySet());
		return multiplier * similarity;
	}
}
