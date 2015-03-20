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
		acronyms = findAcronyms(boundary, numAcronyms);
		lexicalHooks = findLexicalHooks(boundary, numLexicalHooks);
		hasExtra = true;
		return this;
	}
	
	public void addExtra(List<String> acronyms, List<LexicalHook> lexicalHooks){
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
		hasExtra = true;
	}
	
	
	
	public List<String> findAcronyms(int boundary, int numAcronyms){
		Pattern regex = Pattern.compile("[^a-zA-Z\\d][A-Z]+[a-z]*[A-Z]+s?[ :;,\\.]");
		List<String> acronyms = findNotableInExplicit(boundary, numAcronyms, regex, true, true);
		return acronyms;
	}
	
	public List<LexicalHook> findLexicalHooks(int boundary, int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z(\\d][A-Z][a-z]+(( [A-Z][a-z]+(( [A-Z][a-z]+[ :;,\\.])|[ :;,\\.]))|[ :;,\\.])");
		List<String> hooks = findNotableInExplicit(boundary, numLexicalHooks, regex, false, false);
		return hooks.stream().map(hook -> new LexicalHook(hook)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	private List<String> findNotableInExplicit(int boundary, int limitNumber, Pattern regex, boolean trailingS, boolean makeUppercase){
		TObjectIntHashMap<String> counts = findMatchesInExplicitReferencesAroundAuthor(boundary, regex, trailingS, makeUppercase);
		HashMap<Integer, String> sortedEntries = new HashMap<Integer, String>();
		TObjectIntIterator<String> it = counts.iterator();
		while(it.hasNext()){
			it.advance();
			String found = it.key();
			sortedEntries.put(it.value(), found);
		}
		String notExplicitRaw = notExplicitReferencesRaw();
		return sortedEntries.entrySet().stream()
				.filter(e -> e.getKey() > 2) //TODO to strict?
				.sorted((e1, e2) -> {
					Pattern re1 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e1.getValue()) + "s?[ :;,\\.]");
					Pattern re2 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e2.getValue()) + "s?[ :;,\\.]");
					int e1OutsideExplSmooth = countMatches(notExplicitRaw, re1) + 1;
					int e2OutsideExplSmooth = countMatches(notExplicitRaw, re2) + 1;
					int e1InCitedSmooth = countMatches(citedContent.raw, re1) + 1;
					int e2InCitedSmooth = countMatches(citedContent.raw, re2) + 1;
					double score1 = (double)e1.getKey() * (double)e1InCitedSmooth / (double)e1OutsideExplSmooth;
					double score2 = (double)e2.getKey() * (double)e2InCitedSmooth / (double)e2OutsideExplSmooth;
					return (int)Math.signum(score2 - score1);
				})
				.limit(limitNumber)
				.map(e -> e.getValue())
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private TObjectIntHashMap<String> findMatchesInExplicitReferencesAroundAuthor(int boundary, Pattern regex, boolean trailingS, boolean makeUppercase){
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
					String tmpCleanMatch = clean(match, trailingS, makeUppercase);
					if(cleanCitedMainAuthor.contains(tmpCleanMatch) || citedMainAuthor.contains(tmpCleanMatch)
							|| Texts.instance().isStopword(tmpCleanMatch.toLowerCase())){
						continue;
					}
					if(!matchIsProbablyOtherAuthor(match, sentence.text.raw)){
//							System.out.println(match); //TODO
//							System.out.println(vicinityOfAuthor + "\n"); //TODO
						matches.adjustOrPutValue(clean(match, trailingS, makeUppercase),1,1);
					}
				}
			}else{
				System.err.println("AUTHOR NOT FOUND (" + citedMainAuthor + ") in " + sentence.text.raw);//TODO
			}
		});
		
		//Try to determine which of the found matches were actually author names (false matches)
		TObjectIntHashMap<String> falseMatches = new TObjectIntHashMap<String>();
		allSentences().forEach(sentence -> {
			for(String match : matches.keySet()){
				if(sentence.text.raw.contains(match)){
					if(matchIsProbablyOtherAuthor(match, sentence.text.raw)){
						falseMatches.adjustOrPutValue(match, 1, 1);
					}
				}
			}
		});
		
		TObjectIntIterator<String> matchesIt = matches.iterator();
		while(matchesIt.hasNext()){
			matchesIt.advance();
			System.out.println(matchesIt.key() + ": " + matchesIt.value() + " vs " + falseMatches.get(matchesIt.key())); //TODO
			boolean probablyFalse = falseMatches.get(matchesIt.key()) > matchesIt.value();
			if(probablyFalse){
				matchesIt.remove();
			}
		}

		return matches;
	}
	
	private int countMatches(String string, Pattern sub){
		Matcher m = sub.matcher(string);
		int count = 0;
		while(m.find()){
			count ++;
		}
		return count;
	}
	
	private String clean(String dirty, boolean trailingS, boolean makeUppercase){
		String s = dirty.replaceAll("[,\\[\\]\\(\\)\\.\\{\\}\\?\\!\\:\\;]", "").trim();
		if(trailingS && s.endsWith("s")){
			s = s.substring(0, s.length()-1);
		}
		if(makeUppercase){
			s = s.toUpperCase();
		}
		return s;
	}
	
	private boolean matchIsProbablyOtherAuthor(String match, String text){
		match = match.trim(); //spaces might have been included in match
		
		{
			//at least a few characters, no lowercase 
			// ==> likely NOT an author name
			String tmpClean = clean(match, true, false);
			if(tmpClean.length() > 1 && ! tmpClean.matches(".*[a-z].*")){
				return false;
			}
		}
		
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
	
	private Stream<Sentence<T>> allSentences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream());
	}
	
	private String explicitReferencesRaw(){
		String s = explicitReferences().map(sentence -> sentence.text.raw).reduce("", (s1,s2) -> s1 + " " + s2);
		return s;
	}
	
	private String allReferencesRaw(){
		return allSentences().map(sentence -> sentence.text.raw).reduce("", (s1,s2) -> s1 + " " + s2);
	}
	
	private String notExplicitReferencesRaw(){
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
