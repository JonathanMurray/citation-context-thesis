package markovRandomField;

import conceptGraph.ConceptGraph;

public class MRF_WithConcepts extends MRF{

	ConceptGraph conceptGraph;
	
	public MRF_WithConcepts(int neighbourhod, ConceptGraph conceptGraph) {
		super(neighbourhod);
		this.conceptGraph = conceptGraph;
	}
	
	@Override
	protected double similarity(int s1, int s2){
		String[] words1 = sentenceTexts.get(s1).split("\\s+");
		String[] words2 = sentenceTexts.get(s2).split("\\s+");
		return conceptGraph.similarity(words1, words2);
	}
}
