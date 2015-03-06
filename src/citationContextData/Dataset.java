package citationContextData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.IntegerMap;
import util.Printer;
import util.Texts;


public class Dataset {
	public String datasetLabel;
	public String citedMainAuthor;
	public String citedTitle;
	public String citedContent;
	public HashMap<String,Double> citedContentUnigrams;
	public List<Citer> citers;
	public Set<String> acronyms;
	public Set<String> lexicalHooks;
	
	private static Printer printer = new Printer(false);
	
	public static Dataset fromHTMLFile(File htmlFile){
		return ContextHTML_Parser.parseHTML(htmlFile);
	}
	
	public static ArrayList<Dataset> datasetsFromDir(File dir){
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		File[] files = dir.listFiles();
		System.out.print("Creating citation data set from dir " + dir.getAbsolutePath() + ": ");
		for(int i = 0; i < files.length; i++){
			printer.progress(i, 1);
			File htmlFile = files[i];
			if(!htmlFile.getName().endsWith(".html")){
				continue;
			}
			String baseName = htmlFile.getName().substring(0, htmlFile.getName().length()-5);
			File textFile = new File(dir, baseName + ".txt");
			datasets.add(Dataset.fromFiles(htmlFile, textFile));
		}
		System.out.println(" [x]");
		return datasets;
	}
	
	public static Dataset fromFiles(File htmlFile, File citedContentTextFile){
		Dataset dataset = ContextHTML_Parser.parseHTML(htmlFile);
		dataset.citedContent = readTextFile(citedContentTextFile);
		dataset.enhance();
		return dataset;
	}
	
	private static String readTextFile(File f){
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
	
	public Dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<Citer> citers){
		this(datasetLabel, citedMainAuthor, citedTitle, citers, null);
	}

	public Dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<Citer> citers, String citedContent){
		this.datasetLabel = datasetLabel;
		this.citedMainAuthor = citedMainAuthor;
		this.citedTitle = citedTitle;
		this.citedContent = citedContent;
		this.citers = citers;
	}
	
	public void enhance(){
		acronyms = findAcronyms();
		lexicalHooks = findLexicalHooks(5);
		lexicalHooks.remove(citedMainAuthor);
		citedContentUnigrams = Texts.instance().getNgrams(1, citedContent, true, true);
		
		printer.println("DATASET FOR " + citedMainAuthor);
		printer.println(acronyms);
		printer.println(lexicalHooks);
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
				.filter(s -> s.type == SentenceClass.EXPLICIT_REFERENCE);
	}
//	
//	private Stream<Sentence> implicitReferences(){
//		return citers.stream()
//				.flatMap(c -> c.sentences.stream())
//				.filter(s -> s.type == SentenceClass.IMPLICIT_REFERENCE);
//	}
//	
//	private Stream<Sentence> sentences(){
//		return citers.stream()
//				.flatMap(c -> c.sentences.stream());
//	}
//	
//	private Stream<Sentence> nonReferences(){
//		return citers.stream()
//				.flatMap(c -> c.sentences.stream())
//				.filter(s -> s.type == SentenceClass.NOT_REFERENCE);
//	}
	
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
	
//	IntegerMap<String> getNGrams(int n, Sentence sentence){
//		List<String> words = Arrays.asList(sentence.text.split(" +")).stream()
//				.map(s -> s.toLowerCase())
//				.collect(Collectors.toCollection(ArrayList::new));
//		List<String> stopwords = readStopwords();
//		stopwords.add(Texts.NUMBER_TAG);
//		IntegerMap<String> ngramCounts = new IntegerMap<String>();
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
	
//	IntegerMap<String> getNgrams(int n, Stream<Sentence> sentences){
//		if(n < 2){
//			throw new IllegalArgumentException("use unigrams()");
//		} 
//		List<String> words = sentences.flatMap(s -> Arrays.asList(s.text.split(" +")).stream())
//				.map(s -> s.toLowerCase())
//				.collect(Collectors.toCollection(ArrayList::new));
//		List<String> stopwords = readStopwords();
//		stopwords.add(Texts.NUMBER_TAG);
//		IntegerMap<String> ngramCounts = new IntegerMap<String>();
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
	
//	List<String> readStopwords(){
//		try {
//			return Files.lines(Paths.get("src/ml/data/stopwords.txt")).collect(Collectors.toList());
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(0);
//			return null;
//		}
//	}
	
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
					sentencesStr.append("\"text\": \"" + sentence.text + "\"},\n");
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
