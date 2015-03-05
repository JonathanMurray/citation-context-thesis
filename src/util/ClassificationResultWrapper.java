package util;

import java.util.List;

import weka.classifiers.Evaluation;

public class ClassificationResultWrapper extends ClassificationResult{
	
	private Evaluation wekaEvaluation;
	private List<Integer> falsePositives;
	private List<Integer> falseNegatives;
	
	public ClassificationResultWrapper(Evaluation wekaEvaluation, List<Integer> falsePositives, List<Integer> falseNegatives){
		this.wekaEvaluation = wekaEvaluation;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
	}

	@Override
	public double[][] confusionMatrix() {
		return wekaEvaluation.confusionMatrix();
	}

	@Override
	public List<Integer> falsePositiveIndices() {
		return falsePositives;
	}

	@Override
	public List<Integer> falseNegativeIndices() {
		return falseNegatives;
	}
}
