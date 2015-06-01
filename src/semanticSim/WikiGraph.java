package semanticSim;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import dataset.TextUtil;

/**
 * Represents a Wikipedia dump, that contains links between articles.
 * Maps sentences to bags of concepts that can be used for semantic comparison of different sentences
 * @author jonathan
 *
 */
public abstract class WikiGraph{
	
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

	public List<WikiConcept> sentenceToConcepts(Collection<String> words){
		List<WikiConcept> concepts = new ArrayList<WikiConcept>();
		for(String word : words){
			String wordLowerCase = word.toLowerCase();
			try{
				if(allowStopwordsAsConcepts || !TextUtil.instance().isStopword(wordLowerCase)){
					int phraseIndex = getPhraseIndex(wordLowerCase);
					concepts.add(phraseToConcept(phraseIndex));	
				}
			}catch(NoSuchElementException e){
				//phrase is not a registered concept
			}
		}
		return concepts;
	}
	
	public WikiConcept phraseToConcept(int index){
		TIntHashSet related = new TIntHashSet();
		related.add(index);
		try{
			for(int other : getLinksFrom(index).toArray()){
				related.add(other);
			}
		}catch(NoSuchElementException e){
			//No links from phrase found
		}
		
		return new WikiConcept(related); 
	}
	
	protected abstract int getPhraseIndex(String phrase) throws NoSuchElementException;
	protected abstract TIntArrayList getLinksFrom(int index) throws NoSuchElementException;
}