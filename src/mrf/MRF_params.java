package mrf;

public class MRF_params {
	
	private static int DEFAULT_NEIGHBOURHOOD = 2;
	private static double DEFAULT_BELIEF_THRESHOLD = 0.4;
	
	final int neighbourhood;
	final double beliefThreshold;
	
	
	public MRF_params(int neighbourhood, double beliefThreshold) {
		this.neighbourhood = neighbourhood;
		this.beliefThreshold = beliefThreshold;
	}
	
	public MRF_params(){
		this(DEFAULT_NEIGHBOURHOOD, DEFAULT_BELIEF_THRESHOLD);
	}
	
}
