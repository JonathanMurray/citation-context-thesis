package conceptGraph;

import java.util.List;

import org.jsoup.nodes.Element;

import citationContextData.Text;

public class TextWithConcepts extends Text{
	
	public final List<Concept> concepts;

	public TextWithConcepts(String raw, String lemmatized, List<String> lemmatizedWords, List<Concept> concepts) {
		super(raw, lemmatized, lemmatizedWords);
		this.concepts = concepts;
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", "text-with-concepts");
		Element conceptsTag = text.appendElement("concepts");
		for(Concept concept : concepts){
			Element conceptTag = conceptsTag.appendElement("concept");
			for(Integer index : concept.indices){
				conceptTag.appendElement("index").text(index.toString());
			}
		}
		return text;
	}

}
