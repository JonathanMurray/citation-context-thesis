package ml;

public class Sentence {
	String sentiment;
	String text;
	SentenceType type;
	
	public Sentence(String sentiment, String text){
		this.sentiment = sentiment;
		this.text = clean(text);
		this.type = typeFromSentiment(sentiment);
	}
	
	private String clean(String before){
		String after = before.replaceAll("[',:;%\\.\\(\\)]", "");
		after = after.trim();
		after = after.toLowerCase();
		after = after.replaceAll("\\d+", "<NUMBER>");
		return after;
	}
	
	private SentenceType typeFromSentiment(String sentiment){
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
