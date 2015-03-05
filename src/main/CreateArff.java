package main;

import java.io.File;
import java.util.List;

import util.Dirs;
import wekaWrapper.InstanceHandler;
import wekaWrapper.SentenceInstance;

public class CreateArff {
	
	public static void main(String[] args) {
		System.out.println("Create ARFF-file");
		System.out.println("--------------------------");
		List<SentenceInstance> instances = InstanceHandler.createInstancesFromHTMLFiles(new File(Dirs.resources(), "teufel-citation-context-corpus").listFiles(), false, true);
		InstanceHandler.writeToArffFile(instances, new File(Dirs.resources(), "arff/balanced-features-full-dataset.arff"));
		
	}
}
