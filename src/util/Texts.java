package util;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		String dir = Environment.exjobbHome() + "/resources/wordLists/";
		determiners = readLines(dir + "/determinerWords.txt");
		workNouns = readLines(dir + "/workNouns.txt");
		thirdPersonPronouns = readLines(dir + "/thirdPersonPronouns.txt");
		connectors = readLines(dir + "/connectors.txt");
		stopwords = readLinesToSet(dir + "/stopwords.txt");
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
	
	public static List<String> split(String text){
		int pos = text.indexOf(' ') + 1;
		if(pos == 0){ //no spaces in input
			return Arrays.asList(new String[]{text});
		}
		String word;
		ArrayList<String> words = new ArrayList<String>();
		word = text.substring(0, pos - 1);
		if(word.length() > 0){
    		words.add(word);
    	}
        int end;
        while ((end = text.indexOf(' ', pos)) >= 0) {
        	word = text.substring(pos, end);
        	if(word.length() > 0){
        		words.add(word);
        	}
            pos = end + 1;
        }
        return words;
	}
	
	public static String merge(List<String> words){
		if(words.size() == 0){
			return "";
		}
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < words.size() - 1; i++){
			s.append(words.get(i) + " ");
		}
		s.append(words.get(words.size()-1));
		return s.toString();
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
	
	public static String readTextFile(File f){
		try(Scanner sc = new Scanner(new BufferedReader(new FileReader(f)))) {
			StringBuilder s = new StringBuilder();
			while(sc.hasNextLine()){
				s.append(sc.nextLine() + "\n");
			}
			return s.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public boolean startsWithDetWork(String[] words){
		return words.length >= 2 && looseContains(determiners, words[0]) && looseContains(workNouns, words[1]);
	}
	
	public boolean startsWithLimitedDet(String[] words){
		return words.length >= 1 && (words[0].equalsIgnoreCase("this") || words[0].toLowerCase().equalsIgnoreCase("such"));
	}
	
	public boolean startsWith3rdPersonPronoun(String[] words){
		return words.length >= 1 && looseContains(thirdPersonPronouns, words[0]);
	}
	
	public boolean startsWithConnector(String[] words){
		return words.length >= 1 && looseContains(connectors, words[0]);
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
	
	public boolean containsExplicitCitation(List<String> words, String mainAuthor){
		int authorIndex = words.indexOf(mainAuthor);
		if(authorIndex > -1){
			int start = Math.min(authorIndex+1, words.size()-1);
			int end = Math.min(authorIndex+5, words.size());
			List<String> vicinity = words.subList(start, end);
			String year = NUMBER_TAG;
			return vicinity.stream().anyMatch(word -> word.matches(year));
		}
		return false;
	}
	
	public List<String> toLowercaseWords(String text){
		List<String> words = Arrays.asList(text.split("\\s+")).stream()
				.map(s -> s.toLowerCase())
				.collect(Collectors.toCollection(ArrayList::new));
		return words;
	}

	public TObjectIntHashMap<String> getNgrams(int n, String text, final boolean skipStopwords){
		if(text == null){
			throw new IllegalArgumentException("text == null");
		}
		return getNgrams(n, toLowercaseWords(text), skipStopwords);
	}
	
	public TObjectIntHashMap<String> getNgrams(int n, List<String> words, final boolean skipStopwords){
		TObjectIntHashMap<String> ngramCounts = new TObjectIntHashMap<String>();
		for(int i = n - 1; i < words.size(); i++){
			List<String> ngramWords = words.subList(i - n + 1, i + 1);
			if(skipStopwords){
				if(ngramWords.stream().anyMatch(w -> stopwords.contains(w) || w.equals(NUMBER_TAG))){
					continue;
				}
			}
			Stream<String> ngramWordsStream = ngramWords.stream();
			String ngram = ngramWordsStream.reduce((s1,s2) -> s1 + " " + s2).get();
			if(ngram.length() > 0){
				ngramCounts.adjustOrPutValue(ngram, 1, 1);
//				ngramCounts.increment(ngram, 1.0);
			}
		}
		return ngramCounts;
	}
	
	public static String stem(String word){
		Stemmer s = new Stemmer();
		s.add(word);
		s.stem();
		return s.toString();
	}
}
