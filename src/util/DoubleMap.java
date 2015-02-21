package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DoubleMap<K> extends HashMap<K, Double>{
	private static final long serialVersionUID = 1L;


	public void increment(K key, Double amount){
		if(containsKey(key)){
			put(key, get(key) + amount);
		}else{
			put(key, amount);
		}
	}
	
	
	public Set<Entry<K,Double>> getTopN(int n){
		return entrySet().stream()
				.sorted((e1,e2)-> (int)Math.round(Math.signum(e2.getValue() - e1.getValue())))
				.limit(n)
//				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
	
	public void removeIfValue(Predicate<Double> filter){
		Iterator<Entry<K, Double>> it = entrySet().iterator();
		while(it.hasNext()){
			double val = it.next().getValue();
			if(filter.test(val)){
				it.remove();
			}
		}
	}

	
}