package mrf;

public class MRF_params {
	
	private static int DEFAULT_NEIGHBOURHOOD = 3;
	private static double DEFAULT_BELIEF_THRESHOLD = 0.4;
	private static int DEFAULT_MAX_RUNS = 10;
	
	final int neighbourhood;
	final double beliefThreshold;
	final int maxRuns;
	
	
	public MRF_params(int neighbourhood, double beliefThreshold, int maxRuns) {
		this.neighbourhood = neighbourhood;
		this.beliefThreshold = beliefThreshold;
		this.maxRuns = maxRuns;
	}
	
	public MRF_params(){
		this(DEFAULT_NEIGHBOURHOOD, DEFAULT_BELIEF_THRESHOLD, DEFAULT_MAX_RUNS);
	}
	
}
