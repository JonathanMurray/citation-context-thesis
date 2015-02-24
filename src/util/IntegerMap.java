package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntegerMap<K> extends HashMap<K, Integer>{
	private static final long serialVersionUID = 1L;


	public void increment(K key, Integer amount){
		if(containsKey(key)){
			put(key, get(key) + amount);
		}else{
			put(key, amount);
		}
	}
	
	
	public Set<Entry<K,Integer>> getTopN(int n){
		return entrySet().stream()
				.sorted((e1,e2)-> e2.getValue() - e1.getValue())
				.limit(n)
//				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
	
	public void removeIfValue(Predicate<Integer> filter){
		Iterator<Entry<K, Integer>> it = entrySet().iterator();
		while(it.hasNext()){
			int val = it.next().getValue();
			if(filter.test(val)){
				it.remove();
			}
		}
	}
	
	public static <T> IntegerMap<T> merge(Stream<IntegerMap<T>> maps){
		IntegerMap<T> newMap = new IntegerMap<T>();
		maps.forEach(map -> {
			map.entrySet().stream().forEach(entry -> {
				newMap.increment(entry.getKey(),entry.getValue());
			});
		});
		return newMap;
	}

	
	
}