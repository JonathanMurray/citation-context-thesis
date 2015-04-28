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
import dataset.CitingPaper;
import dataset.Dataset;
import dataset.Sentence;
import dataset.SentenceType;
import dataset.Text;
import dataset.TextWithSspace;
import dataset.Texts;


public class InstanceHandler {
	
	private static Printer printer = new Printer(true);
	
	public static void writeToArffFile(List<SentenceInstance> instances, File arffFile){
		
		printer.print("writing to .arff file: " + arffFile.getPath() + " ... ");
		
		List<String> numeric = Arrays.asList(new String[]{
				FeatureName.SENTENCE_NUMBER.toString(),
				FeatureName.ACRONYM.toString(),
				FeatureName.LEXICAL_HOOK.toString(),
				
				//enhanced features
				FeatureName.CITE_PREV_DISTANCE.toString(),
				FeatureName.CITE_NEXT_DISTANCE.toString(),
				FeatureName.CITE_SIMILARITY.toString(),
				FeatureName.TITLE_SIMILARITY.toString(),
				FeatureName.CONTENT_SIMILARITY.toString(),
				
				FeatureName.SEMANTIC_SIMILAR_TO_EXPLICIT.toString(),
				
				FeatureName.MRF_PROBABILITY.toString()
		});
		
		NonThrowingFileWriter writer = new NonThrowingFileWriter(arffFile);
		writer.write("@RELATION sentence\n");
		instances.get(0).features.keySet().stream().sorted().forEach(feature -> {
			if(!feature.equals(FeatureName.TEXT.toString())){//Must come last
				writer.write("@ATTRIBUTE " + feature + " ");
				if(feature.contains("UNIGRAM") || feature.contains("BIGRAM") || feature.contains("TRIGRAM")
						|| numeric.contains(feature)){
					writer.write("NUMERIC\n");
				}else if(feature.equals(FeatureName.TEXT.toString())){
					writer.write("STRING\n");
				}else if(feature.contains(FeatureName.SEMANTIC_VECTOR.toString())){
					writer.write("NUMERIC\n");
				}else{
					writer.write("{true, false}\n");
				}
			}
			
		});
		writer.write("@ATTRIBUTE " + FeatureName.TEXT.toString() + " STRING\n");
		writer.write("@ATTRIBUTE class {" + 
				SentenceType.IMPLICIT_REFERENCE + "," + 
				SentenceType.NOT_REFERENCE + "}\n");
		writer.write("@DATA\n");
		instances.forEach(instance -> {
			instance.features.entrySet().stream()
				.sorted((e1,e2)->e1.getKey().compareTo(e2.getKey()))
				.forEach(e -> {
					if(!e.getKey().equals(FeatureName.TEXT.toString())){ //Text is last
						writer.write(e.getValue() + ",");
					}
			});
			writer.write(instance.features.get(FeatureName.TEXT.toString()) + ",");
			writer.write(instance.instanceClass + "\n");
		});
		writer.close();
		printer.println("[x]");
	}
	
	public static <T extends Text> List<SentenceInstance> createInstances(List<Dataset<T>> datasets, 
			boolean onlyText, boolean balanceData){
		
		List<SentenceInstance> allInstances = new ArrayList<SentenceInstance>();
		for(Dataset<T> dataset : datasets){
			allInstances.addAll(createInstances(dataset, onlyText, balanceData, null));
		}
		return allInstances;
	}
	
	public static <T extends Text> HashMap<String, ArrayList<SentenceInstance>> createInstanceSets(List<Dataset<T>> datasets, 
			boolean onlyText, boolean balanceData){
		
		HashMap<String, ArrayList<SentenceInstance>> instanceSets = new HashMap<String, ArrayList<SentenceInstance>>();
		for(Dataset<T> dataset : datasets){
			instanceSets.put(dataset.datasetLabel, createInstances(dataset, onlyText, balanceData, null));
		}
		return instanceSets;
	}
	
	/**
	 * Sentences that are explicit references are excluded
	 * @param dataset
	 * @return
	 */
	public static <T extends Text> ArrayList<SentenceInstance> createInstances(Dataset<T> dataset, 
			boolean onlyText, boolean balanceData, List<Double> mrfClassificationProbabilities){
		
		ArrayList<SentenceInstance> instances = new ArrayList<SentenceInstance>();
		int totalSentenceIndex = 0;
		for(CitingPaper<T> citer : dataset.citers){
			for(int i = 0; i < citer.sentences.size(); i++){
				Sentence<T> previous = i > 0 ? citer.sentences.get(i-1) : null;
				Sentence<T> sentence = citer.sentences.get(i);
				Sentence<T> next = i < citer.sentences.size() - 1? citer.sentences.get(i+1) : null;
				Map<String,Comparable<?>> features;
				if(onlyText){
					features = extractFeatures(previous, sentence, next, dataset, onlyText, i);
				}else{
					features = extractFeaturesEnhanced(citer.sentences, i, dataset, mrfClassificationProbabilities, totalSentenceIndex);
				}
				if(sentence.type == SentenceType.EXPLICIT_REFERENCE){ //TODO
					continue; //Excluded
				}
				instances.add(new SentenceInstance(features, sentence.type));
				totalSentenceIndex ++;
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
			features.put(FeatureName.CITE_PREV.toString(), texts.containsExplicitCitation(Arrays.asList(prevWords), dataset.citedMainAuthor));
			features.put(FeatureName.AUTHOR.toString(), texts.containsMainAuthor(rawWords, dataset.citedMainAuthor));
			features.put(FeatureName.OTHER_CITE.toString(), texts.containsOtherReferencesButNotThis(sentence.text.raw, rawWords, dataset.citedMainAuthor));
			features.put(FeatureName.ACRONYM.toString(), texts.containsAcronymScore(rawWords, dataset.getAcronyms()));
			features.put(FeatureName.LEXICAL_HOOK.toString(), texts.containsHookScore(sentence.text.raw, dataset.getLexicalHooks()));
			features.put(FeatureName.DET_WORK.toString(), texts.containsDetWork(rawWords));
			features.put(FeatureName.PRONOUN.toString(), texts.startsWith3rdPersonPronoun(rawWords));
			features.put(FeatureName.CONNECTOR.toString(), texts.startsWithConnector(rawWords));
			features.put(FeatureName.HEADING_PREV.toString(), previous != null ? texts.startsWithSectionHeader( previous.text.rawWords) : false);
			features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(rawWords));
			features.put(FeatureName.HEADING_NEXT.toString(), next != null? texts.startsWithSectionHeader(next.text.rawWords) : false);
		}
		features.put(FeatureName.SENTENCE_NUMBER.toString(), sentenceNumber);
		features.put(FeatureName.TEXT.toString(), "'" + sentence.text.raw.replaceAll("'", "") + "'");
		return features;
	}
	
	private static <T extends Text> Map<String, Comparable<?>> extractFeaturesEnhanced(List<Sentence<T>> sentences, int sentenceIndex, Dataset<T> dataset, List<Double> mrfClassificationProbabilities, int totalSentenceIndex){
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
//		features.put(FeatureName.CITE_PREV.toString(), texts.containsExplicitCitation(Arrays.asList(prevWords), dataset.citedMainAuthor));
//		features.put(FeatureName.AUTHOR.toString(), texts.containsMainAuthor(rawWords, dataset.citedMainAuthor));
//		features.put(FeatureName.OTHER_CITE.toString(), texts.containsOtherReferencesButNotThis(sentence.text.raw, rawWords, dataset.citedMainAuthor));
//		features.put(FeatureName.ACRONYM.toString(), texts.containsAcronymScore(rawWords, dataset.getAcronyms()));
//		features.put(FeatureName.LEXICAL_HOOK.toString(), texts.containsHookScore(sentence.text.raw, dataset.getLexicalHooks()));
//		features.put(FeatureName.DET_WORK.toString(), texts.containsDetWork(rawWords));
//		features.put(FeatureName.PRONOUN.toString(), texts.startsWith3rdPersonPronoun(rawWords));
//		features.put(FeatureName.CONNECTOR.toString(), texts.startsWithConnector(rawWords));
//		features.put(FeatureName.HEADING_PREV.toString(), previous != null ? texts.startsWithSectionHeader( previous.text.rawWords) : false);
//		features.put(FeatureName.HEADING.toString(), texts.startsWithSectionHeader(rawWords));
//		features.put(FeatureName.HEADING_NEXT.toString(), next != null? texts.startsWithSectionHeader(next.text.rawWords) : false);
		features.put(FeatureName.TEXT.toString(), "'" + sentence.text.raw.replaceAll("'", "") + "'");
		features.put(FeatureName.SENTENCE_NUMBER.toString(), sentenceIndex);
		
//		Integer distPrevExpl = 4;
//		Integer distNextExpl = 4;	
//		for(int i = 3; i >= 0; i--){
//			int prevInd = sentenceIndex - i;
//			int nextInd = sentenceIndex + i;
//			if(prevInd >= 0 && sentences.get(prevInd).type == SentenceType.EXPLICIT_REFERENCE){
//				distPrevExpl = i;
//			}
//			if(nextInd < sentences.size() - 1 && sentences.get(nextInd).type == SentenceType.EXPLICIT_REFERENCE){
//				distNextExpl = i;
//			}
//		}
//		features.put(FeatureName.CITE_PREV_DISTANCE.toString(), distPrevExpl);
		
		//TODO
//		features.put(FeatureName.CITE_NEXT_DISTANCE.toString(), distNextExpl);
//		
//		features.put(FeatureName.TITLE_SIMILARITY.toString(), sentence.text.similarity(dataset.citedTitle));
//		features.put(FeatureName.CONTENT_SIMILARITY.toString(), sentence.text.similarity(dataset.citedContent));
//		features.put(FeatureName.CITE_SIMILARITY.toString(), sentence.text.similarity(dataset.mergedExplicitCitations));
//		features.put(FeatureName.STARTS_DET.toString(), Texts.instance().startsWithDet(rawWords));
//		features.put(FeatureName.CONTAINS_DET.toString(), Texts.instance().containsDet(rawWords));
//		
//		if(sentence.text instanceof TextWithSspace){
//			double[] vector = ((TextWithSspace)sentence.text).vector;
//			for(int i = 0; i < vector.length; i++){
//				features.put(FeatureName.SEMANTIC_VECTOR.toString() + "_" + i, vector[i]);
//			}
//			double semanticSimToExplicit = ((TextWithSspace)sentence.text).vectorSim((TextWithSspace)dataset.mergedExplicitCitations);
//			features.put(FeatureName.SEMANTIC_SIMILAR_TO_EXPLICIT.toString(), semanticSimToExplicit);
//		}
//		
		if(mrfClassificationProbabilities != null){
			double prob = mrfClassificationProbabilities.get(totalSentenceIndex);
			features.put(FeatureName.MRF_PROBABILITY.toString(), prob);
		}
		
		

		return features;
	}

}
