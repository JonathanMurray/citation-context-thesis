package conceptGraph;

import java.util.HashSet;

public class Concept{
	
	HashSet<Integer> indices;
	
	Concept(HashSet<Integer> indices){
		this.indices = indices;
	}
	
	public boolean related(Concept other){
		for(Integer index : indices){
			if(((Concept)other).indices.contains(index)){
				return true;
			}
		}
		return false;
	}
	

	public String toString(){
		return indices.toString();
	}
}
