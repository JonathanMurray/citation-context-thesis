package parscit_test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class XML_Reader {
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		File f = new File("/home/jonathan/Documents/exjobb/ParsCit/demodata/sample2.txt.out");
		new XML_Reader().parseFile(f);
	}
	
	public void parseFile(File f) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(f);
		NodeList citations = doc.getElementsByTagName("citation");
		for(int i = 0; i < citations.getLength(); i++){
			CitationData citationData = parseCitation((Element)citations.item(i));
			System.out.println(citationData);
		}
	}
	
	private CitationData parseCitation(Element citationElement){
		CitationData citation = new CitationData();
		citation.title = parseCitationTitle(citationElement);
		citation.authors = parseCitationAuthors(citationElement);
		citation.contexts = parseCitationContexts(citationElement);
	
		return citation;
	}
	
	private String parseCitationTitle(Element citation){
		return citation.getElementsByTagName("title").item(0).getTextContent();
	}
	
	private List<String> parseCitationAuthors(Element citation){
		ArrayList<String> authors = new ArrayList<String>();
		NodeList authorNodes = citation.getElementsByTagName("author");
		for(int i = 0; i < authorNodes.getLength(); i++){
			String author = authorNodes.item(i).getTextContent();
			authors.add(author);
		}
		return authors;
	}
	
	private List<ContextData> parseCitationContexts(Element citation){
		List<ContextData> contexts = new ArrayList<ContextData>();
		NodeList contextNodes = citation.getElementsByTagName("context");
		for(int i = 0; i < contextNodes.getLength(); i++){
			Element contextNode = (Element) contextNodes.item(i);
			ContextData context = new ContextData();
			String contextText = contextNode.getTextContent();
			String citStr = contextNode.getAttribute("citStr");
			int citationCharIndex = contextText.indexOf(citStr);
			int citationEndCharIndex = citationCharIndex + citStr.length() - 1;
			context.textBefore = getTextBeforeCitation(contextText, citationCharIndex);
			context.sentence = getCitationSentence(contextText, citationCharIndex, citationEndCharIndex);
			contexts.add(context);
		}
		return contexts;
	}
	
	private String getTextBeforeCitation(String context, int citationCharIndex){
		String beforeCit = context.substring(0, citationCharIndex);
		String[] wordsBefore = beforeCit.split(" ");
		if(wordsBefore[wordsBefore.length - 1].matches("\\(,?")){
			wordsBefore = Arrays.copyOfRange(wordsBefore, 0, wordsBefore.length - 1);
		}
		return lastNWords(wordsBefore, 10);
	}
	
	private String getCitationSentence(String context, int citationCharIndex, int citationEndCharIndex){
		int i = -1;
		int start = 0;
		int end = context.length();
		while(true){
			i = context.indexOf('.', i + 1);
			boolean sentenceBoundaryNotFound = i == -1;
			if(sentenceBoundaryNotFound){ 
				break;
			}
			if(i < citationCharIndex){
				start = i + 1;
			}else if(i > citationEndCharIndex){
				end = i + 1;
				break;
			}
		}
		return context.substring(start, end);
	}
	
	private String lastNWords(String[] words, int n){
		StringBuilder s = new StringBuilder();
		for(int i = Math.min(n, words.length); i > 0; i--){
			s.append(words[words.length - i]).append(" ");
		}
		return s.toString().trim();
	}

	private static class CitationData{
		public String title;
		public List<String> authors;
		public List<ContextData> contexts;
		
		public String toString(){
			return "Title: " + title + "\n" + 
					"Authors: " + Arrays.toString(authors.toArray()) + "\n" + 
					"# Contexts: " + contexts.size() + "\n" +
					"Contexts: \n" + Arrays.toString(contexts.toArray()) + "\n";
		}
	}
	
	private static class ContextData{
		String textBefore;
		String sentence;
		public String toString(){
			return "before: " + textBefore + "\n" + 
					"sentence: " + sentence + "\n";
		}
	}
	
	
}
