package util;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import citationContextData.CitingPaper;
import citationContextData.Dataset;
import citationContextData.Sentence;
import citationContextData.Text;
import citationContextData.TextWithConcepts;
import citationContextData.Xml;

public class NgramIdf {
	
	private final int MIN_COUNT = 4;

	private final static Printer printer = new Printer(true);
	
	public static void main(String[] args) {
		File datasetXmlFile = new File(Environment.resources(), "xml-datasets/A92-1018-with-concepts.xml");
		Dataset<TextWithConcepts> dataset = Xml.parseXmlFile(datasetXmlFile, 0);
		File outXmlFile = new File(Environment.resources(), "xml-datasets/ngram-frequencies.xml");
		new NgramIdf().parseDataset(dataset).writeXml(outXmlFile);
//		WordIdf.fromXmlFile(outXmlFile);
	}
	
	private TObjectIntHashMap<String> unigramIdf;
	private TObjectIntHashMap<String> bigramIdf;
	public int numDocuments;
	
	public int getIdf(int n, String ngram){
		if(n == 1){
			return unigramIdf.containsKey(ngram) ? unigramIdf.get(ngram) : 0;
		}else if(n == 2){
			return bigramIdf.containsKey(ngram) ? bigramIdf.get(ngram) : 0;
		}
		throw new IllegalArgumentException("invalid ngram: " + n);
	}
	
	public NgramIdf(){
		this.unigramIdf = new TObjectIntHashMap<String>();
		this.bigramIdf = new TObjectIntHashMap<String>();
	}
	
	public NgramIdf(TObjectIntHashMap<String> unigramIdf, TObjectIntHashMap<String> bigramIdf, int numDocuments){
		this.unigramIdf = unigramIdf;
		this.bigramIdf = bigramIdf;
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
		TObjectIntHashMap<String> unigramFrequencies = new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> bigramFrequencies = new TObjectIntHashMap<String>();
		int numDocuments = Integer.parseInt(doc.select("numDocuments").text());
		for(Element unigramTag : doc.select("unigrams").select("unigram")){
			int count = Integer.parseInt(unigramTag.attr("count"));
			unigramFrequencies.adjustOrPutValue(unigramTag.text(), count, count);
		}
		for(Element bigramTag : doc.select("bigrams").select("bigram")){
			int count = Integer.parseInt(bigramTag.attr("count"));
			bigramFrequencies.adjustOrPutValue(bigramTag.text(), count, count);
		}
		return new NgramIdf(unigramFrequencies, bigramFrequencies, numDocuments);
	}
	
	public NgramIdf parseOneDocument(List<String> text){
		TObjectDoubleHashMap<String> unigrams = Texts.instance().getNgrams(1, text, true);
		addOther(unigrams, unigramIdf);
		TObjectDoubleHashMap<String> bigrams = Texts.instance().getNgrams(2, text, true);
		addOther(bigrams, bigramIdf);
		numDocuments ++;
		return this;
	}
	
	private void addOther(TObjectDoubleHashMap<String> other, TObjectIntHashMap<String> mine){
		TObjectDoubleIterator<String> it = other.iterator();
		while(it.hasNext()){
			it.advance();
			mine.adjustOrPutValue(it.key(), 1, 1); //TODO only increment by 1, since it's IDF
		}
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
		Element unigramsTag = doc.appendElement("unigrams");
		TObjectIntIterator<String> uniIt = unigramIdf.iterator();
		while(uniIt.hasNext()){
			uniIt.advance();
			if(uniIt.value() >= MIN_COUNT){
				unigramsTag.appendElement("unigram")
				.attr("count", "" + uniIt.value())
				.text(uniIt.key());
			}
		}
		Element bigramsTag = doc.appendElement("bigrams");
		TObjectIntIterator<String> biIt = bigramIdf.iterator();
		while(biIt.hasNext()){
			biIt.advance();
			if(biIt.value() >= MIN_COUNT){
				bigramsTag.appendElement("bigram")
				.attr("count", "" + biIt.value())
				.text(biIt.key());
			}
		}
		return doc;
	}
	
	
}
