package dataset;

/**
 * Parameters used for constructing a dataset
 * @author jonathan
 *
 * @param <T>
 */
public class DatasetParams<T extends Text>{
	public TextParams<T> textParams;
	public boolean withAcronymsHooks;
	public int authorProxyBoundary;
	public int numLexicalHooks;
	public int numAcronyms;

	/**
	 * Without acronyms and hooks
	 * @param textParams
	 * @return
	 */
	public static <T extends Text> DatasetParams<T>  basic(TextParams<T> textParams) {
		return new DatasetParams<>(textParams, false, -1, -1, -1);
	}
	
	/**
	 * With acronyms and hooks
	 * @param textParams
	 * @param authorProxyBoundary
	 * @param numLexicalHooks
	 * @param numAcronyms
	 * @return
	 */
	public static <T extends Text> DatasetParams<T> enhanced(TextParams<T> textParams, int authorProxyBoundary, int numLexicalHooks, int numAcronyms) {
		return new DatasetParams<T>(textParams, true, authorProxyBoundary, numLexicalHooks, numAcronyms);
	}
	
	private DatasetParams(TextParams<T> textParams, boolean withAcronymsHooks, int authorProxyBoundary, int numLexicalHooks, int numAcronyms){
		this.textParams = textParams;
		this.withAcronymsHooks = withAcronymsHooks;
		this.authorProxyBoundary = authorProxyBoundary;
		this.numLexicalHooks = numLexicalHooks;
		this.numAcronyms = numAcronyms;
	}
}