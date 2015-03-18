package citationContextData;


public class Sentence<T extends Text> {
	public String sentiment;
	public SentenceType type;
	public T text;
	
	public Sentence(String sentiment, T text){
		this.text = text;
		this.sentiment = sentiment;
		this.type = typeFromSentiment(sentiment);
	}
	
	public Sentence(SentenceType type, T text){
		this.text = text;
		this.type = type;
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
