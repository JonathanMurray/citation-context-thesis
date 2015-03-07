package citationContextData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.Texts;
import markovRandomField.MRF_citerNgrams;

public class Citer {
	public String title;
	public List<Sentence> sentences;
	public Citer(String title, List<Sentence> sentences){
		this.title = title.replaceAll("[ \t\r\n\f]+", " ");
		this.sentences = sentences;
	}
	
	public MRF_citerNgrams getMRF_citerNgrams(boolean skipStopwords, boolean stem){
		List<HashMap<String,Double>> sentencesUnigrams = new ArrayList<HashMap<String,Double>>();
		List<HashMap<String,Double>> sentencesBigrams = new ArrayList<HashMap<String,Double>>();
		for(Sentence s : sentences){
			sentencesUnigrams.add(Texts.instance().getNgrams(1, s.text, skipStopwords, stem));
			sentencesBigrams.add(Texts.instance().getNgrams(2, s.text, skipStopwords, stem));
		}
		return new MRF_citerNgrams(sentencesUnigrams, sentencesBigrams);
	}
}
