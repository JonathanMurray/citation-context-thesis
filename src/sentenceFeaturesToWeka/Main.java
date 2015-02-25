package sentenceFeaturesToWeka;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.SentenceClass;

class Main {
	
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
				.map(f -> parser.parseHTML(f))
				.collect(Collectors.toList());
		FeatureExtractor extractor = new FeatureExtractor();
		
		List<Instance> instances = datasets.stream()
			.flatMap(dataset -> extractor.createInstances(dataset).stream())
			.collect(Collectors.toCollection(ArrayList::new));
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE).limit(10000),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toList());
		
		
		
		extractor.writeInstancesToFile(instances, Paths.get("src/ml/data/instances.arff"));
	}
}

