package conceptGraph;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

public class Concept{
	
	public TIntHashSet indices;
	public double sqrtSize;
	
	public Concept(TIntHashSet indices){
		this.indices = indices;
		if(indices.size() < 1){
			sqrtSize = 1;
		}else{
			sqrtSize = Math.sqrt(indices.size());
		}
		
	}
	
	public double cosineSimilarity(Concept other){
		TIntIterator it = indices.iterator();
		double sum = 0;
		while(it.hasNext()){
			int index = it.next();
			if(((Concept)other).indices.contains(index)){
				sum += 1;
			}
		}
		if(sum > 0){
			return sum / sqrtSize / other.sqrtSize;
		}
		return 0;
	}

	public String toString(){
		return indices.toString();
	}
}
