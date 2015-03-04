package conceptGraph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.NoSuchElementException;

public class PreBuiltWikiGraph extends WikiGraph{
	
	private TIntObjectHashMap<TIntArrayList> links;
//	private HashMap<Integer, TIntArrayList> links;
	private TObjectIntHashMap<String> indices;
//	private HashMap<String, Integer> indices;
	
	public static PreBuiltWikiGraph fromFiles(String linksPath, String indicesPath){
		return WikiGraphFactory.buildWikiGraph(linksPath, indicesPath);
	}
	
	public PreBuiltWikiGraph(TIntObjectHashMap<TIntArrayList> links, TObjectIntHashMap<String> indices){
		this(links, indices, DEFAULT_SIMILARITY_MULTIPLIER);
	}
	
	public PreBuiltWikiGraph(TIntObjectHashMap<TIntArrayList> links, TObjectIntHashMap<String> indices, double similarityMultiplier){
		super(similarityMultiplier);
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
	
//	private double similarity(String phrase1, String phrase2){
//		
//		phrase1 = phrase1.toLowerCase();
//		phrase2 = phrase2.toLowerCase();
//		
//		if(phrase1.equals(phrase2)){
//			return 1;
//		}
//		
//		List<String> related1 = links.get(phrase1);
//		List<String> related2 = links.get(phrase2);
//		
//		double sim = 0;
//		if(related1 != null){
//			sim += similarity(phrase2, related1)/3;
//		}
//		
//		if(related2 != null){
//			sim += similarity(phrase1, related2)/3;
//		}
//		
//		if(related1 != null && related2 != null){
//			double overlap = Math.sqrt(overlap(related1, related2)) / Math.sqrt(Math.min(related1.size(), related2.size()));
//			System.out.println("overlap: " + overlap);
//			sim += overlap;
//		}
//		
//		return sim;
//	}
	
//	private static <T extends Comparable<T>> double overlap(List<T> listA, List<T> listB){
//		double overlap = 0;
//		int a = 0;
//		int b = 0;
//		while(a < listA.size() && b < listB.size()){
//			int cmp = listA.get(a).compareTo(listB.get(b));
//			if(cmp < 0){
//				a++;
//			}else if(cmp == 0){
//				overlap ++;
//				a++;
//				b++;
//			}else{
//				b++;
//			}
//		}
//		return overlap;
//	}
//	
//	private static double similarity(String phrase, List<String> otherPhrases){
//		double sim = 0;
//		System.out.print(phrase + " MATCHES: ");
//		for(String other : otherPhrases){
//			if(other.equals(phrase)){
//				System.out.println(other + "!");
//				return 1;
//			}
//			if(other.contains(phrase)){
////				double numWordsInExpression = 1.0+(double)expression.split("\\w+").length;
//				System.out.print(other + ", ");
//				sim += 1;
//			}
//		}
//		System.out.println();
//		return Math.sqrt(sim) / Math.sqrt(otherPhrases.size());
//	}
	
	
}
