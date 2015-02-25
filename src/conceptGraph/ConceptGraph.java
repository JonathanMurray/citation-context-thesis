package conceptGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ConceptGraph {
	
	public static void main(String[] args) {
		ConcurrentHashMap<String, List<String>> links = GraphCreator.loadLinksFromFile("conceptGraphLower.ser");
		ConceptGraph graph = new ConceptGraph(links);
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter 2 sentences to compare: ");
		while(true){
			System.out.println("Enter first: ");
			String[] s1 = sc.nextLine().split("\\s+");
			if(s1.length > 0 && s1[0].equals("bye")){
				break;
			}
			System.out.println("Enter second: ");
			String[] s2 = sc.nextLine().split("\\s+");
			System.out.println(graph.similarity(s1, s2));
		}
		sc.close();
	}
	
	
	private ConcurrentHashMap<String, List<String>> links;
	
	public ConceptGraph(ConcurrentHashMap<String, List<String>> links){
		this.links = links;
	}
	
	public double similarity(String[] sentence1, String[] sentence2){
		HashMap<Concept, Double> vec1 = sentenceToVec(sentence1);
		HashMap<Concept, Double> vec2 = sentenceToVec(sentence2);
		System.out.println(vec1);
		System.out.println(vec2);
		
		if(vec1.size() > 0 && vec2.size() > 0){
			return similarity(vec1, vec2);
		}
		return 0;
	}
	
	private double similarity(HashMap<Concept, Double> vector1, HashMap<Concept, Double> vector2){
		double sum = 0;
		for(Concept c1 : vector1.keySet()){
			for(Concept c2 : vector2.keySet()){
				if(c1.related(c2)){
					sum += 1.0;
				}
			}
		}
		return sum / (double)vector1.size() / (double)vector2.size();
	}
	
	private boolean containsRelatedKey(HashMap<Concept, Double> map, Concept concept){
		for(Concept other : map.keySet()){
			if(concept.related(other)){
				return true;
			}
		}
		return false;
	}
	
	private HashMap<Concept, Double> sentenceToVec(String[] sentence){
		HashMap<Concept, Double> vec = new HashMap<Concept, Double>();
		for(int i = 0; i < sentence.length; i++){
			vec.put(phraseToConcept(sentence[i]), 1.0);
			if(i < sentence.length - 1){
				String bigram = sentence[i] + " " + sentence[i+1];
				vec.put(phraseToConcept(bigram), 1.0);
			}
		}
		return vec;
	}
	
	private Concept phraseToConcept(String phrase){
		HashSet<String> related = new HashSet<String>();
		related.add(phrase);
		if(links.containsKey(phrase)){
			related.addAll(links.get(phrase));
		}
		return new Concept(related); 
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
	
	private static class Concept{
		
		HashSet<String> words;
		
		Concept(HashSet<String> words){
			this.words = words;
		}
		
		public boolean related(Concept other){
			for(String word : words){
				if(((Concept)other).words.contains(word)){
					return true;
				}
			}
			return false;
		}
		
	
		public String toString(){
			return words.toString();
		}
	}
}
