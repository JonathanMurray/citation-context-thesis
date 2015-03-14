package citationContextData;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO hard coded regexps, maybe gather them up in separate file
public class Dataset<T extends Text> {
	public final String datasetLabel;
	public final String citedMainAuthor;
	public final String citedTitle;
	public T citedContent;
	public final List<CitingPaper<T>> citers;
	
	private boolean hasExtra;
	private Set<String> acronyms;
	private Set<String> lexicalHooks;
	
	public Set<String> getAcronyms(){
		assertHasExtra();
		return acronyms;
	}
	
	public Set<String> getLexicalHooks(){
		assertHasExtra();
		return lexicalHooks;
	}
	
	private void assertHasExtra(){
		if(!hasExtra){
			throw new UnsupportedOperationException("This dataset is constructed with no acronyms and lexical hooks.");
		}
	}
	
	public static <T extends Text> Dataset<T> dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<CitingPaper<T>> citers, T citedContent){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, citedContent);
	}
	
	public static <T extends Text> Dataset<T> withoutCitedData(String datasetLabel, String citedMainAuthor, String citedTitle, List<CitingPaper<T>> citers){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, null);
	}
	
	/**
	 * Enhances this dataset with acronyms and lexical hooks, 
	 * found in proximity of explicit citations. Same dataset is returned.
	 * @param boundary
	 * @param numLexicalHooks
	 * @return
	 */
	public Dataset<T> withExtra(int boundary, int numLexicalHooks){
		acronyms = findAllAcronyms(boundary);
		lexicalHooks = findLexicalHooks(boundary, numLexicalHooks);
		hasExtra = true;
		return this;
	}
	
	private Dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<CitingPaper<T>> citers, T citedContent){
		this.datasetLabel = datasetLabel;
		this.citedMainAuthor = citedMainAuthor;
		this.citedTitle = citedTitle;
		this.citedContent = citedContent;
		this.citers = citers;
	}
	
	private Set<String> findAllAcronyms(int boundary){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z]+[ ,]");
		return new HashSet<String>(findMatchesInExplicitReferencesAroundAuthor(boundary, regex));
	}
	
	private Set<String> findLexicalHooks(int boundary, int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z][a-z]+[ ,:;]");
		List<String> nonDistinctHooks = findMatchesInExplicitReferencesAroundAuthor(boundary, regex);
		TObjectIntHashMap<String> counts = new TObjectIntHashMap<String>();
		nonDistinctHooks.stream()
			.filter(hook -> !hook.equals(citedMainAuthor))
			.forEach(hook -> {
				counts.adjustOrPutValue(hook, 1, 1);
			});
		
		HashMap<Integer, String> sortedEntries = new HashMap<Integer, String>();
		TObjectIntIterator<String> it = counts.iterator();
		while(it.hasNext()){
			it.advance();
			sortedEntries.put(it.value(), it.key());
		}
	
		return sortedEntries.entrySet().stream()
				.filter(e -> ! e.getValue().equals(citedMainAuthor))
				.sorted((e1, e2) -> e1.getKey() - e2.getKey())
				.limit(numLexicalHooks)
				.map(e -> e.getValue())
				.collect(Collectors.toSet());
	}
	
	private List<String> findMatchesInExplicitReferencesAroundAuthor(int boundary, Pattern regex){
		String author = citedMainAuthor;
		List<String> matches = new ArrayList<String>();
		
		explicitReferences().forEach(sentence -> {
				int index = sentence.text.lemmatized.indexOf(author);
				if(index >= 0){
					int left = Math.max(0, index-boundary);
					int right = Math.min(sentence.text.lemmatized.length()- 1, index+boundary);
					String vicinityOfAuthor = sentence.text.lemmatized.substring(left, right);
					Matcher m = regex.matcher(vicinityOfAuthor);
					while(m.find()){
						String match = m.group();
						match = match.replaceAll("[,\\[\\]\\(\\)]", "").trim();
						matches.add(match);
					}
				}
		});

		return matches;
	}
	
	private Stream<Sentence<T>> explicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.EXPLICIT_REFERENCE);
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append(datasetLabel + "\n");
		s.append("cited: " + citedMainAuthor + ", " + citedTitle + "\n");
		s.append("# citers: " + citers.size() + "\n");
		s.append("cited content length: " + citedContent.raw.length());
		return s.toString();
	}
}
