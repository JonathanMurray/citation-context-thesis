package wekaWrapper;

public enum FeatureName{
	CONTAINS_DET_WORK,//Sentence contains a determiner followed by a work noun
	STARTS_3_PRONOUN,//Sentence starts with a 3rd person pronoun
	STARTS_CONNECTOR,//Sentence starts with connector word, "however", "although" etc
	AFTER_EXPLICIT,//Sentence comes after an explicit reference sentence
	AFTER_HEADING,//Previous sentence started with a section header
	HEADING,//Starts with a section header
	BEFORE_HEADING,//Next sentence starts with a section header
	CONTAINS_ONLY_OTHER_AUTHOR,
	CONTAINS_AUTHOR,//Contains the last name of the main author
	CONTAINS_ACRONYM_SCORE,//Contains acronyms that were mentioned often in explicit references
	CONTAINS_LEXICAL_HOOK_SCORE,//Contains Capitalized words that were mentioned often in explicit references
	SENTENCE_NUMBER,
	TEXT,
	
	//enhanced features
	DISTANCE_PREV_EXPLICIT,
	DISTANCE_NEXT_EXPLICIT,
	SIMILAR_TO_EXPLICIT,
	SIMILAR_TO_CITED_TITLE,
	SIMILAR_TO_CITED_CONTENT,
	STARTS_DET,
	CONTAINS_DET,
	
	SEMANTIC_VECTOR,
	SEMANTIC_SIMILAR_TO_EXPLICIT,
	
	MRF_PROBABILITY;
}