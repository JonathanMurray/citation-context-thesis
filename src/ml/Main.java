package ml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Main {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	public static final String sentimentCorpusDir = dataDir + "teufel-citation-context-corpus/";
	

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		setupForWEKA();
	}
	
	private static void setupForWEKA(){
		CitationContextHTML_Parser parser = new CitationContextHTML_Parser();
		File[] files = Paths.get(sentimentCorpusDir).toFile().listFiles();
//		File file = Paths.get(sentimentCorpusDir + "A92-1018.html").toFile();
		List<CitationContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> parser.readJsoup(f))
				.collect(Collectors.toList());
		
//		CitationContextDataSet dataset = parser.readJsoup(file);
//		dataset.setup();
		SentenceFeatureExtractor extractor = new SentenceFeatureExtractor();
		
		List<String> unigrams = datasets.stream()
			.flatMap(dataset -> dataset.findUnigrams(10).stream())
			.distinct()
			.collect(Collectors.toList());
		System.out.println(unigrams);
		NGrams ngrams = new NGrams(unigrams, new ArrayList<String>(), new ArrayList<String>());
		
		List<Instance> instances = datasets.stream()
			.flatMap(dataset -> extractor.extractInstances(dataset, ngrams).stream())
			.collect(Collectors.toCollection(ArrayList::new));
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceType.NOT_REFERENCE).limit(1000),
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
