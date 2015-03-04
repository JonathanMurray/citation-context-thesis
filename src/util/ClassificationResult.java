package util;

public abstract class ClassificationResult {
	public abstract double precision();
	public abstract double recall();
	public abstract double[][] confusionMatrix();
	public String confusionMatrixToString(){
		double[][] m = confusionMatrix();
		return m[0][0] + "\t" + m[0][1] + "\n" +
			   m[1][0] + "\t" + m[1][1];
	}
	
	public double fMeasure(){
		return fMeasure(1);
	}
	
	public double fMeasure(double beta){
		return fMeasure(precision(), recall(), beta);
	}
	
	protected double fMeasure(double precision, double recall, double beta){
		return (1+Math.pow(beta, 2)) * (precision*recall)/(Math.pow(beta, 2)*precision + recall);
	}
}
