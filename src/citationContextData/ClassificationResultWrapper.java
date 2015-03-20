package citationContextData;

import weka.classifiers.Evaluation;

public class ClassificationResultWrapper extends ClassificationResult{
	
	private String label;
	private Evaluation wekaEvaluation;
	private long passedMillis;
	
	public ClassificationResultWrapper(String label, Evaluation wekaEvaluation, long passedMillis){
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
}
