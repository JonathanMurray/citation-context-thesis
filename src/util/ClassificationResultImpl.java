package util;

import java.util.ArrayList;
import java.util.List;



public class ClassificationResultImpl extends ClassificationResult{
	private int truePositives;
	private int falsePositives;
	private int trueNegatives;
	private int falseNegatives;
	private List<Integer> fpIndices;
	private List<Integer> fnIndices;
	private long passedMillis;
	
	public ClassificationResultImpl(){
		this(0,0,0,0,new ArrayList<Integer>(),new ArrayList<Integer>(), 0);
	}
	
	public ClassificationResultImpl(int truePositives, int falsePositives, int trueNegatives, int falseNegatives, List<Integer> fpIndices, List<Integer> fnIndices, long passedMillis){
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.trueNegatives = trueNegatives;
		this.falseNegatives = falseNegatives;
		this.fpIndices = fpIndices;
		this.fnIndices = fnIndices;
		this.passedMillis = passedMillis;
	}
	
	public void add(ClassificationResultImpl other){
		truePositives += other.truePositives;
		falsePositives += other.falsePositives;
		trueNegatives += other.trueNegatives;
		falseNegatives += other.falseNegatives;
		fpIndices.addAll(other.fpIndices);
		fnIndices.addAll(other.fnIndices);
	}
	
	@Override
	public double[][] confusionMatrix() {
		return new double[][]{
				new double[]{	truePositives, 	falseNegatives}, 
				new double[]{	falsePositives, trueNegatives}
		};
	}

	@Override
	public List<Integer> falsePositiveIndices() {
		return fpIndices;
	}

	@Override
	public List<Integer> falseNegativeIndices() {
		return fnIndices;
	}

	@Override
	public long getPassedMillis() {
		return passedMillis;
	}
}
