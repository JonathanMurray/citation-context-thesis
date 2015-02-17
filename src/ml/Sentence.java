package ml;

import java.util.ArrayList;
import java.util.List;

public class Sentence{
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
		return new Sentence(AZ, SentenceType.IMPLICIT_REFERENCE, new ArrayList<String>(), explicit, text);
	}
	
	public static Sentence createNonReference(String text, String AZ){
		return new Sentence(AZ, SentenceType.NOT_REFERENCE, new ArrayList<String>(), null, text);
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