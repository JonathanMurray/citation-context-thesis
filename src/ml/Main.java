package ml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import util.IncrementableMap;

public class Main {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	public static final String sentimentCorpusDir = dataDir + "teufel-citation-context-corpus/";
	

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		setupForWEKA();
	}
	
	private static void setupForWEKA(){
		ContextHTML_Parser parser = new ContextHTML_Parser();
		File[] files = Paths.get(sentimentCorpusDir).toFile().listFiles();
		List<ContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> parser.readJsoup(f))
				.collect(Collectors.toList());
		FeatureExtractor extractor = new FeatureExtractor();
		
//		IncrementableMap<String> implicitUnigrams = IncrementableMap.countAndMergeTopOccurences(10, datasets.stream()
//				.map(dataset -> dataset.getUnigramsInImplicitReferences()));
//		
//		
//		IncrementableMap<String> unigramCounts = new IncrementableMap<String>();
//		datasets.stream().forEach(dataset -> {
//			for(Entry<String,Integer> e : dataset.getUnigramsInImplicitReferences().getTopN(10)){
//				unigramCounts.increment(e.getKey(), 1);
//			}
//		});
//		
//		ArrayList<String> unigrams = unigramCounts.getTopN(12).stream()
//				.map(e -> e.getKey())
//				.collect(Collectors.toCollection(ArrayList::new));
//		System.out.println(unigrams);
//		
//		IncrementableMap<String> bigramCounts = new IncrementableMap<String>();
//		datasets.stream().forEach(dataset -> {
//			for(Entry<String,Integer> e : dataset.findBigrams().getTopN(10)){
//				bigramCounts.increment(e.getKey(), 1);
//			}
//		});
//		ArrayList<String> bigrams = bigramCounts.getTopN(12).stream()
//				.map(e -> e.getKey())
//				.collect(Collectors.toCollection(ArrayList::new));
//		System.out.println(bigrams);
		
		NGrams ngrams = NGrams.fromDatasets(datasets);
		
		List<Instance> instances = datasets.stream()
			.flatMap(dataset -> extractor.createInstances(dataset, ngrams).stream())
			.collect(Collectors.toCollection(ArrayList::new));
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceType.NOT_REFERENCE).limit(2000),
				instances.stream().filter(i -> i.instanceClass == SentenceType.IMPLICIT_REFERENCE)
		).collect(Collectors.toList());
		
		extractor.writeInstancesToFile(instances, Paths.get("src/ml/data/instances.arff"));
	}

	
	private static void testAZ(){
//		AZ_XML_Parser parser = new AZ_XML_Parser();
//		List<OldSentence> sentences = parser.parseSentences(new File(CFC_Dir + "9405001.cfc-scixml"));
//		Stream<String> authors = sentences.stream()
//				.flatMap(sentence -> sentence.cited.stream())
//				.distinct();
//		Set<String> mainAuthors = authors.map(citation -> citation.trim().split(" ")[0])
//				.collect(Collectors.toSet());
//		SentenceFeatureExtractor featureExtractor = new SentenceFeatureExtractor();
//		List<Instance> instances = featureExtractor.extractInstances(sentences, mainAuthors);
//		System.out.println(Arrays.toString(instances.toArray()));
//		featureExtractor.writeInstancesToFile(instances, Paths.get("src/ml/data/instances.arff"));
	}
}
