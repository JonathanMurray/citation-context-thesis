package markovRandomField;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sentenceFeaturesToWeka.Citer;
import sentenceFeaturesToWeka.ContextDataSet;
import sentenceFeaturesToWeka.ContextHTML_Parser;
import sentenceFeaturesToWeka.Sentence;
import sentenceFeaturesToWeka.SentenceType;
import util.CosineSimilarity;
import util.DoubleMap;
import util.Stemmer;
import util.Texts;


public class Main {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	public static final String sentimentCorpusDir = dataDir + "teufel-citation-context-corpus/";
	
	
	public static void main(String[] args) {
		ContextDataSet dataset = new ContextHTML_Parser().readJsoup(Paths.get(sentimentCorpusDir + "A92-1018.html").toFile());
		
		System.out.println(dataset.citedTitle);
		
		String citedAbstract = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements. Only a lexicon and some unlabeled training text are required. Accuracy exceeds 96%. We describe implementation strategies and optimizations which result in high-speed operation. Three applications for tagging are described: phrase recognition; word sense disambiguation; and grammatical function assignment.";
		
//		dataset.citers.get(0).sentences.forEach( s -> {
//			System.out.println(sentenceToWordVec(s));
//			System.out.println(CosineSimilarity.calculateCosineSimilarity(sentenceToWordVec(s), textToWordVec(firstPaperAbstract)));
//			System.out.println();
//		});
		
		System.out.println("\n");
		
		Citer citer = dataset.citers.get(2);
		
		List<String> sentences = citer.sentences.stream().sequential()
				.map(s -> s.text)
				.collect(Collectors.toCollection(ArrayList::new));
		
		List<SentenceType> sentenceTypes = citer.sentences.stream().sequential()
				.map(s -> s.type)
				.collect(Collectors.toCollection(ArrayList::new));
		
		new Main().go(sentences, sentenceTypes, dataset.citedMainAuthor, citedAbstract);
	}
	
	private static void testWithSmallData(){
		String[] texts = new String[6];
		texts[0] = "We describe the design, prototyping and evaluation of ARC, a system for automatically compiling a list of authoritative web resources on any (sufficiently broad) topic.";
		texts[1] = "The goal of ARC is to compile resource lists similar to those provided by Yahoo! or Infoseek.";
		texts[2] = "The fundamental difference is that these services construct lists either manually or through a	combination of human and automated effort, while ARC operates fully automatically.";
		texts[3] = "We describe the evaluation of ARC, Yahoo!, and Infoseek resource lists by a panel of human users.";
		texts[4] = "This evaluation suggests that the resources found by ARC frequently fare almost as well as, and sometimes better than, lists of resources that are manually compiled or classified into a topic.";
		texts[5] = "We also provide examples of ARC resource lists for the reader to examine."; 
		
		String referencedAbstract = "In this paper we report on our experience using WebSQL, a high level declarative query"
						+ "language for extracting information from the Web. WebSQL takes advantage of multiple"
						+ "index servers without requiring users to know about them, and integrates full-text with"
						+ "topology-based queries. The WebSQL query engine is a library of Java classes, and"
						+ "WebSQL queries can be embedded into Java programs much in the same way as SQL"
						+ "queries are embedded in C programs. This allows us to access the Web from Java at a";
		
		
		new Main().go(Arrays.asList(texts), null, "Arocena", referencedAbstract);
	}
	
	static final int NO = 0;
	static final int YES = 1;
	
	int numSentences;
	List<HashMap<String,Double>> sentenceVectors;
	List<double[]> beliefs;
	List<Map<Integer,double[]>> allReceivedMessages;
	int neighbourhood = 4;
	
	
	/**
	 * This must be sentences from one single paper, and the ordering is critical.
	 * @param sentenceTexts
	 * @param mainAuthor
	 */
	public void go(List<String> sentenceTexts, List<SentenceType> sentenceTypes, String mainAuthor, String referencedText){
		sentenceVectors = new ArrayList<HashMap<String,Double>>();
		beliefs = new ArrayList<double[]>();
		
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		
		for(String text : sentenceTexts){
			HashMap<String, Double> vector = textToWordVec(text); 
			sentenceVectors.add(vector);
			unnormalizedBeliefs.add(selfBelief(text, vector, mainAuthor, textToWordVec(referencedText)));
		}
		
		double maxBelief = unnormalizedBeliefs.stream().max(Double::compareTo).get();
		double minBelief = unnormalizedBeliefs.stream().min(Double::compareTo).get();
		
		int j = 0; 
		for(double unnormalizedBelief : unnormalizedBeliefs){
			double normalized = (unnormalizedBelief - minBelief) / (maxBelief - minBelief);
//			System.out.println("sentence: " + sentenceTexts.get(j));
//			System.out.println("belief: " + normalized);
			beliefs.add(new double[]{1 - normalized, normalized});
			j ++;
		}
		
		numSentences = sentenceVectors.size();
		initMessages();
		
		for(int i = 0; i < 10; i++){
			oneLoop();	
		}
		
		int truePos = 0;
		int falsePos = 0;
		
		for(int i = 0; i < numSentences; i++){
			double[] belief = finalBelief(i);
			System.out.println(sentenceTypes.get(i) + " - " + Arrays.toString(belief) + ":  " + sentenceTexts.get(i));
			if(belief[1] > 0.5){
				if(sentenceTypes.get(i) == SentenceType.NOT_REFERENCE){
					falsePos ++;
				}else{
					truePos ++;
				}
			}
		}
		
		System.out.println("false: " + falsePos);
		System.out.println("true: " + truePos);
	}
	
	private static double selfBelief(String sentence, HashMap<String,Double> sentenceVector, String mainAuthor, HashMap<String,Double> referencedText){
		String[] words = sentence.split(" ");
		double explicit = Texts.instance().containsMainAuthor(sentence, mainAuthor) ? 1:0;
		double detWork = Texts.instance().startsWithDetWork(words) ? 1:0;
		double det = Texts.instance().startsWithLimitedDet(words) ? 1:0;
		double cosSim = CosineSimilarity.calculateCosineSimilarity(sentenceVector, referencedText);
		double unnormalizedBelief = explicit + detWork + det + (cosSim); //TODO mult. cossim
		return unnormalizedBelief;
	}
	
	private static HashMap<String, Double> sentenceToWordVec(Sentence sentence){
		return textToWordVec(sentence.text);
	}
	
	private static HashMap<String, Double> textToWordVec(String sentence){
		List<String> words = Texts.instance().removeStopwords(sentence.split(" "));
		DoubleMap<String> wordVector = new DoubleMap<String>();
		words.stream().forEach(word -> {
			wordVector.increment(stem(word.toLowerCase()), 1.0);
		});
		return wordVector;
	}
	
	private static String stem(String word){
		Stemmer s = new Stemmer();
		s.add(word);
		s.stem();
		return s.toString();
	}
	
	
	
	private void initMessages(){
		allReceivedMessages = new ArrayList<Map<Integer,double[]>>();
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
	
	private double[] finalBelief(int sentence){
		double[] productReceived = productOfValues(allReceivedMessages.get(sentence));
		double[] belief = beliefs.get(sentence);
		double[] totalBeliefAboutSelf = new double[]{
				belief[NO] * productReceived[NO], 
				belief[YES] * productReceived[YES]};
		fixProbabilityVector(totalBeliefAboutSelf);
		return totalBeliefAboutSelf;
	}
	
	private void oneLoop(){
		System.out.println();
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
					fixProbabilityVector(totalBeliefAboutSelf);
					HashMap<String, Double> sender = sentenceVectors.get(from);
					HashMap<String, Double> receiver = sentenceVectors.get(to);
					
					double[] message = new double[2];
					
					double[][] compatibility = new double[][]{
							compatibility(NO, sender, receiver),
							compatibility(YES, sender, receiver)
					};
					
//					System.out.println(compatibility[YES][YES]);
					
					message[NO] =
							(totalBeliefAboutSelf[NO] * compatibility[NO][NO]) + 
							(totalBeliefAboutSelf[YES] * compatibility[YES][NO]);
					message[YES] =
							(totalBeliefAboutSelf[NO] * compatibility[NO][YES]) + 
							(totalBeliefAboutSelf[YES] * compatibility[YES][YES]);
					double normalization = 1/(message[NO] + message[YES]);
					message[NO]*=normalization;
					message[YES]*=normalization;
					
					
					allReceivedMessages.get(to).put(from, message);
				}
			}
			received.clear(); //Clear all messages. They will fill up before next time
		}
	}
	
	private void fixProbabilityVector(double[] probabilities){
		if(probabilities.length != 2){
			throw new IllegalArgumentException();
		}
		double sum = probabilities[0] + probabilities[1];
		probabilities[0] /= sum;
		probabilities[1] /= sum;
	}
	
	private double[] productOfValues(Map<Integer,double[]> map){
		double[] prod = new double[]{1,1};
		for(int i : map.keySet()){
			prod[0] *= map.get(i)[0];
			prod[1] *= map.get(i)[1];
		}
		return prod;
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
	
	
	//TODO Change the way sentences are compared,
	//Maybe look for det+work in the later sentence?
	//Problem: We don't know how far away they are from eachother
	private double[] compatibility(int context1, HashMap<String, Double> s1, HashMap<String, Double> s2){
		if(context1 == NO){
			return new double[]{0.5, 0.5};
		}
		double cosSimilarity = CosineSimilarity.calculateCosineSimilarity(s1, s2);
//		System.out.println("sim(   " + s1 + "   ,   " + s2 + "   ==   " + cosSimilarity);
		double probContext = 1 / (1 + Math.exp( - 2 * cosSimilarity)); //TODO multiplying cossim
		return new double[]{1 - probContext, probContext};
//		return context2 == YES? probContext : 1 - probContext;
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
