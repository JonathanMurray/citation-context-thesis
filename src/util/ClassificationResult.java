package util;

import java.util.List;

public abstract class ClassificationResult {
	
	public abstract long getPassedMillis();
	public abstract List<Integer> falsePositiveIndices();
	public abstract List<Integer> falseNegativeIndices();
	public abstract double[][] confusionMatrix();
	
	public double posPrecision(){
		double[][] m = confusionMatrix();
		double posPrecision = m[0][0] / (m[0][0] + m[1][0]);
		return posPrecision;
	}
	
	public double posRecall(){
		double[][] m = confusionMatrix();
		double posRecall = m[0][0] / (m[0][0] + m[0][1]);
		return posRecall;
	}
	
	public double negPrecision(){
		double[][] m = confusionMatrix();
		double negPrecision = m[1][1] / (m[1][1] + m[0][1]);
		return negPrecision;
	}
	
	public double negRecall(){
		double[][] m = confusionMatrix();
		double negRecall = m[1][1] / (m[1][1] + m[1][0]);
		return negRecall;
	}
	
	public String confusionMatrixToString(){
		double[][] m = confusionMatrix();
		return m[0][0] + "\t" + m[0][1] + "\n" +
			   m[1][0] + "\t" + m[1][1];
	}
	
	public double positiveFMeasure(double beta){
		return fMeasure(posPrecision(), posRecall(), beta);
	}
	
	public double negativeFMeasure(double beta){
		return fMeasure(negPrecision(), negRecall(), beta);
	}
	
	public double microAvgFMeasure(double beta){
		double[][] m = confusionMatrix();
		double totalTP = m[0][0] + m[1][1];
		double totalFP = m[1][0] + m[0][1];
		double totalFN = m[1][0] + m[0][1];
		double microPrecision = totalTP / (totalTP + totalFP);
		double microRecall = totalTP / (totalTP + totalFN);
		//TODO NOTE In the 2-class scenario, micro_precision == micro_recall
		return fMeasure(microPrecision, microRecall, beta);
	}
	
	public double macroAvgFMeasure(double beta){
		double macroPrecision = (posPrecision() + negPrecision()) / 2;
		double macroRecall = (posRecall() + negRecall()) / 2;
		return fMeasure(macroPrecision, macroRecall, beta);
	}
	
	protected double fMeasure(double precision, double recall, double beta){
		return (1+Math.pow(beta, 2)) * (precision*recall)/(Math.pow(beta, 2)*precision + recall);
	}
}
