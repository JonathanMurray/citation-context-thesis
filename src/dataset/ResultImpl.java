package dataset;

import java.util.ArrayList;
import java.util.List;

import weka.classifiers.evaluation.Prediction;



public class ResultImpl extends Result{
	private String label;
	private int truePositives;
	private int falsePositives;
	private int trueNegatives;
	private int falseNegatives;
	private long passedMillis;
	private List<Double> classificationProbabilities;
	private ArrayList<Prediction> predictions;
	
	public ResultImpl(String label){
		this(label, 0,0,0,0,new ArrayList<Double>(), 0, new ArrayList<Prediction>());
	}
	
	public ResultImpl(String label, int truePositives, int falsePositives, int trueNegatives, int falseNegatives, List<Double> classificationProbabilities, 
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
	
	public List<Double> classificationProbabilities(){
		return classificationProbabilities;
	}
	
	public String label(){
		return label;
	}
	
	public void add(ResultImpl other){
		if(!label.equals(other.label)){
			label = label + "+" + other.label;
		}
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		trueNegatives += other.trueNegatives;
		falseNegatives += other.falseNegatives;
		passedMillis += other.passedMillis;
		classificationProbabilities.addAll(other.classificationProbabilities);
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
	
	public static ResultImpl mergeMany(List<ResultImpl> results){
		ResultImpl merged = new ResultImpl("merged");
		for(ResultImpl result : results){
			merged.add(result);
		}
		return merged;
	}
}
