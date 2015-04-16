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

import util.Environment;
import util.Lemmatizer;
import util.Printer;
import wekaWrapper.SentenceInstance;

public class LemmatizeCorpus {
	
	static Printer printer = new Printer(true);
	static Pattern NUMBER = Pattern.compile("\\d+");
	
	static HashSet<String> bad = new HashSet<String>(Arrays.asList(new String[]{
			"-lrb-", "-rrb-", "-lsb-", "-rsb-", ".", ",", ":", "-", ";", "!", "?"
	}));
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		String fullTextDir = "full-text";
		String lemmaDir = "lemmas-sentences";
		boolean splitSentences = true;
		if(args.length == 3){
			fullTextDir = args[0];
			lemmaDir = args[1];
			splitSentences = Boolean.parseBoolean(args[2]);
		}else if(args.length != 0){
			System.out.println("Usage:");
			System.out.println("0 args or");
			System.out.println("3 args: 'full_text_dir' 'lemma_dir' 'split_sentences'");
			return;
		}
		
		File in = new File(Environment.resources() + "/corpus/" + fullTextDir);
		File out = new File(Environment.resources() + "/corpus/" + lemmaDir);
		lemmatize(in, out, splitSentences);
	}
	
	static void lemmatize(File inDir, File outDir, boolean splitSentences) throws FileNotFoundException, IOException{
		
		Printer.printBigHeader("Lemmatize corpus");
		System.out.println();
		System.out.println("from " + inDir.getName());
		System.out.println("to " + outDir.getName());
		System.out.println();
		
		printer.resetProgress();
		for(File txtFile : Arrays.asList(inDir.listFiles())){
			printer.progress();
//			System.out.print(txtFile.getName() + " ... ");
			String text = IOUtils.toString(new FileReader(txtFile));
			System.out.print("Getting lemmas ... ");
			List<String> lemmas = Lemmatizer.instance().lemmatize(text);
			System.out.println("[x]");
//			new ArrayList().subList(fromIndex, toIndex)
			if(splitSentences){
				int prevBoundary = -1;
				int nextBoundary;
				int sentenceIndex = 1;
//				System.out.println("lemmas size: " + lemmas.size());
				while(true){
//					System.out.println();
//					System.out.println("previous boundary: " + prevBoundary);
//					System.out.println(sentenceIndex);
					int nextBoundaryLocal = lemmas.subList(prevBoundary + 1, lemmas.size()).indexOf(".");
					if(nextBoundaryLocal == -1){
						nextBoundary = lemmas.size();
					}else{
						nextBoundary = nextBoundaryLocal + prevBoundary + 1;
					}
					String fileName = txtFile.getName().substring(0, txtFile.getName().length() - 4) + "-" + sentenceIndex + ".txt";
					sentenceIndex ++;
					File f = new File(outDir, fileName);
					writeLemmasToFile(lemmas.subList(prevBoundary+1, nextBoundary), f);
					if(nextBoundary >= lemmas.size() - 1){
						break;
					}
					prevBoundary = nextBoundary;
				}
			}else{
				File outFile = new File(outDir, txtFile.getName());
				writeLemmasToFile(lemmas, outFile);				
			}
//			System.out.println("[x]");
//			fileIndex ++;
		}
	}
	
	private static void writeLemmasToFile(List<String> lemmas, File outFile) throws IOException{
		try{
			FileWriter out = new FileWriter(outFile);
			List<String> cleanLemmas = new ArrayList<String>();
			for(int i = 0; i < lemmas.size(); i++){
//				boolean stopword = Texts.instance().isStopword(lemmas.get(i));
				boolean number = NUMBER.matcher(lemmas.get(i)).matches();
				if(!bad.contains(lemmas.get(i)) && !number){
					cleanLemmas.add(lemmas.get(i));
				}
				boolean lastOrFirst = i == 0 || i == lemmas.size() - 1;
				if(lemmas.get(i).equals("-") && !lastOrFirst && !cleanLemmas.isEmpty()){
					// "w1 - w2" --[merge_to]--> "w1w2"
					String w1 = lemmas.get(i-1);
					String w2 = lemmas.get(i+1);
					cleanLemmas.set(cleanLemmas.size()-1, w1+w2);
					i++; //Skip w2
				}
			}
			
			for(String cleanLemma : cleanLemmas){
				out.write(cleanLemma + " ");
			}
			out.close();
//			System.out.println("[x]");
		}catch(Exception e){
			e.printStackTrace(System.out);
			System.out.println();
			System.out.println("lemmas: " + lemmas);
			System.exit(0);
		}
	}
}
