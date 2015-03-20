package citationContextData;

import java.util.ArrayList;
import java.util.List;



public class ClassificationResultImpl extends ClassificationResult{
	private String label;
	private int truePositives;
	private int falsePositives;
	private int trueNegatives;
	private int falseNegatives;
	private List<Integer> fpIndices;
	private List<Integer> fnIndices;
	private long passedMillis;
	
	public ClassificationResultImpl(String label){
		this(label, 0,0,0,0,new ArrayList<Integer>(),new ArrayList<Integer>(), 0);
	}
	
	public ClassificationResultImpl(String label, int truePositives, int falsePositives, int trueNegatives, int falseNegatives, List<Integer> fpIndices, List<Integer> fnIndices, long passedMillis){
		this.label = label;
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.trueNegatives = trueNegatives;
		this.falseNegatives = falseNegatives;
		this.fpIndices = fpIndices;
		this.fnIndices = fnIndices;
		this.passedMillis = passedMillis;
	}
	
	public String label(){
		return label;
	}
	
	public void add(ClassificationResultImpl other){
		if(!label.equals(other.label)){
			label = label + "+" + other.label;
		}
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		trueNegatives += other.trueNegatives;
		falseNegatives += other.falseNegatives;
		fpIndices.addAll(other.fpIndices);
		fnIndices.addAll(other.fnIndices);
		passedMillis += other.passedMillis;
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
}
