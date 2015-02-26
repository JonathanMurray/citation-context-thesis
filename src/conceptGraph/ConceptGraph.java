package conceptGraph;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ConceptGraph {
	
	public static double SIMILARITY_CONSTANT = 0.01;
	
	private HashMap<Integer, TIntArrayList> links;
	private HashMap<String, Integer> indices;
	
	public static ConceptGraph fromFiles(String linksPath, String indicesPath){
		System.out.println("links: " + linksPath);
		System.out.println("indices: " + indicesPath);
		return GraphCreator.loadConceptGraph(linksPath, indicesPath);
	}
	
	public ConceptGraph(HashMap<Integer, TIntArrayList> links, HashMap<String, Integer> indices){
		this.links = links;
		this.indices = indices;
	}
	
	public double similarity(String[] sentence1, String[] sentence2){
		List<Concept> vec1 = sentenceToConcepts(sentence1);
		List<Concept> vec2 = sentenceToConcepts(sentence2);
		
		if(vec1.size() > 0 && vec2.size() > 0){
			return similarity(vec1, vec2);
		}
		return 0;
	}
	
	private double similarity(List<Concept> concepts1, List<Concept> concepts2){
		double sum = 0;
		for(Concept c1 : concepts1){
			for(Concept c2 : concepts2){
				if(c1.related(c2)){
					sum += 1.0;
				}
			}
		}
		return SIMILARITY_CONSTANT * sum / (double)concepts1.size() / (double)concepts2.size();
	}

	private List<Concept> sentenceToConcepts(String[] sentence){
		List<Concept> concepts = new ArrayList<Concept>();
		for(int i = 0; i < sentence.length; i++){
			String wordLowerCase = sentence[i].toLowerCase();
			if(indices.containsKey(wordLowerCase)){
				int phraseIndex = indices.get(wordLowerCase);
				concepts.add(phraseToConcept(phraseIndex));
			}
		}
		return concepts;
	}
	
	private Concept phraseToConcept(int index){
		HashSet<Integer> related = new HashSet<Integer>();
		related.add(index);
		if(links.containsKey(index)){
			for(int other : links.get(index).toArray()){
				related.add(other);
			}
		}
		return new Concept(related); 
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
	
	private static class Concept{
		
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
}
