package ml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.IncrementableMap;


public class ContextDataSet {
	String citedMainAuthor;
	String citedTitle;
	List<Citer> citers;
	Set<String> acronyms;
	Set<String> lexicalHooks;
	
	
	
	public ContextDataSet(String citedMainAuthor, String citedTitle, List<Citer> citers){
		this.citedMainAuthor = citedMainAuthor;
		this.citedTitle = citedTitle;
		this.citers = citers;
		setup();
	}
	
	private void setup(){
		acronyms = findAcronyms();
		lexicalHooks = findLexicalHooks(3);
		lexicalHooks.remove(citedMainAuthor);
//		unigrams = findUnigrams(10);
//		bigrams = findBigrams(5);
//		trigrams = findTrigrams(5);
	}
	
	
	private Set<String> findAcronyms(){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z]+[ ,]");
		return new HashSet<String>(findMatchesInExplicitReferencesAroundAuthor(regex));
	}
	
	private Set<String> findLexicalHooks(int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z][a-z]+[ ,]");
		List<String> nonDistinctHooks = findMatchesInExplicitReferencesAroundAuthor(regex);
		IncrementableMap<String> counts = new IncrementableMap<String>();
		nonDistinctHooks.stream()
			.filter(hook -> !hook.equals(citedMainAuthor))
			.forEach(hook -> {
				counts.increment(hook, 1);
			});
		return counts.getTopN(numLexicalHooks).stream()
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
	
	private List<String> findMatchesInExplicitReferencesAroundAuthor(Pattern regex){
		String author = citedMainAuthor;
		List<String> matches = new ArrayList<String>();
		
		explicitReferences().forEach(sentence -> {
				int index = sentence.text.indexOf(author);
				if(index >= 0){
					int boundary = 50;
					int left = Math.max(0, index-boundary);
					int right = Math.min(sentence.text.length()- 1, index+boundary);
					String vicinityOfAuthor = sentence.text.substring(left, right);
					Matcher m = regex.matcher(vicinityOfAuthor);
					while(m.find()){
						String match = m.group();
						match = match.replaceAll("[,\\[\\]\\(\\)]", "").trim();
						matches.add(match);
					}
				}
		});

		return matches;
	}
	
	private Stream<Sentence> explicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.EXPLICIT_REFERENCE);
	}
	
	private Stream<Sentence> implicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.IMPLICIT_REFERENCE);
	}
	
	private Stream<Sentence> sentences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream());
	}
	
	
	
	IncrementableMap<String> findUnigrams(){
		final List<String> stopwords = readStopwords();
		IncrementableMap<String> wordCounts = new IncrementableMap<String>();
		words(sentences()).filter(word -> !stopwords.contains(word.toLowerCase()))
			.forEach(word -> wordCounts.increment(word, 1));
		return wordCounts;
	}
	
	
	
	IncrementableMap<String> findBigrams(){
		List<String> words = words(sentences()).collect(Collectors.toList());
		List<String> stopwords = readStopwords();
		IncrementableMap<String> bigramCounts = new IncrementableMap<String>();
		for(int i = 2; i < words.size(); i++){
			if(!stopwords.contains(words.get(i-1)) && !stopwords.contains(words.get(i))){
				bigramCounts.increment(words.get(i-1) + " " + words.get(i), 1);
			}
		}
		return bigramCounts;
	}
	
	IncrementableMap<String> findTrigrams(){
		List<String> words = words(sentences()).collect(Collectors.toList());
		List<String> stopwords = readStopwords();
		IncrementableMap<String> trigramCounts = new IncrementableMap<String>();
		for(int i = 3; i < words.size(); i++){
			if(!stopwords.contains(words.get(i-2)) && !stopwords.contains(words.get(i-1)) && !stopwords.contains(words.get(i))){
				trigramCounts.increment(words.get(i-2) + " " + words.get(i-1) + " " + words.get(i), 1);
			}
		}
		return trigramCounts;
	}
	
	List<String> readStopwords(){
		try {
			return Files.lines(Paths.get("src/ml/data/stopwords.txt")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	private Stream<String> words(Stream<Sentence> sentences){
		return sentences()
			.flatMap(s -> Arrays.asList(s.text.split(" ")).stream())
			.map(s -> s.replaceAll("[,\\.\\(\\)]", ""))
			.filter(s -> !s.equals(""));
	}
	
	/**
	 * Writes a json structure of the whole data set
	 * @param file
	 */
	public void writeToJson(File file){
		try(FileWriter writer = new FileWriter(file)){
			writer.write("{\n");
			writer.write("\"citedMainAuthor\": \"" + citedMainAuthor + "\",\n");
			writer.write("\"citedTitle\": \"" + citedTitle + "\",\n");
			writer.write("\"citers\": [\n");
			StringBuilder citersStr = new StringBuilder();
			for(Citer citer : citers.subList(0, 2)){
				citersStr.append("{\n");
				citersStr.append("\"title\": \"" + citer.title.replace('\n', ' ') + "\",\n");
				citersStr.append("\"sentences\": [\n");
				StringBuilder sentencesStr = new StringBuilder();
				for(Sentence sentence : citer.sentences.subList(0, 2)){
					sentencesStr.append("{\n");
					sentencesStr.append("\"type\": \"" + sentence.sentiment + "\",\n");
					sentencesStr.append("\"text\": \"" + sentence.text.replace('\n', ' ') + "\"},\n");
				}
				citersStr.append(sentencesStr.substring(0, sentencesStr.length() - 2)); //get rid of last comma
				citersStr.append("\n");
				citersStr.append("]},\n");
			}
			writer.write(citersStr.substring(0, citersStr.length() - 2)); //get rid of last comma
			writer.write("\n");
			writer.write("]}");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	
}
