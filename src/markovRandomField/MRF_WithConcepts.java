package markovRandomField;

import conceptGraph.WikiGraph;

public class MRF_WithConcepts extends MRF{

	WikiGraph conceptGraph;
	
	public MRF_WithConcepts(int neighbourhod, WikiGraph conceptGraph) {
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
