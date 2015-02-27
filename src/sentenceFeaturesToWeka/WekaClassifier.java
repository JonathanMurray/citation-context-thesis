package sentenceFeaturesToWeka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import util.ClassificationResult;
import util.ClassificationResultWrapper;
import util.Printer;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class WekaClassifier {
	
	private  Printer printer = new Printer(true);
	
	private Classifier classifier;
	private StringToWordVector filter;
	
	public WekaClassifier(){
		try {
			classifier = setupSMO();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private SMO setupSMO() throws Exception{
		SMO classifier = new SMO();
		String classifierOptions = "-C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1";
		String kernelOptions = "-E 1.0 -C 250007";
		classifier.setOptions(weka.core.Utils.splitOptions(classifierOptions));
		Kernel kernel = new PolyKernel();
		kernel.setOptions( weka.core.Utils.splitOptions(kernelOptions));
		classifier.setKernel(kernel);
		
		printer.println("Classifier-options: " + Arrays.toString(classifier.getOptions()));
		
		return classifier;
	}
	
	public void trainOnData(Instances data){
		try{
			printer.println("Training on " + data.size() + " instances");
			data = balanceData(data, countClasses(data));
			
			filter = createFilter(data);
			data = filterData(data, filter);
			
			classifier.buildClassifier(data);
			printer.println("Training done.");
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
//	private Instances resample(Instances data, double costFactor) throws Exception{
//		CostMatrix costMatrix = new CostMatrix(2);
//		costMatrix.setCell(0, 1, 1000.0);
//		System.out.println(costMatrix);
//		return costMatrix.applyCostMatrix(data, null);
//	}
	
	private int[] countClasses(Instances data){
		data.setClassIndex(data.numAttributes()-1);
		int[] counts = new int[2];
		for(Instance i : data){
			counts[(int) i.classValue()] ++;
		}
		return counts;
	}
	
	private static Instances fromFile(File arffFile){
		try {
			return new Instances(new BufferedReader(new FileReader(arffFile)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Instances fromFiles(File... files){
		try{
			Instances instances = null;
			for(File file : files){
				if(instances == null){
					instances = fromFile(file);
				}else{
					instances = merge(instances, fromFile(file));
				}
			}
			return instances;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static Instances fromDirExcept(File dir, File exception){
		
		ArrayList<File> files = new ArrayList(Arrays.asList(dir.listFiles()));
		
		files.remove(exception);
		return fromFiles(files.toArray(new File[0]));
//		return fromFiles(files.get(0), files.get(1));//TODO
	}
	
	public ClassificationResult testOnData(Instances data){
		try {
			data = filterData(data, filter);
			printer.println("testing on " + data.numInstances() + " instances");
			Evaluation eval = new Evaluation(data);
			eval.evaluateModel(classifier, data);
			printer.println(matrixToString(eval.confusionMatrix()));
			return new ClassificationResultWrapper(eval);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private Instances filterData(Instances data, StringToWordVector filter) throws Exception{
		data.setClassIndex(data.numAttributes() - 1);
		Instances filteredData = Filter.useFilter(data, filter);
		System.out.println("Filtering changed # attributes from " + data.numAttributes() + " to " + filteredData.numAttributes());
		return filteredData;
	}
	
	private StringToWordVector createFilter(Instances data) throws Exception{
		data.setClassIndex(data.numAttributes() - 1);
		String ngramOptions = "-max 3 -min 1 -delimiters \" \\r\\n\\t.,;:\\\'\\\"()?!\"";
		String[] ngramOptionsVec = weka.core.Utils.splitOptions(ngramOptions);
		String str2WordOptions = "-R 11 -P NGRAMS_ -W 1000 -prune-rate -1.0 -N 0 -L -stemmer weka.core.stemmers.LovinsStemmer -stopwords-handler weka.core.stopwords.Null -M 1";
		String[] string2WordOptions = weka.core.Utils.splitOptions(str2WordOptions);
		
		NGramTokenizer ngramTokenizer = new NGramTokenizer();
		ngramTokenizer.setOptions(ngramOptionsVec);
		
		StringToWordVector string2WordVec = new StringToWordVector();
		string2WordVec.setOptions(string2WordOptions);
		string2WordVec.setTokenizer(ngramTokenizer);
		string2WordVec.setInputFormat(data);
		printer.println("Filter-options: " + Arrays.toString(string2WordVec.getOptions()));
		return string2WordVec;
	}
	
	private Instances balanceData(Instances data, int[] classCounts){
		printer.println("class counts: " + Arrays.toString(classCounts));
		int quotient = classCounts[1] / classCounts[0];
		int i = 0;
		Instances balanced = new Instances(data);
		Iterator<Instance> it = balanced.iterator();
		while(it.hasNext()){
			Instance instance = it.next();
			if(instance.classValue() == 1){
				if(i < quotient){
					i++;
					it.remove();
				}else{
					i = 0;
				}
			}
		}
		printer.println("Balancing changed data size from " + data.size() + " to " + balanced.size());
		return balanced;
	}
	
	public ClassificationResult crossValidate(Instances data, int numFolds){
		try{
			
			data = balanceData(data, countClasses(data));
			
			data = filterData(data, createFilter(data));
//			data = resample(data, 10000);
			
			Evaluation eval = new Evaluation(data);
			printer.println("Validating ...");
			eval.crossValidateModel(classifier, data, numFolds, new Random(1));
			
			printer.println(eval.toSummaryString());
			printer.println("confusion matrix:");
			printer.println(matrixToString(eval.confusionMatrix()));
			printer.println("F-measure(0):" + eval.fMeasure(0));
			printer.println("F-measure(1):" + eval.fMeasure(1));
			printer.println("F-measure weighted: " + eval.weightedFMeasure());
			
			System.out.println("avg cost: " + eval.avgCost());
			return new ClassificationResultWrapper(eval);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private String matrixToString(double[][] m){
		return m[0][0] + "  " + m[0][1] + "\n" + m[1][0] + "  " + m[1][1];
	}
	
	/**
	 * @author mountrix (on stackoverflow)
	 */
	private static Instances merge(Instances data1, Instances data2) throws Exception {
	    // Check where are the string attributes
	    int asize = data1.numAttributes();
	    boolean strings_pos[] = new boolean[asize];
	    for(int i=0; i<asize; i++)
	    {
	        Attribute att = data1.attribute(i);
	        strings_pos[i] = ((att.type() == Attribute.STRING) ||
	                          (att.type() == Attribute.NOMINAL));
	    }

	    // Create a new dataset
	    Instances dest = new Instances(data1);
	    dest.setRelationName(data1.relationName() + "+" + data2.relationName());

	    DataSource source = new DataSource(data2);
	    Instances instances = source.getStructure();
	    Instance instance = null;
	    while (source.hasMoreElements(instances)) {
	        instance = source.nextElement(instances);
	        dest.add(instance);

	        // Copy string attributes
	        for(int i=0; i<asize; i++) {
	            if(strings_pos[i]) {
	                dest.instance(dest.numInstances()-1)
	                    .setValue(i,instance.stringValue(i));
	            }
	        }
	    }

	    return dest;
	}
}

