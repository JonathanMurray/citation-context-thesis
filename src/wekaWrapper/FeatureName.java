package wekaWrapper;

public enum FeatureName{
	//Sentence contains a determiner followed by a work noun
	DET_WORK,
	
	//Sentence starts with a 3rd person pronoun
	PRONOUN,
	
	//Sentence starts with connector word, "however", "although" etc
	CONNECTOR,
	
	//Sentence comes after an explicit reference sentence
	AFTER_EXPLICIT,
	
	//Previous sentence started with a section header
	AFTER_HEADING,
	
	//Starts with a section header
	HEADING,
	
	//Next sentence starts with a section header
	BEFORE_HEADING,
	
	//Contains the last name of the main author
	CONTAINS_AUTHOR,
	
	//Contains acronyms that were mentioned often in explicit references
	CONTAINS_ACRONYM,
	
	//Contains Capitalized words that were mentioned often in explicit references
	CONTAINS_LEXICAL_HOOK;
	
}