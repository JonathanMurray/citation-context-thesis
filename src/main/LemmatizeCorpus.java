package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tika.io.IOUtils;

import dataset.Texts;
import util.Environment;
import util.Lemmatizer;

public class LemmatizeCorpus {
	
	static Pattern NUMBER = Pattern.compile("\\d+");
	
	static HashSet<String> bad = new HashSet<String>(Arrays.asList(new String[]{
			"-lrb-", "-rrb-", "-lsb-", "-rsb-", ".", ",", ":", "-", ";", "!", "?"
	}));
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File in = new File(Environment.resources() + "/corpus/full-text");
		File out = new File(Environment.resources() + "/corpus/lemmas");
		lemmatize(in, out);
	}
	
	static void lemmatize(File inDir, File outDir) throws FileNotFoundException, IOException{
		
		for(File txtFile : inDir.listFiles()){
			System.out.print(txtFile.getName() + " ... ");
			String text = IOUtils.toString(new FileReader(txtFile));
			List<String> lemmas = Lemmatizer.instance().lemmatize(text);
			File outFile = new File(outDir, txtFile.getName());
			FileWriter out = new FileWriter(outFile);
			List<String> cleanLemmas = new ArrayList<String>();
			for(int i = 0; i < lemmas.size(); i++){
				boolean stopword = Texts.instance().isStopword(lemmas.get(i));
				boolean number = NUMBER.matcher(lemmas.get(i)).matches();
				if(!bad.contains(lemmas.get(i)) && !stopword && !number){
					cleanLemmas.add(lemmas.get(i));
				}
				boolean lastOrFirst = i == 0 || i == lemmas.size() - 1;
				if(lemmas.get(i).equals("-") && !lastOrFirst){
					// "w1 - w2" --[merge_to]--> "w1w2"
					cleanLemmas.set(cleanLemmas.size()-1, lemmas.get(i-1) + lemmas.get(i+1));
					i++; //Skip w2
				}
			}
			
			for(String cleanLemma : cleanLemmas){
				out.write(cleanLemma + " ");
			}
			out.close();
			System.out.println("[x]");
		}
	}
}
