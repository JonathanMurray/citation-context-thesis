package dataset;

import java.util.ArrayList;

import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.Prediction;

public class ResultWrapper extends Result{
	
	private String label;
	private Evaluation wekaEvaluation;
	private long passedMillis;
	
	public ResultWrapper(String label, Evaluation wekaEvaluation, long passedMillis){
		this.label = label;
		this.wekaEvaluation = wekaEvaluation;
		this.passedMillis = passedMillis;
	}
	
	public String label(){
		return label;
	}

	@Override
	public double[][] confusionMatrix() {
		return wekaEvaluation.confusionMatrix();
	}

	@Override
	public long getPassedMillis() {
		return passedMillis;
	}

	@Override
	public ArrayList<Prediction> predictions() {
		return wekaEvaluation.predictions();
	}
}
