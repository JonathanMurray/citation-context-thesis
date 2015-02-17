package ml;

public enum Feature{
	//Sentence contains a determiner followed by a work noun
	DET_WORK,
	
	//Sentence starts with a 3rd person pronoun
	PRONOUN,
	
	//Sentence starts with connector word, "however", "although" etc
	CONNECTOR,
	
	AFTER_EXPLICIT,
	
	AFTER_HEADING,
	
	HEADING,
	
	BEFORE_HEADING,
	
	CONTAINS_AUTHOR;
	
}