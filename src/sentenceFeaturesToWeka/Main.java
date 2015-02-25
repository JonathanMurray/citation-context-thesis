package sentenceFeaturesToWeka;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;
import citationContextData.SentenceClass;

class Main {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String CFC_Dir = dataDir + "CFC_distribution/2006_paper_training/";
	public static final String sentimentCorpusDir = dataDir + "teufel-citation-context-corpus/";
	

	public static void main(String[] args) throws Exception {
		Path path = Paths.get("src/util/data/instances.arff");
		runWeka(path);
	}
	
	private static void runWeka(Path path) throws Exception{
		runWeka(new DataSource(new FileInputStream(path.toFile())));
	}
	
	private static void runWeka(DataSource source) throws Exception{
		String ngramOptions = "-max 3 -min 1 -delimiters \" \\r\\n\\t.,;:\\\'\\\"()?!\"";
		String str2WordOptions = "-R 11 -P NGRAMS_ -W 1000 -prune-rate -1.0 -N 0 -L -stemmer weka.core.stemmers.LovinsStemmer -stopwords-handler weka.core.stopwords.Null -M 1";
		String classifierOptions = "-C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1";
		String kernelOptions = "-E 1.0 -C 250007";
		runWeka(source, ngramOptions, str2WordOptions, kernelOptions, classifierOptions);
	}
	
	private static void runWeka(DataSource source, String ngramOptions, String str2WordOptions, String kernelOptions, String classifierOptions) throws Exception{
		Instances data = source.getDataSet();
		data.setClassIndex(data.numAttributes() - 1);
		
		NGramTokenizer ngramTokenizer = new NGramTokenizer();
		String[] ngramOptionsVec = weka.core.Utils.splitOptions(ngramOptions);
		ngramTokenizer.setOptions(ngramOptionsVec);
		
		StringToWordVector string2WordVec = new StringToWordVector();
		String[] string2WordOptions = weka.core.Utils.splitOptions(str2WordOptions);
		string2WordVec.setOptions(string2WordOptions);
		string2WordVec.setTokenizer(ngramTokenizer);
		string2WordVec.setInputFormat(data);
		
		Instances filteredData = Filter.useFilter(data, string2WordVec);
		
		
		SMO classifier = new SMO();
		
		classifier.setOptions(weka.core.Utils.splitOptions(classifierOptions));
		Kernel polyKernel = new PolyKernel();
		String[] polyKernelOptions = weka.core.Utils.splitOptions(kernelOptions);
		polyKernel.setOptions(polyKernelOptions);
		classifier.setKernel(polyKernel);
		
		System.out.println("Training classifier ...\n");
		
		classifier.setDebug(true);
		
		classifier.buildClassifier(filteredData);
		
		Evaluation eval = new Evaluation(filteredData);
		System.out.println("Validating ...");
		eval.crossValidateModel(classifier, filteredData, 2, new Random(1));
		System.out.println(eval.toSummaryString());
		System.out.println("confusion matrix:");
		double[][] cm = eval.confusionMatrix();
		System.out.println(cm[0][0] + "  " + cm[0][1] + "\n" + cm[1][0] + "  " + cm[1][1]);
		System.out.println("F-measure(0):" + eval.fMeasure(0));
		System.out.println("F-measure(1):" + eval.fMeasure(1));
	}
	
	private static void createArffFile(Path path){
		List<Instance> instances = createInstances();
		FeatureExtractor.writeInstancesToFile(instances, path);
	}
	
	private static List<Instance> createInstances(){
		File[] files = Paths.get(sentimentCorpusDir).toFile().listFiles();
		List<ContextDataSet> datasets = Arrays.asList(files).stream()
				.map(f -> ContextHTML_Parser.parseHTML(f))
				.collect(Collectors.toList());
		List<Instance> instances = datasets.stream()
			.flatMap(dataset -> FeatureExtractor.createInstances(dataset).stream())
			.collect(Collectors.toCollection(ArrayList::new));
		instances = Stream.concat(
				instances.stream().filter(i -> i.instanceClass == SentenceClass.NOT_REFERENCE).limit(10000),
				instances.stream().filter(i -> i.instanceClass == SentenceClass.IMPLICIT_REFERENCE)
		).collect(Collectors.toList());
		return instances;
	}
}

