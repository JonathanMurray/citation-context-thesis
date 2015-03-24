package dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import util.NonThrowingFileWriter;
import util.Printer;

public class NgramIdf {
	
	public final static int DEFAULT_NGRAM_MIN_COUNT = 5;
	public final static int DEFAULT_SKIPGRAM_MIN_COUNT = 10;
	
	private final static String TAG_NGRAMS = "ngrams";
	private final static int MAX_N = 3;
	private final static Printer printer = new Printer(true);
	
	public Ngrams idf;
	public int numDocuments;
	
	public NgramIdf(){
		idf = Ngrams.empty(MAX_N);
	}
	
	public NgramIdf(Ngrams ngramsIdf, int numDocuments){
		this.idf = ngramsIdf;
		this.numDocuments = numDocuments;
	}
	
	public static NgramIdf fromXmlFile(File xmlFile, int minCount){
		try {
			printer.print("Creating idf from " + xmlFile.getPath() + " ... ");
			Document doc = Jsoup.parse(xmlFile, null);
			NgramIdf ngramIdf = fromXml(doc, minCount);
			printer.println("[x] (" + ngramIdf.idf.size(1) + " unigrams, " 
					+ ngramIdf.idf.size(2) + " bigrams, " + ngramIdf.idf.size(3) + " trigrams)");
			return ngramIdf;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static NgramIdf fromXml(Document doc, int minCount){
		int numDocuments = Integer.parseInt(doc.select("numDocuments").text());
		Ngrams ngrams = Ngrams.fromXml(doc.select(TAG_NGRAMS).first(), minCount);
		return new NgramIdf(ngrams, numDocuments);
	}
	
	public <T extends Text> NgramIdf parseDataset(Dataset<T> dataset, Type type){
		printer.print("NgramIdf (" + type.toString() + ") parsing dataset: " + dataset.datasetLabel + " ... ");
		//TODO citedContent is several sentences, which is a bit strange
		parseOneDocument(dataset.citedContent.lemmas, type); 
		printer.resetProgress();
		for(CitingPaper<T> c : dataset.citers){
			printer.progress();
			for(Sentence<T> s : c.sentences){
				parseOneDocument(s.text.lemmas, type);
			}
		}
		printer.println(" [x]");
		return this;
	}
	
	public NgramIdf parseOneDocument(List<String> words, Type type){
		int maxSkip = 2;
		switch(type){
		case NGRAM:
			Ngrams ngrams = new Ngrams(NgramExtractor.allNgrams(MAX_N, words));
			idf.add(ngrams, true);
			break;
		case SKIPGRAM:
			Ngrams skipgramsObj = new Ngrams(NgramExtractor.allSkipgrams(MAX_N, 0, maxSkip, words));
			idf.add(skipgramsObj, true);
			break;
		}
		numDocuments ++;
		return this;
	}
	
	public void writeXml(File xmlFile, int minCount){
		printer.print("Writing frequencies to " + xmlFile.getPath() + " ... ");
		Document doc = toXml(minCount);
		try(NonThrowingFileWriter writer = new NonThrowingFileWriter(xmlFile)){
			writer.write(doc.toString());
		}
		printer.println("[x]");
	}
	
	public Document toXml(int minCount){
		Document doc = new Document("");
		doc.appendElement("numDocuments").text("" + numDocuments);
		doc.appendChild(idf.toXml(TAG_NGRAMS, minCount));
		return doc;
	}
	
	public static enum Type{
		NGRAM,
		SKIPGRAM;
	}
}
