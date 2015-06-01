package dataset;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
public class AcronymsHooksExtractor<T extends Text>{
	
	private Dataset<T> dataset;
	
	public AcronymsHooksExtractor(Dataset<T> dataset){
		this.dataset = dataset;
	}
	
	public List<String> findAcronyms(int boundary, int numAcronyms){
		Pattern regex = Pattern.compile("[^a-zA-Z\\d][A-Z]+[a-z]*[A-Z]+s?[ :;,\\.]");
		List<String> acronyms = findNotableInExplicit(boundary, numAcronyms, regex, true, true);
		return acronyms;
	}
	
	public List<LexicalHook> findLexicalHooks(int boundary, int numLexicalHooks){
		String before = "[^a-zA-Z\\d]";
		String after = "[ :;,\\.\\-\\)]";  
		String word = "[A-Z][a-z]+";
		String anotherWord = " " + word;
		String moreWords = "(" + anotherWord + "((" + anotherWord + after + ")|" + after + "))";
		String regexString = before + word + "(" + moreWords + "|" + after + ")";
		Pattern regex = Pattern.compile(regexString);
		List<String> hooks = findNotableInExplicit(boundary, numLexicalHooks, regex, false, false);
		return hooks.stream().map(hook -> new LexicalHook(hook)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	private List<String> findNotableInExplicit(int boundary, int limitNumber, Pattern regex, boolean trailingS, boolean makeUppercase){
		HashMap<String,Integer> counts = findMatchesInExplicitReferencesAroundAuthor(boundary, regex, trailingS, makeUppercase);
		String notExplicitRaw = dataset.notExplicitReferencesRaw();
		return counts.entrySet().stream()
				.filter(e -> e.getValue() > 2) //TODO to strict?
				.sorted((e1, e2) -> {
					Pattern re1 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e1.getKey()) + "s?[ :;,\\.]");
					Pattern re2 = Pattern.compile("[^a-zA-Z\\d]" + Pattern.quote(e2.getKey()) + "s?[ :;,\\.]");
					int e1OutsideExplSmooth = countMatches(notExplicitRaw, re1) + 1;
					int e2OutsideExplSmooth = countMatches(notExplicitRaw, re2) + 1;
					int e1InCitedSmooth = countMatches(dataset.citedContent.raw, re1) + 1;
					int e2InCitedSmooth = countMatches(dataset.citedContent.raw, re2) + 1;
					double score1 = (double)e1.getValue() * (double)e1InCitedSmooth / (double)e1OutsideExplSmooth;
					double score2 = (double)e2.getValue() * (double)e2InCitedSmooth / (double)e2OutsideExplSmooth;
					System.out.println(e1.getKey() + ": " + e1OutsideExplSmooth + "; " + e1InCitedSmooth + "; " + e1.getValue()); //TODO
					System.out.println(e2.getKey() + ": " + e2OutsideExplSmooth + "; " + e2InCitedSmooth + "; " + e2.getValue()); //TODO
					return (int)Math.signum(score2 - score1);
				})
				.limit(limitNumber)
				.map(e -> e.getKey())
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private HashMap<String, Integer> findMatchesInExplicitReferencesAroundAuthor(int boundary, Pattern regex, boolean trailingS, boolean makeUppercase){
		HashMap<String, Integer> matches = new HashMap<String, Integer>();
		dataset.explicitReferences().forEach(sentence -> { 
			int index = sentence.text.raw.indexOf(dataset.citedMainAuthor);
			if(index < 0){
				index = sentence.text.raw.indexOf(dataset.cleanCitedMainAuthor);
			}
			if(index >= 0){
				int left = Math.max(0, index-boundary);
				int right = Math.min(sentence.text.raw.length(), index+boundary);
				String vicinityOfAuthor = sentence.text.raw.substring(left, right);
				Matcher m = regex.matcher(vicinityOfAuthor);
				while(m.find()){
					String match = m.group();
					String cleanMatch = clean(match, trailingS, makeUppercase);
					if(dataset.cleanCitedMainAuthor.contains(cleanMatch) || dataset.citedMainAuthor.contains(cleanMatch)
							|| TextUtil.instance().isStopword(cleanMatch.toLowerCase())){
						continue;
					}
					if(!matchIsProbablyOtherAuthor(match, sentence.text.raw)){
						if(matches.containsKey(cleanMatch)){
							matches.put(cleanMatch, matches.get(cleanMatch) + 1);
						}else{
							matches.put(cleanMatch, 1);
						}
					}
				}
			}else{
				System.err.println("AUTHOR NOT FOUND (" + dataset.citedMainAuthor + ") in " + sentence.text.raw);//TODO
			}
		});
		
		//Try to determine which of the found matches were in fact author names (false matches)
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
		Iterator<Entry<String,Integer>> it = matches.entrySet().iterator();
		while(it.hasNext()){
			Entry<String,Integer> e = it.next();
//			System.out.println(e.getKey() + ": " + e.getValue() + " vs " + falseMatches.get(e.getKey())); //TODO
			boolean probablyFalse = falseMatches.get(e.getKey()) > e.getValue();
			if(probablyFalse){
				it.remove();
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
		
		ArrayList<String> restWords = TextUtil.split(rest).collect(Collectors.toCollection(ArrayList::new));
		if(!restWords.isEmpty()){
			if(restWords.get(0).equals("et") || restWords.get(0).equals("and") || restWords.get(0).matches("\\A(19|20)?\\d\\d\\z")){
				return true;
			}
		}
		return false;
	}
	
}
