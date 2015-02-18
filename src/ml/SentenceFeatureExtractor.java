package ml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SentenceFeatureExtractor {
	
	private static List<String> determiners;
	private static List<String> workNouns;
	private static List<String> thirdPersonPronouns;
	private static List<String> connectors;
	
	public static final String HEADER_PATTERN = "\\d+\\.\\d+.*";
	
	public SentenceFeatureExtractor(){
		try {
			setup();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void writeInstancesToFile(List<Instance> instances, Path path){
		
		try(FileWriter writer = new FileWriter(path.toFile())) {
			writer.write("@RELATION sentence\n");
			for(Feature feature : Feature.values()){
				writer.write("@ATTRIBUTE " + feature.toString() + " {true, false}\n");
			}
			writer.write("@ATTRIBUTE class {" + 
					SentenceType.EXPLICIT_REFERENCE + "," + 
					SentenceType.IMPLICIT_REFERENCE + "," + 
					SentenceType.NOT_REFERENCE + "}\n");
			writer.write("@DATA\n");
			instances.forEach(instance -> {
				try {
					for(Feature feature : Feature.values()){
						writer.write(instance.features.get(feature) + ",");
					}
					writer.write(instance.instanceClass + "\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
//		@RELATION my-relation
//		 
//		@ATTRIBUTE a NUMERIC
//		@ATTRIBUTE b NUMERIC
//		@ATTRIBUTE class {Good,Bad}
//
//		@DATA
//		1,2,Good
//		2,2,Bad
//		5,3,Bad
//		1,0,Good
//		3,1,Bad
//		1,5,Bad
//		3,0,Good
//		4,2,Good
	}
	
	private void setup() throws IOException{
		String packageName = getClass().getPackage().getName();
		determiners = readLines("src/" + packageName + "/determinerWords.txt");
		workNouns = readLines("src/" + packageName + "/workNouns.txt");
		thirdPersonPronouns = readLines("src/" + packageName + "/thirdPersonPronouns.txt");
		connectors = readLines("src/" + packageName + "/connectors.txt");
	}
	
	
	
	public List<Instance> extractInstances(List<Sentence> sentences, Set<String> mainAuthors){
		List<Instance> instances = new ArrayList<Instance>();
		for(int i = 0; i < sentences.size(); i++){
			Sentence previous = i > 0 ? sentences.get(i-1) : null;
			Sentence sentence = sentences.get(i);
			Sentence next = i < sentences.size() - 1? sentences.get(i+1) : null;
			Instance instance = new Instance(extractFeatures(previous, sentence, next, mainAuthors), sentence.type);
			instances.add(instance);
			previous = sentence;
		}
		return instances;
	}
	
	
	
	public Map<Feature, Boolean> extractFeatures(Sentence previousSentence, Sentence sentence, Sentence nextSentence, Set<String> mainAuthors){
		Map<Feature, Boolean> features = new HashMap<Feature, Boolean>();
		String[] words = sentence.text.split(" ");
		features.put(Feature.DET_WORK, containsDetWork(words));
		features.put(Feature.PRONOUN, startsWith3rdPersonPronoun(words));
		features.put(Feature.CONNECTOR, startsWithConnector(words));
		features.put(Feature.AFTER_EXPLICIT, isAfterExplicitReference(previousSentence));
		features.put(Feature.AFTER_HEADING, startsWithSectionHeader(previousSentence));
		features.put(Feature.HEADING, startsWithSectionHeader(sentence));
		features.put(Feature.BEFORE_HEADING, startsWithSectionHeader(nextSentence));
		features.put(Feature.CONTAINS_AUTHOR, containsMainAuthor(words, mainAuthors));
		
		return features;
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
	
	public boolean containsMainAuthor(String[] words, Set<String> allCited){
		List<String> wordsArray = Arrays.asList(words);
		return wordsArray.stream().anyMatch(word -> allCited.contains(word));
	}
	
	private List<String> readLines(String filePath) throws IOException{
		return Files.lines(Paths.get(filePath)).collect(Collectors.toList());
	}
	
	private boolean looseContains(List<String> list, String str){
		str = str.toLowerCase();
		if(str.endsWith("s")){
			str = str.substring(0, str.length() - 1);
		}
		return list.contains(str);
	}
	
}
