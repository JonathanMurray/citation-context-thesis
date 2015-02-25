package conceptGraph;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ConceptGraph {
	
	public static void main(String[] args) {
		ConcurrentHashMap<String, List<String>> links = GraphCreator.loadLinksFromFile("conceptGraphLower.ser");
		ConceptGraph graph = new ConceptGraph(links);
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter 2 phrases to compare: ");
		while(true){
			System.out.println("Enter first: ");
			String w1 = sc.nextLine();
			if(w1.equals("bye")){
				break;
			}
			System.out.println("Enter second: ");
			String w2 = sc.nextLine();
			System.out.println(w1 + ": " + links.get(w1));
			System.out.println(w2 + ": " + links.get(w2));
			System.out.println(graph.similarity(w1, w2));
			
		}
		sc.close();
	}
	
	
	private ConcurrentHashMap<String, List<String>> links;
	
	public ConceptGraph(ConcurrentHashMap<String, List<String>> links){
		this.links = links;
	}
	
	private double similarity(String phrase1, String phrase2){
		
		phrase1 = phrase1.toLowerCase();
		phrase2 = phrase2.toLowerCase();
		
		if(phrase1.equals(phrase2)){
			return 1;
		}
		
		List<String> related1 = links.get(phrase1);
		List<String> related2 = links.get(phrase2);
		
		double sim = 0;
		if(related1 != null){
			sim += similarity(phrase2, related1)/3;
		}
		
		if(related2 != null){
			sim += similarity(phrase1, related2)/3;
		}
		
		if(related1 != null && related2 != null){
			double overlap = Math.sqrt(overlap(related1, related2)) / Math.sqrt(Math.min(related1.size(), related2.size()));
			System.out.println("overlap: " + overlap);
			sim += overlap;
		}
		
		return sim;
	}
	
	private static <T extends Comparable<T>> double overlap(List<T> listA, List<T> listB){
		double overlap = 0;
		int a = 0;
		int b = 0;
		while(a < listA.size() && b < listB.size()){
			int cmp = listA.get(a).compareTo(listB.get(b));
			if(cmp < 0){
				a++;
			}else if(cmp == 0){
				overlap ++;
				a++;
				b++;
			}else{
				b++;
			}
		}
		return overlap;
	}
	
	private static double similarity(String phrase, List<String> otherPhrases){
		double sim = 0;
		System.out.print(phrase + " MATCHES: ");
		for(String other : otherPhrases){
			if(other.equals(phrase)){
				System.out.println(other + "!");
				return 1;
			}
			if(other.contains(phrase)){
//				double numWordsInExpression = 1.0+(double)expression.split("\\w+").length;
				System.out.print(other + ", ");
				sim += 1;
			}
		}
		System.out.println();
		return Math.sqrt(sim) / Math.sqrt(otherPhrases.size());
	}
	
//	private static class Concept{
//		HashSet<String> words;
//		Concept(HashSet<String> words){
//			this.words = words;
//		}
//		@Override
//		public boolean equals(Object other){
//			if(other instanceof Concept){
//				for(String word : words){
//					if(((Concept)other).words.contains(word)){
//						return true;
//					}
//				}
//			}
//			return false;
//		}
//	}
}
