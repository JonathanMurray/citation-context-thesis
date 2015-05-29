package dataset;

import java.util.List;

/**
 * Represents the papers that cite 20 target papers in the datasets.
 * These papers are the ones whose sentences are to be classified.
 * @author jonathan
 *
 * @param <T>
 */
public class CitingPaper<T extends Text> {
	public String title;
	public List<Sentence<T>> sentences;
	public CitingPaper(String title, List<Sentence<T>> sentences){
		this.title = title.replaceAll("[ \t\r\n\f]+", " ");
		this.sentences = sentences;
	}
}
