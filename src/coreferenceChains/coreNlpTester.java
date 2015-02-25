package coreferenceChains;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;

public class coreNlpTester {
	public static void main(String[] args) {
	    Properties props = new Properties();
//	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    // read some text in the text variable
	    String text = "Kupiec (1991) extends Baum-Welch re-estimation to arbitrary (nonCNF) CFGs.\n"
	    		+ "Baum-Welch re-estimation can be used with restricted or unrestricted grammars/models in the sense that some of the parameters corresponding to possible productions over a given (non-)terminal category set/set of states can be given an initial probability of zero.\n"
	    		+ "Unrestricted grammars/models quickly become impractical because the number of parameters requiring estimation becomes large and these algorithms are polynomial in the length of the input and number of free parameters.\n"
	    		+ "27 Computational Linguistics Volume 19, Number 1 Typically, in applications of Markov modeling in speech recognition, the derivation used to analyze a given input is not of interest; rather what is sought is the best (most likely) model of the input.\n"
	    		+ "In any application of these or similar techniques to parsing, though, the derivation selected is of prime interest.\n";
	    
	    try {
			text = Files.lines(Paths.get("/media/jonathan/92970132-26c5-4868-b498-a29efcaa77c2/Documents/exjobb/BART/TEST-WRITER.txt"))
					.reduce("", (s1, s2) -> s1 + "\n" + s2);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	    

	    
	    
	    // create an empty Annotation just with the given text
	    Annotation document = new Annotation(text);
	    
	    // run all Annotators on this text
	    long time = System.currentTimeMillis();
	    pipeline.annotate(document);
	    System.out.println(pipeline.timingInformation());
	    long passedTime = System.currentTimeMillis() - time;
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
//	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
//	    for(CoreMap sentence: sentences) {
//	      // traversing the words in the current sentence
//	      // a CoreLabel is a CoreMap with additional token-specific methods
//	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
//	        // this is the text of the token
//	        String word = token.get(TextAnnotation.class);
//	        // this is the POS tag of the token
//	        String pos = token.get(PartOfSpeechAnnotation.class);
//	        // this is the NER label of the token
//	        String ne = token.get(NamedEntityTagAnnotation.class);       
//	      }
//
//	      // this is the parse tree of the current sentence
//	      Tree tree = sentence.get(TreeAnnotation.class);
//
//	      // this is the Stanford dependency graph of the current sentence
//	      SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
//	    }

	    // This is the coreference link graph
	    // Each chain stores a set of mentions that link to each other,
	    // along with a method for getting the most representative mention
	    // Both sentence and token offsets start at 1!
	    Map<Integer, CorefChain> chains = 
	      document.get(CorefChainAnnotation.class);
	    
	    chains.entrySet().stream()
	    	.forEach(e -> {
	    		CorefChain chain = e.getValue();
	    		if(chain.getMentionMap().size() > 1){
	    			System.out.println(chain);
	    		}
	    	});
	    
	}
}
