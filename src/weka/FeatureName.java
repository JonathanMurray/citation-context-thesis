package weka;

/**
 * Most features used in the WEKA-classification.
 * @author jonathan
 *
 */
public enum FeatureName{
	DET_WORK,//Sentence contains a determiner followed by a work noun
	PRONOUN,//Sentence starts with a 3rd person pronoun
	CONNECTOR,//Sentence starts with connector word, "however", "although" etc
	CITE_PREV,//Sentence comes after an explicit reference sentence
	HEADING_PREV,//Previous sentence started with a section header
	HEADING,//Starts with a section header
	HEADING_NEXT,//Next sentence starts with a section header
	OTHER_CITE,
	AUTHOR,//Contains the last name of the main author
	ACRONYM,//Contains acronyms that were mentioned often in explicit references
	LEXICAL_HOOK,//Contains Capitalized words that were mentioned often in explicit references
	SENTENCE_NUMBER,
	TEXT,
	
	//enhanced features
	CITE_PREV_DISTANCE,
	CITE_NEXT_DISTANCE,
	CITE_SIMILARITY,
	TITLE_SIMILARITY,
	CONTENT_SIMILARITY,
	STARTS_DET,
	CONTAINS_DET,
	
	SEMANTIC_VECTOR,
	SEMANTIC_SIMILAR_TO_EXPLICIT,
	
	MRF_PROBABILITY;
}