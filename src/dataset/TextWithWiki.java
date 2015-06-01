package dataset;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;

import semanticSim.WikiConcept;
import util.CosineSimilarity;

/**
 * Represents a piece of text as well as a set of Wikipedia-concepts,
 * that can be compared to other sentences with cosine similarity
 * @author jonathan
 *
 */
public class TextWithWiki extends TextWithNgrams{
	
	protected static final String XML_TEXT_CLASS = "text-with-concepts";
	
	public final TObjectDoubleHashMap<Integer> conceptMap;

	public TextWithWiki(String raw, List<String> rawWords, List<String> lemmatizedWords, 
			Ngrams ngrams, List<WikiConcept> concepts) {
		super(raw, rawWords, lemmatizedWords, ngrams);
		conceptMap = new TObjectDoubleHashMap<Integer>();
		for(WikiConcept c : concepts){
			TIntIterator it = c.indices.iterator();
			while(it.hasNext()){
				int index = it.next();
				conceptMap.adjustOrPutValue(index, 1, 1);
			}
		}
	}
	
//	@Override
//	protected Element toXml(){
//		Element text = super.toXml();
//		text.attr("class", XML_TEXT_CLASS);
//		Element conceptsTag = text.appendElement("concepts");
//		
//		for(Concept concept : concepts){
//			StringBuilder indicesString = new StringBuilder();
//			Element conceptTag = conceptsTag.appendElement("concept");
//			TIntIterator it = concept.indices.iterator();
//			while(it.hasNext()){
//				int index = it.next();
//				indicesString.append(index + " ");
//			}
//			conceptTag.text(indicesString.substring(0, indicesString.length()-1));
//		}
//		return text;
//	}
	
	public static TextWithWiki fromXml(Element textTag){
		TextWithNgrams textWithNgrams = TextWithNgrams.fromXml(textTag);
		ArrayList<WikiConcept> concepts = new ArrayList<WikiConcept>();
		for(Element conceptTag : textTag.select("concepts").select("concept")){
			TIntHashSet indices = new TIntHashSet();
			String indicesString = conceptTag.text();
			TextUtil.split(indicesString)
				.map(indexString -> Integer.parseInt(indexString))
				.forEach(index -> indices.add(index));
			concepts.add(new WikiConcept(indices));
		}
		return new TextWithWiki(textWithNgrams.raw, textWithNgrams.rawWords, textWithNgrams.lemmas, 
				textWithNgrams.ngramsTfIdf, concepts);
	}

	@Override
	public double similarity(Object o) {
		TextWithWiki other = (TextWithWiki)o;
//		double conceptSum = 0;
//		for(Concept c1 : concepts){
//			for(Concept c2 : other.concepts){
//				conceptSum += c1.cosineSimilarity(c2);
//			}
//		}
//		double conceptSimilarity;
//		if(concepts.size() == 0 || other.concepts.size() == 0){
//			conceptSimilarity = 0;
//		}else{
//			conceptSimilarity = conceptSum / concepts.size() / other.concepts.size();
//		}
		return CosineSimilarity.calculateCosineSimilarity(conceptMap, other.conceptMap);
//		return conceptSimilarity;
	}

}
