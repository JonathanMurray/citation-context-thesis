package conceptGraph;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import util.Texts;

public abstract class WikiGraph implements ConceptGraph{
	
	public static final boolean DEFAULT_ALLOW_STOPWORDS_AS_CONCEPTS = false;
	private boolean allowStopwordsAsConcepts;
	
	public WikiGraph(boolean allowStopwordsAsConcepts){
		this.allowStopwordsAsConcepts = allowStopwordsAsConcepts;
	}
	
	public WikiGraph(){
		this(DEFAULT_ALLOW_STOPWORDS_AS_CONCEPTS);
	}
	
	final public void setAllowStopwordsAsConcepts(boolean allow){
		allowStopwordsAsConcepts = allow;
	}
	
	@Override
	public double similarity(Collection<String> sentence1, Collection<String> sentence2){
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
		return sum / (double)concepts1.size() / (double)concepts2.size();
	}

	public List<Concept> sentenceToConcepts(Collection<String> sentence){
		List<Concept> concepts = new ArrayList<Concept>();
		for(String word : sentence){
			String wordLowerCase = word.toLowerCase();
			try{
				if(allowStopwordsAsConcepts || !Texts.instance().isStopword(wordLowerCase)){
					int phraseIndex = getPhraseIndex(wordLowerCase);
					concepts.add(phraseToConcept(phraseIndex));	
				}
			}catch(NoSuchElementException e){
				//phrase is not a registered concept
			}
		}
		return concepts;
	}
	
	public Concept phraseToConcept(int index){
		HashSet<Integer> related = new HashSet<Integer>();
		related.add(index);
		try{
			for(int other : getLinksFrom(index).toArray()){
				related.add(other);
			}
		}catch(NoSuchElementException e){
			//No links from phrase found
		}
		
		return new Concept(related); 
	}
	
	protected abstract int getPhraseIndex(String phrase) throws NoSuchElementException;
	protected abstract TIntArrayList getLinksFrom(int index) throws NoSuchElementException;
}