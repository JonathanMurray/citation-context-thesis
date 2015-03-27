package dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

import concepts.SynsetExtractor;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.WordID;

public class TextWithSynsets extends TextWithNgrams{
	
	private static final String TAG_SYNSETS = "synsets";
	private static final String XML_TEXT_CLASS = "text-with-synsets";

	private static final Pattern SPACE = Pattern.compile("\\s+");
	
	private List<ISynset> synsets;
	
	
	public TextWithSynsets(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf,
			List<ISynset> synsets) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		this.synsets = synsets;
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		Element synsetsTag = text.appendElement(TAG_SYNSETS);
		StringBuilder synsetsText = new StringBuilder();
		System.out.println(synsets);
		for(ISynset synset : synsets){
			System.out.println(synset); //TODO
			IWordID wordId = synset.getWord(1).getID();
			synsetsText.append(wordId.toString() + " "); //store word-ids since it's more readable
		}
		synsetsTag.text(synsetsText.toString());
		return text;
	}
	
	public static TextWithSynsets fromXml(Element textTag, IDictionary dict){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		String synsetsText = textTag.select(TAG_SYNSETS).first().text();
		String[] split = SPACE.split(synsetsText);
		List<ISynset> synsets = new ArrayList<ISynset>();
		for(String id : split){ //store word-ids since it's more readable
			ISynset synset = dict.getWord(WordID.parseWordID(id)).getSynset();
			synsets.add(synset);
		}
		return new TextWithSynsets(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf, synsets);
	}
	
	@Override
	public double similarity(Object o){
		TextWithNgrams other = (TextWithNgrams)o;
		return ngramsTfIdf.similarity(other.ngramsTfIdf);
	}
	
}
