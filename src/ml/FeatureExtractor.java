package ml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import old.NGrams;

import org.apache.commons.lang3.StringUtils;

import util.NonThrowingFileWriter;


public class FeatureExtractor {
	
	private List<String> determiners;
	private List<String> workNouns;
	private List<String> thirdPersonPronouns;
	private List<String> connectors;
	
	public static final String HEADER_PATTERN = "\\d+\\.\\d+.*";
	
	public FeatureExtractor(){
		try {
			setup();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void writeInstancesToFile(List<Instance> instances, Path path){
		
		System.out.println("writeInstanceToFile - " + path + " ...");
		
		NonThrowingFileWriter writer = new NonThrowingFileWriter(path.toFile());
		writer.write("@RELATION sentence\n");
		instances.get(0).features.keySet().stream().sorted().forEach(feature -> {
			writer.write("@ATTRIBUTE " + feature + " ");
			if(feature.contains("UNIGRAM") || feature.contains("BIGRAM") || feature.contains("TRIGRAM")){
				writer.write("NUMERIC\n");
			}else if(feature.equals("TEXT")){
				writer.write("STRING\n");
			}else{
				writer.write("{true, false}\n");
			}
		});
		writer.write("@ATTRIBUTE class {" + 
				SentenceType.IMPLICIT_REFERENCE + "," + 
				SentenceType.NOT_REFERENCE + "}\n");
		writer.write("@DATA\n");
		instances.forEach(instance -> {
			instance.features.entrySet().stream()
				.sorted((e1,e2)->e1.getKey().compareTo(e2.getKey()))
				.forEach(e -> {
					writer.write(e.getValue() + ",");
			});
			writer.write(instance.instanceClass + "\n");
		});
		writer.close();
	}
	
	private void setup() throws IOException{
		String packageName = getClass().getPackage().getName();
		determiners = readLines("src/" + packageName + "/data/determinerWords.txt");
		workNouns = readLines("src/" + packageName + "/data/workNouns.txt");
		thirdPersonPronouns = readLines("src/" + packageName + "/data/thirdPersonPronouns.txt");
		connectors = readLines("src/" + packageName + "/data/connectors.txt");
	}
	
	/**
	 * Sentences that are explicit references are excluded
	 * @param dataset
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Instance> createInstances(ContextDataSet dataset, NGrams ngrams){
		List<Instance> instances = new ArrayList<Instance>();
		for(Citer citer : dataset.citers){
			for(int i = 0; i < citer.sentences.size(); i++){
				Sentence previous = i > 0 ? citer.sentences.get(i-1) : null;
				Sentence sentence = citer.sentences.get(i);
				Sentence next = i < citer.sentences.size() - 1? citer.sentences.get(i+1) : null;
				Map<String, Comparable> features = extractFeatures(previous, sentence, next, dataset, ngrams);
				if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
					continue; //Excluded
				}
				instances.add(new Instance(features, sentence.type));
			}
		}
		System.out.println("extractInstances - done");
		return instances;
	}
	
	
	
	@SuppressWarnings("rawtypes")
	private Map<String, Comparable> extractFeatures(Sentence previous, Sentence sentence, Sentence next, ContextDataSet dataset, NGrams ngrams){
		Map<String, Comparable> features = new HashMap<String, Comparable>();
		String[] words = sentence.text.split(" ");
		features.put(SentenceFeature.DET_WORK.toString(), containsDetWork(words));
		features.put(SentenceFeature.PRONOUN.toString(), startsWith3rdPersonPronoun(words));
		features.put(SentenceFeature.CONNECTOR.toString(), startsWithConnector(words));
		features.put(SentenceFeature.AFTER_EXPLICIT.toString(), isAfterExplicitReference(previous));
		features.put(SentenceFeature.AFTER_HEADING.toString(), startsWithSectionHeader(previous));
		features.put(SentenceFeature.HEADING.toString(), startsWithSectionHeader(sentence));
		features.put(SentenceFeature.BEFORE_HEADING.toString(), startsWithSectionHeader(next));
		features.put(SentenceFeature.CONTAINS_AUTHOR.toString(), containsMainAuthor(sentence, dataset.citedMainAuthor));
		features.put(SentenceFeature.CONTAINS_ACRONYM.toString(), containsAcronyms(sentence, dataset.acronyms));
		features.put(SentenceFeature.CONTAINS_LEXICAL_HOOK.toString(), containsLexicalHooks(sentence, dataset.lexicalHooks));
//		for(String unigram : ngrams.unigrams){
//			features.put("UNIGRAM_" + unigram, countNgram(sentence, unigram));
//		}
//		for(String bigram : ngrams.bigrams){
//			features.put("BIGRAM_" + bigram.replaceAll(" ", "_"), countNgram(sentence, bigram));
//		}
//		for(String trigram : ngrams.trigrams){
//			features.put("TRIGRAM_" + trigram.replaceAll(" " , "_"), countNgram(sentence, trigram));
//		}
		features.put("TEXT", "'" + sentence.text + "'");
		
		return features;
	}
	
//	private int countNgram(Sentence sentence, String ngram){
//		return StringUtils.countMatches(NGrams.cleanString(sentence.text), ngram);
//	}
//	

	private boolean containsDetWork(String[] words){
		for(int i = 1; i < words.length; i++){
			if(looseContains(workNouns, words[i])){
				if(looseContains(determiners, words[i-1])){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean startsWith3rdPersonPronoun(String[] words){
		return looseContains(thirdPersonPronouns, words[0]);
	}
	
	private boolean startsWithConnector(String[] words){
		return looseContains(connectors, words[0]);
	}
	
	private boolean isAfterExplicitReference(Sentence previousSentence){
		return previousSentence != null ? previousSentence.type == SentenceType.EXPLICIT_REFERENCE : false;
	}
	
	private boolean startsWithSectionHeader(Sentence sentence){
		return sentence != null ? sentence.text.matches(HEADER_PATTERN) : false;
	}
	
	private boolean containsMainAuthor(Sentence sentence, String mainAuthor){
		return sentence.text.contains(mainAuthor);
	}
	
	private boolean containsAcronyms(Sentence sentence, Set<String> acronyms){
		return acronyms.stream().anyMatch(acronym -> sentence.text.contains(acronym));
	}
	
	private boolean containsLexicalHooks(Sentence sentence, Set<String> lexicalHooks){
		return lexicalHooks.stream().anyMatch(hook -> sentence.text.contains(hook));
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
