package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IncrementableMap<T> extends HashMap<T, Integer>{
	private static final long serialVersionUID = 1L;


	public void increment(T key, Integer amount){
		if(containsKey(key)){
			put(key, get(key) + 1);
		}else{
			put(key, 1);
		}
	}
	
	
	public Set<Entry<T,Integer>> getTopN(int n){
		return entrySet().stream()
				.sorted((e1,e2)-> e2.getValue() - e1.getValue())
				.limit(n)
//				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
	
	public void removeIfValue(Predicate<Integer> filter){
		Iterator<Entry<T, Integer>> it = entrySet().iterator();
		while(it.hasNext()){
			int val = it.next().getValue();
			if(filter.test(val)){
				it.remove();
			}
		}
	}

	
	
}
