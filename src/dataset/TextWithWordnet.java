package dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.jsoup.nodes.Element;

import semanticSim.WordNet;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

/**
 * Not used in the thesis
 * @author jonathan
 *
 */
public class TextWithWordnet extends Text{
	
	public static final String XML_TEXT_CLASS = "text-with-wordnet";
	private WordNet wordnet;

	public TextWithWordnet(String raw, List<String> rawWords, List<String> lemmas, WordNet wordnet) {
		super(raw, rawWords, lemmas);
		this.wordnet = wordnet;
	}
	
	@Override
	public Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		return text;
	}
	
	public static TextWithWordnet fromXml(Element textTag, WordNet wordnet){
		Text text = Text.fromXml(textTag);
		return new TextWithWordnet(text.raw, text.rawWords, text.lemmas, wordnet);
	}

	@Override
	public double similarity(Object o) {
		
		
		Stream<IWord> words1 = lemmas.stream()
				.map(lemma -> wordnet.dict.getIndexWord(lemma, POS.NOUN))
				.filter(indexWord -> indexWord != null)
				.flatMap(indexWord -> indexWord.getWordIDs().stream())
				.map(wordId -> wordnet.dict.getWord(wordId));
				
		ArrayList<IWord> words2 = lemmas.stream()
				.map(lemma -> wordnet.dict.getIndexWord(lemma, POS.NOUN))
				.filter(indexWord -> indexWord != null)
				.flatMap(indexWord -> indexWord.getWordIDs().stream())
				.map(wordId -> wordnet.dict.getWord(wordId))
				.collect(Collectors.toCollection(ArrayList::new));
		
		MutableDouble sum = new MutableDouble(0);
		words1.parallel().forEach(word1 -> {
			if(words2.stream().anyMatch(word2 -> word2.equals(word1))){
				sum.add(1);
			}else{
				for(IWordID related1 : word1.getRelatedWords()){
					if(words2.stream().anyMatch(word2 -> word2.equals(related1))){
						sum.add(1);
						break;
					}
				}
			}
		});
		return sum.doubleValue() / (double) words1.count();
	}

}
