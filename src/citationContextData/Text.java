package citationContextData;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import util.Lemmatizer;
import util.Texts;

public class Text {
	public String raw;
	public String lemmatized;
	public List<String> lemmatizedWords;
	
	public Text(String raw, String lemmatized, List<String> lemmatizedWords) {
		this.raw = raw;
		this.lemmatized = lemmatized;
		this.lemmatizedWords = lemmatizedWords;
	}
	
	public Text(String raw){
		this.raw = raw;
		lemmatizedWords = Lemmatizer.instance().lemmatize(raw);
		lemmatized = Texts.merge(lemmatizedWords);
	}
	
	public Text(String raw, String lemmatized){
		this.raw = raw;
		this.lemmatized = lemmatized;
		lemmatizedWords = Texts.split(lemmatized);
	}
	
	/**
	 * Create a Jsoup element representing this object.
	 * Should be overriden by subclasses
	 * @return
	 */
	protected Element toXml(){
		Element text = new Element(Tag.valueOf("text"), "");
		text.attr("class", "text");
		text.appendElement("raw").text(raw == null ? "" : raw);
		text.appendElement("lemmatized").text(lemmatized == null ? "" : lemmatized);
		return text;
	}
	
	protected static Text fromXml(Element textTag){
		String raw = textTag.select("raw").text();
		String lemmatized = textTag.select("lemmatized").text();
		return new Text(raw, lemmatized);
	}
	
}
