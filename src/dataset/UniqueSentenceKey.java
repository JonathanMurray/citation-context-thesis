package dataset;

public class UniqueSentenceKey<T extends Text> {
	public String citerTitle;
	public int sentenceIndex;
	public UniqueSentenceKey(String citerTitle, int sentenceIndex) {
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
		if(!(other instanceof UniqueSentenceKey<?>)){
			return false;
		}
		UniqueSentenceKey<T> o = ((UniqueSentenceKey<T>)other);
		return citerTitle.equals(o.citerTitle) && sentenceIndex == o.sentenceIndex;
	}
}
