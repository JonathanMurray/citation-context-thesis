package ml;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	
	
	public static void main(String[] args){
		AZ_XML_Parser parser = new AZ_XML_Parser();
		List<Sentence> sentences = parser.parseSentences(new File(CFC_Dir + "9405001.cfc-scixml"));
		Stream<String> authors = sentences.stream()
				.flatMap(sentence -> sentence.cited.stream())
				.distinct();
		Set<String> mainAuthors = authors.map(citation -> citation.trim().split(" ")[0])
				.collect(Collectors.toSet());
		SentenceFeatureExtractor featureExtractor = new SentenceFeatureExtractor();
		List<Instance> instances = featureExtractor.extractInstances(sentences, mainAuthors);
		System.out.println(Arrays.toString(instances.toArray()));
		featureExtractor.writeInstancesToFile(instances, Paths.get("src/ml/instances.arff"));
	}
}
