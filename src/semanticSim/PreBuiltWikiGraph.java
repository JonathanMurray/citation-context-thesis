package semanticSim;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.NoSuchElementException;

/**
 * The default version of Wikigraph. It is constructed fully before use.
 * @author jonathan
 *
 */
public class PreBuiltWikiGraph extends WikiGraph{
	
	private TIntObjectHashMap<TIntArrayList> links;
	private TObjectIntHashMap<String> indices;
	
	public PreBuiltWikiGraph(TIntObjectHashMap<TIntArrayList> links, TObjectIntHashMap<String> indices){
		this(links, indices, DEFAULT_ALLOW_STOPWORDS_AS_CONCEPTS);
	}
	
	public PreBuiltWikiGraph(TIntObjectHashMap<TIntArrayList> links, TObjectIntHashMap<String> indices, boolean allowStopwordsAsConcepts){
		super(allowStopwordsAsConcepts);
		this.links = links;
		this.indices = indices;
	}
	
	@Override
	protected int getPhraseIndex(String phrase){
		if(indices.containsKey(phrase)){
			return indices.get(phrase);
		}
		throw new NoSuchElementException();
	}

	@Override
	protected TIntArrayList getLinksFrom(int index){
		if(links.containsKey(index)){
			return links.get(index);
		}
		throw new NoSuchElementException();
	}
}
