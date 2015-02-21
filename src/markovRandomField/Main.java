package markovRandomField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sentenceFeaturesToWeka.Sentence;
import sentenceFeaturesToWeka.SentenceType;

public class Main {
	
	public static void main(String[] args) {
		new Main();
	}
	
	static final int NO = 0;
	static final int YES = 1;
	
	int numSentences;
	List<Sentence> sentences;
	List<double[]> beliefs;
	List<Map<Integer,double[]>> allReceivedMessages;
	int neighbourhood = 1;
	
	Main(){
		
		sentences = new ArrayList<Sentence>();
		sentences.add(new Sentence(SentenceType.NOT_REFERENCE, "Ax"));
		sentences.add(new Sentence(SentenceType.NOT_REFERENCE, "Bx"));
		sentences.add(new Sentence(SentenceType.NOT_REFERENCE, "Cx"));
		sentences.add(new Sentence(SentenceType.NOT_REFERENCE, "D"));
		
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
					Sentence sender = sentences.get(from);
					Sentence receiver = sentences.get(to);
					
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
					System.out.println("from: " + sender.text);
					System.out.println("to: " + receiver.text);
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
	
	double compatibility(int context1, int context2, Sentence s1, Sentence s2){
		if(context1 == NO){
			return 0.5;
		}
		double cosSimilarity = cosSimilarity(s1, s2);
		double probContext = 1 / (1 + Math.exp(-cosSimilarity));
		return context2 == YES? probContext : 1 - probContext;
	}
	
	double cosSimilarity(Sentence s1, Sentence s2){
		
		if(s1.text.contains("x") && s2.text.contains("x")){
			return 10;
		}
		return 0;
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
