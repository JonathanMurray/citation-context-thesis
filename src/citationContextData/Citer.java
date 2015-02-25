package citationContextData;

import java.util.List;

public class Citer {
	public String title;
	public List<Sentence> sentences;
	public Citer(String title, List<Sentence> sentences){
		this.title = title.replaceAll("[ \t\r\n\f]+", " ");
		this.sentences = sentences;
	}
}
