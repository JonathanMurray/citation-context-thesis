package dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

import util.Environment;
import concepts.SynsetExtractor;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.WordID;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class TextWithSynsets extends TextWithNgrams{
	
	public static void main(String[] args) {
		test2();
	}
	
	public static void test1(){
		Collection<Concept> concepts = db.getAllConcepts("dog", "n");
		ISynsetID s = WordID.parseWordID("WID-05809192-N-01-content").getSynsetID();
		Concept c1 = new Concept("02710044-n");
		Concept c2 = new Concept("10023039-n");
		
		Relatedness r= path.calcRelatednessOfSynset(c1, c2);
		System.out.println(r.getScore());
		
//		System.out.println(c1);
//		System.out.println(s);
//		for(Concept c : concepts){
//			System.out.println(c.getSynset());
//		}
	}

	public static void test2(){
		String res = Environment.resources();
		NgramIdf ngramIdf = NgramIdf.fromXmlFile(new File(res, "xml-datasets/ngram-frequencies.xml"), 5);
		StanfordCoreNLP pipeline = SynsetExtractor.createPipeline();
		IDictionary dict = SynsetExtractor.dictFromDir(new File(res, "wordnet-dict").getPath());
		TextParams<TextWithSynsets> p = TextParams.withSynsets(ngramIdf, pipeline, dict);
		TextWithSynsets t1 = TextFactory.createText(p, "I have a dog that eats a lot");
		TextWithSynsets t2 = TextFactory.createText(p, "All animals need food.");
		double sim = t1.similarity(t2);
		System.out.println("Similarity: " + sim);
	}
	
	private static final String TAG_SYNSETS = "synsets";
	private static final String XML_TEXT_CLASS = "text-with-synsets";
	
	private static final ILexicalDatabase db = new NictWordNet();
    private static final RelatednessCalculator lin = new Lin(db);
    private static final RelatednessCalculator wup = new WuPalmer(db);
    private static final RelatednessCalculator path = new Path(db);

	private static final Pattern SPACE = Pattern.compile("\\s+");
	
	private List<ISynset> synsets;
	private List<Concept> concepts;
	
	
	public TextWithSynsets(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf,
			List<ISynset> synsets) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		this.synsets = synsets;
		setupConcepts();
	}
	
	private void setupConcepts(){
		concepts = new ArrayList<Concept>();
		for(ISynset s : synsets){
			String synsetString = s.toString();
			String conceptString = synsetString.substring(4, 13);
			char pos = Character.toLowerCase(synsetString.charAt(13));
			conceptString += pos;
			concepts.add(new Concept(conceptString));
		}
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
		TextWithSynsets other = (TextWithSynsets)o;
		System.out.println("my synsets: " + synsets);
		System.out.println("other synsets: " + other.synsets);
		double ngramSimilarity =  ngramsTfIdf.similarity(other.ngramsTfIdf);
		double synsetSimilarity = 0;
		for(Concept concept : concepts){
			for(Concept otherConcept : other.concepts){
				synsetSimilarity += path.calcRelatednessOfSynset(concept, otherConcept).getScore();
			}
		}
		return (ngramSimilarity + synsetSimilarity)/2;
	}
	
}
