package citationContextData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import util.Printer;

public class Xml {
	
	private static final Printer printer = new Printer(true);
	
	private static final String TAG_DATASETS = "datasets";
	private static final String TAG_DATASET = "dataset";
	private static final String TAG_DATASET_LABEL = "label";
	private static final String TAG_CITED = "cited";
	private static final String TAG_CITERS = "citers";
	private static final String TAG_CITER = "citer";
	private static final String TAG_SENTENCES = "sentences";
	private static final String TAG_SENTENCE = "sentence";
	private static final String TAG_TEXT = "text";
	private static final String ATTR_AUTHOR = "main-author";
	private static final String ATTR_TITLE = "title";
	private static final String ATTR_SENTENCE_TYPE = "type";
	
	public static void main(String[] args) {
		File dir = new File("/home/jonathan/Documents/eclipse-workspace/exjobb/resources/teufel-citation-context-corpus");
//		Dataset<TextWithNgrams> d = DatasetFactory.fromFiles(
//				DatasetParams.basic(TextParams.basic(TextWithNgrams.class)), 
//				new File(dir, "A92-1018.html"), 
//				new File(dir, "A92-1018.txt"));
//		System.out.println("DATASET: " + d);
//		writeToXml(d, new File(dir, "TEST-ngrams.xml"));
		Dataset<TextWithNgrams> dataset = parseFromXml(new File(dir, "TEST-ngrams.xml"));
		System.out.println("dataset: " + dataset);
	}
	
	public static <T extends Text> void writeToXml(Dataset<T> dataset, File file){
		try(FileWriter writer = new FileWriter(file)){
			printer.print("Constructing XML ... ");
			Document doc = constructXml(dataset);
			printer.println("[x]");
			printer.print("Writing XML ... ");
			writer.write(doc.toString());
			printer.println("[x]");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static <T extends Text> Document constructXml(Dataset<T> dataset){
		Document doc = new Document("");
		Element datasetTag = doc.appendElement(TAG_DATASETS).appendElement(TAG_DATASET);
		datasetTag.appendElement(TAG_DATASET_LABEL).text(dataset.datasetLabel);
		Element cited = datasetTag.appendElement(TAG_CITED);
		cited.attr(ATTR_AUTHOR, dataset.citedMainAuthor);
		cited.attr(ATTR_TITLE, dataset.citedTitle);
		cited.appendChild(dataset.citedContent.toXml());
		
		Element citers = datasetTag.appendElement(TAG_CITERS);
		for(CitingPaper<T> citer : dataset.citers){
			citers.appendChild(citer(citer));
		}
		return doc;
	}
	
	public static <T extends Text> Element citer(CitingPaper<T> citer){
		Element citerTag = new Element(Tag.valueOf(TAG_CITER), "");
		citerTag.attr(ATTR_TITLE, citer.title);
		Element sentences = citerTag.appendElement(TAG_SENTENCES);
		for(Sentence<T> sentence : citer.sentences){
			sentences.appendChild(sentence(sentence));
		}
		return citerTag;
	}
	
	public static <T extends Text> Element sentence(Sentence<T> sentence){
		Element sentenceTag = new Element(Tag.valueOf(TAG_SENTENCE), "");
		sentenceTag.attr(ATTR_SENTENCE_TYPE, sentence.type.toString());
		sentenceTag.appendChild(sentence.text.toXml());
		return sentenceTag;
	}
	
	public static <T extends Text> Dataset<T> parseFromXml(File xmlFile){
		try {
			printer.print("Parsing XML from " + xmlFile.getPath() + " ... ");
			Document doc = Jsoup.parse(xmlFile, null);
			printer.println("[x]");
			printer.print("Creating dataset from XML ... ");
			Dataset<T> dataset = parseXml(doc);
			printer.println("[x]");
			return dataset;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static <T extends Text> Dataset<T> parseXml(Document doc){
		
		Element datasets = doc.child(0);
		Element dataset = datasets.select(TAG_DATASET).first();
		
		String label = dataset.select(TAG_DATASET_LABEL).first().text();
		Element citedTag = dataset.select(TAG_CITED).first();
		Element citersTag = dataset.select(TAG_CITERS).first();
		
		Element citedContentTag = citedTag.select(TAG_TEXT).first(); //TODO
		
		String mainAuthor = citedTag.attr(ATTR_AUTHOR);
		String title = citedTag.attr(ATTR_TITLE);
		
		List<CitingPaper<T>> citers = new ArrayList<CitingPaper<T>>();
		int i = 0;
		for(Element citerTag : citersTag.children()){
			i++;
			printer.progress(i, 1);
			citers.add(citer(citerTag));
		}
		
		return Dataset.dataset(label, mainAuthor, title, citers, text(citedContentTag));
	}
	
	public static <T extends Text> CitingPaper<T> citer(Element citerTag){
		List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
		String title = citerTag.attr(ATTR_TITLE);
		for(Element sentenceTag : citerTag.select(TAG_SENTENCES).first().children()){
			Sentence<T> sentence = sentence(sentenceTag);
			sentences.add(sentence);
		}
		return new CitingPaper<T>(title, sentences);
	}
	
	public static <T extends Text> Sentence<T> sentence(Element sentenceTag){
		String type = sentenceTag.attr(ATTR_SENTENCE_TYPE);
		Element textTag = sentenceTag.select(TAG_TEXT).first();
		return new Sentence<T>(SentenceType.valueOf(type), text(textTag));
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T text(Element textTag){
		String textClass = textTag.attr("class");
		if(textClass.equals("text")){
			return (T) Text.fromXml(textTag);
		}else if(textClass.equals("text-with-ngrams")){
			return (T) TextWithNgrams.fromXml(textTag);
		}else if(textClass.equals("text-with-concepts")){
			throw new UnsupportedOperationException();
		}else{
			throw new IllegalArgumentException("Unknown text-class: " + textClass);
		}
	}
	
}
