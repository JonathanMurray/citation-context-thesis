package mrf;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import util.ClassificationResult;
import util.ClassificationResultImpl;
import util.Printer;
import util.Texts;
import util.Timer;
import citationContextData.Dataset;
import citationContextData.LexicalHook;
import citationContextData.Sentence;
import citationContextData.SentenceType;
import citationContextData.Text;


public class MRF_classifier<T extends Text> {
	
	protected static Printer printer = new Printer(true);
	private static final double DELTA = 0.02;
	private static final int MAX_RUNS = 10;
	private static final int NO = 0;
	private static final int YES = 1;
	
	protected final MRF_params params;
	protected Dataset<T> data;
	protected List<Sentence<T>> sentences; //For the citer of the current iteration
	
	private double minSimilarity;
	private double maxSimilarity;
	
//	private double minNeighbourSimilarity;
//	private double maxNeighbourSimilarity;
	
	private List<TIntDoubleHashMap> relatednessMemoization;
	protected List<double[]> selfBeliefs;
	protected List<Map<Integer,double[]>> allReceivedMessages;
	
	public MRF_classifier(MRF_params params){
		this.params = params;
	}
	
	public List<ClassificationResult> classify(Collection<Dataset<T>> datasets){
		System.out.println("Classifying multiple datasets ...");
		List<ClassificationResult> results = new ArrayList<ClassificationResult>();
		for(Dataset<T> dataset : datasets){
			results.add(classify(dataset));
		}
		return results;
	}
	
	public ClassificationResultImpl classify(Dataset<T> dataset){
		ClassificationResultImpl result = new ClassificationResultImpl();
		System.out.println("\nMRF classifying " + dataset.datasetLabel + ":");
		System.out.println("ACRONYMS: " + dataset.getAcronyms());
		System.out.println("HOOKS: " + dataset.getLexicalHooks()); 
		printer.print("Classifying citers: ");
		for(int i = 0; i < dataset.citers.size(); i++){
			printer.progress(i, 1);
			result.add(classifyOneCiter(i, dataset));
//			if(i == 3){
//				break; //TODO
//			}
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
//				printer.println("Done after " + i + " iterations.");
				break;
			}
		}
		return getResults(params.beliefThreshold, t.getMillis());
	}
	
	private void setup(int citerIndex, Dataset<T> dataset){
		this.data = dataset;
		this.sentences = dataset.citers.get(citerIndex).sentences;
		
		int numSentences = sentences.size();
		
		relatednessMemoization = new ArrayList<TIntDoubleHashMap>();
		for(int i = 0; i < numSentences; i++){
			relatednessMemoization.add(new TIntDoubleHashMap());
		}
		
//		setupRelatednessNormalization();
		
		TDoubleArrayList similarities = getSimilarities(dataset.citedContent, dataset.citedTitle, dataset.mergedExplicitCitations);
		
		selfBeliefs = new ArrayList<double[]>();
		List<Double> unnormalizedBeliefs = new ArrayList<Double>();
		for(int i = 0; i < numSentences; i++){
			double similarToCited = similarities.get(i);
			double unnormalizedBelief = selfBelief(sentences.get(i), dataset.citedMainAuthor, similarToCited, dataset.getAcronyms(), dataset.getLexicalHooks());
			unnormalizedBeliefs.add(unnormalizedBelief);
		}
		
		double maxBelief = 1.0; //If the highest belief sentence 
		//gets all its belief from similarity, we don't want to increase it to 1
		double minBelief = Double.MAX_VALUE;
		int maxIndex = 0;
		for(int i = 0; i < numSentences; i++){
			if(sentences.get(i).type == SentenceType.EXPLICIT_REFERENCE){
				continue; // Don't let explicit citations raise the max-belief
			}
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
			if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
				normalized = 1.0; //TODO set explicit sentences to 100% probability
			}else{
				if(maxBelief > minBelief){
					normalized = (unnormalizedBelief - minBelief) / (maxBelief - minBelief);
				}else{
					System.out.println(maxBelief + " !> " + minBelief);
					normalized = 0.5;
				}

				
				
				
				List<String> rawWords = Texts.split(sentence.text.raw)
						.collect(Collectors.toCollection(ArrayList::new));
				
				boolean containsOtherRefs = Texts.instance().containsOtherReferencesButNotThis(sentence.text.raw, 
						rawWords, data.citedMainAuthor); 
				boolean sectionHeader = Texts.instance().startsWithSectionHeader(rawWords);
				
				
				if(containsOtherRefs){
					normalized -= 0.2;
//					if(sentence.type == SentenceType.IMPLICIT_REFERENCE){
//						System.out.println(sentence.type + ":");
//						System.out.println(sentence.text.raw);
//						System.out.println();
//					}
				}
				if(sectionHeader){
					normalized -= 0.3;
//					if(sentence.type == SentenceType.IMPLICIT_REFERENCE){
//						System.out.println(sentence.type + ":");
//						System.out.println(sentence.text.raw);
//						System.out.println();
//					}
				}
				
				
				
				
				final double MIN_ALLOWED = 0.2; //TODO
				if(normalized < MIN_ALLOWED){
					normalized = MIN_ALLOWED; 
				}
			}
			selfBeliefs.add(new double[]{1 - normalized, normalized});
		}
	}
	
//	private void setupRelatednessNormalization(){
//		minNeighbourSimilarity = Double.MAX_VALUE;
//		maxNeighbourSimilarity = Double.MIN_VALUE;
//		int numSentences = sentences.size();
//		for(int from = 0; from < numSentences; from++){
//			int leftmostNeighbour = Math.max(0, from - params.neighbourhood);
//			int rightmostNeighbour = Math.min(numSentences - 1, from + params.neighbourhood);
//			for(int to = leftmostNeighbour; to <= rightmostNeighbour; to++){
//				if(to != from){
//					T fromText = sentences.get(from).text;
//					T toText = sentences.get(to).text;
//					double rel = fromText.similarity(toText);
//					minNeighbourSimilarity = Math.min(minNeighbourSimilarity, rel);
//					maxNeighbourSimilarity = Math.max(maxNeighbourSimilarity, rel);
//				}
//			}
//		}
//	}
	
	private TDoubleArrayList getSimilarities(T citedContent, T citedTitle, T mergedExplicitCitations){
		TDoubleArrayList similarities = new TDoubleArrayList();
		minSimilarity = Double.MAX_VALUE;
		maxSimilarity = Double.MIN_VALUE;
		for(Sentence<T> s : sentences){
			double sim = s.text.similarity(citedContent) + s.text.similarity(citedTitle)
					+ s.text.similarity(mergedExplicitCitations);
			minSimilarity = Math.min(minSimilarity, sim);
			maxSimilarity = Math.max(maxSimilarity, sim);
			similarities.add(sim);
		}
		for(int i = 0; i < similarities.size(); i++){
			double normalized = (similarities.get(i) - minSimilarity) / (maxSimilarity-minSimilarity);
			similarities.set(i, normalized);
		}
		
		
		
		
		
		return similarities;
	}
	
	private double selfBelief(
			Sentence<T> sentence, 
			String authorLastName, 
			double similarity, 
			List<String> acronyms,
			List<LexicalHook> lexicalHooks){
		
		List<String> rawWords = sentence.text.rawWords;
		
		double score = 0; 
		Printer p = new Printer(false);
		
//		if(sentence.type == SentenceType.NOT_REFERENCE && Texts.instance().containsHookWithIndex(sentence.text.raw, lexicalHooks)){
//			p.enabled = true;
//		}
		p.println("\n\n" + sentence.text.raw); //TODO
		
//		if(Texts.instance().containsExplicitCitation(words, authorLastName)){
//			score +=  params.selfBelief.explicitCitWeight;
//		}
		
		p.println("Similarity: " + similarity);
		
		if(Texts.instance().containsMainAuthor(rawWords, authorLastName)){
			score += 1.5;
			p.println("contains main author"); //TODO
		}
		
		
		
//		if(Texts.instance().startsWithDetWork(words)){
//			score += params.selfBelief.detWorkWeight;
//		}
//		if(Texts.instance().startsWithLimitedDet(words)){
//			score += params.selfBelief.limitedDetWeight;
//		}
		score += similarity;
		
//		boolean hooks = Texts.instance().containsHookWithIndex(sentence.text.raw, lexicalHooks);
//		boolean acronym = Texts.instance().containsAcronymWithIndex(rawWords, acronyms); 
		
		int hookIndex = Texts.instance().containsHookWithIndex(sentence.text.raw, lexicalHooks);
		int acronymIndex = Texts.instance().containsAcronymWithIndex(rawWords, acronyms);
		int bestIndex = -1;
		if(hookIndex > -1 && acronymIndex > -1){
			bestIndex = Math.min(hookIndex, acronymIndex);
		}else if(hookIndex != -1){
			bestIndex = hookIndex;
		}else if(acronymIndex != -1){
			bestIndex = acronymIndex;
		}
		
		if(bestIndex != -1){
			score += 1.5/((double)bestIndex+1);
		}
		
//		if(words.get(0).equals("It")){
//			score += params.selfBelief.itWeight;
//		}
//		
		
		if(Double.isNaN(score)){
			throw new RuntimeException();
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
	
	private ClassificationResultImpl getResults(double beliefThreshold, long passedMillis){
		int truePos = 0;
		int falsePos = 0;
		int trueNeg = 0;
		int falseNeg = 0;
		
		ArrayList<Integer> fpIndices = new ArrayList<Integer>();
		ArrayList<Integer> fnIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < sentences.size(); i++){
			Sentence<T> sentence = sentences.get(i);
			double[] belief = finalBelief(i);
			if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
//				System.out.println();
//				System.out.println("EXPLICIT: " + sentence.text.raw);
//				System.out.println();
				continue; //Don't count explicit citations in result!
			}

			DecimalFormat f = new DecimalFormat("#.##");
//			System.out.println( sentence.type + " (" + f.format(selfBeliefs.get(i)[1]) + " -> " + f.format(belief[1]) +  "):   " + sentence.text.raw); //TODO
			
			//TODO ignore those too far away from explicit
			boolean closeToExplicit = false;
			final int LIMIT = 3;
			for(int j = Math.max(0, i-LIMIT); j < Math.min(sentences.size()-1, i+LIMIT); j++){
				if(sentences.get(j).type==SentenceType.EXPLICIT_REFERENCE){
					closeToExplicit = true;
				}
			}
			
			double HIGHER_THRESH = 0.98;
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
					
					
					
//					if(sentences.get(i).type == SentenceType.IMPLICIT_REFERENCE){
//						Sentence prev=  null;
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
//						System.out.println("cited title  : " + s.text.similarity(data.citedTitle));
//						System.out.println("citations    : " + s.text.similarity(data.mergedExplicitCitations));
//						System.out.println("Related to prev: " + relatednessToPrevious(s.text));
//						System.out.println();
//						List<String> rawWords = Texts.split(sentence.text.raw).collect(Collectors.toCollection(ArrayList::new));
//						if(Texts.instance().containsOtherReferencesButNotThis(sentence.text.raw, rawWords, data.citedMainAuthor)){
//							System.out.println("contains other ref!");
//						}
//						if(Texts.instance().startsWithSectionHeader(rawWords)){
//							System.out.println("section header!");
//						}
//					}

					fnIndices.add(i);
					falseNeg ++;
				}
			}
		}
		
		return new ClassificationResultImpl(truePos, falsePos, trueNeg, falseNeg, fpIndices, fnIndices, passedMillis);	}
	
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
				(Math.pow(totalBeliefAboutSelf[NO], 2) * compatibility[NO][NO]) + //TODO different from original (^ 2)
				(Math.pow(totalBeliefAboutSelf[YES], 2) * compatibility[YES][NO]);
		message[YES] =
				(Math.pow(totalBeliefAboutSelf[NO], 2) * compatibility[NO][YES]) + 
				(Math.pow(totalBeliefAboutSelf[YES], 2) * compatibility[YES][YES]);
		
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
		
		double relatedness = relatedness(s1, s2);
		double probSame;
		
		if(context1 == NO){
			probSame = 0.5 + (Math.pow(relatedness,2) / 4);
			return new double[]{probSame,1-probSame};
		}
		
//		System.out.println(relatedness); //TODO
//		double probSame = 1 / (1 + Math.exp( - relatedness)); //interval : [0.5 , 1]
		probSame = 0.5 + (Math.pow(relatedness,2) / 2);
		if(probSame > 1.01){
			System.err.println("compatability for " + s1 + ", " + s2 + " == " + probSame);//TODO
		}
//		probSame -= 0.1; //interval : [0.4 , 0.9]
//		System.out.println(probContext); //TODO
		return new double[]{1 - probSame, probSame};
	}
	
	private double relatedness(int s1, int s2){
		
		if(relatednessMemoization.get(s1).get(s2) != 0){
			return relatednessMemoization.get(s1).get(s2);
		}
		if(relatednessMemoization.get(s2).get(s1) != 0){
			return relatednessMemoization.get(s2).get(s1);
		}
		
		T t1 = sentences.get(s1).text;
		T t2 = sentences.get(s2).text;
		
//		double relatedness = (t1.similarity(t2) - minNeighbourSimilarity) / (maxNeighbourSimilarity - minNeighbourSimilarity);;
		double relatedness = 0;
		if(s2 == s1 + 1){
			relatedness = relatednessToPrevious(t2);
		}else if(s1 == s2 + 1){
			relatedness = relatednessToPrevious(t1);
		}
		
		relatedness = Math.max(relatedness, t1.similarity(t2));
		relatednessMemoization.get(s1).put(s2, relatedness);
		return relatedness;
	}
	
	private double relatednessToPrevious(T text){
		
		if(Texts.instance().startsWithConnector(text.rawWords)){
			return 0.9;
		}
		
		if(Texts.instance().containsDetWork(text.rawWords)){
			return 0.8;
		}
		
		if(Texts.instance().startsWith3rdPersonPronoun(text.rawWords)){
			return 0.8;
		}
		
		if(Texts.instance().startsWithDet(text.rawWords)){
			return 0.7;
		}
		
		if(Texts.instance().containsDet(text.rawWords) || text.rawWords.get(0).equals("It")){
			return 0.6;
		}
		
		return 0.4; //the fact they are next to each other
	}
	
}
