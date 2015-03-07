package markovRandomField;

import conceptGraph.ConceptGraph;

public class MRF_withConcepts extends MRF{

	ConceptGraph conceptGraph;
	
	public MRF_withConcepts(int neighbourhod, ConceptGraph conceptGraph) {
		super(neighbourhod);
		this.conceptGraph = conceptGraph;
	}
	
	@Override
	protected double similarity(int s1, int s2){
		String[] words1 = currentCiter.sentences.get(s1).text.split("\\s+");
		String[] words2 = currentCiter.sentences.get(s2).text.split("\\s+");
		return conceptGraph.similarity(words1, words2);
	}
}
