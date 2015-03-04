package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Texts {
	
	private List<String> determiners;
	private List<String> workNouns;
	private List<String> thirdPersonPronouns;
	private List<String> connectors;
	private HashSet<String> stopwords;
	
	public static final String NUMBER_TAG = "<NUMBER>";
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
		stopwords = readLinesToSet("src/" + packageName + "/data/stopwords.txt");
	}
	
	public List<String> removeStopwords(String[] words){
		List<String> filtered = new ArrayList<String>();
		for(String w : words){
			if(!stopwords.contains(w)){
				filtered.add(w);
			}
		}
		return filtered;
	}
	
	public boolean isStopword(String word){
		return stopwords.contains(word);
	}
	
	private HashSet<String> readLinesToSet(String filePath) throws IOException{
		return Files.lines(Paths.get(filePath)).collect(Collectors.toCollection(HashSet::new));
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
	
	public boolean startsWithDetWork(String[] words){
		return looseContains(determiners, words[0]) && looseContains(workNouns, words[1]);
	}
	
	public boolean startsWithLimitedDet(String[] words){
		return words[0].equalsIgnoreCase("this") || words[0].toLowerCase().equalsIgnoreCase("such");
	}
	
	public boolean startsWith3rdPersonPronoun(String[] words){
		return looseContains(thirdPersonPronouns, words[0]);
	}
	
	public boolean startsWithConnector(String[] words){
		return looseContains(connectors, words[0]);
	}
	
	
	public boolean startsWithSectionHeader(String sentence){
		return sentence != null ? sentence.matches(HEADER_PATTERN) : false;
	}
	
	public boolean containsMainAuthor(String sentence, String mainAuthor){
		return sentence.contains(mainAuthor);
	}
	
	public boolean containsAcronyms(String sentence, Set<String> acronyms){
		return acronyms.stream().anyMatch(acronym -> sentence.contains(acronym));
	}
	
	public boolean containsLexicalHooks(String sentence, Set<String> lexicalHooks){
		return lexicalHooks.stream().anyMatch(hook -> sentence.contains(hook) || sentence.contains(hook.toLowerCase()));
	}
	
	private boolean looseContains(List<String> list, String str){
		
		if(str.endsWith("s")){
			str = str.substring(0, str.length() - 1);
		}
		return list.contains(str) || list.contains(str.toLowerCase());
	}
	
	public boolean containsExplicitReference(List<String> words, String mainAuthor){
		int authorIndex = words.indexOf(mainAuthor);
		if(authorIndex > -1){
			int start = Math.min(authorIndex+1, words.size()-1);
			int end = Math.min(authorIndex+5, words.size());
			List<String> vicinity = words.subList(start, end);
//			String year = "\\d\\d\\d\\d";
			String year = NUMBER_TAG;
			return vicinity.stream().anyMatch(word -> word.matches(year));
		}
		return false;
	}

	public DoubleMap<String> getNGrams(int n, String sentence){
		List<String> words = Arrays.asList(sentence.split(" +")).stream()
				.map(s -> s.toLowerCase())
				.collect(Collectors.toCollection(ArrayList::new));
		HashSet<String> stopwords = new HashSet<String>(this.stopwords); //TODO Expensive
		stopwords.add(Texts.NUMBER_TAG);
		DoubleMap<String> ngramCounts = new DoubleMap<String>();
		for(int i = n - 1; i < words.size(); i++){
			List<String> ngramWords = words.subList(i - n + 1, i + 1);
			boolean ngramContainsStopword = ngramWords.stream()
					.anyMatch(stopwords::contains);
			if(!ngramContainsStopword){
				Optional<String> ngram = ngramWords.stream()
						.reduce((s1,s2) -> s1 + " " + s2);
				ngramCounts.increment(ngram.get(), 1.0);
			}
		}
		return ngramCounts;
	}
}
