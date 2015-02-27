package util;

import weka.classifiers.Evaluation;

public class ClassificationResultWrapper implements ClassificationResult{
	
	private Evaluation wekaEvaluation;
	private final static int POSITIVE_CLASS = 0;
	
	public ClassificationResultWrapper(Evaluation wekaEvaluation){
		this.wekaEvaluation = wekaEvaluation;
	}

	@Override
	public double precision() {
		return wekaEvaluation.precision(POSITIVE_CLASS);
	}

	@Override
	public double recall() {
	return wekaEvaluation.recall(POSITIVE_CLASS);
	}

	@Override
	public double fMeasure() {
		return wekaEvaluation.weightedFMeasure();
	}
	
	
}
