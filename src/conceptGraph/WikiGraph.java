package conceptGraph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collection;
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

	public List<Concept> sentenceToConcepts(Collection<String> words){
		List<Concept> concepts = new ArrayList<Concept>();
		for(String word : words){
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
		TIntHashSet related = new TIntHashSet();
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