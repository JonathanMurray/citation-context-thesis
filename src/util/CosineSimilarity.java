package util;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Iterator;

/**
 * This class calculates the cosine similarity between two clusters, or entities 
 * in our cases. Each cluster consists of one mention at the initialization phase, 
 * because we initialize each mention as a cluster.
 * <P>
 * In the search phase, we need to merge two clusters, each of which contains at 
 * least one mention. So in this way, we need to extract features from the cluster 
 * pairs. We use List to represent the features of each cluster and then compute 
 * the cosine similarity between the feature pairs.
 * <P>
 * The formula to calculate the cosine similarity is borrowed from link:
 * http://en.wikipedia.org/wiki/Cosine_similarity
 * 
 * @author xie
 * 
 * (https://github.com/xiejuncs/)
 *
 * Jonathan made string keys generic 25/2 - 2015
 */
public class CosineSimilarity {
//
//	/**
//	 * calculate the cosine similarity between feature vectors of two clusters
//	 * 
//	 * The feature vector is represented as HashMap<String, Double>. 
//	 * 
//	 * @param firstFeatures The feature vector of the first cluster
//	 * @param secondFeatures The feature vector of the second cluster 
//	 * @return the similarity measure
//	 */
//	public static <K> Double calculateCosineSimilarity(HashMap<K, Double> firstFeatures, HashMap<K, Double> secondFeatures) {
//		if(firstFeatures.size() < 1 || secondFeatures.size() < 1){
//			throw new IllegalArgumentException("Empty feature vector"); //Added by Jonathan 23/2-2015
//		}
//		Double similarity = 0.0;
//		Double sum = 0.0;	// the numerator of the cosine similarity
//		Double fnorm = 0.0;	// the first part of the denominator of the cosine similarity
//		Double snorm = 0.0;	// the second part of the denominator of the cosine similarity
//		Set<K> fkeys = firstFeatures.keySet();
//		Iterator<K> fit = fkeys.iterator();
//		while (fit.hasNext()) {
//			K featurename = fit.next();
//			boolean containKey = secondFeatures.containsKey(featurename);
//			if (containKey) {
//				sum = sum + firstFeatures.get(featurename) * secondFeatures.get(featurename);
//			}
//		}
//		fnorm = calculateNorm(firstFeatures);
//		snorm = calculateNorm(secondFeatures);
//		similarity = sum / (fnorm * snorm);
//		return similarity;
//	}
//	
//	/**
//	 * calculate the norm of one feature vector
//	 * 
//	 * @param feature of one cluster
//	 * @return
//	 */
//	public static <Key> Double calculateNorm(HashMap<Key, Double> feature) {
//		Double norm = 0.0;
//		Set<Key> keys = feature.keySet();
//		Iterator<Key> it = keys.iterator();
//		while (it.hasNext()) {
//			Key featurename = it.next();
//			norm = norm + Math.pow(feature.get(featurename), 2);
//		}
//		return Math.sqrt(norm);
//	}
	
	public static double calculateCosineSimilarity(TObjectDoubleHashMap<String> a, TObjectDoubleHashMap<String> b){
		if(a.size() < 1 || b.size() < 1){
			return 0;
		}
		double sum = 0;
		Iterator<String> aIt = a.keySet().iterator();
		while(aIt.hasNext()){
			String key = aIt.next();
			if(b.containsKey(key)){
				sum += a.get(key) * b.get(key);
			}
		}
		return sum / (calculateNorm(a) * calculateNorm(b));
	}
	
	public static double calculateNorm(TObjectDoubleHashMap<String> feature){
		double norm = 0;
		Iterator<String> it = feature.keySet().iterator();
		while(it.hasNext()){
			norm += Math.pow(feature.get(it.next()), 2);
		}
		return Math.sqrt(norm);
	}
	

	
}
