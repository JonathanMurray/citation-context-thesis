package wekaWrapper;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import util.Environment;
import util.Printer;
import util.Timer;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.J48;
import weka.classifiers.meta.Vote;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import dataset.Result;
import dataset.ResultWrapper;

public class WekaClassifier {
	
	private static Printer printer = new Printer(true);
	private static Printer debug = new Printer(false);
	
	private final boolean useVoting;
	private List<AbstractClassifier> votingClassifiers;
	private Vote vote;
	private AbstractClassifier classifier;
	private StringToWordVector filter;
	
	public WekaClassifier(AbstractClassifier classifier){
		this.classifier = classifier;
		debug.println("Classifier-options: " + Arrays.toString(classifier.getOptions()));
		useVoting = false;
	}
	
	public WekaClassifier(List<AbstractClassifier> votingClassifiers){
		this.votingClassifiers = votingClassifiers;
		useVoting = true;
	}
	
	public static WekaClassifier SMO(){
		try {
			return new WekaClassifier(setupSMO());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static WekaClassifier NaiveBayes(){
		return new WekaClassifier(new NaiveBayes());
	}
	
	public static WekaClassifier KNN(){
		return new WekaClassifier(new IBk());
	}
	
	public static WekaClassifier J48(){
		J48 j48 = new J48();
		return new WekaClassifier(j48);
	}
	
	public static WekaClassifier Adaboost(){
		AdaBoostM1 adaboost = new AdaBoostM1();
		return new WekaClassifier(adaboost);
	}
	
	public static WekaClassifier Vote(){
		try{
			List<AbstractClassifier> votingClassifiers = new ArrayList<AbstractClassifier>();
			votingClassifiers.add(setupSMO());
			votingClassifiers.add(new AdaBoostM1());
			votingClassifiers.add(new J48());
			return new WekaClassifier(votingClassifiers);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private static SMO setupSMO() throws Exception{
		SMO classifier = new SMO();
		String classifierOptions = "-N 0 -V 3";
		String kernelOptions = "-E 1.0 -C 250007";
		classifier.setOptions(weka.core.Utils.splitOptions(classifierOptions));
		Kernel kernel = new PolyKernel();
		kernel.setOptions( weka.core.Utils.splitOptions(kernelOptions));
		classifier.setKernel(kernel);
		return classifier;
	}
	
	public static Instances fromFiles(File... files){
		printer.print("Creating WEKA-instances from files: " + Arrays.toString(files) + " ... ");
		try{
			Instances instances = null;
			printer.resetProgress();
			for(int i = 0; i < files.length; i++){
				File file = files[i];
				printer.progress();
				if(instances == null){
					instances = fromFile(file);
				}else{
					instances = merge(instances, fromFile(file));
				}
			}
			System.out.println(" [x]");
			return instances;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static List<Instances> manyFromFiles(File... files){
		printer.print("Creating several WEKA-instance-sets from files: " + Arrays.toString(files) + " ... ");
		List<Instances> sets = new ArrayList<Instances>();
		for(File file : files){
			sets.add(fromFile(file));
		}
		return sets;
	}
	
	public static Instances fromDirExcept(File dir, File exception){
		
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(dir.listFiles()));
		
		files.remove(exception);
		return fromFiles(files.toArray(new File[0]));
//		return fromFiles(files.get(0), files.get(1));//TODO
	}
	
	public Result crossValidateMerged(String label, List<Instances> datasets, int numFolds){
		final boolean balanceData = false; 
		Instances merged = mergeDatasets(datasets, -1);
		return crossValidate(label, merged, numFolds, balanceData);
	}
	
	public List<Result> crossValidateMany(List<String> labels, List<Instances> datasets){
		final int numFolds = 10;
		final boolean balanceData = false; 
		ArrayList<Result> results = new ArrayList<Result>();
		for(int i = 0; i < labels.size(); i++){
			Instances dataset = datasets.get(i);
			String label = labels.get(i);
			Printer.printBigProgressHeader(i, labels.size());
			results.add(crossValidate(label, dataset, numFolds, balanceData));
		}
		return results;
	}
	
	public Result crossValidate(String label, Instances data, int numFolds, boolean balanceData){
		try{
			printer.print("Cross validating " + label + " (" + numFolds + " folds, " + data.numInstances() + " instances) ... ");
			Timer t = new Timer();
			if(balanceData){
				data = balanceData(data, countClasses(data));
			}
			filter = createNGramFilter(data,1,3);
			data = filterData(data, filter);
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(classifier, data, numFolds, new Random());
			ResultWrapper result = new ResultWrapper(label, eval, t.getMillis());
			printer.println("[x] (" + t.getSecString() + ")  Pos. F(1): " + result.positiveFMeasure(1) + " F(3): " + result.positiveFMeasure(3));
			return result; 
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public List<Result> manualCrossValidation(List<String> labels, List<Instances> balancedDatasets, List<Instances> fullDatasets){
		if(labels.size() != balancedDatasets.size() || labels.size() != fullDatasets.size()){
			throw new IllegalArgumentException(labels.size() + "  " + balancedDatasets.size() + "   " + fullDatasets.size());
		}
		List<Result> results = new ArrayList<Result>(); 
		for(int testIndex = 0; testIndex < balancedDatasets.size(); testIndex++){
			Printer.printBigProgressHeader(testIndex, balancedDatasets.size());
			Instances testSet = fullDatasets.get(testIndex);
			Instances trainSet = mergeDatasets(balancedDatasets, testIndex);
			trainOnData("merged", trainSet, false);
			String label = labels.get(testIndex);
			Result res = testOnData(label, testSet);
			results.add(res);
		}
		return results;
	}
	
	public static Instances mergeDatasets(List<Instances> datasets, int exceptIndex){
		Instances merged = null;
		printer.print("merging datasets ... ");
		for(int trainIndex = 0; trainIndex < datasets.size(); trainIndex++){
			if(trainIndex == exceptIndex){
				continue;
			}
			if(merged == null){
				merged = datasets.get(trainIndex);
			}else{
				merged = merge(merged, datasets.get(trainIndex));
			}
		}
		if(merged == null){
			throw new IllegalArgumentException(datasets + "     " + exceptIndex);
		}
		printer.println("[x]");
		return merged;
	}
	
	/**
	 * Train the classifier on given data. The data might be modified
	 * in the process, so don't reuse the data for another classifier!
	 * @param data
	 */
	public void trainOnData(String label, Instances data, boolean balanceData){
		try{
			printer.print("Training on " + label + " ... ");
			Timer t = new Timer().reset();
			if(balanceData){
				data = balanceData(data, countClasses(data));
			}
			filter = createNGramFilter(data,1,3);
			data = filterData(data, filter);
			if(useVoting){
				vote = new Vote();
				for(AbstractClassifier voter : votingClassifiers){
					voter.buildClassifier(data);
					vote.addPreBuiltClassifier(voter);
				}
			}else{
				classifier.buildClassifier(data);
			}
			printer.println("[x] (" + t.getSecString() + ")");
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}

	public Result testOnData(String label, Instances data){
		try {
			Timer t = new Timer().reset();
			printer.print("Testing on " + label + " ... ");
			data = filterData(data, filter);
			Evaluation eval = new Evaluation(data);
			if(useVoting){
				eval.evaluateModel(vote, data);
			}else{
				eval.evaluateModel(classifier, data);
			}
			
			ResultWrapper result = new ResultWrapper(label, eval, t.getMillis());
			printer.println("[x]  (" + t.getSecString() + ")  F1: " + result.positiveFMeasure(1) + ",  F3: " + result.positiveFMeasure(3) + "\n");
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public HashMap<String,Double> evaluateAttributes(Instances data){
		try {
			printer.print("Filtering data ... ");
			filter = createNGramFilter(data,1,3);
			data = filterData(data, filter);
			printer.println("[x]");
			HashMap<String, Double> infogains = new HashMap<String,Double>();
			InfoGainAttributeEval eval = new InfoGainAttributeEval();
			printer.print("Evaluating attributes ... ");
			eval.buildEvaluator(data);
			for(int i  = 0; i < data.numAttributes(); i++){
				String name = data.attribute(i).name();
				double infogain = eval.evaluateAttribute(i);
				infogains.put(name, infogain);
			}
			printer.println("[x]");
			return infogains;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void ROC(Instances data){
		try{
			data.setClassIndex(data.numAttributes()-1);
			System.out.println("count classes: " + Arrays.toString(countClasses(data)));
			data = filterData(data, createNGramFilter(data,2,3));
			
			Evaluation eval = new Evaluation(data);
			int numFolds = 5;
			eval.crossValidateModel(classifier, data, numFolds, new Random());
			int classIndex = 0;	
			plotROC_curve(eval.predictions(), classIndex);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static void plotROC_curve(ArrayList<Prediction> predictions, int classIndex){
		try{
			ThresholdCurve tc = new ThresholdCurve();
			Instances result = tc.getCurve(predictions, classIndex);
			System.out.println("area: " + ThresholdCurve.getROCArea(result));
		 
		     // plot curve
		    ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
		    vmc.setROCString("(Area under ROC = " + ThresholdCurve.getROCArea(result) + ")");
		    vmc.setName(result.relationName());
		    
		    System.out.println("Recall index: " + result.attribute("Recall").index());
		    System.out.println("Precision index: " + result.attribute("Precision").index());
		    
//		    vmc.setXIndex(result.attribute("Recall").index());
//		    vmc.setYIndex(result.attribute("Precision").index());
		    
		    PlotData2D tempd = new PlotData2D(result);
		    
		    
		    tempd.setPlotName(result.relationName());
		    tempd.addInstanceNumberAttribute();
		    
		    
		    // specify which points are connected
		    boolean[] cp = new boolean[result.numInstances()];
	     	for (int n = 1; n < cp.length; n++)
		       cp[n] = true;
		    tempd.setConnectPoints(cp);
		     
		    // add plot
		    vmc.addPlot(tempd);
		 
		    // display curve
		    String plotName = vmc.getName();
		    final javax.swing.JFrame jframe = new javax.swing.JFrame("Weka Classifier Visualize: "+plotName);
		    jframe.setSize(1366,768);
		    jframe.getContentPane().setLayout(new BorderLayout());
		    jframe.getContentPane().add(vmc, BorderLayout.CENTER);
		    jframe.addWindowListener(new java.awt.event.WindowAdapter() {
		    	public void windowClosing(java.awt.event.WindowEvent e) {
		    		System.out.println("window closing.");
		    		jframe.dispose();
			    }
			});
		    jframe.setVisible(true);
		    jframe.toFront();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private int[] countClasses(Instances data){
		for(int i = 0; i < data.numAttributes(); i++){
			System.out.println(i + ": " + data.attribute(i).name());
		}
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
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	private Instances filterData(Instances data, StringToWordVector filter) throws Exception{
		data.setClassIndex(data.numAttributes() - 1);
		Instances filteredData = Filter.useFilter(data, filter);
		debug.println("Filtering changed # attributes from " + data.numAttributes() + " to " + filteredData.numAttributes());
		return filteredData;
	}
	
	private StringToWordVector createNGramFilter(Instances data, int minNgram, int maxNgram) throws Exception{
		data.setClassIndex(data.numAttributes() - 1);
		String ngramOptions = "-max " + maxNgram + " -min " + minNgram + " -delimiters \" \\r\\n\\t.,;:\\\'\\\"()?!\"";
		String[] ngramOptionsVec = weka.core.Utils.splitOptions(ngramOptions);
		int textAttributeNumber = data.numAttributes() - 1; //Number rather than index
		String stopwordsFile = Environment.resources() + "/wordLists.stopwords.txt";
		
		final int minWords = 1;
		String str2WordOptions = "-C -R " + textAttributeNumber + 
			" -P NGRAMS_ -W 1000 -prune-rate -1.0 -N 1 -I -L " //counts, normalize, use tf-idf
			+ "-stemmer weka.core.stemmers.LovinsStemmer ";
//			+ "-stopwords-handler "
//			+ "\"weka.core.stopwords.WordsFromFile -stopwords " + stopwordsFile + " -M " + minWords + "\"";
		
		String[] string2WordOptions = weka.core.Utils.splitOptions(str2WordOptions);
		
		NGramTokenizer ngramTokenizer = new NGramTokenizer();
		ngramTokenizer.setOptions(ngramOptionsVec);
		
		StringToWordVector string2WordVec = new StringToWordVector();
		string2WordVec.setOptions(string2WordOptions);
		string2WordVec.setTokenizer(ngramTokenizer);
		string2WordVec.setInputFormat(data);
		debug.println("Filter-options: " + Arrays.toString(string2WordVec.getOptions()));
		return string2WordVec;
	}
	
	private Instances balanceData(Instances data, int[] classCounts){
//		printer.println("class counts: " + Arrays.toString(classCounts));
		int quotient = classCounts[1] / classCounts[0];
		int i = 0;
		Iterator<Instance> it = data.iterator();
		int sizeBefore = data.size();
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
		debug.println("Balancing changed data size from " + sizeBefore + " to " + data.size());
		return data;
	}
	
	private String matrixToString(double[][] m){
		return m[0][0] + "  " + m[0][1] + "\n" + m[1][0] + "  " + m[1][1];
	}
	
	/**
	 * @author mountrix (on stackoverflow)
	 */
	private static Instances merge(Instances data1, Instances data2) {
	   try{
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
		    if(data1.relationName().equals(data2.relationName())){
		    	dest.setRelationName(data1.relationName());
		    }else{
		    	dest.setRelationName(data1.relationName() + "+" + data2.relationName());
		    }

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
	   }catch(Exception e){
		   e.printStackTrace();
		   System.exit(0);
		   return null;
	   }
	}
}

