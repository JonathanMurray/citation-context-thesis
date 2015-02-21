package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import sentenceFeaturesToWeka.Sentence;
import sentenceFeaturesToWeka.SentenceType;

public class Texts {
	
	private List<String> determiners;
	private List<String> workNouns;
	private List<String> thirdPersonPronouns;
	private List<String> connectors;
	
	public static final String HEADER_PATTERN = "\\d+\\.\\d+.*";
	
	private static Texts instance;
	
	public static Texts instance(){
		if(instance == null){
			instance = new Texts();
		}
		return instance;
	}
	
	private Texts(){
		try {
			setup();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void setup() throws IOException{
		String packageName = getClass().getPackage().getName();
		determiners = readLines("src/" + packageName + "/data/determinerWords.txt");
		workNouns = readLines("src/" + packageName + "/data/workNouns.txt");
		thirdPersonPronouns = readLines("src/" + packageName + "/data/thirdPersonPronouns.txt");
		connectors = readLines("src/" + packageName + "/data/connectors.txt");
	}
	
	private List<String> readLines(String filePath) throws IOException{
		return Files.lines(Paths.get(filePath)).collect(Collectors.toList());
	}
	
	public boolean containsDetWork(String[] words){
		for(int i = 1; i < words.length; i++){
			if(looseContains(workNouns, words[i])){
				if(looseContains(determiners, words[i-1])){
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean startsWith3rdPersonPronoun(String[] words){
		return looseContains(thirdPersonPronouns, words[0]);
	}
	
	public boolean startsWithConnector(String[] words){
		return looseContains(connectors, words[0]);
	}
	
	public boolean isAfterExplicitReference(Sentence previousSentence){
		return previousSentence != null ? previousSentence.type == SentenceType.EXPLICIT_REFERENCE : false;
	}
	
	public boolean startsWithSectionHeader(Sentence sentence){
		return sentence != null ? sentence.text.matches(HEADER_PATTERN) : false;
	}
	
	public boolean containsMainAuthor(Sentence sentence, String mainAuthor){
		return sentence.text.contains(mainAuthor);
	}
	
	public boolean containsAcronyms(Sentence sentence, Set<String> acronyms){
		return acronyms.stream().anyMatch(acronym -> sentence.text.contains(acronym));
	}
	
	public boolean containsLexicalHooks(Sentence sentence, Set<String> lexicalHooks){
		return lexicalHooks.stream().anyMatch(hook -> sentence.text.contains(hook));
	}
	
	private boolean looseContains(List<String> list, String str){
		str = str.toLowerCase();
		if(str.endsWith("s")){
			str = str.substring(0, str.length() - 1);
		}
		return list.contains(str);
	}
}
