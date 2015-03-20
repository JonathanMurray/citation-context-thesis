package dataset;

public class DatasetParams<T extends Text>{
	public TextParams<T> textParams;
	public boolean isEnhanced;
	public int authorProxyBoundary;
	public int numLexicalHooks;
	public int numAcronyms;

	public static <T extends Text> DatasetParams<T>  basic(TextParams<T> textParams) {
		return new DatasetParams<>(textParams, false, -1, -1, -1);
	}
	
	public static <T extends Text> DatasetParams<T> enhanced(TextParams<T> textParams, int authorProxyBoundary, int numLexicalHooks, int numAcronyms) {
		return new DatasetParams<T>(textParams, true, authorProxyBoundary, numLexicalHooks, numAcronyms);
	}
	
	private DatasetParams(TextParams<T> textParams, boolean withExtra, int authorProxyBoundary, int numLexicalHooks, int numAcronyms){
		this.textParams = textParams;
		this.isEnhanced = withExtra;
		this.authorProxyBoundary = authorProxyBoundary;
		this.numLexicalHooks = numLexicalHooks;
		this.numAcronyms = numAcronyms;
	}
}