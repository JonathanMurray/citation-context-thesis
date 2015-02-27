package util;



public class ClassificationResultImpl implements ClassificationResult{
	private int truePositives;
	private int falsePositives;
	private int total;
	
	public ClassificationResultImpl(int truePositives, int falsePositives, int total){
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.total = total;
	}
	
	public void add(ClassificationResultImpl other){
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		total += other.total;
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append("precision: " + truePositives + "/" + (truePositives+falsePositives) + "\n");
		s.append("recall: " + truePositives + "/" + total + "\n");
		double precision = precision();
		double recall = recall();
		s.append("F-score: " + fMeasure(precision, recall, 1) + "\n");
		s.append("F2: " + fMeasure(precision, recall, 2));
		s.append("F3: " + fMeasure(precision, recall, 3));
		s.append("F4: " + fMeasure(precision, recall, 4));
		return s.toString();
	}
	
	public double precision(){
		return truePositives / ((double)truePositives + falsePositives);
	}
	
	public double recall(){
		return truePositives / (double)total;
	}
	
	public double fMeasure(){
		return fMeasure(1);
	}
	
	public double fMeasure(double beta){
		return fMeasure(precision(), recall(), beta);
	}
	
	private double fMeasure(double precision, double recall, double beta){
		return (1+Math.pow(beta, 2)) * (precision*recall)/(Math.pow(beta, 2)*precision + recall);
	}
}
