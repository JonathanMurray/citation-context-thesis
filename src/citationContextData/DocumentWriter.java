package citationContextData;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import util.NonThrowingFileWriter;

public class DocumentWriter {
	
	public static void main(String[] args) {
		File inFile = Paths.get("/home/jonathan/Documents/exjobb/data/teufel-citation-context-corpus/A92-1018.html").toFile();
		ContextDataSet dataset = new ContextHTML_Parser().parseHTML(inFile);
		Citer citer = dataset.citers.get(0);
		writeSentences(citer.sentences, Paths.get("TEST-WRITER.txt").toFile());
	}
	
	public static void writeSentences(List<Sentence> sentences, File outFile){
		NonThrowingFileWriter writer = new NonThrowingFileWriter(outFile);
		for(Sentence s : sentences){
			writer.write(s.unprocessedText + "\n");
		}
		writer.close(); 
	}

}
