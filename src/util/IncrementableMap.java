package util;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class IncrementableMap<T> {
	
	private HashMap<T, Integer> map;
	
	public IncrementableMap(){
		map = new HashMap<T, Integer>();
	}
	
	public void increment(T key, Integer amount){
		if(map.containsKey(key)){
			map.put(key, map.get(key) + 1);
		}else{
			map.put(key, 1);
		}
	}
	
	public int get(T key){
		return map.get(key);
	}
	
	public Set<T> getTopN(int n){
		return map.entrySet().stream()
				.sorted((e1,e2)-> e2.getValue() - e1.getValue())
				.limit(n)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
}
