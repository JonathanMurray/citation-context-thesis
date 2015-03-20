package dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public class Text {
	
	protected static final String XML_TEXT_CLASS = "text";
	
	public String raw;
	public List<String> rawWords;
	public List<String> lemmas;
	
	public Text(String raw, List<String> rawWords, List<String> lemmas) {
		this.raw = raw;
		this.rawWords = rawWords;
		this.lemmas = lemmas;
	}

	
	/**
	 * Create a Jsoup element representing this object.
	 * Should be overriden by subclasses
	 * @return
	 */
	protected Element toXml(){
		Element text = new Element(Tag.valueOf("text"), "");
		text.attr("class", XML_TEXT_CLASS);
		text.appendElement("raw").text(raw == null ? "" : raw);
		Element lemmasTag = text.appendElement("lemmas");
		StringBuilder lemmasString = new StringBuilder();
		for(String lemma : lemmas){
			lemmasString.append(lemma + " ");
		}
		if(lemmasString.length() > 0){
			lemmasTag.text(lemmasString.substring(0, lemmasString.length()-1));
		}else{
			lemmasTag.text("");
		}
		return text;
	}
	
	protected static Text fromXml(Element textTag){
		String raw = textTag.select("raw").text();
		ArrayList<String> rawWords = Texts.split(raw).collect(Collectors.toCollection(ArrayList::new));
		String lemmasString = textTag.select("lemmas").text();
		List<String> lemmas = Texts.split(lemmasString).collect(Collectors.toCollection(ArrayList::new));
		return new Text(raw, rawWords, lemmas);
	}
	
	public double similarity(Object other) {
		throw new UnsupportedOperationException();
	}
}
