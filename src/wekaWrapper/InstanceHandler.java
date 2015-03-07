package wekaWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.NonThrowingFileWriter;
import util.Texts;
import citationContextData.CitingPaper;
import citationContextData.ContextHTML_Parser;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.SentenceClass;


public class InstanceHandler {
	
	public static void writeToArffFile(List<SentenceInstance> instances, File arffFile){
		
		System.out.println("writeInstanceToFile - " + arffFile.getPath() + " ...");
		
		NonThrowingFileWriter writer = new NonThrowingFileWriter(arffFile);
		writer.write("@RELATION sentence\n");
		instances.get(0).features.keySet().stream().sorted().forEach(feature -> {
			writer.write("@ATTRIBUTE " + feature + " ");
			if(feature.contains("UNIGRAM") || feature.contains("BIGRAM") || feature.contains("TRIGRAM") || feature.equals(FeatureName.SENTENCE_NUMBER.toString())){
				writer.write("NUMERIC\n");
			}else if(feature.equals(FeatureName.TEXT.toString())){
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
	 * 
	 * @param htmlFiles Must have .html-file endings
	 * @param onlyText
	 * @return
	 */
	public static List<SentenceInstance> createInstancesFromHTMLFiles(File[] htmlFiles, int authorProxyBoundary, int numLexicalHooks, boolean onlyText, boolean balanceData){
		
		List<WekaDataset> datasets = Arrays.asList(htmlFiles).stream()
				.filter(f -> f.getName().endsWith(".html")) 
				.map(f -> ContextHTML_Parser.parseHTML(f))
				.map(d -> d.getWekaDataset(authorProxyBoundary, numLexicalHooks))
				.collect(Collectors.toList());
		ArrayList<SentenceInstance> instances = datasets.stream()
				.flatMap(dataset -> InstanceHandler.createInstances(dataset, onlyText, balanceData).stream())
				.collect(Collectors.toCollection(ArrayList::new));

		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toCollection(ArrayList::new));
		
		return instances;
	}
	
	/**
	 * Sentences that are explicit references are excluded
	 * @param dataset
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<SentenceInstance> createInstances(WekaDataset dataset, boolean onlyText, boolean balanceData){
		List<SentenceInstance> instances = new ArrayList<SentenceInstance>();
		for(CitingPaper citer : dataset.citers){
			for(int i = 0; i < citer.sentences.size(); i++){
				Sentence previous = i > 0 ? citer.sentences.get(i-1) : null;
				Sentence sentence = citer.sentences.get(i);
				Sentence next = i < citer.sentences.size() - 1? citer.sentences.get(i+1) : null;
				Map<String, Comparable> features = extractFeatures(previous, sentence, next, dataset, onlyText, i);
				if(sentence.type == SentenceClass.EXPLICIT_REFERENCE){ //TODO
					continue; //Excluded
				}
				instances.add(new SentenceInstance(features, sentence.type));
			}
		}
		if(balanceData){
			balanceData(instances);
		}
		System.out.println("extractInstances - done");
		return instances;
	}
	
	public static void balanceData(List<SentenceInstance> data){
		//TODO should shuffle data?
		int numTotal = data.size();
		long numPos = data.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE).count();
		int q = (int)(numTotal / (double)numPos);
		
		int i = 0;
		Iterator<SentenceInstance> it = data.iterator();
		while(it.hasNext()){
			if(it.next().instanceClass == SentenceClass.NOT_REFERENCE){
				if(i < q){
					it.remove();
					i++;
				}else{
					i = 0;
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static Map<String, Comparable> extractFeatures(Sentence previous, Sentence sentence, Sentence next, WekaDataset dataset, boolean onlyText, int sentenceNumber){
		Texts texts = Texts.instance();
		Map<String, Comparable> features = new HashMap<String, Comparable>();
		String[] words = sentence.text.split("\\s+");
		String[] prevWords = previous != null? previous.text.split("\\s+") : new String[0];
		if(!onlyText){
			features.put(FeatureName.DET_WORK.toString(), texts.containsDetWork(words));
			features.put(FeatureName.PRONOUN.toString(), texts.startsWith3rdPersonPronoun(words));
			features.put(FeatureName.CONNECTOR.toString(), texts.startsWithConnector(words));
			features.put(FeatureName.AFTER_EXPLICIT.toString(), texts.containsExplicitCitation(Arrays.asList(prevWords), dataset.citedMainAuthor));
			features.put(FeatureName.AFTER_HEADING.toString(), texts.startsWithSectionHeader(previous != null ? previous.text : ""));
			features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(sentence.text));
			features.put(FeatureName.BEFORE_HEADING.toString(), texts.startsWithSectionHeader(next != null? next.text : ""));
			features.put(FeatureName.CONTAINS_AUTHOR.toString(), texts.containsMainAuthor(sentence.text, dataset.citedMainAuthor));
			features.put(FeatureName.CONTAINS_ACRONYM.toString(), texts.containsAcronyms(sentence.text, dataset.acronyms));
			features.put(FeatureName.CONTAINS_LEXICAL_HOOK.toString(), texts.containsLexicalHooks(sentence.text, dataset.lexicalHooks));
		}
		features.put(FeatureName.SENTENCE_NUMBER.toString(), sentenceNumber);
		features.put(FeatureName.TEXT.toString(), "'" + sentence.text + "'");
		return features;
	}

}
