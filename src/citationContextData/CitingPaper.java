package citationContextData;

import java.util.List;

public class CitingPaper<T extends Text> {
	public String title;
	public List<Sentence<T>> sentences;
	public CitingPaper(String title, List<Sentence<T>> sentences){
		this.title = title.replaceAll("[ \t\r\n\f]+", " ");
		this.sentences = sentences;
	}
}
