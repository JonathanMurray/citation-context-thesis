package markovRandomField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sentenceFeaturesToWeka.Sentence;
import util.CosineSimilarity;
import util.DoubleMap;

public class Main {
	
	public static void main(String[] args) {
		new Main();
	}
	
	static final int NO = 0;
	static final int YES = 1;
	
	int numSentences;
	List<HashMap<String,Double>> sentences;
	List<double[]> beliefs;
	List<Map<Integer,double[]>> allReceivedMessages;
	int neighbourhood = 1;
	
	Main(){
		
		sentences = new ArrayList<HashMap<String,Double>>();
		sentences.add(sentenceToWordVec("a b c"));
		sentences.add(sentenceToWordVec("a b d e"));
		sentences.add(sentenceToWordVec("c x x"));
		sentences.add(sentenceToWordVec("y y c"));
		
		numSentences = sentences.size();
		beliefs = new ArrayList<double[]>();
		beliefs.add(new double[]{0.1, 0.9});
		beliefs.add(new double[]{0.9, 0.1});
		beliefs.add(new double[]{0.1, 0.9});
		beliefs.add(new double[]{0.5, 0.5});
		allReceivedMessages = new ArrayList<Map<Integer,double[]>>();
		initMessages();
		for(int i = 0; i < 3; i++){
			oneLoop();	
		}
	}
	
	private static HashMap<String, Double> sentenceToWordVec(Sentence sentence){
		return sentenceToWordVec(sentence.text);
	}
	
	private static HashMap<String, Double> sentenceToWordVec(String sentence){
		String[] words = sentence.split(" ");
		DoubleMap<String> wordVector = new DoubleMap<String>();
		Arrays.asList(words).stream().forEach(word -> {
			wordVector.increment(word, 1.0);
		});
		return wordVector;
	}
	
	
	
	private void initMessages(){
		
		for(int s = 0; s < numSentences; s++){
			Map<Integer, double[]> receivedMessages = new HashMap<Integer, double[]>();
			for(int m = Math.max(0, s-neighbourhood); m <= Math.min(s+neighbourhood, numSentences-1); m++){
				if(m != s){
					receivedMessages.put(m, new double[]{1,1}); //start value for msg
				}
			}
			allReceivedMessages.add(receivedMessages);
		}
	}
	
	private void oneLoop(){
		for(int from = 0; from < numSentences; from++){
			double[] belief = beliefs.get(from);
			Map<Integer, double[]> received = allReceivedMessages.get(from);
			int leftmostNeighbour = Math.max(0, from - neighbourhood);
			int rightmostNeighbour = Math.min(numSentences - 1, from + neighbourhood);
			for(int to = leftmostNeighbour; to <= rightmostNeighbour; to++){
				if(to != from){
					double[] productReceived = productOfValuesExcept(received, to);
					double[] totalBeliefAboutSelf = new double[]{
							belief[NO] * productReceived[NO], 
							belief[YES] * productReceived[YES]};
					HashMap<String, Double> sender = sentences.get(from);
					HashMap<String, Double> receiver = sentences.get(to);
					
					double[] message = new double[2];
					
//					System.out.println(from);
//					System.out.println("self no: " + totalBeliefAboutSelf[NO]);
//					System.out.println("self yes: " + totalBeliefAboutSelf[YES]);
					
					
					message[NO] =
							(totalBeliefAboutSelf[NO] * compatibility(NO, NO, sender, receiver)) + 
							(totalBeliefAboutSelf[YES] * compatibility(YES, NO, sender, receiver));
					message[YES] =
							(totalBeliefAboutSelf[NO] * compatibility(NO, YES, sender, receiver)) + 
							(totalBeliefAboutSelf[YES] * compatibility(YES, YES, sender, receiver));
					double normalization = 1/(message[NO] + message[YES]);
					message[NO]*=normalization;
					message[YES]*=normalization;
					System.out.println("from: " + sender);
					System.out.println("to: " + receiver);
					System.out.println("msg: " + Arrays.toString(message));
					System.out.println();
					allReceivedMessages.get(to).put(from, message);
				}
			}
			received.clear(); //Clear all messages. They will fill up before next time
		}
	}
	
	
	private double[] productOfValuesExcept(Map<Integer,double[]> map, int exceptionKey){
		double[] prod = new double[]{1,1};
		for(int i : map.keySet()){
			if(i != exceptionKey){
				prod[0] *= map.get(i)[0];
				prod[1] *= map.get(i)[1];
			}
		}
		return prod;
	}
	
	double compatibility(int context1, int context2, HashMap<String, Double> s1, HashMap<String, Double> s2){
		if(context1 == NO){
			return 0.5;
		}
		double cosSimilarity = CosineSimilarity.calculateCosineSimilarity(s1, s2);
		System.out.println("sim(   " + s1 + "   ,   " + s2 + "   ==   " + cosSimilarity);
		double probContext = 1 / (1 + Math.exp(-cosSimilarity));
		return context2 == YES? probContext : 1 - probContext;
	}
	
	
	
	
	
	
//	public void go(ContextDataSet dataset){
//		Citer citer = dataset.citers.get(0);
//		List<Sentence> sentences = citer.sentences;
//		go(sentences);
//	}
//	
//	private void go(List<Sentence> sentences){
//		List<Node> nodes = new ArrayList<Node>();
//		for(int i = 0; i < sentences.size(); i++){
//			double[] selfBelief = new double[]{0.3,0.7};
//			nodes.add(new Node(selfBelief));
//		}
//		
//		
//	}
//	
//	
//	
//	
//	
//	private static class Node{
//		double[] selfBelief;
//		double[][] receivedMessages;
//		int numNeighbours;
//		
//		Node(double[] selfBelief, int numNeighbours){
//			this.selfBelief = selfBelief;
//			this.numNeighbours = numNeighbours;
//			receivedMessages = new double[numNeighbours][];
//			for(int i = 0; i < numNeighbours; i++){
//				receivedMessages[i] = new double[]{1,1}; //just a start message, actually not a valid msg 
//			}
//		}
//		
//		
//		
//		double[] createMessageTo(Node receiver){
//			double notContext = 
//					selfBelief[0] * compatibility(false, receiver)[0] * productOthersBelief[0]
//				+ selfBelief[1] * compatibility(true, receiver)[0] * productOthersBelief[1];
//			
//			double context = 
//					selfBelief[0] * compatibility(false, receiver)[1] * productOthersBelief[0]
//				+ selfBelief[1] * compatibility(true, receiver)[1] * productOthersBelief[1];
//			
//			
//			
//			return new double[]{notContext, context};
//		}
//	}

}
