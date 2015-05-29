package dataset;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.Printer;
import util.Timer;

/**
 * Represents one instance of the dataset, which means there is one reference that is studied
 * (out of the total 20) and several citers whose sentences are classified. The acronyms and 
 * lexical hooks correspond the reference.
 * @author jonathan
 *
 * @param <T>
 */
public class Dataset<T extends Text> {
	public final String datasetLabel;
	public final String citedMainAuthor;
	public final String cleanCitedMainAuthor;
	public T citedTitle;
	public T citedContent;
	public T mergedExplicitCitations;
	public final List<CitingPaper<T>> citers;
	
	public boolean hasAcronymsHooks;
	private List<String> acronyms;
	private List<LexicalHook> lexicalHooks;
	
	private Dataset(String datasetLabel, String citedMainAuthor, T citedTitle, List<CitingPaper<T>> citers, T citedContent, T mergedExplicitCitations){
		this.datasetLabel = datasetLabel;
		this.citedMainAuthor = citedMainAuthor;
		cleanCitedMainAuthor = Normalizer.normalize(citedMainAuthor, Normalizer.Form.NFD).replaceAll("[^\\x00-\\x7F]", "");
		this.citedTitle = citedTitle;
		this.citedContent = citedContent;
		this.citers = citers;
		this.mergedExplicitCitations = mergedExplicitCitations;
	}
	
	public static <T extends Text> Dataset<T> full(String datasetLabel, String citedMainAuthor, T citedTitle, 
			List<CitingPaper<T>> citers, T citedContent, T mergedExplicitCitations){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, citedContent, mergedExplicitCitations);
	}
	
	public static <T extends Text> Dataset<T> withoutCitedData(String datasetLabel, String citedMainAuthor, 
			T citedTitle, List<CitingPaper<T>> citers, T mergedExplicitCitations){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, null, mergedExplicitCitations);
	}
	
	public List<String> getAcronyms(){
		assertHasExtra();
		return acronyms;
	}
	
	public List<LexicalHook> getLexicalHooks(){
		assertHasExtra();
		return lexicalHooks;
	}
	
	private void assertHasExtra(){
		if(!hasAcronymsHooks){
			throw new UnsupportedOperationException("This dataset is constructed with no acronyms and lexical hooks.");
		}
	}
	
	public List<Sentence<T>> getSentences(){
		return citers.stream()
				.flatMap(citer -> citer.sentences.stream())
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	/**
	 * Enhances this dataset with acronyms and lexical hooks, 
	 * found in proximity of explicit citations. Same dataset is returned.
	 * @param boundary
	 * @param numLexicalHooks
	 * @return
	 */
	public Dataset<T> findAcronymsHooks(int boundary, int numLexicalHooks, int numAcronyms){
		Printer printer = new Printer(true);
		Timer t = new Timer();
		printer.print("Finding extras for " + datasetLabel + " ... ");
		AcronymsHooksExtractor<T> extractor = new AcronymsHooksExtractor<T>(this);
		printer.print("acronyms .. ");
		acronyms = extractor.findAcronyms(boundary, numAcronyms);
		printer.print("~ hooks .. ");
		lexicalHooks = extractor.findLexicalHooks(boundary, numLexicalHooks);
		hasAcronymsHooks = true;
		printer.println("[x]  (" + t.getSecString() + ")");
		return this;
	}
	
	public void addAcronymsHooks(List<String> acronyms, List<LexicalHook> lexicalHooks){
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
		hasAcronymsHooks = true;
	}
	
	Stream<Sentence<T>> explicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.EXPLICIT_REFERENCE);
	}
	
	Stream<Sentence<T>> allSentences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream());
	}
	
	String explicitReferencesRaw(){
		String s = explicitReferences().map(sentence -> sentence.text.raw).reduce("", (s1,s2) -> s1 + " " + s2);
		return s;
	}
	
	String allReferencesRaw(){
		return allSentences().map(sentence -> sentence.text.raw).reduce("", (s1,s2) -> s1 + " " + s2);
	}
	
	String notExplicitReferencesRaw(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type != SentenceType.EXPLICIT_REFERENCE)
				.map(s -> s.text.raw)
				.reduce("", (s1,s2) -> s1 + " " + s2);
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append(datasetLabel + "\n");
		if(hasAcronymsHooks){
			s.append("acronyms: " + acronyms + "\n");
			s.append("lexical hooks: " + lexicalHooks + "\n");
		}
		s.append("cited: " + citedMainAuthor + ", " + citedTitle + "\n");
		s.append("# citers: " + citers.size() + "\n");
		s.append("cited content length: " + citedContent.raw.length());
		return s.toString();
	}
}
