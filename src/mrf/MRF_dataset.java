package mrf;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import citationContextData.Dataset;

public class MRF_dataset extends Dataset{
	public HashMap<String,Double> citedContentUnigrams;
	public Set<String> acronyms;
	public Set<String> lexicalHooks;
	public List<MRF_citerNgrams> citersNgrams;
	
	public MRF_dataset(Dataset dataset, HashMap<String,Double> citedContentUnigrams, Set<String> acronyms, Set<String> lexicalHooks, List<MRF_citerNgrams> citersNgrams) {
		super(dataset);
		this.citedContentUnigrams = citedContentUnigrams;
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
		this.citersNgrams = citersNgrams;
	}
}
