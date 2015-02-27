package wekaWrapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.NonThrowingFileWriter;
import util.Texts;
import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.Sentence;
import citationContextData.SentenceClass;


public class InstanceHandler {
	
	public static void writeToArffFile(List<SimpleInstance> instances, Path path){
		
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
	public static List<SimpleInstance> createInstances(ContextDataSet dataset){
		List<SimpleInstance> instances = new ArrayList<SimpleInstance>();
		for(Citer citer : dataset.citers){
			for(int i = 0; i < citer.sentences.size(); i++){
				Sentence previous = i > 0 ? citer.sentences.get(i-1) : null;
				Sentence sentence = citer.sentences.get(i);
				Sentence next = i < citer.sentences.size() - 1? citer.sentences.get(i+1) : null;
				Map<String, Comparable> features = extractFeatures(previous, sentence, next, dataset);
				if(sentence.type == SentenceClass.EXPLICIT_REFERENCE){
					continue; //Excluded
				}
				instances.add(new SimpleInstance(features, sentence.type));
			}
		}
		System.out.println("extractInstances - done");
		return instances;
	}
	
	@SuppressWarnings("rawtypes")
	private static Map<String, Comparable> extractFeatures(Sentence previous, Sentence sentence, Sentence next, ContextDataSet dataset){
		Texts texts = Texts.instance();
		Map<String, Comparable> features = new HashMap<String, Comparable>();
		String[] words = sentence.text.split(" ");
		String[] prevWords = previous != null? previous.text.split(" ") : new String[0];
		features.put(FeatureName.DET_WORK.toString(), texts.containsDetWork(words));
		features.put(FeatureName.PRONOUN.toString(), texts.startsWith3rdPersonPronoun(words));
		features.put(FeatureName.CONNECTOR.toString(), texts.startsWithConnector(words));
		features.put(FeatureName.AFTER_EXPLICIT.toString(), texts.containsExplicitReference(Arrays.asList(prevWords), dataset.citedMainAuthor));
		features.put(FeatureName.AFTER_HEADING.toString(), texts.startsWithSectionHeader(previous != null ? previous.text : ""));
		features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(sentence.text));
		features.put(FeatureName.BEFORE_HEADING.toString(), texts.startsWithSectionHeader(next != null? next.text : ""));
		features.put(FeatureName.CONTAINS_AUTHOR.toString(), texts.containsMainAuthor(sentence.text, dataset.citedMainAuthor));
		features.put(FeatureName.CONTAINS_ACRONYM.toString(), texts.containsAcronyms(sentence.text, dataset.acronyms));
		features.put(FeatureName.CONTAINS_LEXICAL_HOOK.toString(), texts.containsLexicalHooks(sentence.text, dataset.lexicalHooks));
		features.put("TEXT", "'" + sentence.text + "'");
		
		return features;
	}

}
