package util;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Iterator;

public class CosineSimilarity {
	
	public static <T> double calculateCosineSimilarity(TObjectDoubleHashMap<T> a, TObjectDoubleHashMap<T> b){
		if(a.size() < 1 || b.size() < 1){
			return 0;
		}
		double sum = 0;
		Iterator<T> aIt = a.keySet().iterator();
		while(aIt.hasNext()){
			T key = aIt.next();
			if(b.containsKey(key)){
				sum += a.get(key) * b.get(key);
			}
		}
		return sum / (calculateNorm(a) * calculateNorm(b));
	}
	
	public static <T> double calculateNorm(TObjectDoubleHashMap<T> feature){
		double norm = 0;
		Iterator<T> it = feature.keySet().iterator();
		while(it.hasNext()){
			norm += Math.pow(feature.get(it.next()), 2);
		}
		return Math.sqrt(norm);
	}
}
