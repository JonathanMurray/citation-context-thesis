package citationContextData;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The task of this class is to extract NOTABLE 'lexical hooks' and 'acronyms' 
 * from a dataset, that can be used as features for finding implicit citations.
 * The hooks and acronyms should preferably occur often in explicit citations
 * as well as in the cited paper. 
 * 
 * Special care is taken to avoid extracting author names and such as lexical hooks.
 */
public class DatasetExtrasExtractor<T extends Text>{
	
	private Dataset<T> dataset;
	
	public DatasetExtrasExtractor(Dataset<T> dataset){
		this.dataset = dataset;
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
		String notExplicitRaw = dataset.notExplicitReferencesRaw();
		return sortedEntries.entrySet().stream()
				.filter(e -> e.getKey() > 2) //TODO to strict?
				.sorted((e1, e2) -> {
					Pattern re1 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e1.getValue()) + "s?[ :;,\\.]");
					Pattern re2 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e2.getValue()) + "s?[ :;,\\.]");
					int e1OutsideExplSmooth = countMatches(notExplicitRaw, re1) + 1;
					int e2OutsideExplSmooth = countMatches(notExplicitRaw, re2) + 1;
					int e1InCitedSmooth = countMatches(dataset.citedContent.raw, re1) + 1;
					int e2InCitedSmooth = countMatches(dataset.citedContent.raw, re2) + 1;
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
		dataset.explicitReferences().forEach(sentence -> {
			int index = sentence.text.raw.indexOf(dataset.citedMainAuthor);
			if(index < 0){
				index = sentence.text.raw.indexOf(dataset.cleanCitedMainAuthor);
			}
			if(index >= 0){
				int left = Math.max(0, index-boundary);
				int right = Math.min(sentence.text.raw.length() - 1, index+boundary);
				String vicinityOfAuthor = sentence.text.raw.substring(left, right);
				Matcher m = regex.matcher(vicinityOfAuthor);
				while(m.find()){
					String match = m.group();
					String tmpCleanMatch = clean(match, trailingS, makeUppercase);
					if(dataset.cleanCitedMainAuthor.contains(tmpCleanMatch) || dataset.citedMainAuthor.contains(tmpCleanMatch)
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
				System.err.println("AUTHOR NOT FOUND (" + dataset.citedMainAuthor + ") in " + sentence.text.raw);//TODO
			}
		});
		
		//Try to determine which of the found matches were actually author names (false matches)
		TObjectIntHashMap<String> falseMatches = new TObjectIntHashMap<String>();
		dataset.allSentences().forEach(sentence -> {
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
	
}
