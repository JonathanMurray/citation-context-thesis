package main;

import java.io.File;
import java.util.List;

import util.Environment;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SentenceInstance;

public class CreateArff {
	
	public static void main(String[] args) {
		System.out.println("Create ARFF-file");
		System.out.println("--------------------------");
		List<SentenceInstance> instances = InstanceHandler.createInstancesFromHTMLFiles(new File(Environment.resources(), "teufel-citation-context-corpus").listFiles(), 20, 5, false, true);
		InstanceHandler.writeToArffFile(instances, new File(Environment.resources(), "arff/balanced-features-full-dataset.arff"));
		
	}
}
