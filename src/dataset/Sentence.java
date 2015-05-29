package dataset;


public class Sentence<T extends Text> {
//	public String sentiment;
	public SentenceType type;
	public T text;
	public int sentenceIndex; //In a specific paper (starts from 0)
	
	public Sentence(String sentiment, T text, int sentenceIndex){
		this.text = text;
//		this.sentiment = sentiment;
		this.type = typeFromSentiment(sentiment);
		this.sentenceIndex = sentenceIndex;
	}
	
	public Sentence(SentenceType type, T text, int sentenceIndex){
		this.text = text;
		this.type = type;
		this.sentenceIndex = sentenceIndex;
	}
	
	public static SentenceType typeFromSentiment(String sentiment){
		if(sentiment.equals("x") || sentiment.equals("xc")){ //there are some xc, I'm not sure why
			return SentenceType.NOT_REFERENCE;
		}
		if(sentiment.equals("oc") || sentiment.equals("pc") || sentiment.equals("nc")){
			return SentenceType.EXPLICIT_REFERENCE;
		}
		if(sentiment.equals("o") || sentiment.equals("p") || sentiment.equals("n")){
			return SentenceType.IMPLICIT_REFERENCE;
		}
		throw new RuntimeException("unknown AZ: " + sentiment);
	}
}
