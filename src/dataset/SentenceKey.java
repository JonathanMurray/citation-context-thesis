package dataset;

public class SentenceKey<T extends Text> {
	public String citerTitle;
	public int sentenceIndex;
	public SentenceKey(String citerTitle, int sentenceIndex) {
		this.citerTitle = citerTitle;
		this.sentenceIndex = sentenceIndex;
	}
	
	public String toString(){
		return citerTitle + "[" + sentenceIndex + "]";
	}
	
	@Override
	public int hashCode(){
		return (citerTitle + sentenceIndex).hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof SentenceKey<?>)){
			return false;
		}
		SentenceKey<T> o = ((SentenceKey<T>)other);
		return citerTitle.equals(o.citerTitle) && sentenceIndex == o.sentenceIndex;
	}
}
