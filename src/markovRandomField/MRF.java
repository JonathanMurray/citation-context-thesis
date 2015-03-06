package markovRandomField;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import util.ClassificationResultImpl;
import util.CosineSimilarity;
import util.Printer;
import util.Texts;
import util.Timer;
import citationContextData.Citer;
import citationContextData.SentenceClass;
import citationContextData.Dataset;


public class MRF {
	
	private static final int NO = 0;
	private static final int YES = 1;
	
	private static Printer printer = new Printer(true);
	
	private int numSentences;
	protected List<HashMap<String,Double>> unigramVectors;
	protected List<HashMap<String,Double>> bigramVectors;
	protected List<String> sentenceTexts;
	protected List<SentenceClass> sentenceTypes;
	List<double[]> beliefs;
	List<Map<Integer,double[]>> allReceivedMessages;
	
	public final static int DEFAULT_NEIGHBOURHOOD = 4;
	private int neighbourhood;
	
	public MRF(){
		this(DEFAULT_NEIGHBOURHOOD);
	}
	
	public MRF(int neighbourhood){
		this.neighbourhood = neighbourhood;
	}
	
	public ClassificationResultImpl classify(Dataset dataset){
		ClassificationResultImpl result = new ClassificationResultImpl();
		printer.print("MRF classifying citers: ");
		for(int i = 0; i < dataset.citers.size(); i++){
			printer.progress(i, 1);
			result.add(classifyOneCiter(i, dataset));
		}
		System.out.println();
		return result;
	}
	
	public ClassificationResultImpl classifyOneCiter(int citerIndex, Dataset dataset){
		Citer citer = dataset.citers.get(citerIndex);
		List<String> sentences = citer.sentences.stream().sequential()
				.map(s -> s.text)
				.collect(Collectors.toCollection(ArrayList::new));
		
		List<SentenceClass> sentenceTypes = citer.sentences.stream().sequential()
				.map(s -> s.type)
				.collect(Collectors.toCollection(ArrayList::new));
		
		return classifyOneCiter(sentences, sentenceTypes, dataset.citedMainAuthor, dataset.citedContentUnigrams, dataset);
	}
	
	public ClassificationResultImpl classifyOneCiter(
			List<String> sentenceTexts, 
			List<SentenceClass> sentenceTypes, 
			String mainAuthor, 
			HashMap<String,Double> citedContentNGrams, 
			Dataset dataset){

		Timer t = new Timer();
		
		setup(sentenceTexts, sentenceTypes, mainAuthor, citedContentNGrams, dataset);
		initMessages();
		for(int i = 0; i < 5; i++){
			oneLoop();	
		}
		
		return getClassificationResults(0.7, t.getMillis());
	}
	
	private void setup(List<String> sentenceTexts, List<SentenceClass> sentenceTypes, String mainAuthor, HashMap<String,Double> citedContentUnigrams, Dataset dataset){
		
		this.sentenceTexts = sentenceTexts;
		this.sentenceTypes = sentenceTypes;
		unigramVectors = new ArrayList<HashMap<String,Double>>();
		bigramVectors = new ArrayList<HashMap<String,Double>>();
		beliefs = new ArrayList<double[]>();
		numSentences = sentenceTexts.size();
		
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		
		for(String text : sentenceTexts){
			HashMap<String, Double> sentenceUnigrams = Texts.instance().getNgrams(1, text, true, true); 
			unigramVectors.add(sentenceUnigrams);
			bigramVectors.add(Texts.instance().getNgrams(2, text, true, true));
			unnormalizedBeliefs.add(selfBelief(text, sentenceUnigrams, mainAuthor, citedContentUnigrams, dataset.acronyms, dataset.lexicalHooks));
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
	
	private static double selfBelief(
			String sentence, 
			HashMap<String,Double> sentenceUnigrams, 
			String mainAuthor, 
			HashMap<String,Double> citedContentUnigrams, 
			Set<String> acronyms,
			Set<String> lexicalHooks){
		
		String[] words = sentence.split("\\s+");
		double explicit = Texts.instance().containsExplicitReference(Arrays.asList(words), mainAuthor) ? 2:0;
		double detWork = Texts.instance().startsWithDetWork(words) ? 0:0;
		double det = Texts.instance().startsWithLimitedDet(words) ? 0:0;
		double cosSim = 0;
		if(sentenceUnigrams.size() > 0){
			cosSim = CosineSimilarity.calculateCosineSimilarity(sentenceUnigrams, citedContentUnigrams);
		}
		double acr = Texts.instance().containsAcronyms(sentence, acronyms) ? 1:0;
		double hooks = Texts.instance().containsLexicalHooks(sentence, lexicalHooks)? 1:0;
		
		return 0.01 + explicit + detWork + det + cosSim + acr + hooks; //TODO mult. cossim
	}
	
	private ClassificationResultImpl getClassificationResults(double beliefThreshold, long passedMillis){
		int truePos = 0;
		int falsePos = 0;
		int trueNeg = 0;
		int falseNeg = 0;
		
		ArrayList<Integer> fpIndices = new ArrayList<Integer>();
		ArrayList<Integer> fnIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < numSentences; i++){
			double[] belief = finalBelief(i);
//			System.out.println(sentenceTypes.get(i) + " - " + beliefToString(beliefs.get(i)) + " -> " + beliefToString(belief) + ":  " + sentenceTexts.get(i));
			if(belief[1] > beliefThreshold){
				if(sentenceTypes.get(i) == SentenceClass.NOT_REFERENCE){
					fpIndices.add(i);
					falsePos ++;
				}else{
					truePos ++;
				}
			}else{
				if(sentenceTypes.get(i) == SentenceClass.NOT_REFERENCE){
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
	
	protected double similarity(int s1, int s2){
		HashMap<String,Double> s1Vec = unigramVectors.get(s1);
		HashMap<String,Double> s2Vec = unigramVectors.get(s2);
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
		String[] words = sentenceTexts.get(sentence).split("\\s+");
		return 4 * ((Texts.instance().containsDetWork(words)? 1:0)
			+ (Texts.instance().startsWith3rdPersonPronoun(words)? 1:0)
			+ (Texts.instance().startsWithConnector(words)? 1:0));
	}
	
	

	
}
