package markovRandomField;

import java.util.HashMap;
import java.util.Set;

import citationContextData.Dataset;

public class MRF_dataset extends Dataset{
	public HashMap<String,Double> citedContentUnigrams;
	public Set<String> acronyms;
	public Set<String> lexicalHooks;
	
	public MRF_dataset(Dataset dataset, HashMap<String,Double> citedContentUnigrams, Set<String> acronyms, Set<String> lexicalHooks) {
		super(dataset);
		this.citedContentUnigrams = citedContentUnigrams;
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
	}
}
