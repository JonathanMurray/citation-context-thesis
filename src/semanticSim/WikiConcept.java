package semanticSim;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Represents a set of Wikipedia articles that are related through hyperlinks. 
 * @author jonathan
 *
 */
public class WikiConcept{
	
	//Each index corresponds to a Wikipedia article
	public TIntHashSet indices;
	public double sqrtSize;
	
	public WikiConcept(TIntHashSet indices){
		this.indices = indices;
		if(indices.size() < 1){
			sqrtSize = 1;
		}else{
			sqrtSize = Math.sqrt(indices.size());
		}
	}
	
	public double similarity(WikiConcept other){
		TIntIterator it = indices.iterator();
//		double sum = 0;
		while(it.hasNext()){
			int index = it.next();
			if(((WikiConcept)other).indices.contains(index)){
//				sum += 1;
				return 1;
			}
		}
//		if(sum > 0){
//			return sum / sqrtSize / other.sqrtSize;
//		}
		return 0;
	}

	public String toString(){
		return indices.toString();
	}
}
