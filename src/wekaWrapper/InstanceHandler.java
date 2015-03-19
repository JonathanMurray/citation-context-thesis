package wekaWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import util.NonThrowingFileWriter;
import util.Printer;
import util.Texts;
import citationContextData.CitingPaper;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.SentenceType;
import citationContextData.Text;


public class InstanceHandler {
	
	private static Printer printer = new Printer(true);
	
	public static void writeToArffFile(List<SentenceInstance> instances, File arffFile){
		
		printer.print("writing to .arff file: " + arffFile.getPath() + " ... ");
		
		List<String> numeric = Arrays.asList(new String[]{
				FeatureName.SENTENCE_NUMBER.toString(),
				FeatureName.CONTAINS_ACRONYM_SCORE.toString(),
				FeatureName.CONTAINS_LEXICAL_HOOK_SCORE.toString(),
				
				//enhanced features
				FeatureName.DISTANCE_PREV_EXPLICIT.toString(),
				FeatureName.DISTANCE_NEXT_EXPLICIT.toString(),
				FeatureName.SIMILAR_TO_EXPLICIT.toString(),
				FeatureName.SIMILAR_TO_CITED_TITLE.toString(),
				FeatureName.SIMILAR_TO_CITED_CONTENT.toString()
		});
		
		NonThrowingFileWriter writer = new NonThrowingFileWriter(arffFile);
		writer.write("@RELATION sentence\n");
		instances.get(0).features.keySet().stream().sorted().forEach(feature -> {
			writer.write("@ATTRIBUTE " + feature + " ");
			if(feature.contains("UNIGRAM") || feature.contains("BIGRAM") || feature.contains("TRIGRAM")
					|| numeric.contains(feature)){
				writer.write("NUMERIC\n");
			}else if(feature.equals(FeatureName.TEXT.toString())){
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
		printer.println("[x]");
	}
	
	public static <T extends Text> List<SentenceInstance> createInstances(List<Dataset<T>> datasets, 
			boolean onlyText, boolean balanceData){
		
		List<SentenceInstance> allInstances = new ArrayList<SentenceInstance>();
		for(Dataset<T> dataset : datasets){
			allInstances.addAll(createInstances(dataset, onlyText, balanceData));
		}
		return allInstances;
	}
	
	public static <T extends Text> HashMap<String, ArrayList<SentenceInstance>> createInstanceSets(List<Dataset<T>> datasets, 
			boolean onlyText, boolean balanceData){
		
		HashMap<String, ArrayList<SentenceInstance>> instanceSets = new HashMap<String, ArrayList<SentenceInstance>>();
		for(Dataset<T> dataset : datasets){
			instanceSets.put(dataset.datasetLabel, createInstances(dataset, onlyText, balanceData));
		}
		return instanceSets;
	}
	
	/**
	 * Sentences that are explicit references are excluded
	 * @param dataset
	 * @return
	 */
	public static <T extends Text> ArrayList<SentenceInstance> createInstances(Dataset<T> dataset, 
			boolean onlyText, boolean balanceData){
		
		ArrayList<SentenceInstance> instances = new ArrayList<SentenceInstance>();
		for(CitingPaper<T> citer : dataset.citers){
			for(int i = 0; i < citer.sentences.size(); i++){
				Sentence<T> previous = i > 0 ? citer.sentences.get(i-1) : null;
				Sentence<T> sentence = citer.sentences.get(i);
				Sentence<T> next = i < citer.sentences.size() - 1? citer.sentences.get(i+1) : null;
//				Map<String, Comparable<?>> features = extractFeatures(previous, sentence, next, dataset, onlyText, i);
				Map<String, Comparable<?>> features = extractFeaturesEnhanced(citer.sentences, i, dataset);
				if(sentence.type == SentenceType.EXPLICIT_REFERENCE){ //TODO
					continue; //Excluded
				}
				instances.add(new SentenceInstance(features, sentence.type));
			}
		}
		if(balanceData){
			balanceData(instances);
		}
		return instances;
	}
	
	public static void balanceData(List<SentenceInstance> data){
		//TODO should shuffle data?
		int numTotal = data.size();
		long numPos = data.stream().filter(i -> i.instanceClass == SentenceType.IMPLICIT_REFERENCE).count();
		int quote = (int)(numTotal / (double)numPos);
		int i = 0;
		Iterator<SentenceInstance> it = data.iterator();
		while(it.hasNext()){
			if(it.next().instanceClass == SentenceType.NOT_REFERENCE){
				if(i < quote){
					it.remove();
					i++;
				}else{
					i = 0;
				}
			}
		}
	}
	
	private static <T extends Text> Map<String, Comparable<?>> extractFeatures(Sentence<T> previous, Sentence<T> sentence, Sentence<T> next, Dataset<T> dataset, boolean onlyText, int sentenceNumber){
		Texts texts = Texts.instance();
		Map<String, Comparable<?>> features = new HashMap<String, Comparable<?>>();
		List<String> rawWords = sentence.text.rawWords;
		String[] prevWords = previous != null? previous.text.rawWords.toArray(new String[0]) : new String[0];
		if(!onlyText){
			features.put(FeatureName.STARTS_DET_WORK.toString(), texts.containsDetWork(rawWords));
			features.put(FeatureName.STARTS_3_PRONOUN.toString(), texts.startsWith3rdPersonPronoun(rawWords));
			features.put(FeatureName.STARTS_CONNECTOR.toString(), texts.startsWithConnector(rawWords));
			features.put(FeatureName.AFTER_EXPLICIT.toString(), texts.containsExplicitCitation(Arrays.asList(prevWords), dataset.citedMainAuthor));
			features.put(FeatureName.AFTER_HEADING.toString(), previous != null ? texts.startsWithSectionHeader( previous.text.rawWords) : false);
			features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(rawWords));
			features.put(FeatureName.BEFORE_HEADING.toString(), next != null? texts.startsWithSectionHeader(next.text.rawWords) : false);
			features.put(FeatureName.CONTAINS_AUTHOR.toString(), texts.containsMainAuthor(rawWords, dataset.citedMainAuthor));
			features.put(FeatureName.CONTAINS_ACRONYM_SCORE.toString(), texts.containsAcronymScore(rawWords, dataset.getAcronyms()));
			features.put(FeatureName.CONTAINS_LEXICAL_HOOK_SCORE.toString(), texts.containsHookScore(sentence.text.raw, dataset.getLexicalHooks()));
		}
		features.put(FeatureName.SENTENCE_NUMBER.toString(), sentenceNumber);
		features.put(FeatureName.TEXT.toString(), "'" + sentence.text.raw.replaceAll("'", "") + "'");
		return features;
	}
	
	private static <T extends Text> Map<String, Comparable<?>> extractFeaturesEnhanced(List<Sentence<T>> sentences, int sentenceIndex, Dataset<T> dataset){
		Texts texts = Texts.instance();
		Map<String, Comparable<?>> features = new HashMap<String, Comparable<?>>();
		Sentence<T> sentence = sentences.get(sentenceIndex);
		Sentence<T> previous = null;
		Sentence<T> next = null;
		if(sentenceIndex > 0){
			previous = sentences.get(sentenceIndex - 1);
		}
		if(sentenceIndex < sentences.size() - 1){
			next = sentences.get(sentenceIndex + 1);
		}
		
		List<String> rawWords = sentence.text.rawWords;
		String[] prevWords = previous != null? previous.text.rawWords.toArray(new String[0]) : new String[0];
		features.put(FeatureName.STARTS_DET_WORK.toString(), texts.containsDetWork(rawWords));
		features.put(FeatureName.STARTS_3_PRONOUN.toString(), texts.startsWith3rdPersonPronoun(rawWords));
		features.put(FeatureName.STARTS_CONNECTOR.toString(), texts.startsWithConnector(rawWords));
		features.put(FeatureName.AFTER_EXPLICIT.toString(), texts.containsExplicitCitation(Arrays.asList(prevWords), dataset.citedMainAuthor));
		features.put(FeatureName.AFTER_HEADING.toString(), previous != null ? texts.startsWithSectionHeader( previous.text.rawWords) : false);
		features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(rawWords));
		features.put(FeatureName.BEFORE_HEADING.toString(), next != null? texts.startsWithSectionHeader(next.text.rawWords) : false);
		features.put(FeatureName.CONTAINS_AUTHOR.toString(), texts.containsMainAuthor(rawWords, dataset.citedMainAuthor));
		features.put(FeatureName.CONTAINS_ACRONYM_SCORE.toString(), texts.containsAcronymScore(rawWords, dataset.getAcronyms()));
		features.put(FeatureName.CONTAINS_LEXICAL_HOOK_SCORE.toString(), texts.containsHookScore(sentence.text.raw, dataset.getLexicalHooks()));
		features.put(FeatureName.SENTENCE_NUMBER.toString(), sentenceIndex);
		features.put(FeatureName.TEXT.toString(), "'" + sentence.text.raw.replaceAll("'", "") + "'");
		
		Integer distPrevExpl = 4;
		Integer distNextExpl = 4;
		
		for(int i = 3; i >= 0; i--){
			int prevInd = sentenceIndex - i;
			int nextInd = sentenceIndex + i;
			if(prevInd >= 0 && sentences.get(prevInd).type == SentenceType.EXPLICIT_REFERENCE){
				distPrevExpl = i;
			}
			if(nextInd < sentences.size() - 1 && sentences.get(nextInd).type == SentenceType.EXPLICIT_REFERENCE){
				distNextExpl = i;
			}
		}
		
		features.put(FeatureName.DISTANCE_PREV_EXPLICIT.toString(), distPrevExpl);
		features.put(FeatureName.DISTANCE_NEXT_EXPLICIT.toString(), distNextExpl);
		features.put(FeatureName.SIMILAR_TO_EXPLICIT.toString(), sentence.text.similarity(dataset.mergedExplicitCitations));
		features.put(FeatureName.SIMILAR_TO_CITED_TITLE.toString(), sentence.text.similarity(dataset.citedTitle));
		features.put(FeatureName.SIMILAR_TO_CITED_CONTENT.toString(), sentence.text.similarity(dataset.citedContent));
		features.put(FeatureName.STARTS_DET.toString(), Texts.instance().startsWithDet(rawWords));
		features.put(FeatureName.CONTAINS_DET.toString(), Texts.instance().containsDet(rawWords));
		
		return features;
	}

}
