package dataset;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dataset<T extends Text> {
	public final String datasetLabel;
	public final String citedMainAuthor;
	public final String cleanCitedMainAuthor;
	public T citedTitle;
	public T citedContent;
	public T mergedExplicitCitations;
	public final List<CitingPaper<T>> citers;
	
	public boolean hasExtra;
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
		if(!hasExtra){
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
	public Dataset<T> findExtra(int boundary, int numLexicalHooks, int numAcronyms){
		DatasetExtrasExtractor<T> extractor = new DatasetExtrasExtractor<T>(this);
		acronyms = extractor.findAcronyms(boundary, numAcronyms);
		lexicalHooks = extractor.findLexicalHooks(boundary, numLexicalHooks);
		hasExtra = true;
		return this;
	}
	
	public void addExtra(List<String> acronyms, List<LexicalHook> lexicalHooks){
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
		hasExtra = true;
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
		if(hasExtra){
			s.append("acronyms: " + acronyms + "\n");
			s.append("lexical hooks: " + lexicalHooks + "\n");
		}
		s.append("cited: " + citedMainAuthor + ", " + citedTitle + "\n");
		s.append("# citers: " + citers.size() + "\n");
		s.append("cited content length: " + citedContent.raw.length());
		return s.toString();
	}
}
