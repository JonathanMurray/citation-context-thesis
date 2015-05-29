package dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

import util.Environment;
import concepts.SynsetExtractor;
import edu.cmu.lti.jawjaw.pobj.POS;
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
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * Represents a piece of text with a set of WordNet synsets extracted from the text
 * @author jonathan
 *
 */
public class TextWithSynsets extends TextWithNgrams{
	
	public static void main(String[] args) {
		test2();
	}
	
	public static void test1(){
		Collection<Concept> concepts = db.getAllConcepts("dog", "n");
		ISynsetID s = WordID.parseWordID("WID-05809192-N-01-content").getSynsetID();
		Concept c1 = new Concept("02710044-n");
		Concept c2 = new Concept("10023039-n");
		
//		Relatedness r= path.calcRelatednessOfSynset(c1, c2);
//		System.out.println(r.getScore());
		
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

	private static final Pattern SPACE = Pattern.compile("\\s+");
	private static final String TAG_SYNSETS = "synsets";
	private static final String XML_TEXT_CLASS = "text-with-synsets";
	
	private static final ILexicalDatabase db = new NictWordNet();
    static{
    	SynsetSimilarity.setup(db);
    }
    

	private TDoubleArrayList scoreBuffer = new TDoubleArrayList();
	private List<ISynset> synsets;
	private List<MyConcept> concepts;
	
	
	
	public TextWithSynsets(String raw, List<String> rawWords, List<String> lemmas, Ngrams ngramsTfIdf,
			List<ISynset> synsets) {
		super(raw, rawWords, lemmas, ngramsTfIdf);
		this.synsets = synsets;
		setupConcepts();
	}
	
	private void setupConcepts(){
		concepts = new ArrayList<MyConcept>();
		for(ISynset s : synsets){
			String synsetString = s.toString();
			String conceptString = synsetString.substring(11, 20);
			char pos = Character.toLowerCase(synsetString.charAt(20));
			conceptString += pos;
//			System.out.println("\n\n\n\n" + synsetString);
//			System.out.println("\n" + pos);
			concepts.add(new MyConcept(conceptString, POS.valueOf("" + pos)));
		}
	}
	
	@Override
	protected Element toXml(){
		Element text = super.toXml();
		text.attr("class", XML_TEXT_CLASS);
		Element synsetsTag = text.appendElement(TAG_SYNSETS);
		StringBuilder synsetsText = new StringBuilder();
//		System.out.println(synsets);
		for(ISynset synset : synsets){
//			System.out.println(synset); //TODO
			IWordID wordId = synset.getWord(1).getID();
			synsetsText.append(wordId.toString() + " "); //store word-ids since it's more readable
		}
		synsetsTag.text(synsetsText.toString());
		return text;
	}
	
	public static TextWithSynsets fromXml(Element textTag, IDictionary dict){
		TextWithNgrams text = TextWithNgrams.fromXml(textTag);
		String synsetsText = textTag.select(TAG_SYNSETS).first().text().trim();
		String[] split = SPACE.split(synsetsText);
		List<ISynset> synsets = new ArrayList<ISynset>();
		try {
			dict.open();
			for(String id : split){ //store word-ids since it's more readable
				if(id.length() > 0){
					ISynset synset = dict.getWord(WordID.parseWordID(id)).getSynset();
					synsets.add(synset);	
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		dict.close();
		
		return new TextWithSynsets(text.raw, text.rawWords, text.lemmas, text.ngramsTfIdf, synsets);
	}
	
	@Override
	public double similarity(Object o){
		TextWithSynsets other = (TextWithSynsets)o;
//		double ngramSimilarity =  ngramsTfIdf.similarity(other.ngramsTfIdf, 3);
		double synsetSimilarity = 0;
		int numScores = 0;
		scoreBuffer.reset();
		for(MyConcept concept : concepts){
			for(MyConcept otherConcept : other.concepts){
				double score = SynsetSimilarity.INSTANCE.sim(concept, otherConcept);
				if(score != -1){ 
//					synsetSimilarity += score; //TODO different calculators have different min/max-scores. We want [0-1] 
					numScores ++;
					scoreBuffer.add(score);
				}
			}
		}
		
		//TODO only count highest 50% of scores
		if(numScores > 0){
//			scoreBuffer.sort();
//			int start = scoreBuffer.size()/2;
			int start = 0;
			for(int i = start; i < scoreBuffer.size(); i++){
				synsetSimilarity += scoreBuffer.get(i);
			}
			synsetSimilarity /= ((double)scoreBuffer.size() - start);
//			synsetSimilarity /= numScores;
		}
		
		if(Double.isNaN(synsetSimilarity)){
			throw new RuntimeException("similarity == NaN");
		}
		
		
//		if(synsetSimilarity > 0.3){
//			System.out.println("Similarity: " + synsetSimilarity);
//			System.out.println(this.synsets + "\n");
//			System.out.println(other.synsets + "\n");
//			System.out.println(this.raw + "\n");
//			System.out.println(other.raw);
//			System.out.println("\n\n\n");
//		}
		
		return synsetSimilarity;
//		
//		final double synsetWeight = 0.2; //TODO ad-hoc. Common value for synsetsim is around 0.4, ngrams give 0 very often, and sometimes > 0.1
//		double weightedSynset = synsetWeight*synsetSimilarity;
//		double weightedNgram = (1-synsetWeight)*ngramSimilarity;
////		System.out.println(Printer.toString(weightedSynset) + "  ---  " + weightedNgram);
//		return weightedSynset + weightedNgram;
	}
	
	private static class SynsetSimilarity{
		HashMap<MyConcept, TObjectDoubleHashMap<MyConcept>> cached = new HashMap<MyConcept, TObjectDoubleHashMap<MyConcept>>();

		private RelatednessCalculator lin = new Lin(db);
	    private RelatednessCalculator wup = new WuPalmer(db);
	    private RelatednessCalculator path = new Path(db);
	    
		static SynsetSimilarity INSTANCE;
		
		private static void setup(ILexicalDatabase db){
			INSTANCE = new SynsetSimilarity();
			INSTANCE.lin = new Lin(db);
			INSTANCE.wup = new WuPalmer(db);
			INSTANCE.path = new Path(db);
		}
		
		static int hits = 0;
			
		double sim(MyConcept a, MyConcept b){
			if(cached.containsKey(a)){
				if(cached.get(a).containsKey(b)){
					return cached.get(a).get(b);
				}else{
					double sim = simNoCache(a, b);
					cached.get(a).put(b, sim);
//					hits++;
//					if(hits % 100 == 0){
//						System.out.println(hits + " hits!");
//					}
					return sim;
				}
			}else if(cached.containsKey(b)){
				if(cached.get(b).containsKey(a)){
					return cached.get(b).get(a);
				}else{
					double sim = simNoCache(a, b);
					cached.get(b).put(a, sim);
//					hits++;
//					if(hits % 100 == 0){
//						System.out.println(hits + " hits!");
//					}
					return sim;
				}
			}else{
				TObjectDoubleHashMap<MyConcept> aMap = new TObjectDoubleHashMap<MyConcept>();
				cached.put(a, aMap);
				if(cached.size() % 500 == 0){
					System.out.println("Synsets cache size: " + cached.size());
				}
				
				double sim = simNoCache(a, b);
				aMap.put(b, sim);
				return sim;
			}
		}
		
		private double simNoCache(Concept a, Concept b){
			return path.calcRelatednessOfSynset(a, b).getScore();
		}
		
				
	}
	
	private static class MyConcept extends Concept{
		public MyConcept(String str, POS pos) {
			super(str, pos);
		}

		@Override
		public int hashCode(){
			return (super.getSynset() + super.getPos().toString()).hashCode();
		}
		
		public boolean equals(Object other){
			return other instanceof MyConcept && hashCode() == other.hashCode();
		}
		
	}
	
}
