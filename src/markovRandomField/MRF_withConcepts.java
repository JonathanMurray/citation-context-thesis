package markovRandomField;

import java.util.Collection;

import conceptGraph.ConceptGraph;

public class MRF_withConcepts extends MRF{

	ConceptGraph conceptGraph;
	
	public MRF_withConcepts(int neighbourhood, double beliefThreshold, ConceptGraph conceptGraph) {
		super(neighbourhood, beliefThreshold);
		this.conceptGraph = conceptGraph;
	}
	
	@Override
	protected double similarity(int s1, int s2){
		Collection<String> words1 = currentCiterNgrams.sentencesUnigrams.get(s1).keySet();
		Collection<String> words2 = currentCiterNgrams.sentencesUnigrams.get(s2).keySet();
//		String[] words1 = currentCiter.sentences.get(s1).text.split("\\s+");
//		String[] words2 = currentCiter.sentences.get(s2).text.split("\\s+");
		return conceptGraph.similarity(words1, words2);
	}
}
