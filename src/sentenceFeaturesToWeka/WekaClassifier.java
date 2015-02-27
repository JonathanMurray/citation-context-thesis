package sentenceFeaturesToWeka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import util.ClassificationResult;
import util.ClassificationResultWrapper;
import util.Printer;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Instances;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class WekaClassifier {
	
	private static Printer printer = new Printer(true);
	
	
	public static ClassificationResult trainOnArffFile(Path path){
		try {
			Instances data = new Instances(new BufferedReader(new FileReader(path.toFile())));
			return trainOnData(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ClassificationResult trainOnData(Instances data){
		String ngramOptions = "-max 3 -min 1 -delimiters \" \\r\\n\\t.,;:\\\'\\\"()?!\"";
		String str2WordOptions = "-R 11 -P NGRAMS_ -W 1000 -prune-rate -1.0 -N 0 -L -stemmer weka.core.stemmers.LovinsStemmer -stopwords-handler weka.core.stopwords.Null -M 1";
		String classifierOptions = "-C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1";
		String kernelOptions = "-E 1.0 -C 250007";
		try{
			return train(data, ngramOptions, str2WordOptions, kernelOptions, classifierOptions);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private static ClassificationResult train(Instances data, String ngramOptions, String str2WordOptions, String kernelOptions, String classifierOptions) throws Exception{
		
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
		
		SMO smo = new SMO();
		smo.setOptions(weka.core.Utils.splitOptions(classifierOptions));
		Kernel polyKernel = new PolyKernel();
		String[] polyKernelOptions = weka.core.Utils.splitOptions(kernelOptions);
		polyKernel.setOptions(polyKernelOptions);
		smo.setKernel(polyKernel);
		
		
		CostMatrix costMatrix = new CostMatrix(2);
		costMatrix.setCell(0, 1, 100.0);
		filteredData = costMatrix.applyCostMatrix(filteredData, new Random());
		System.out.println(costMatrix);
		
//		printer.println("Training classifier ...\n");
//		smo.buildClassifier(filteredData);
		
		Evaluation eval = new Evaluation(filteredData);
		printer.println("Validating ...");
		eval.crossValidateModel(smo, filteredData, 4, new Random(1));
		
		printer.println(eval.toSummaryString());
		printer.println("confusion matrix:");
		double[][] cm = eval.confusionMatrix();
		printer.println(cm[0][0] + "  " + cm[0][1] + "\n" + cm[1][0] + "  " + cm[1][1]);
		printer.println("F-measure(0):" + eval.fMeasure(0));
		printer.println("F-measure(1):" + eval.fMeasure(1));
		printer.println("F-measure weighted: " + eval.weightedFMeasure());
		
		System.out.println("avg cost: " + eval.avgCost());
		
		return new ClassificationResultWrapper(eval);
	}
	
	
}

