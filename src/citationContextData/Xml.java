package citationContextData;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import util.NgramIdf;
import util.Printer;
import util.Texts;
import conceptGraph.WikiGraph;
import conceptGraph.WikiGraphFactory;

public class Xml {
	
	private static final Printer printer = new Printer(true);
	
	private static final String TAG_DATASET = "dataset";
	private static final String TAG_ACRONYMS = "acronyms";
	private static final String TAG_LEXICAL_HOOKS = "lexicalHooks";
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
		
		NgramIdf wordIdf = NgramIdf.fromXmlFile(new File(resources, "xml-datasets/ngram-frequencies.xml"));
		
		Dataset<TextWithConcepts> dataset = DatasetFactory.fromFiles(
				DatasetParams.basic(TextParams.withWikiConcepts(wordIdf, wiki)), 
				new File(dir, datasetLabel + ".html"), 
				new File(dir, datasetLabel + ".txt"));
		dataset.findExtra(80, 2, 2);
		System.out.println("DATASET: " + dataset);
		writeToXml(dataset, new File(resources, "xml-datasets/" + datasetLabel + "-with-concepts.xml"));
		
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
			datasetTag.appendElement(TAG_LEXICAL_HOOKS).text(Texts.merge(hooks));
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
	
	public static <T extends Text> Dataset<T> parseXmlFile(File xmlFile, int maxNumCiters){
		try {
			printer.print("Parsing XML from " + xmlFile.getPath() + " ... ");
			
			Document doc = Jsoup.parse(new BufferedInputStream(new FileInputStream(xmlFile)), null, "", Parser.xmlParser());
			printer.println("[x]");
			printer.print("Creating dataset from XML ... ");
			Dataset<T> dataset = parseXml(doc, maxNumCiters);
			printer.println("[x]");
			return dataset;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static <T extends Text> Dataset<T> parseXml(Document doc, int maxNumCiters){
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
		for(Element citerTag : citersTag.children()){
			i++;
			if(maxNumCiters > 0 && i > maxNumCiters){
				break;
			}
			printer.progress(i, 1);
			citers.add(citer(citerTag));
			
		}
		Dataset<T> dataset = Dataset.full(label, mainAuthor, text(citedTitleTag), citers, text(citedContentTag), text(mergedExplicitTag));
		if(datasetTag.select(TAG_ACRONYMS).size() == 1 && datasetTag.select(TAG_LEXICAL_HOOKS).size() == 1){
			List<String> acronyms = Texts.split(datasetTag.select(TAG_ACRONYMS).text())
					.collect(Collectors.toCollection(ArrayList::new));
			List<String> hooks = Texts.split(datasetTag.select(TAG_LEXICAL_HOOKS).text())
					.collect(Collectors.toCollection(ArrayList::new));
			List<LexicalHook> lexicalHooks = hooks.stream().map(hook -> new LexicalHook(hook))
					.collect(Collectors.toCollection(ArrayList::new));
			dataset.addExtra(acronyms, lexicalHooks);
		}
		
		return dataset;
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
		if(textClass.equals(Text.XML_TEXT_CLASS)){
			return (T) Text.fromXml(textTag);
		}else if(textClass.equals(TextWithNgrams.XML_TEXT_CLASS)){
			return (T) TextWithNgrams.fromXml(textTag);
		}else if(textClass.equals(TextWithConcepts.XML_TEXT_CLASS)){
			return (T) TextWithConcepts.fromXml(textTag);
		}else{
			throw new IllegalArgumentException("Unknown text-class: " + textClass);
		}
	}
	
}
