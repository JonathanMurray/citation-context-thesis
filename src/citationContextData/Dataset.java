package citationContextData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mrf.MRF_citerNgrams;
import mrf.MRF_dataset;
import util.DoubleMap;
import util.IntegerMap;
import util.Printer;
import util.Texts;
import wekaWrapper.WekaDataset;


public class Dataset {
	public String datasetLabel;
	public String citedMainAuthor;
	public String citedTitle;
	public String citedContent;
	public List<CitingPaper> citers;
	
	private static Printer printer = new Printer(true);
	
	public static Dataset fromHTMLFile(File htmlFile){
		return ContextHTML_Parser.parseHTML(htmlFile);
	}
	
	public static ArrayList<Dataset> datasetsFromDir(File dir){
		ArrayList<Dataset> datasets = new ArrayList<Dataset>();
		File[] files = dir.listFiles();
		printer.print("Creating citation data set from dir " + dir.getAbsolutePath() + ": ");
		for(int i = 0; i < files.length; i++){
			printer.progress(i, 1);
			File htmlFile = files[i];
			if(!htmlFile.getName().endsWith(".html")){
				continue;
			}
			String baseName = htmlFile.getName().substring(0, htmlFile.getName().length()-5);
			File textFile = new File(dir, baseName + ".txt");
			datasets.add(Dataset.fromFiles(htmlFile, textFile));
		}
		printer.println(" [x]");
		return datasets;
	}
	
	public static Dataset fromFiles(File htmlFile, File citedContentTextFile){
		Dataset dataset = ContextHTML_Parser.parseHTML(htmlFile);
		dataset.citedContent = readTextFile(citedContentTextFile);
		return dataset;
	}
	
	private static String readTextFile(File f){
		try(Scanner sc = new Scanner(new BufferedReader(new FileReader(f)))) {
			StringBuilder s = new StringBuilder();
			while(sc.hasNextLine()){
				s.append(sc.nextLine() + "\n");
			}
			return s.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public Dataset(Dataset dataset){
		this(dataset.datasetLabel, dataset.citedMainAuthor, dataset.citedTitle, dataset.citers, dataset.citedContent);
	}
	
	public Dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<CitingPaper> citers){
		this(datasetLabel, citedMainAuthor, citedTitle, citers, null);
	}

	public Dataset(String datasetLabel, String citedMainAuthor, String citedTitle, List<CitingPaper> citers, String citedContent){
		this.datasetLabel = datasetLabel;
		this.citedMainAuthor = citedMainAuthor;
		this.citedTitle = citedTitle;
		this.citedContent = citedContent;
		this.citers = citers;
	}
	
	public MRF_dataset getMRF_dataset(int authorProxyBoundary, int numLexicalHooks, boolean ngramsIgnoreStopwords, boolean ngramsStem){
		DoubleMap<String> citedContentUnigrams = Texts.instance().getNgrams(1, citedContent, true, true);
		Set<String> acronyms = findAcronyms(authorProxyBoundary);
		Set<String> lexicalHooks = findLexicalHooks(authorProxyBoundary, numLexicalHooks);
		lexicalHooks.remove(citedMainAuthor);
		ArrayList<MRF_citerNgrams> citersNgrams = citers.stream()
				.map(citer -> citer.getMRF_citerNgrams(ngramsIgnoreStopwords, ngramsStem))
				.collect(Collectors.toCollection(ArrayList::new));
		
		return new MRF_dataset(this, citedContentUnigrams, acronyms, lexicalHooks, citersNgrams);
	}
	
	public WekaDataset getWekaDataset(int authorProxyBoundary, int numLexicalHooks){
		Set<String> acronyms = findAcronyms(authorProxyBoundary);
		Set<String> lexicalHooks = findLexicalHooks(authorProxyBoundary, numLexicalHooks);
		lexicalHooks.remove(citedMainAuthor);
		return new WekaDataset(this, acronyms, lexicalHooks);
	}
	
	
	private Set<String> findAcronyms(int boundary){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z]+[ ,]");
		return new HashSet<String>(findMatchesInExplicitReferencesAroundAuthor(boundary, regex));
	}
	
	private Set<String> findLexicalHooks(int boundary, int numLexicalHooks){
		Pattern regex = Pattern.compile("[^a-zA-Z][A-Z][a-z]+[ ,:;]");
		List<String> nonDistinctHooks = findMatchesInExplicitReferencesAroundAuthor(boundary, regex);
		IntegerMap<String> counts = new IntegerMap<String>();
		nonDistinctHooks.stream()
			.filter(hook -> !hook.equals(citedMainAuthor))
			.forEach(hook -> {
				counts.increment(hook, 1);
			});
		return counts.getTopN(numLexicalHooks).stream()
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}
	
	private List<String> findMatchesInExplicitReferencesAroundAuthor(int boundary, Pattern regex){
		String author = citedMainAuthor;
		List<String> matches = new ArrayList<String>();
		
		explicitReferences().forEach(sentence -> {
				int index = sentence.text.indexOf(author);
				if(index >= 0){
					int left = Math.max(0, index-boundary);
					int right = Math.min(sentence.text.length()- 1, index+boundary);
					String vicinityOfAuthor = sentence.text.substring(left, right);
					Matcher m = regex.matcher(vicinityOfAuthor);
					while(m.find()){
						String match = m.group();
						match = match.replaceAll("[,\\[\\]\\(\\)]", "").trim();
						matches.add(match);
					}
				}
		});

		return matches;
	}
	
	private Stream<Sentence> explicitReferences(){
		return citers.stream()
				.flatMap(c -> c.sentences.stream())
				.filter(s -> s.type == SentenceClass.EXPLICIT_REFERENCE);
	}
	
	/**
	 * Writes a json structure of the whole data set
	 * @param file
	 */
	public void writeToJson(File file){
		try(FileWriter writer = new FileWriter(file)){
			writer.write("{\n");
			writer.write("\"citedMainAuthor\": \"" + citedMainAuthor + "\",\n");
			writer.write("\"citedTitle\": \"" + citedTitle + "\",\n");
			writer.write("\"citers\": [\n");
			StringBuilder citersStr = new StringBuilder();
			for(CitingPaper citer : citers.subList(0, 2)){
				citersStr.append("{\n");
				citersStr.append("\"title\": \"" + citer.title.replace('\n', ' ') + "\",\n");
				citersStr.append("\"sentences\": [\n");
				StringBuilder sentencesStr = new StringBuilder();
				for(Sentence sentence : citer.sentences.subList(0, 2)){
					sentencesStr.append("{\n");
					sentencesStr.append("\"type\": \"" + sentence.sentiment + "\",\n");
					sentencesStr.append("\"text\": \"" + sentence.text + "\"},\n");
				}
				citersStr.append(sentencesStr.substring(0, sentencesStr.length() - 2)); //get rid of last comma
				citersStr.append("\n");
				citersStr.append("]},\n");
			}
			writer.write(citersStr.substring(0, citersStr.length() - 2)); //get rid of last comma
			writer.write("\n");
			writer.write("]}");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	
}
