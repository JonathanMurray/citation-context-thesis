package wekaWrapper;

import java.util.HashMap;
import java.util.Set;

import citationContextData.Dataset;

public class WekaDataset extends Dataset{
	public Set<String> acronyms;
	public Set<String> lexicalHooks;
	
	public WekaDataset(Dataset dataset, Set<String> acronyms, Set<String> lexicalHooks) {
		super(dataset);
		this.acronyms = acronyms;
		this.lexicalHooks = lexicalHooks;
	}
}
