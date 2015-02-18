package ml;

import java.util.ArrayList;
import java.util.List;

public class OldSentence{
//	final SentenceType type;
//	final List<String> cited;
//	final OldSentence explicitReference;
//	final String text;
//	final String AZ;
//	
//	OldSentence(String AZ, SentenceType type, List<String> cited, OldSentence explicitReference, String text){
//		this.type = type;
//		this.cited = cited;
//		this.explicitReference = explicitReference;
//		this.text = text;
//		this.AZ = AZ;
//	}
//	
//	public static OldSentence createExplicit(String text, String AZ, List<String> cited){
//		return new OldSentence(AZ, SentenceType.EXPLICIT_REFERENCE, cited, null, text);
//	}
//	
//	public static OldSentence createImplicit(String text, String AZ, OldSentence explicit){
//		return new OldSentence(AZ, SentenceType.IMPLICIT_REFERENCE, new ArrayList<String>(), explicit, text);
//	}
//	
//	public static OldSentence createNonReference(String text, String AZ){
//		return new OldSentence(AZ, SentenceType.NOT_REFERENCE, new ArrayList<String>(), null, text);
//	}
//	
//	public String toString(){
//		switch(type){
//		case EXPLICIT_REFERENCE:
//			return "Explicit ref (" + AZ + ") to " + cited + ":  " + text;
//		case IMPLICIT_REFERENCE:
//			return "Implicit ref (" + AZ + ") to " + explicitReference.cited + ":  " + text;
//		case NOT_REFERENCE:
//			return "Not reference (" + AZ + "):   " + text;
//		default:
//			throw new RuntimeException();	
//		}
//	}
	
}