package dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import weka.classifiers.evaluation.Prediction;

/**
 * Implementation of the abstract Result class
 * @author jonathan
 *
 * @param <T>
 */
public class ResultImpl<T extends Text> extends Result{
	private String label;
	private int truePositives;
	private int falsePositives;
	private int trueNegatives;
	private int falseNegatives;
	private long passedMillis;
	private HashMap<SentenceKey<T>, Double> classificationProbabilities;
	private ArrayList<Prediction> predictions;
	
	public ResultImpl(String label){
		this(label, 0,0,0,0,new HashMap<SentenceKey<T>, Double>(), 0, new ArrayList<Prediction>());
	}
	
	public ResultImpl(String label, int truePositives, int falsePositives, int trueNegatives, int falseNegatives, HashMap<SentenceKey<T>, Double> classificationProbabilities, 
			long passedMillis, ArrayList<Prediction> predicitions){
		this.label = label;
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.trueNegatives = trueNegatives;
		this.falseNegatives = falseNegatives;
		this.passedMillis = passedMillis;
		this.classificationProbabilities = classificationProbabilities;
		this.predictions = predicitions;
	}
	
	public HashMap<SentenceKey<T>, Double> classificationProbabilities(){
		return classificationProbabilities;
	}
	
	public String label(){
		return label;
	}
	
	public void add(ResultImpl<T> other){
		if(!label.equals(other.label)){
			label = label + "+" + other.label;
		}
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		trueNegatives += other.trueNegatives;
		falseNegatives += other.falseNegatives;
		passedMillis += other.passedMillis;
		classificationProbabilities.putAll(other.classificationProbabilities);
		predictions.addAll(other.predictions);
	}
	
	@Override
	public double[][] confusionMatrix() {
		return new double[][]{
				new double[]{	truePositives, 	falseNegatives}, 
				new double[]{	falsePositives, trueNegatives}
		};
	}

	@Override
	public long getPassedMillis() {
		return passedMillis;
	}

	@Override
	public ArrayList<Prediction> predictions() {
		return predictions;
	}
	
	public static <T2 extends Text> ResultImpl<T2> mergeMany(List<ResultImpl<T2>> results){
		ResultImpl<T2> merged = new ResultImpl<T2>("merged");
		for(ResultImpl<T2> result : results){
			merged.add(result);
		}
		return merged;
	}
}
