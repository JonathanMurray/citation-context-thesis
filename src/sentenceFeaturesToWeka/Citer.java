package sentenceFeaturesToWeka;

import java.util.List;

public class Citer {
	public String title;
	public List<Sentence> sentences;
	public Citer(String title, List<Sentence> sentences){
		this.title = title;
		this.sentences = sentences;
	}
}
