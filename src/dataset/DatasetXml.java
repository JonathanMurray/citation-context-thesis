package dataset;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import util.Environment;
import util.Printer;
import concepts.WikiGraph;
import concepts.WikiGraphFactory;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;

public class DatasetXml {
	
	private static final Printer printer = new Printer(true);
	
	private static final String TAG_DATASET = "dataset";
	private static final String TAG_ACRONYMS = "acronyms";
	private static final String TAG_LEXICAL_HOOKS = "lexicalHooks";
	private static final String TAG_LEXICAL_HOOK = "hook";
	private static final String TAG_DATASET_LABEL = "label";
	private static final String TAG_MERGED_EXPLICIT = "merged-explicit-citatations";
	private static final String TAG_CITED = "cited";
	private static final String TAG_CITERS = "citers";
	private static final String TAG_CITER = "citer";
	private static final String TAG_SENTENCES = "sentences";
	private static final String TAG_SENTENCE = "sentence";
	private static final String TAG_CONTENT = "content";
	private static final String TAG_TITLE = "title";
	private static final String TAG_TEXT = "text";
	private static final String ATTR_AUTHOR = "main-author";
	private static final String ATTR_TITLE = "title";
	private static final String ATTR_SENTENCE_TYPE = "type";
	
	public static void main(String[] args) {
		File resources = new File("/home/jonathan/Documents/eclipse-workspace/exjobb/resources");
		File dir = new File(resources, "teufel-citation-context-corpus");
	
		final boolean ALLOW_STOPWORDS_CONCEPTS = false;
		WikiGraph wiki = WikiGraphFactory.loadWikiGraph(
				new File(resources, "ser/linksSingleWords.ser"), 
				new File(resources, "ser/toIndexSingleWords.ser"), 
				ALLOW_STOPWORDS_CONCEPTS);
		
		final String datasetLabel = "A92-1018";
		
//		NgramIdf wordIdf = NgramIdf.fromXmlFile(new File(resources, "xml-datasets/ngram-frequencies.xml"), 5);
		
//		Dataset<TextWithConcepts> dataset = DatasetFactory.fromFiles(
//				DatasetParams.basic(TextParams.withWikiConcepts(wordIdf, wiki)), 
//				new File(dir, datasetLabel + ".html"), 
//				new File(dir, datasetLabel + ".txt"));
//		dataset.findExtra(80, 2, 2);
//		System.out.println("DATASET: " + dataset);
//		writeToXml(dataset, new File(resources, "xml-datasets/" + datasetLabel + "-with-concepts.xml"));
		
//		Dataset<TextWithConcepts> dataset = parseFromXml(new File(dir, "TEST-ngrams.xml"));
//		System.out.println("dataset: " + dataset);
	}
	
	public static <T extends Text> void writeToXml(Dataset<T> dataset, File file){
		try(FileWriter writer = new FileWriter(file)){
			printer.print("Constructing XML for " + dataset.datasetLabel + " ... ");
			Document doc = toXml(dataset);
			printer.println("[x]");
			printer.print("Writing XML to " + file.getPath() + " ... ");
			writer.write(doc.toString());
			printer.println("[x]");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static <T extends Text> Document toXml(Dataset<T> dataset){
		Document doc = new Document("");
		Element datasetTag = doc.appendElement(TAG_DATASET);
		datasetTag.appendElement(TAG_DATASET_LABEL).text(dataset.datasetLabel);
		if(dataset.hasExtra){
			datasetTag.appendElement(TAG_ACRONYMS).text(Texts.merge(dataset.getAcronyms()));
			List<String> hooks = dataset.getLexicalHooks().stream()
					.map(h -> h.hook).collect(Collectors.toCollection(ArrayList::new));
			Element hooksTag = datasetTag.appendElement(TAG_LEXICAL_HOOKS);
			for(String hook : hooks){
				hooksTag.appendElement(TAG_LEXICAL_HOOK).text(hook);
			}
		}
		datasetTag.appendElement(TAG_MERGED_EXPLICIT).appendChild(dataset.mergedExplicitCitations.toXml());
		Element cited = datasetTag.appendElement(TAG_CITED);
		cited.attr(ATTR_AUTHOR, dataset.citedMainAuthor);
		cited.appendElement(TAG_TITLE).appendChild(dataset.citedTitle.toXml());
		cited.appendElement(TAG_CONTENT).appendChild(dataset.citedContent.toXml());
		
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
	
	
	
	
	
	
	
	
	
	
	


    public static void parseQuick(File file)  {
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	        InputStream in = new BufferedInputStream(new FileInputStream(file));
	        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
	        streamReader.nextTag(); // Advance to "book" element
	        streamReader.nextTag(); // Advance to "person" element

	        int persons = 0;
	        while (streamReader.hasNext()) {
	            if (streamReader.isStartElement()) {
	                switch (streamReader.getLocalName()) {
	                case "first": {
	                    System.out.print("First Name : ");
	                    System.out.println(streamReader.getElementText());
	                    break;
	                }
	                case "last": {
	                    System.out.print("Last Name : ");
	                    System.out.println(streamReader.getElementText());
	                    break;
	                }
	                case "age": {
	                    System.out.print("Age : ");
	                    System.out.println(streamReader.getElementText());
	                    break;
	                }
	                case "person" : {
	                    persons ++;
	                }
	                }
	            }
	            streamReader.next();
	        }
	        System.out.print(persons);
	        System.out.println(" persons");
		} catch (FileNotFoundException | XMLStreamException e) {
			e.printStackTrace();
		}
    }

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static <T extends Text> Dataset<T> parseXmlFile(Class<T> textClass, File xmlFile, int maxNumCiters){
		try {
			printer.print("Parsing XML (" + textClass + ") from " + xmlFile.getPath() + " ... ");
			Document doc = Jsoup.parse(new BufferedInputStream(new FileInputStream(xmlFile)), null, "", Parser.xmlParser());
			printer.println("[x]");
			printer.print("Creating dataset from XML ... ");
			Dataset<T> dataset = datasetFromXml(textClass, doc, maxNumCiters);
			printer.println(" [x]");
			return dataset;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static <T extends Text> Dataset<T> datasetFromXml(Class<T> textClass, Document doc, int maxNumCiters){
		Element datasetTag = doc.child(0);
		
		String label = datasetTag.select(TAG_DATASET_LABEL).first().text();
		Element mergedExplicitTag = datasetTag.select(TAG_MERGED_EXPLICIT).first().select(TAG_TEXT).first();
		Element citedTag = datasetTag.select(TAG_CITED).first();
		Element citersTag = datasetTag.select(TAG_CITERS).first();
		
		Element citedContentTag = citedTag.select(TAG_CONTENT).first().select(TAG_TEXT).first(); 
		Element citedTitleTag = citedTag.select(TAG_TITLE).first().select(TAG_TEXT).first(); 
		String mainAuthor = citedTag.attr(ATTR_AUTHOR);
		
		List<CitingPaper<T>> citers = new ArrayList<CitingPaper<T>>();
		int i = 0;
		printer.resetProgress();
		for(Element citerTag : citersTag.children()){
			i++;
			if(maxNumCiters > 0 && i > maxNumCiters){
				break;
			}
			printer.progress();
			citers.add(citer(textClass, citerTag));
			
		}
		Dataset<T> dataset = Dataset.full(label, mainAuthor, text(textClass, citedTitleTag), citers, 
				text(textClass, citedContentTag), text(textClass, mergedExplicitTag));
		if(datasetTag.select(TAG_ACRONYMS).size() == 1 && datasetTag.select(TAG_LEXICAL_HOOKS).size() == 1){
			List<String> acronyms = Texts.split(datasetTag.select(TAG_ACRONYMS).text())
					.collect(Collectors.toCollection(ArrayList::new));
			Element hooksTag = datasetTag.select(TAG_LEXICAL_HOOKS).first();
			List<LexicalHook> lexicalHooks = new ArrayList<LexicalHook>();
			for(Element hookTag : hooksTag.select(TAG_LEXICAL_HOOK)){
				lexicalHooks.add(new LexicalHook(hookTag.text()));
			}
			dataset.addExtra(acronyms, lexicalHooks);
		}
		
		return dataset;
	}
	
	public static <T extends Text> CitingPaper<T> citer(Class<T> textClass, Element citerTag){
		List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
		String title = citerTag.attr(ATTR_TITLE);
		int sentenceIndex = 0;
		for(Element sentenceTag : citerTag.select(TAG_SENTENCES).first().children()){
			Sentence<T> sentence = sentence(textClass, sentenceTag, sentenceIndex);
			sentences.add(sentence);
			sentenceIndex ++;
		}
		return new CitingPaper<T>(title, sentences);
	}
	
	public static <T extends Text> Sentence<T> sentence(Class<T> textClass, Element sentenceTag, int sentenceIndex){
		String type = sentenceTag.attr(ATTR_SENTENCE_TYPE);
		Element textTag = sentenceTag.select(TAG_TEXT).first();
		return new Sentence<T>(SentenceType.valueOf(type), text(textClass, textTag), sentenceIndex);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Text> T text(Class<T> textClass, Element textTag){
		if(textClass.equals(Text.class)){
			return (T) Text.fromXml(textTag);
		}else if(textClass.equals(TextWithNgrams.class)){
			return (T) TextWithNgrams.fromXml(textTag);
		}else if(textClass.equals(TextWithSkipgrams.class)){
			return (T) TextWithSkipgrams.fromXml(textTag);
		}else if(textClass.equals(TextWithWiki.class)){
			return (T) TextWithWiki.fromXml(textTag);
		}else if(textClass.equals(TextWithSynsets.class)){
			File wordnetDir = new File(Environment.resources(), "wordnet-dict");
			IDictionary dict = new Dictionary(wordnetDir);
			return (T) TextWithSynsets.fromXml(textTag, dict);
		}else if(textClass.equals(TextWithSspace.class)){
			return (T) TextWithSspace.fromXml(textTag, SSpaceWrapper.instance());
		}else{
			throw new IllegalArgumentException("Unknown text-class: " + textClass);
		}
	}
	
}
