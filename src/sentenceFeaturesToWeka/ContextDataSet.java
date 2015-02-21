package sentenceFeaturesToWeka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.IntegerMap;


public class ContextDataSet {
	public String citedMainAuthor;
	public String citedTitle;
	public List<Citer> citers;
	public Set<String> acronyms;
	public Set<String> lexicalHooks;

	public ContextDataSet(String citedMainAuthor, String citedTitle, List<Citer> citers){
		this.citedMainAuthor = citedMainAuthor;
		this.citedTitle = citedTitle;
		this.citers = citers;
		setup();
	}
	
	private void setup(){
		acronyms = findAcronyms();
		lexicalHooks = findLexicalHooks(5);
		System.out.println("DATASET FOR " + citedMainAuthor);
		System.out.println(acronyms);
		System.out.println(lexicalHooks);
		lexicalHooks.remove(citedMainAuthor);
	}
	
	
	private Set<String> findAcronyms(){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z]+[ ,]");
		return new HashSet<String>(findMatchesInExplicitReferencesAroundAuthor(regex));
	}
	
	private Set<String> findLexicalHooks(int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z][a-z]+[ ,:;]");
		List<String> nonDistinctHooks = findMatchesInExplicitReferencesAroundAuthor(regex);
		IntegerMap<String> counts = new IntegerMap<String>();
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
					int boundary = 20;
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
	
	private Stream<Sentence> nonReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.NOT_REFERENCE);
	}
	
//	
//	
//	IncrementableMap<String> getUnigramsInImplicitReferences(){
//		return getUnigrams(implicitReferences());
//	}
//	
//	IncrementableMap<String> getUnigramsInNonReferences(){
//		return getUnigrams(nonReferences());
//	}
//	
//	private IncrementableMap<String> getUnigrams(Stream<Sentence> sentences){
//		final List<String> stopwords = readStopwords();
//		IncrementableMap<String> wordCounts = new IncrementableMap<String>();
//		words(sentences).filter(word -> !stopwords.contains(word.toLowerCase()))
//			.forEach(word -> wordCounts.increment(word, 1));
//		return wordCounts;
//	}
	
//	IncrementableMap<String> getNgrams(int n, Stream<Sentence> sentences){
//		if(n < 2){
//			throw new IllegalArgumentException("use unigrams()");
//		} 
//		List<String> words = words(sentences).collect(Collectors.toList());
//		List<String> stopwords = readStopwords();
//		stopwords.add(NGrams.NUMBER);
//		IncrementableMap<String> ngramCounts = new IncrementableMap<String>();
//		for(int i = n - 1; i < words.size(); i++){
//			List<String> ngramWords = words.subList(i - n + 1, i + 1);
//			boolean ngramContainsStopword = ngramWords.stream()
//					.anyMatch(stopwords::contains);
//			if(!ngramContainsStopword){
//				Optional<String> ngram = ngramWords.stream()
//						.reduce((s1,s2) -> s1 + " " + s2);
//				ngramCounts.increment(ngram.get(), 1);
//			}
//		}
//		return ngramCounts;
//	}
//	
//	IncrementableMap<String> getBigramsInImplicitReferences(){
//		return getNgrams(2, implicitReferences());
//	}
//	
//	IncrementableMap<String> getBigramsInNonReferences(){
//		return getNgrams(2, nonReferences());
//	}
//
//	IncrementableMap<String> getTrigramsInImplicitReferences(){
//		return getNgrams(3, implicitReferences());
//	}
//	
//	IncrementableMap<String> getTrigramsInNonReferences(){
//		return getNgrams(3, nonReferences());
//	}
	
	List<String> readStopwords(){
		try {
			return Files.lines(Paths.get("src/ml/data/stopwords.txt")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
//	private Stream<String> words(Stream<Sentence> sentences){
//		return sentences
//			.flatMap(s -> Arrays.asList(NGrams.cleanString(s.text).split(" +")).stream());
//	}
	
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
