package citationContextData;

import java.util.HashMap;
import java.util.Set;

public class EnhancedDataset extends Dataset{
	public HashMap<String,Double> citedContentUnigrams;
	public Set<String> acronyms;
	public Set<String> lexicalHooks;
	
	public EnhancedDataset(Dataset dataset, HashMap<String,Double> citedContentUnigrams, Set<String> acronyms, Set<String> lexicalHooks) {
		super(dataset);
		this.citedContentUnigrams = citedContentUnigrams;
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
	}
	
}
