package sentenceFeaturesToWeka;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import old.NGrams;
import util.NonThrowingFileWriter;
import util.Texts;


public class FeatureExtractor {
	
	public FeatureExtractor(){

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
				SentenceClass.IMPLICIT_REFERENCE + "," + 
				SentenceClass.NOT_REFERENCE + "}\n");
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
				if(sentence.type == SentenceClass.EXPLICIT_REFERENCE){
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
		Texts texts = Texts.instance();
		Map<String, Comparable> features = new HashMap<String, Comparable>();
		String[] words = sentence.text.split(" ");
		String[] prevWords = previous.text.split(" ");
		features.put(SentenceFeature.DET_WORK.toString(), texts.containsDetWork(words));
		features.put(SentenceFeature.PRONOUN.toString(), texts.startsWith3rdPersonPronoun(words));
		features.put(SentenceFeature.CONNECTOR.toString(), texts.startsWithConnector(words));
		features.put(SentenceFeature.AFTER_EXPLICIT.toString(), texts.containsExplicitReference(Arrays.asList(prevWords), dataset.citedMainAuthor));
		features.put(SentenceFeature.AFTER_HEADING.toString(), texts.startsWithSectionHeader(previous.text));
		features.put(SentenceFeature.HEADING.toString(), texts.startsWithSectionHeader(sentence.text));
		features.put(SentenceFeature.BEFORE_HEADING.toString(), texts.startsWithSectionHeader(next.text));
		features.put(SentenceFeature.CONTAINS_AUTHOR.toString(), texts.containsMainAuthor(sentence.text, dataset.citedMainAuthor));
		features.put(SentenceFeature.CONTAINS_ACRONYM.toString(), texts.containsAcronyms(sentence.text, dataset.acronyms));
		features.put(SentenceFeature.CONTAINS_LEXICAL_HOOK.toString(), texts.containsLexicalHooks(sentence.text, dataset.lexicalHooks));
		features.put("TEXT", "'" + sentence.text + "'");
		
		return features;
	}
}
