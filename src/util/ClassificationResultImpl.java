package util;



public class ClassificationResultImpl extends ClassificationResult{
	private int truePositives;
	private int falsePositives;
	private int trueNegatives;
	private int falseNegatives;
	
	public ClassificationResultImpl(int truePositives, int falsePositives, int trueNegatives, int falseNegatives){
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.trueNegatives = trueNegatives;
		this.falseNegatives = falseNegatives;
	}
	
	public void add(ClassificationResultImpl other){
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		trueNegatives += other.trueNegatives;
		falseNegatives += other.falseNegatives;
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append("precision: " + truePositives + "/" + (truePositives+falsePositives) + "\n");
		s.append("recall: " + truePositives + "/" + (truePositives+falseNegatives) + "\n");
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
		return truePositives / ((double)truePositives + falseNegatives);
	}
	
	@Override
	public double[][] confusionMatrix() {
		return new double[][]{
				new double[]{	truePositives, 	falseNegatives}, 
				new double[]{	falsePositives, trueNegatives}
		};
	}
}
