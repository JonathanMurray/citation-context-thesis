package dataset;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import util.NonThrowingFileWriter;
import util.Printer;

public class NgramIdf {
	
	private final static String TAG_NGRAMS = "ngrams";
	private final static int MAX_N = 3;
	private final static Printer printer = new Printer(true);
	
	public Ngrams ngramsIdf;
	public int numDocuments;
	
	public NgramIdf(){
		ngramsIdf = Ngrams.empty(MAX_N);
	}
	
	public NgramIdf(Ngrams ngramsIdf, int numDocuments){
		this.ngramsIdf = ngramsIdf;
		this.numDocuments = numDocuments;
	}
	
	public static NgramIdf fromXmlFile(File xmlFile){
		try {
			printer.print("Creating wordIdf from file " + xmlFile.getPath() + " ... ");
			Document doc = Jsoup.parse(xmlFile, null);
			NgramIdf wordIdf = fromXml(doc);
			printer.println("[x]");
			return wordIdf;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static NgramIdf fromXml(Document doc){
		int numDocuments = Integer.parseInt(doc.select("numDocuments").text());
		Ngrams ngrams = Ngrams.fromXml(doc.select(TAG_NGRAMS).first());
		return new NgramIdf(ngrams, numDocuments);
	}
	
	public <T extends Text> NgramIdf parseDataset(Dataset<T> dataset){
		printer.print("NgramIdf parse dataset: " + dataset.datasetLabel + " ... ");
		parseOneDocument(dataset.citedContent.lemmas); //TODO citedContent might be several sentences, which is a bit strange
		int i  = 0;
		for(CitingPaper<T> c : dataset.citers){
			i++;
			printer.progress(i, 1);
			for(Sentence<T> s : c.sentences){
				parseOneDocument(s.text.lemmas);
			}
		}
		printer.println(" [x]");
		return this;
	}
	
	public NgramIdf parseOneDocument(List<String> words){
		Ngrams ngrams = Texts.instance().getAllNgrams(MAX_N, words);
		ngramsIdf.add(ngrams, true);
		numDocuments ++;
		return this;
	}
	
	public void writeXml(File xmlFile){
		printer.print("Writing frequencies to " + xmlFile.getPath() + " ... ");
		Document doc = toXml();
		try(NonThrowingFileWriter writer = new NonThrowingFileWriter(xmlFile)){
			writer.write(doc.toString());
		}
		printer.println("[x]");
	}
	
	public Document toXml(){
		Document doc = new Document("");
		doc.appendElement("numDocuments").text("" + numDocuments);
		doc.appendChild(ngramsIdf.toXml(TAG_NGRAMS));
		return doc;
	}
}
