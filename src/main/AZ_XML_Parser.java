package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AZ_XML_Parser {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	
	public static final String TAG_SENTENCE = "S";
	public static final String TAG_REFERENCE = "REF";
	
	//Authoritative Zoning
	public static final String ATTR_AZ = "AZ";
	public static final String AZ_CONTRASTING = "CTR";
	public static final String AZ_OTHERS = "OTH";
	
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		AZ_XML_Parser reader = new AZ_XML_Parser();
		reader.parseSentences(new File(CFC_Dir + "9405001.cfc-scixml"));
	}

	public List<Sentence> parseSentences(File file) throws SAXException, IOException, ParserConfigurationException{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(file);
		NodeList sentenceTags = doc.getElementsByTagName(TAG_SENTENCE);
		int distanceToRecentReference = 0;
		Sentence recentExplicitReference = null;
		List<Sentence> sentences = new ArrayList<Sentence>();
		for(int i = 0; i < sentenceTags.getLength(); i++){
			Element sentenceTag = (Element) sentenceTags.item(i);
			Sentence sentence = parseSentence(sentenceTag, distanceToRecentReference, recentExplicitReference);
			switch(sentence.type){
			case EXPLICIT_REFERENCE:
				distanceToRecentReference = 1;
				recentExplicitReference = sentence; 
				break;
			case IMPLICIT_REFERENCE:
				distanceToRecentReference = 1;
				break;
			case NOT_REFERENCE:
				distanceToRecentReference ++;
			}
			sentences.add(sentence);
			System.out.println(sentence);
		}
		return sentences;
	}
	
	public Sentence parseSentence(Element sentenceTag, int distanceToRecentReference, Sentence recentExplicitReference){
		String AZ = sentenceTag.getAttributes().getNamedItem(ATTR_AZ).getNodeValue();
		boolean isReference = AZ.equals(AZ_CONTRASTING) || AZ.equalsIgnoreCase(AZ_OTHERS);
		String text = sentenceTag.getTextContent();
		if(isReference){
			NodeList referencesInSentence = sentenceTag.getElementsByTagName(TAG_REFERENCE);
			List<String> cited = new ArrayList<String>();
			for(int j = 0; j < referencesInSentence.getLength(); j++){
				String ref = referencesInSentence.item(j).getTextContent();
				cited.add(ref);
			}
			boolean isExplicitReference = !cited.isEmpty();
			if(isExplicitReference){
				return Sentence.createExplicit(text, AZ, cited);
			}else{
				//A problem with this approach is when the work of a new author is mentioned,
				//before the author is introduced. The algorithm might then assume that the 
				//first sentences (before the author is cited explicitly) belong to a previous
				//explicit citation, which is in fact not true.
				if(distanceToRecentReference <= 2 && recentExplicitReference != null){
					return Sentence.createImplicit(text, AZ, recentExplicitReference);
				}
			}
		}
		return Sentence.createNonReference(text, AZ);
	}
	
	public static enum SentenceType{
		EXPLICIT_REFERENCE,
		IMPLICIT_REFERENCE,
		NOT_REFERENCE;
	}
	
	public static class Sentence{
		final SentenceType type;
		final List<String> cited;
		final Sentence explicitReference;
		final String text;
		final String AZ;
		
		Sentence(String AZ, SentenceType type, List<String> cited, Sentence explicitReference, String text){
			this.type = type;
			this.cited = cited;
			this.explicitReference = explicitReference;
			this.text = text;
			this.AZ = AZ;
		}
		
		public static Sentence createExplicit(String text, String AZ, List<String> cited){
			return new Sentence(AZ, SentenceType.EXPLICIT_REFERENCE, cited, null, text);
		}
		
		public static Sentence createImplicit(String text, String AZ, Sentence explicit){
			return new Sentence(AZ, SentenceType.IMPLICIT_REFERENCE, null, explicit, text);
		}
		
		public static Sentence createNonReference(String text, String AZ){
			return new Sentence(AZ, SentenceType.NOT_REFERENCE, null, null, text);
		}
		
		public String toString(){
			switch(type){
			case EXPLICIT_REFERENCE:
				return "Explicit ref (" + AZ + ") to " + cited + ":  " + text;
			case IMPLICIT_REFERENCE:
				return "Implicit ref (" + AZ + ") to " + explicitReference.cited + ":  " + text;
			case NOT_REFERENCE:
				return "Not reference (" + AZ + "):   " + text;
			default:
				throw new RuntimeException();	
			}
		}
		
	}
}
