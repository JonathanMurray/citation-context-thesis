package dataset;

import java.util.stream.Stream;

/**
 * Represents a lexical hook in the dataset. A lexical hook is a capitalized
 * phrase such as "Hidden Markov Model" or "Machine Learning". 
 * @author jonathan
 *
 */
public class LexicalHook {
	public String hook;
	public boolean hasAcronym;
	public String acronym;
	
	private String info;
	
	public LexicalHook(String hook){
		this.hook = hook;
		
		if(hook.length() > 0){
			Stream<String> words = TextUtil.split(hook);
			StringBuilder s = new StringBuilder();
			words.forEach(word -> s.append(word.charAt(0)));
			if(s.length() > 1){
				hasAcronym = true;
				acronym = s.toString().toUpperCase();
			}
		}
		info = hook;
		if(hasAcronym){
			info += " <" + acronym + ">";
		}
	}
	
	public String toString(){
		return info;
	}
}
