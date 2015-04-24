package mrf;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import util.Printer;
import util.Timer;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import dataset.Dataset;
import dataset.LexicalHook;
import dataset.ResultImpl;
import dataset.Sentence;
import dataset.SentenceType;
import dataset.Text;
import dataset.Texts;


public class Original_MRF_classifier<T extends Text> {
	
	protected static Printer printer = new Printer(false);
	private static final double DELTA = 0.02;
	private static final int NO = 0;
	private static final int YES = 1;
	
	int DEBUG_FALSE_NEG = 0;
	int DEBUG_FALSE_NEG_AFTER_EXPL = 0;
	
	TDoubleIntHashMap EXPL_BELIEF_CHANGES = new TDoubleIntHashMap();
	TDoubleIntHashMap IMPL_BELIEF_CHANGES = new TDoubleIntHashMap();
	TDoubleIntHashMap NO_CITE_BELIEF_CHANGES = new TDoubleIntHashMap();
	
	double minSimilarity;
	double maxSimilarity;
	
	protected final MRF_params params;
	protected Dataset<T> data;
	protected List<Sentence<T>> sentences; //For the citer of the current iteration
	
	
	private HashMap<Integer, Double> DEBUG_SIMILARITIES = new HashMap<Integer,Double>();
	
	private List<TIntDoubleHashMap> relatednessMemoization;
	protected List<double[]> selfBeliefs;
	protected List<Map<Integer,double[]>> allReceivedMessages;
	
	public Original_MRF_classifier(MRF_params params){
		this.params = params;
	}
	
	public List<ResultImpl> classify(Collection<Dataset<T>> datasets){
		System.out.println("Classifying multiple datasets ...");
		List<ResultImpl> results = new ArrayList<ResultImpl>();
		for(Dataset<T> dataset : datasets){
			results.add(classify(dataset));
		}
		System.out.println("expl: " + EXPL_BELIEF_CHANGES);
		System.out.println("impl: " + IMPL_BELIEF_CHANGES);
		System.out.println("no cite: " + NO_CITE_BELIEF_CHANGES);
		return results;
	}
	
	public ResultImpl classify(Dataset<T> dataset){
		ResultImpl sumResult = new ResultImpl(dataset.datasetLabel);
		System.out.print("\nMRF classifying " + dataset.datasetLabel + "  ");
		System.out.print(dataset.getAcronyms() + ", ");
		System.out.print(dataset.getLexicalHooks()); 
		printer.print(" ... ");
		printer.resetProgress();
		DEBUG_FALSE_NEG = 0;
		DEBUG_FALSE_NEG_AFTER_EXPL = 0;
		for(int i = 0; i < dataset.citers.size(); i++){
			printer.progress();
			ResultImpl res = classifyOneCiter(i, dataset);
			sumResult.add(res);
		}
		System.out.println();
		System.out.println("False neg: " + DEBUG_FALSE_NEG);
		System.out.println("False neg after expl: " + DEBUG_FALSE_NEG_AFTER_EXPL);
		printer.println(" pos F(1):" + Printer.toString(sumResult.positiveFMeasure(1)) + ", pos F(3):" + Printer.toString(sumResult.positiveFMeasure(3)));
		printer.println(sumResult.confusionMatrixToString());
		return sumResult;
	}
	
	public ResultImpl classifyOneCiter(int citerIndex, Dataset<T> dataset){
		Timer t = new Timer();
		setup(citerIndex, dataset);
		initMessages();
		int run = 0;
		while(true){ 
			if(params.maxRuns > -1 && run >= params.maxRuns){
				break;
			}
			boolean anyChange = iterate();
			if(!anyChange){
				printer.println("Done after " + run + " iterations.");
				break;
			}
			
			run++;
		}
		return getResults(dataset.datasetLabel, params.beliefThreshold, t.getMillis());
	}
	
	private void setup(int citerIndex, Dataset<T> dataset){
		this.data = dataset;
		this.sentences = dataset.citers.get(citerIndex).sentences;
		
		int numSentences = sentences.size();
		
		relatednessMemoization = new ArrayList<TIntDoubleHashMap>();
		for(int i = 0; i < numSentences; i++){
			relatednessMemoization.add(new TIntDoubleHashMap());
		}
		
		TDoubleArrayList similarities = getSimilarities(dataset.citedContent, dataset.citedTitle, dataset.mergedExplicitCitations);
		
		selfBeliefs = new ArrayList<double[]>();
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		for(int i = 0; i < numSentences; i++){
			double similarity = similarities.get(i);
			double unnormalizedBelief = selfBelief(i, sentences.get(i), dataset.citedMainAuthor, similarity, dataset.getAcronyms(), dataset.getLexicalHooks());
			unnormalizedBeliefs.add(unnormalizedBelief);
		}
		
		double maxBelief = 0;  
		double minBelief = Double.MAX_VALUE;
		int maxIndex = 0;
		for(int i = 0; i < numSentences; i++){
//			if(sentences.get(i).type == SentenceType.EXPLICIT_REFERENCE){
//				continue; // Don't let explicit citations raise the max-belief
//			}
			double unnormalized = unnormalizedBeliefs.get(i);
			if(unnormalized < minBelief){
				minBelief = unnormalized;
			}
			if(unnormalized > maxBelief){
				maxBelief = unnormalized;
				maxIndex = i;
			}
		}
//		System.out.println("belief interval: " + minBelief + " --> " + maxBelief);
//		System.out.println("max belief sentence: " + sentences.get(maxIndex).text.raw);
		//TODO
		
		for(int i = 0; i < numSentences; i++){
			double unnormalizedBelief = unnormalizedBeliefs.get(i);
			double normalized;
			Sentence<T> sentence = sentences.get(i);
			if(maxBelief > minBelief){
				normalized = (unnormalizedBelief - minBelief) / (maxBelief - minBelief);
			}else{
				System.out.println(maxBelief + " !> " + minBelief);
				normalized = 0.5;
			}
//			if(sentence.type != SentenceType.NOT_REFERENCE){
//				System.out.println("\n" + data.citedMainAuthor + "   " +  sentence.text.raw);
//				System.out.println(sentence.type);
//				System.out.println("sim: " + DEBUG_SIMILARITIES.get(i));
//				System.out.println("belief: " + normalized);	
//			}
			
			selfBeliefs.add(new double[]{1 - normalized, normalized});
		}
	}
	
	private TDoubleArrayList getSimilarities(T citedContent, T citedTitle, T mergedExplicitCitations){
		TDoubleArrayList similarities = new TDoubleArrayList();
		minSimilarity = Double.MAX_VALUE;
		maxSimilarity = Double.MIN_VALUE;
		for(Sentence<T> s : sentences){
			double sim = s.text.similarity(citedContent);
//					+ s.text.similarity(citedTitle)
//					+ s.text.similarity(mergedExplicitCitations);
			minSimilarity = Math.min(minSimilarity, sim);
			maxSimilarity = Math.max(maxSimilarity, sim);
			similarities.add(sim);
		}
		
		//Normalization
		for(int i = 0; i < similarities.size(); i++){
			double normalized;
			if(minSimilarity == maxSimilarity){
				System.out.println("minsim == maxsim == " + minSimilarity);
				normalized = 0.5;
			}else{
				normalized = (similarities.get(i) - minSimilarity) / (maxSimilarity-minSimilarity);
			}
			if(Double.isNaN(normalized)){
				throw new RuntimeException("min:" + minSimilarity + ", max:" + maxSimilarity + ", i=" + i);
			}
			similarities.set(i, normalized); //TODO
		}
		return similarities;
	}
	
	private double selfBelief(
			int sentenceIndex,
			Sentence<T> sentence, 
			String authorLastName, 
			double similarity, 
			List<String> acronyms,
			List<LexicalHook> lexicalHooks){
		
		List<String> rawWords = sentence.text.rawWords;
		
		Printer p = new Printer(false);
		
		double score = 0;
		
		
		score += similarity;
		
		p.println("\n\n" + sentence.text.raw); //TODO
		p.println("Similarity: " + similarity);
		DEBUG_SIMILARITIES.put(sentenceIndex, similarity);
		
		if(Texts.instance().containsExplicitCitation(rawWords, authorLastName)){
			score += 1;
			p.println("contains main author"); //TODO
		}
		
		if(Texts.instance().mrfContainsDetWork(rawWords) || Texts.instance().startsWithLimitedDet(rawWords)){
			score += 1;
		}
		
//		if(Texts.instance().startsWithLimitedDet(rawWords)){
//			score += 1;
//		}
		
		
		
		
		if(Double.isNaN(score)){
			throw new RuntimeException("score == NaN");
		}
		
		return score;
	}
	
	private void initMessages(){
		allReceivedMessages = new ArrayList<Map<Integer,double[]>>();
		int numSentences = sentences.size();
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
	
	private ResultImpl getResults(String label, double beliefThreshold, long passedMillis){
		int truePos = 0;
		int falsePos = 0;
		int trueNeg = 0;
		int falseNeg = 0;
		
		ArrayList<Integer> fpIndices = new ArrayList<Integer>();
		ArrayList<Integer> fnIndices = new ArrayList<Integer>();
		ArrayList<Double> classificationProbabilities = new ArrayList<Double>();
		ArrayList<Prediction> predictions = new ArrayList<Prediction>();
		
		for(int i = 0; i < sentences.size(); i++){
			Sentence<T> sentence = sentences.get(i);
			double[] belief = finalBelief(i);
			classificationProbabilities.add(belief[1]);
			
			double beliefChange = belief[1] - selfBeliefs.get(i)[1];
			double roundedChange = (double)(Math.round(beliefChange * 20.0)) / 20.0;
			if(roundedChange > 0.25){
				roundedChange = 0.3; //Put them all in same bucket
			}
			
			if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
				EXPL_BELIEF_CHANGES.adjustOrPutValue(roundedChange, 1, 1);
			}else if(sentence.type == SentenceType.IMPLICIT_REFERENCE){
				IMPL_BELIEF_CHANGES.adjustOrPutValue(roundedChange, 1, 1);
			}else if(sentence.type == SentenceType.NOT_REFERENCE){
				NO_CITE_BELIEF_CHANGES.adjustOrPutValue(roundedChange, 1, 1);
			}
			
			
			if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
//				System.out.println();
//				System.out.println("EXPLICIT: " + sentence.text.raw);
//				System.out.println();
				continue; //Don't count explicit citations in result!
			}

			DecimalFormat f = new DecimalFormat("#.##");
//			System.out.println( sentence.type + " (" + f.format(selfBeliefs.get(i)[1]) + " -> " + f.format(belief[1]) +  "):   " + sentence.text.raw); //TODO
			
			//TODO ignore those too far away from explicit
//			boolean closeToExplicit = false;
//			final int LIMIT = 3;
//			for(int j = Math.max(0, i-LIMIT); j < Math.min(sentences.size()-1, i+LIMIT); j++){
//				if(sentences.get(j).type==SentenceType.EXPLICIT_REFERENCE){
//					closeToExplicit = true;
//				}
//			}
			
			
			if(sentence.type == SentenceType.NOT_REFERENCE){
				predictions.add(new NominalPrediction(0.0, belief));
			}else{
				predictions.add(new NominalPrediction(1.0, belief));
			}
			
//			double HIGHER_THRESH = 0.98;
			boolean predictInContext = belief[1] > beliefThreshold;// && closeToExplicit || belief[1] > HIGHER_THRESH;
			if(predictInContext){
				if(sentence.type == SentenceType.NOT_REFERENCE){
					
//					Sentence prev=  null;
//					if(i > 0){
//						prev = sentences.get(i-1);
//					}
					
//					System.out.println();
//					System.out.println("close to explicit: " + closeToExplicit);
//					if(i > 0){
//						System.out.println(prev.type + "(" + f.format(selfBeliefs.get(i-1)[1]) + " -> " + f.format(finalBelief(i-1)[1]) +  "):   " + prev.text.raw);
//					}
//					
//					System.out.println("FP (" + f.format(selfBeliefs.get(i)[1]) + " -> " + f.format(belief[1]) +  "):   " + sentence.text.raw); //TODO
//					System.out.println(sentence.text.lemmas);
//					System.out.println();
//					fnIndices.add(i);
					
					
					
					fpIndices.add(i);
					falsePos ++;
				}else{
					truePos ++;
				}
			}else{
				if(sentence.type == SentenceType.NOT_REFERENCE){
					trueNeg ++;
				}else{
					
					
					
					DEBUG_FALSE_NEG ++;
					if(sentences.get(i).type == SentenceType.IMPLICIT_REFERENCE){
						if(sentences.get(i-1).type == SentenceType.EXPLICIT_REFERENCE){
							DEBUG_FALSE_NEG_AFTER_EXPL ++;
						}
					}
					
//					if(sentences.get(i).type == SentenceType.IMPLICIT_REFERENCE){
//						Sentence<T> prev=  null;
//						if(i > 0){
//							prev = sentences.get(i-1);
//						}
//						Sentence<T> s = sentences.get(i);
//						System.out.println("\n");
//						System.out.println("PREV (" + prev.type + "  " + f.format(selfBeliefs.get(i-1)[1]) 
//								+ " -> " + f.format(finalBelief(i-1)[1]) + "): " + prev.text.raw);
//						System.out.println("(" + data.citedMainAuthor + "), " + data.citedTitle.raw);
//						System.out.println("(" + f.format(selfBeliefs.get(i)[1]) + " -> " 
//								+ f.format(belief[1]) +  "):   " + sentence.text.raw); 
//						System.out.println(s.text.lemmas);
//						System.out.println("cited content: " + s.text.similarity(data.citedContent));
////						System.out.println("cited title  : " + s.text.similarity(data.citedTitle));
////						System.out.println("citations    : " + s.text.similarity(data.mergedExplicitCitations));
//						System.out.println();
//					}

					fnIndices.add(i);
					falseNeg ++;
				}
			}
		}
		
		return new ResultImpl(label, truePos, falsePos, trueNeg, falseNeg, classificationProbabilities, passedMillis, predictions);	
	}
	
	private double[] finalBelief(int sentence){
		double[] productReceived = productOfValues(allReceivedMessages.get(sentence));
		double[] belief = selfBeliefs.get(sentence);
		double[] totalBeliefAboutSelf = new double[]{
				belief[NO] * productReceived[NO], 
				belief[YES] * productReceived[YES]};
		normalizeProbabilityVector(totalBeliefAboutSelf);
		
//		Sentence<T> s = sentences.get(sentence);
//		if(s.type == SentenceType.IMPLICIT_REFERENCE){
//			System.out.println(sentence + ". " + s.type + "\t" + s.text.rawWords);
//			NumberFormat f = new DecimalFormat("#0.00"); 
//			System.out.println(f.format(belief[1]) + " -> " + f.format(totalBeliefAboutSelf[1]));
//			System.out.println();
//		}
			
		return totalBeliefAboutSelf;
	}
	
	private String beliefToString(double[] belief){
		NumberFormat formatter = new DecimalFormat("#0.00");     
		return formatter.format(belief[1]);
	}

	private boolean iterate(){
		int numSentences = sentences.size();
		boolean anyChange = false;
		for(int from = 0; from < numSentences; from++){
			double[] belief = selfBeliefs.get(from);
			
//			System.out.println(from + ": ");
//			System.out.println(sentences.get(from).text.raw);
//			System.out.println("sim: " + DEBUG_SIMILARITIES.get(from));
//			System.out.println("belief: " + beliefToString(belief) + "\n");
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
				totalBeliefAboutSelf[NO] * compatibility[NO][NO] + 
				totalBeliefAboutSelf[YES] * compatibility[YES][NO];
		message[YES] =
				totalBeliefAboutSelf[NO] * compatibility[NO][YES]+ 
				totalBeliefAboutSelf[YES] * compatibility[YES][YES];
		
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
		T t1 = sentences.get(s1).text;
		T t2 = sentences.get(s2).text;
		
		
		if(context1 == NO){
			return new double[]{0.5,0.5};
		}
		
		double similarity = t1.similarity(t2);
		double probSame = 1.0 / (1 + Math.exp( - similarity)); //interval : [0.5 , 1]
		if(probSame > 1.01){
			System.err.println("ERROR: compatibility for " + s1 + ", " + s2 + " == " + probSame);//TODO
		}
//		probSame -= 0.1; //interval : [0.4 , 0.9]
//		System.out.println(probContext); //TODO
		return new double[]{1 - probSame, probSame};
	}
	
//	private double relatedness(int s1, int s2){
//		
//		if(relatednessMemoization.get(s1).get(s2) != 0){
//			return relatednessMemoization.get(s1).get(s2);
//		}
//		if(relatednessMemoization.get(s2).get(s1) != 0){
//			return relatednessMemoization.get(s2).get(s1);
//		}
//		
//		T t1 = sentences.get(s1).text;
//		T t2 = sentences.get(s2).text;
//		
////		double relatedness = (t1.similarity(t2) - minNeighbourSimilarity) / (maxNeighbourSimilarity - minNeighbourSimilarity);
////		double relatedness = 0;
////		if(s2 == s1 + 1){
////			relatedness = relatednessToPrevious(t2);
////		}else if(s1 == s2 + 1){
////			relatedness = relatednessToPrevious(t1);
////		}
//		double relatedness = t1.similarity(t2);
////		double similarity = (unnormalizedSimilarity - minSimilarity)/(maxSimilarity-minSimilarity);
////		relatedness = Math.max(relatedness, similarity);
//		relatednessMemoization.get(s1).put(s2, relatedness);
//		return relatedness;
//	}
	
//	private double relatednessToPrevious(T text){
//		
//		if(Texts.instance().startsWithConnector(text.rawWords)){
//			return 0.9;
//		}
//		
//		if(Texts.instance().containsDetWork(text.rawWords)){
//			return 0.8;
//		}
//		
//		if(Texts.instance().startsWith3rdPersonPronoun(text.rawWords)){
//			return 0.8;
//		}
//		
//		if(Texts.instance().startsWithDet(text.rawWords)){
//			return 0.7;
//		}
//		
//		if(Texts.instance().containsDet(text.rawWords) || text.rawWords.get(0).equals("It")){
//			return 0.6;
//		}
//		
//		return 0.4; //the fact they are next to each other
//	}
	
}
