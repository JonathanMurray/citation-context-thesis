package sentenceFeaturesToWeka;

import util.Texts;

public class Sentence {
	public String sentiment;
	public String text;
	public SentenceClass type;
	
	public Sentence(String sentiment, String text){
		this.sentiment = sentiment;
		this.text = clean(text);
		this.type = typeFromSentiment(sentiment);
	}
	
	public Sentence(SentenceClass type, String text){
		this.type = type;
		this.text = clean(text);
	}
	
	private String clean(String before){
		String after = before.replaceAll("[',:;%\\.\\(\\)\\~\\\\\\[\\]\\{\\}\\/]", " ")
			.replaceAll("\n", " ")
			.replaceAll(" +", " ")
			.trim()
			.replaceAll("\\d+", Texts.NUMBER_TAG);
		return after;
	}
	
	private SentenceClass typeFromSentiment(String sentiment){
		if(sentiment.equals("x") || sentiment.equals("xc")){ //there are some xc, I'm not sure why
			return SentenceClass.NOT_REFERENCE;
		}
		if(sentiment.equals("oc") || sentiment.equals("pc") || sentiment.equals("nc")){
			return SentenceClass.EXPLICIT_REFERENCE;
		}
		if(sentiment.equals("o") || sentiment.equals("p") || sentiment.equals("n")){
			return SentenceClass.IMPLICIT_REFERENCE;
		}
		throw new RuntimeException("unknown AZ: " + sentiment);
	}
}
