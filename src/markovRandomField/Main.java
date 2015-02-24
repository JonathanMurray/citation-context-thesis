package markovRandomField;

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import sentenceFeaturesToWeka.SentenceClass;
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
		System.out.println("main author: " + dataset.citedMainAuthor);
		
		String citedAbstract = "We present an implementation of a part-of-speech tagger based on a hidden Markov model. The methodology enables robust and accurate tagging with few resource requirements. Only a lexicon and some unlabeled training text are required. Accuracy exceeds 96%. We describe implementation strategies and optimizations which result in high-speed operation. Three applications for tagging are described: phrase recognition; word sense disambiguation; and grammatical function assignment.";
		
		new Main().runManyAndPrintResults(dataset.citers, dataset.citedMainAuthor, citedAbstract, dataset);
	}
	
	static final int NO = 0;
	static final int YES = 1;
	
	int numSentences;
	List<HashMap<String,Double>> sentenceVectors;
	List<HashMap<String,Double>> bigramVectors;
	List<String> sentenceTexts;
	List<SentenceClass> sentenceTypes;
	List<double[]> beliefs;
	List<Map<Integer,double[]>> allReceivedMessages;
	int neighbourhood = 4;
	
	public void runManyAndPrintResults(List<Citer> citers, String mainAuthor, String referencedText, ContextDataSet dataset){
		Result result = new Result(0,0,0);
		for(Citer citer : citers){
			result.add(run(citer, mainAuthor, referencedText, dataset));
		}
		printResults(result);
	}
	
	private void printResults(Result result){
		System.out.println("precision: " + result.truePositives + "/" + (result.truePositives+result.falsePositives));
		System.out.println("recall: " + result.truePositives + "/" + result.total);
		double precision = result.truePositives / (double)(result.truePositives + result.falsePositives);
		double recall = result.truePositives / (double)result.total;
		System.out.println("F-score: " + fMeasure(precision, recall, 1));
		System.out.println("F3: " + fMeasure(precision, recall, 3));
	}
	
	private double fMeasure(double precision, double recall, double beta){
		return (1+Math.pow(beta, 2)) * (precision*recall)/(Math.pow(beta, 2)*precision + recall);
	}
	
	public Result run(Citer citer, String mainAuthor, String referencedText, ContextDataSet dataset){
		
		System.out.println("run - citer: " + citer.title + "...");
		
		List<String> sentences = citer.sentences.stream().sequential()
				.map(s -> s.text)
				.collect(Collectors.toCollection(ArrayList::new));
		
		List<SentenceClass> sentenceTypes = citer.sentences.stream().sequential()
				.map(s -> s.type)
				.collect(Collectors.toCollection(ArrayList::new));
		
		return run(sentences, sentenceTypes, dataset.citedMainAuthor, referencedText, dataset);
	}
	
	/**
	 * This must be sentences from one single paper, and the ordering is critical.
	 * @param sentenceTexts
	 * @param mainAuthor
	 */
	public Result run(List<String> sentenceTexts, List<SentenceClass> sentenceTypes, String mainAuthor, String referencedText, ContextDataSet dataset){
		this.sentenceTexts = sentenceTexts;
		this.sentenceTypes = sentenceTypes;
		sentenceVectors = new ArrayList<HashMap<String,Double>>();
		bigramVectors = new ArrayList<HashMap<String,Double>>();
		beliefs = new ArrayList<double[]>();
		numSentences = sentenceTexts.size();
		
		setup(mainAuthor, referencedText, dataset);
		initMessages();
		
		for(int i = 0; i < 5; i++){
			oneLoop();	
		}
		
		return getResults();
	}
	
	private void setup(String mainAuthor, String referencedText, ContextDataSet dataset){
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		
		for(String text : sentenceTexts){
			HashMap<String, Double> vector = textToWordVec(text); 
			sentenceVectors.add(vector);
			bigramVectors.add(Texts.instance().getNGrams(2, text));
			unnormalizedBeliefs.add(selfBelief(text, vector, mainAuthor, textToWordVec(referencedText), dataset));
		}
		
		double maxBelief = unnormalizedBeliefs.stream().max(Double::compareTo).get();
		double minBelief = unnormalizedBeliefs.stream().min(Double::compareTo).get();
		
		for(double unnormalizedBelief : unnormalizedBeliefs){
			double normalized;
			if(maxBelief > minBelief){
				normalized = (unnormalizedBelief - minBelief) / (maxBelief - minBelief);
			}else{
				System.out.println(maxBelief + " !> " + minBelief);
				normalized = 0.5;
			}
			if(normalized == 0){
				normalized = 0.1; //TODO don't want any 0-probabilities
			}
//			System.out.println("sentence: " + sentenceTexts.get(j));
//			System.out.println("belief: " + normalized);
			beliefs.add(new double[]{1 - normalized, normalized});
		}
	}
	
	private static HashMap<String, Double> sentenceToWordVec(Sentence sentence){
		return textToWordVec(sentence.text);
	}
	
	private static HashMap<String, Double> textToWordVec(String sentence){
		List<String> words = Texts.instance().removeStopwords(sentence.split(" +"));
		DoubleMap<String> wordVector = new DoubleMap<String>();
		words.stream().forEach(word -> {
			wordVector.increment(stem(word.toLowerCase()), 1.0);
		});
		return wordVector;
	}
	
	private static double selfBelief(String sentence, HashMap<String,Double> sentenceVector, String mainAuthor, HashMap<String,Double> referencedText, ContextDataSet dataset){
		String[] words = sentence.split(" +");
		double explicit = Texts.instance().containsExplicitReference(Arrays.asList(words), mainAuthor) ? 2:0;
		double detWork = Texts.instance().startsWithDetWork(words) ? 0:0;
		double det = Texts.instance().startsWithLimitedDet(words) ? 0:0;
		double cosSim = 0;
		if(sentenceVector.size() > 0){
			cosSim = CosineSimilarity.calculateCosineSimilarity(sentenceVector, referencedText);
		}
		double acronyms = Texts.instance().containsAcronyms(sentence, dataset.acronyms) ? 1:0;
		double hooks = Texts.instance().containsLexicalHooks(sentence, dataset.lexicalHooks)? 1:0;
		
		return 0.01 + explicit + detWork + det + cosSim + acronyms + hooks; //TODO mult. cossim
	}
	
	private Result getResults(){
		int truePos = 0;
		int falsePos = 0;
		int pos = 0;
		
		for(int i = 0; i < numSentences; i++){
			double[] belief = finalBelief(i);
//			System.out.println(sentenceTypes.get(i) + " - " + beliefToString(selfBelief) + " -> " + beliefToString(belief) + ":  " + sentenceTexts.get(i));
			if(sentenceTypes.get(i) != SentenceClass.NOT_REFERENCE){
				pos ++;
			}
			if(belief[1] > 0.65){
				if(sentenceTypes.get(i) == SentenceClass.NOT_REFERENCE){
					falsePos ++;
				}else{
					truePos ++;
				}
			}
		}
		
		return new Result(truePos, falsePos, pos);
	}
	
	private double[] finalBelief(int sentence){
		double[] productReceived = productOfValues(allReceivedMessages.get(sentence));
		double[] belief = beliefs.get(sentence);
		double[] totalBeliefAboutSelf = new double[]{
				belief[NO] * productReceived[NO], 
				belief[YES] * productReceived[YES]};
		normalizeProbabilityVector(totalBeliefAboutSelf);
		return totalBeliefAboutSelf;
	}
	
	String beliefToString(double[] belief){
		NumberFormat formatter = new DecimalFormat("#0.00");     
		return formatter.format(belief[1]);
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
					receivedMessages.put(m, new double[]{0.5,0.5}); //start value for msg
				}
			}
			allReceivedMessages.add(receivedMessages);
		}
	}
	
	private void oneLoop(){
		for(int from = 0; from < numSentences; from++){
			double[] belief = beliefs.get(from);
			Map<Integer, double[]> receivedMessages = allReceivedMessages.get(from);
			int leftmostNeighbour = Math.max(0, from - neighbourhood);
			int rightmostNeighbour = Math.min(numSentences - 1, from + neighbourhood);
			for(int to = leftmostNeighbour; to <= rightmostNeighbour; to++){
				if(to != from){
					sendMessage(from, to, receivedMessages, belief);
				}
			}
		}
	}
	
	private void sendMessage(int from, int to, Map<Integer, double[]> receivedMessages, double[] belief){
		double[] productReceived = productOfValuesExcept(receivedMessages, to);
		double[] totalBeliefAboutSelf = new double[]{
				belief[NO] * productReceived[NO], 
				belief[YES] * productReceived[YES]};
		normalizeProbabilityVector(totalBeliefAboutSelf);
		
		double[] message = new double[2];
		
		double[][] compatibility = new double[][]{
				compatibility(NO, from, to),
				compatibility(YES, from, to)
		};
		
		message[NO] =
				(totalBeliefAboutSelf[NO] * compatibility[NO][NO]) + 
				(totalBeliefAboutSelf[YES] * compatibility[YES][NO]);
		message[YES] =
				(totalBeliefAboutSelf[NO] * compatibility[NO][YES]) + 
				(totalBeliefAboutSelf[YES] * compatibility[YES][YES]);
		
		normalizeProbabilityVector(message);
		
		allReceivedMessages.get(to).put(from, message);
	}
	
	private void normalizeProbabilityVector(double[] probabilities){
		if(probabilities.length != 2){
			throw new IllegalArgumentException();
		}
		double sum = probabilities[0] + probabilities[1];
		if(sum == 0){
			probabilities = new double[]{0.5,0.5};
		}else{
			probabilities[0] /= sum;
			probabilities[1] /= sum;
		}
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
	
	private double[] compatibility(int context1, int s1, int s2){

		if(context1 == NO){
			return new double[]{0.5, 0.5};
		}
		double relatedness = relatedness(s1, s2);
		double probContext = 1 / (1 + Math.exp( - relatedness)); 
		return new double[]{1 - probContext, probContext};
	}
	
	private double relatedness(int s1, int s2){
		double sim = similarity(s1, s2);
		if(s2 == s1 + 1){
			return sim + relatedToPrevious(s2);
		}
		if(s1 == s2 + 1){
			return sim + relatedToPrevious(s1);
		}
		return sim;
	}
	
	private double similarity(int s1, int s2){
		HashMap<String,Double> s1Vec = sentenceVectors.get(s1);
		HashMap<String,Double> s2Vec = sentenceVectors.get(s2);
		double cosSim = 0;
		if(s1Vec.size() > 0 && s2Vec.size() > 0){
			cosSim = CosineSimilarity.calculateCosineSimilarity(s1Vec, s2Vec);
		}
		
		HashMap<String,Double> bigramVec1 = bigramVectors.get(s1);
		HashMap<String,Double> bigramVec2 = bigramVectors.get(s2);

		double bigramSim = 0;
		if(bigramVec1.size() > 0 && bigramVec2.size() > 0){
			bigramSim = CosineSimilarity.calculateCosineSimilarity(bigramVec1, bigramVec2);
		}
		return cosSim + bigramSim; //Originally only cosSim
	}
	
	private double relatedToPrevious(int sentence){
		String[] words = sentenceTexts.get(sentence).split(" +");
		return 4 * ((Texts.instance().containsDetWork(words)? 1:0)
			+ (Texts.instance().startsWith3rdPersonPronoun(words)? 1:0)
			+ (Texts.instance().startsWithConnector(words)? 1:0));
	}
	
	private static class Result{
		private int truePositives;
		private int falsePositives;
		private int total;
		
		public Result(int truePositives, int falsePositives, int total){
			this.truePositives = truePositives;
			this.falsePositives = falsePositives;
			this.total = total;
		}
		
		public void add(Result other){
			truePositives += other.truePositives;
			falsePositives += other.falsePositives;
			total += other.total;
		}
	}
	
}
