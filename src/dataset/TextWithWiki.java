package dataset;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;

import util.CosineSimilarity;
import concepts.Concept;

public class TextWithWiki extends TextWithNgrams{
	
	protected static final String XML_TEXT_CLASS = "text-with-concepts";
	
//	public final List<Concept> concepts;
	public final TObjectDoubleHashMap<Integer> conceptMap;
//	public final double logNumConcepts;

	public TextWithWiki(String raw, List<String> rawWords, List<String> lemmatizedWords, 
			Ngrams ngrams, List<Concept> concepts) {
		super(raw, rawWords, lemmatizedWords, ngrams);
//		this.concepts = concepts;
		conceptMap = new TObjectDoubleHashMap<Integer>();
		for(Concept c : concepts){
			TIntIterator it = c.indices.iterator();
			while(it.hasNext()){
				int index = it.next();
				conceptMap.adjustOrPutValue(index, 1, 1);
			}
		}
//		if(concepts.size() < 2){
//			logNumConcepts = Math.log(2);
//		}else{
//			logNumConcepts = Math.log(concepts.size());
//		}
		
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
		ArrayList<Concept> concepts = new ArrayList<Concept>();
		for(Element conceptTag : textTag.select("concepts").select("concept")){
			TIntHashSet indices = new TIntHashSet();
			String indicesString = conceptTag.text();
			Texts.split(indicesString)
				.map(indexString -> Integer.parseInt(indexString))
				.forEach(index -> indices.add(index));
			concepts.add(new Concept(indices));
		}
		return new TextWithWiki(textWithNgrams.raw, textWithNgrams.rawWords, textWithNgrams.lemmas, 
				textWithNgrams.ngramsTfIdf, concepts);
	}

	@Override
	public double similarity(Object o) {
//		double ngramSimilarity = super.similarity(o);
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
//		return ngramSimilarity;// * conceptSimilarity; //TODO
	}

}
