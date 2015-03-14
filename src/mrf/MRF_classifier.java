package mrf;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.ClassificationResultImpl;
import util.Printer;
import util.Texts;
import util.Timer;
import citationContextData.CitingPaper;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.SentenceType;
import citationContextData.Text;


public abstract class MRF_classifier<T extends Text> {
	
	protected static Printer printer = new Printer(true);
	private static final double DELTA = 0.05;
	private static final int MAX_RUNS = 50;
	private static final int NO = 0;
	private static final int YES = 1;
	
	protected final MRF_params params;
	protected Dataset<T> data;
	protected CitingPaper<T> currentCiter;
	
	private List<TIntDoubleHashMap> relatednessMemoization;
	protected List<double[]> beliefs;
	protected List<Map<Integer,double[]>> allReceivedMessages;
	
	public MRF_classifier(MRF_params params){
		this.params = params;
	}
	
	public ClassificationResultImpl classify(Dataset<T> dataset){
		ClassificationResultImpl result = new ClassificationResultImpl();
		printer.print("MRF classifying citers: ");
		for(int i = 0; i < dataset.citers.size(); i++){
			printer.progress(i, 1);
			result.add(classifyOneCiter(i, dataset));
		}
		printer.println("");
		return result;
	}
	
	public ClassificationResultImpl classifyOneCiter(int citerIndex, Dataset<T> dataset){
		Timer t = new Timer();
		setup(citerIndex, dataset);
		initMessages();
		for(int i = 0; i < MAX_RUNS; i++){ 
			boolean anyChange = iterate();
			if(!anyChange){
				break;
			}
		}
		return getClassificationResults(params.beliefThreshold, t.getMillis());
	}
	
	private void setup(int citerIndex, Dataset<T> dataset){
		
		this.data = dataset;
		this.currentCiter = dataset.citers.get(citerIndex);
		
		int numSentences = currentCiter.sentences.size();
		
		relatednessMemoization = new ArrayList<TIntDoubleHashMap>();
		for(int i = 0; i < numSentences; i++){
			relatednessMemoization.add(new TIntDoubleHashMap());
		}
		
		beliefs = new ArrayList<double[]>();
		
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		
		for(int i = 0; i < numSentences; i++){
			T text = currentCiter.sentences.get(i).text;
			unnormalizedBeliefs.add(selfBelief(text, dataset.citedMainAuthor, dataset.citedContent, dataset.getAcronyms(), dataset.getLexicalHooks()));
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
			beliefs.add(new double[]{1 - normalized, normalized});
		}
	}
	
	private double selfBelief(
			T sentence, 
			String authorLastName, 
			T citedContent, 
			Set<String> acronyms,
			Set<String> lexicalHooks){
		
		double score = 0.01; //Ensure not 0
		
		List<String> words = sentence.lemmatizedWords;
		String[] wordsArray = words.toArray(new String[0]);
		if(Texts.instance().containsExplicitCitation(words, authorLastName)){
			score +=  params.selfBelief.explicitCitWeight;
		}
		if(Texts.instance().startsWithDetWork(wordsArray)){
			score += params.selfBelief.detWorkWeight;
		}
		if(Texts.instance().startsWithLimitedDet(wordsArray)){
			score += params.selfBelief.limitedDetWeight;
		}
		score += similarity(sentence, citedContent);
		if(Texts.instance().containsAcronyms(sentence.lemmatized, acronyms)){
			score += params.selfBelief.acronymWeight;
		}
		if(Texts.instance().containsLexicalHooks(sentence.lemmatized, lexicalHooks)){
			score += params.selfBelief.hooksWeight;
		}
		return score;
	}
	
	private ClassificationResultImpl getClassificationResults(double beliefThreshold, long passedMillis){
		int truePos = 0;
		int falsePos = 0;
		int trueNeg = 0;
		int falseNeg = 0;
		
		ArrayList<Integer> fpIndices = new ArrayList<Integer>();
		ArrayList<Integer> fnIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < currentCiter.sentences.size(); i++){
			Sentence<T> sentence = currentCiter.sentences.get(i);
			double[] belief = finalBelief(i);
			if(belief[1] > beliefThreshold){
				if(sentence.type == SentenceType.NOT_REFERENCE){
					fpIndices.add(i);
					falsePos ++;
				}else{
					truePos ++;
				}
			}else{
				if(sentence.type == SentenceType.NOT_REFERENCE){
					trueNeg ++;
				}else{
					fnIndices.add(i);
					falseNeg ++;
				}
			}
		}
		
		return new ClassificationResultImpl(truePos, falsePos, trueNeg, falseNeg, fpIndices, fnIndices, passedMillis); //TODO
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

	private void initMessages(){
		allReceivedMessages = new ArrayList<Map<Integer,double[]>>();
		int numSentences = currentCiter.sentences.size();
		for(int s = 0; s < numSentences; s++){
			Map<Integer, double[]> receivedMessages = new HashMap<Integer, double[]>();
			for(int m = Math.max(0, s-params.neighbourhood); m <= Math.min(s+params.neighbourhood, numSentences-1); m++){
				if(m != s){
					receivedMessages.put(m, new double[]{0.5,0.5}); //start value for msg
				}
			}
			allReceivedMessages.add(receivedMessages);
		}
	}
	
	private boolean iterate(){
		int numSentences = currentCiter.sentences.size();
		boolean anyChange = false;
		for(int from = 0; from < numSentences; from++){
			double[] belief = beliefs.get(from);
			Map<Integer, double[]> receivedMessages = allReceivedMessages.get(from);
			int leftmostNeighbour = Math.max(0, from - params.neighbourhood);
			int rightmostNeighbour = Math.min(numSentences - 1, from + params.neighbourhood);
			for(int to = leftmostNeighbour; to <= rightmostNeighbour; to++){
				if(to != from){
					boolean msgChanged = sendMessage(from, to, receivedMessages, belief);
					if(msgChanged){
						anyChange = true;
					}
				}
			}
		}
		return anyChange;
	}
	
	private boolean sendMessage(int from, int to, Map<Integer, double[]> receivedMessages, double[] belief){
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
		
		boolean msgChanged = false;
		if(allReceivedMessages.get(to).containsKey(from)){
			double[] prevMsg = allReceivedMessages.get(to).get(from);
			
			if(Math.abs(prevMsg[0] - message[0]) > DELTA || Math.abs(prevMsg[1] - message[1]) > DELTA){
				msgChanged = true;
			}
		}
		
		allReceivedMessages.get(to).put(from, message);
		return msgChanged;
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
		if(relatednessMemoization.get(s1).get(s2) != 0){
			return relatednessMemoization.get(s1).get(s2);
		}
		if(relatednessMemoization.get(s2).get(s1) != 0){
			return relatednessMemoization.get(s2).get(s1);
		}
		T t1 = currentCiter.sentences.get(s1).text;
		T t2 = currentCiter.sentences.get(s2).text;
		double relatedness = similarity(t1, t2);
		if(s2 == s1 + 1){
			relatedness += relatednessToPrevious(t2);
		}else if(s1 == s2 + 1){
			relatedness += relatednessToPrevious(t1);
		}
		relatednessMemoization.get(s1).put(s2, relatedness);
		return relatedness;
	}
	

	protected abstract double similarity(T s1, T s2);
	
	private double relatednessToPrevious(T text){
		String[] words = text.lemmatizedWords.toArray(new String[0]);
		return 4 * ((Texts.instance().containsDetWork(words)? 1:0)
			+ (Texts.instance().startsWith3rdPersonPronoun(words)? 1:0)
			+ (Texts.instance().startsWithConnector(words)? 1:0));
	}
	
}
