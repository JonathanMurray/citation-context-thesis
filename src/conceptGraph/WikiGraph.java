package conceptGraph;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class WikiGraph implements ConceptGraph{
	public static final double DEFAULT_SIMILARITY_MULTIPLIER = 0.01;
	private double similarityMultiplier;
	
	public WikiGraph(double similarityMultiplier){
		this.similarityMultiplier = similarityMultiplier;
	}
	
	public WikiGraph(){
		this(DEFAULT_SIMILARITY_MULTIPLIER);
	}
	
	public static WikiGraph fromFiles(String linksPath, String indicesPath){
		return WikiGraphFactory.buildWikiGraph(linksPath, indicesPath);
	}
	
	final public void setSimilarityMultiplier(double mult){
		similarityMultiplier = mult;
	}
	
	final public double similarity(String[] sentence1, String[] sentence2){
		List<Concept> vec1 = sentenceToConcepts(sentence1);
		List<Concept> vec2 = sentenceToConcepts(sentence2);
		
		if(vec1.size() > 0 && vec2.size() > 0){
			return similarity(vec1, vec2);
		}
		return 0;
	}
	
	private double similarity(List<Concept> concepts1, List<Concept> concepts2){
		double sum = 0;
		for(Concept c1 : concepts1){
			for(Concept c2 : concepts2){
				if(c1.related(c2)){
					sum += 1.0;
				}
			}
		}
		return similarityMultiplier * sum / (double)concepts1.size() / (double)concepts2.size();
	}

	private List<Concept> sentenceToConcepts(String[] sentence){
		List<Concept> concepts = new ArrayList<Concept>();
		for(int i = 0; i < sentence.length; i++){
			String wordLowerCase = sentence[i].toLowerCase();
			try{
				int phraseIndex = getPhraseIndex(wordLowerCase);
				concepts.add(phraseToConcept(phraseIndex));
			}catch(NoSuchElementException e){
				//That's fine
			}
		}
		return concepts;
	}
	
	private Concept phraseToConcept(int index){
		HashSet<Integer> related = new HashSet<Integer>();
		related.add(index);
		try{
			for(int other : getLinksFrom(index).toArray()){
				related.add(other);
			}
		}catch(NoSuchElementException e){
			//That's fine
		}
		
		return new Concept(related); 
	}
	
	protected abstract int getPhraseIndex(String phrase) throws NoSuchElementException;
	protected abstract TIntArrayList getLinksFrom(int index) throws NoSuchElementException;
}