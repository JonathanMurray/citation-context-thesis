package mrf;

public class MRF_params {
	
	private static int DEFAULT_NEIGHBOURHOOD = 2;
	private static double DEFAULT_BELIEF_THRESHOLD = 0.7;
	
	final int neighbourhood;
	final double beliefThreshold;
	final SelfBelief selfBelief;
	final Relatedness relatedness;
	
	
	public MRF_params(int neighbourhood, double beliefThreshold, SelfBelief selfBelief, Relatedness relatedness) {
		this.neighbourhood = neighbourhood;
		this.beliefThreshold = beliefThreshold;
		this.selfBelief = selfBelief;
		this.relatedness = relatedness;
	}
	
	public MRF_params(){
		this(DEFAULT_NEIGHBOURHOOD, DEFAULT_BELIEF_THRESHOLD, new SelfBelief(), new Relatedness());
	}
	
	public static class SelfBelief{
//		private static double DEFAULT_EXPLICITCIT_WEIGHT = 3;
		private static double DEFAULT_AUTHOR_WEIGHT = 3;
		private static double DEFAULT_DETWORK_WEIGHT = 1;
		private static double DEFAULT_LIMITEDDET_WEIGHT = 1;
		private static double DEFAULT_ACRONYM_WEIGHT = 1;
		private static double DEFAULT_HOOKS_WEIGHT = 1;
		private static double DEFAULT_IT_WEIGHT = 1;
		private static double DEFAULT_HEADER_WEIGHT = -2;
		
//		final double explicitCitWeight;
		final double authorWeight;
		final double detWorkWeight;
		final double limitedDetWeight;
		final double acronymWeight;
		final double hooksWeight;
		final double itWeight;
		final double headerWeight;
		
		public SelfBelief(double authorWeight, double detWorkWeight,
				double limitedDetWeight, double acronymWeight, double hooksWeight, 
				double itWeight, double headerWeight) {
//			this.explicitCitWeight = explicitCitWeight;
			this.authorWeight = authorWeight;
			this.detWorkWeight = detWorkWeight;
			this.limitedDetWeight = limitedDetWeight;
			this.acronymWeight = acronymWeight;
			this.hooksWeight = hooksWeight;
			this.itWeight = itWeight;
			this.headerWeight = headerWeight;
		}
		
		public SelfBelief(){
			this(DEFAULT_AUTHOR_WEIGHT, DEFAULT_DETWORK_WEIGHT, 
					DEFAULT_LIMITEDDET_WEIGHT, DEFAULT_ACRONYM_WEIGHT, 
					DEFAULT_HOOKS_WEIGHT, 
					DEFAULT_IT_WEIGHT, DEFAULT_HEADER_WEIGHT);
		}
	}
	
	public static class Relatedness{
		
		private static double DEFAULT_DETWORK_WEIGHT = 1;
		private static double DEFAULT_PRONOUN_WEIGHT = 1;
		private static double DEFAULT_CONNECTOR_WEIGHT = 1;
		private static double DEFAULT_IT_WEIGHT = 1;
//		private static double DEFAULT_SIMILARITY_WEIGHT = 100;
		
		final double detWorkWeight;
		final double pronounWeight;
		final double connectorWeight;
		final double itWeight;
//		final double similarityWeight;
		
		public Relatedness(double detWorkWeight, double pronounWeight, 
				double connectorWeight, double itWeight) {
			this.detWorkWeight = detWorkWeight;
			this.pronounWeight = pronounWeight;
			this.connectorWeight = connectorWeight;
			this.itWeight = itWeight;
//			this.similarityWeight = similarityWeight;
		}
		
		public Relatedness(){
			this(DEFAULT_DETWORK_WEIGHT, DEFAULT_PRONOUN_WEIGHT, DEFAULT_CONNECTOR_WEIGHT, DEFAULT_IT_WEIGHT);
		}
	}
}
