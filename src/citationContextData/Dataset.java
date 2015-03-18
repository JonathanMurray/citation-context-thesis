package citationContextData;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import util.Texts;


//TODO hard coded regexps, maybe gather them up in separate file
public class Dataset<T extends Text> {
	public final String datasetLabel;
	public final String citedMainAuthor;
	public final String cleanCitedMainAuthor;
	public T citedTitle;
	public T citedContent;
	public T mergedExplicitCitations;
	public final List<CitingPaper<T>> citers;
	
	public boolean hasExtra;
	private Set<String> acronyms;
	private Set<String> lexicalHooks;
	
	private Dataset(String datasetLabel, String citedMainAuthor, T citedTitle, List<CitingPaper<T>> citers, T citedContent, T mergedExplicitCitations){
		this.datasetLabel = datasetLabel;
		this.citedMainAuthor = citedMainAuthor;
		cleanCitedMainAuthor = Normalizer.normalize(citedMainAuthor, Normalizer.Form.NFD).replaceAll("[^\\x00-\\x7F]", "");
		this.citedTitle = citedTitle;
		this.citedContent = citedContent;
		this.citers = citers;
		this.mergedExplicitCitations = mergedExplicitCitations;
	}
	
//	private void assertNotNull(Object... args){
//		for(int i = 0; i < args.length; i++){
//			if(args[i] == null){
//				throw new IllegalArgumentException("Arg" + (i+1) + " == null");
//			}
//		}
//	}
	
	public static <T extends Text> Dataset<T> full(String datasetLabel, String citedMainAuthor, T citedTitle, 
			List<CitingPaper<T>> citers, T citedContent, T mergedExplicitCitations){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, citedContent, mergedExplicitCitations);
	}
	
	public static <T extends Text> Dataset<T> withoutCitedData(String datasetLabel, String citedMainAuthor, 
			T citedTitle, List<CitingPaper<T>> citers, T mergedExplicitCitations){
		return new Dataset<T>(datasetLabel, citedMainAuthor, citedTitle, citers, null, mergedExplicitCitations);
	}
	
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
	public Dataset<T> findExtra(int boundary, int numLexicalHooks){
		acronyms = findAllAcronyms(boundary);
		lexicalHooks = findLexicalHooks(boundary, numLexicalHooks);
		hasExtra = true;
		return this;
	}
	
	public void addExtra(Set<String> acronyms, Set<String> lexicalHooks){
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
		hasExtra = true;
	}
	
	
	
	public Set<String> findAllAcronyms(int boundary){
		Pattern regex = Pattern.compile("[^a-zA-Z\\d][A-Z]+[a-z]*[A-Z]+s?[ :;,\\.]");
		Set<String> acronyms = findNotableInExplicit(boundary, 1, regex, true);
		return acronyms;
	}
	
	public Set<String> findLexicalHooks(int boundary, int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z(\\d][A-Z][a-z]+(( [A-Z][a-z]+(( [A-Z][a-z]+[ :;,\\.])|[ :;,\\.]))|[ :;,\\.])");
		return findNotableInExplicit(boundary, numLexicalHooks, regex, false);
	}
	
	private Set<String> findNotableInExplicit(int boundary, int limitNumber, Pattern regex, boolean trailingS){
		TObjectIntHashMap<String> counts = findMatchesInExplicitReferencesAroundAuthor(boundary, regex, trailingS);
		HashMap<Integer, String> sortedEntries = new HashMap<Integer, String>();
		TObjectIntIterator<String> it = counts.iterator();
		while(it.hasNext()){
			it.advance();
			String found = it.key();// clean(it.key(), trailingS);
			sortedEntries.put(it.value(), found);
		}
		
		String explicitReferencesRaw = explicitReferencesRaw();
	
		return sortedEntries.entrySet().stream()
				.filter(e -> e.getKey() > 1)
				.sorted((e1, e2) -> {
					int e2Common = (int)Math.ceil(Math.sqrt(StringUtils.countMatches(explicitReferencesRaw, e2.getValue())));
					int e1Common = (int)Math.ceil(Math.sqrt(StringUtils.countMatches(explicitReferencesRaw, e1.getValue())));
					int e1InCited = StringUtils.countMatches(citedContent.raw, e1.getValue());
					int e2InCited = StringUtils.countMatches(citedContent.raw, e2.getValue());
					return e2.getKey()*e2InCited/e2Common - e1.getKey()*e1InCited/e1Common;
				})
				.limit(limitNumber)
				.map(e -> e.getValue())
				.collect(Collectors.toSet());
	}
	
	private TObjectIntHashMap<String> findMatchesInExplicitReferencesAroundAuthor(int boundary, Pattern regex, boolean trailingS){
		TObjectIntHashMap<String> matches = new TObjectIntHashMap<String>();
		explicitReferences().forEach(sentence -> {
				int index = sentence.text.raw.indexOf(citedMainAuthor);
				if(index < 0){
					index = sentence.text.raw.indexOf(cleanCitedMainAuthor);
				}
				if(index >= 0){
					int left = Math.max(0, index-boundary);
					int right = Math.min(sentence.text.raw.length() - 1, index+boundary);
					String vicinityOfAuthor = sentence.text.raw.substring(left, right);
					Matcher m = regex.matcher(vicinityOfAuthor);
					while(m.find()){
						String match = m.group();
						String cleanMatch = clean(match, false);
						if(cleanCitedMainAuthor.contains(cleanMatch) || citedMainAuthor.contains(cleanMatch)
								|| Texts.instance().isStopword(cleanMatch.toLowerCase())){
							continue;
						}
						if(!matchIsProbablyOtherAuthor(match, sentence.text.raw)){
							System.out.println(match); //TODO
							System.out.println(vicinityOfAuthor + "\n"); //TODO
							matches.adjustOrPutValue(match,1,1);
						}
					}
				}else{
					System.err.println("AUTHOR NOT FOUND (" + citedMainAuthor + ") in " + sentence.text.raw);//TODO
				}
		});
		
		TObjectIntHashMap<String> cleanMatches = new TObjectIntHashMap<String>();
		TObjectIntIterator<String> matchesIt = matches.iterator();
		while(matchesIt.hasNext()){
			matchesIt.advance();
			cleanMatches.adjustOrPutValue(clean(matchesIt.key(), trailingS), matchesIt.value(), matchesIt.value());
		}
		
		//Try to determine which of the found matches were actually author names (false matches)
		TObjectIntHashMap<String> falseMatches = new TObjectIntHashMap<String>();
		allReferences().forEach(sentence -> {
			for(String match : cleanMatches.keySet()){
				if(sentence.text.raw.contains(match)){
					if(matchIsProbablyOtherAuthor(match, sentence.text.raw)){
						falseMatches.adjustOrPutValue(match, 1, 1);
					}
				}
			}
		});
		
		
		
		TObjectIntIterator<String> cleanMatchesIt = cleanMatches.iterator();
		while(cleanMatchesIt.hasNext()){
			cleanMatchesIt.advance();
//			System.out.println(cleanMatchesIt.key() + ": " + cleanMatchesIt.value() + " vs " + falseMatches.get(cleanMatchesIt.key())); //TODO
			boolean probablyFalse = falseMatches.get(cleanMatchesIt.key()) > cleanMatchesIt.value();
			if(probablyFalse){
				cleanMatchesIt.remove();
			}
		}

		return cleanMatches;
	}
	
	private String clean(String dirty, boolean trailingS){
		String s = dirty.replaceAll("[,\\[\\]\\(\\)\\.\\{\\}\\?\\!\\:\\;]", "").trim();
		if(trailingS && s.endsWith("s")){
			s = s.substring(0, s.length()-1);
		}
		return s;
	}
	
	private boolean matchIsProbablyOtherAuthor(String match, String text){
		match = match.trim(); //spaces might have been included in match
		int index = text.indexOf(match);
		if(index == -1){
			throw new IllegalArgumentException();
		}
		int after = index + match.length() + 1;
		if(after >= text.length()){
			return false;
		}
		String rest = text.substring(after, text.length());
		
		ArrayList<String> restWords = Texts.split(rest).collect(Collectors.toCollection(ArrayList::new));
		if(!restWords.isEmpty()){
			if(restWords.get(0).equals("et") || restWords.get(0).equals("and") || restWords.get(0).matches("\\A(19|20)?\\d\\d\\z")){
				return true;
			}
		}
		return false;
	}
	
	private Stream<Sentence<T>> explicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceType.EXPLICIT_REFERENCE);
	}
	
	private Stream<Sentence<T>> allReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream());
	}
	
	private String explicitReferencesRaw(){
		String s = explicitReferences().map(sentence -> sentence.text.raw).reduce("", (s1,s2) -> s1 + " " + s2);
		return s;
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
