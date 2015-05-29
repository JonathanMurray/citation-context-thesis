package dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import util.Printer;
import util.Timer;

public class DatasetFactory {
	
	private static Printer printer = new Printer(true);
	
	private static String getTypeFromClassAttr(String classes){
		return classes.split(" ")[1];
	}
	
	public static <T extends Text> ArrayList<Dataset<T>> fromHtmlDir(DatasetParams<T> params, File dir){
		ArrayList<Dataset<T>> datasets = new ArrayList<Dataset<T>>();
		File[] files = dir.listFiles();
		Timer t = new Timer();
		printer.println("\n-----------------------------------------");
		printer.println("CREATING DATASETS FROM DIR: " + dir.getAbsolutePath() + ": ");
		printer.println("-----------------------------------------");
		for(int i = 0; i < files.length; i++){
			File htmlFile = files[i];
			if(!htmlFile.getName().endsWith(".html")){
				continue;
			}
			String baseName = htmlFile.getName().substring(0, htmlFile.getName().length()-5);
			File textFile = new File(dir, baseName + ".txt");
			datasets.add(fromFiles(params, htmlFile, textFile));
		}
		printer.println("Created datasets from dir (" + t.getSecString() + ")");
		return datasets;
	}
	
	public static <T extends Text> Dataset<T> fromFiles(DatasetParams<T> params, File htmlFile, File citedContentTextFile){
		final int CITED_CONTENT_MAX_LINES = -1;
		String text = Texts.readTextFile(citedContentTextFile, CITED_CONTENT_MAX_LINES);
		Dataset<T> dataset = fromHtmlFile(params, htmlFile, text);
		return dataset;
	}
	
	/**
	 * 
	 * Returns incomplete dataset. cited-content missing
	 */
	public static <T extends Text> Dataset<T> fromHtmlFile(DatasetParams<T> params, File htmlFile, String citedContent){
		try {
			Document doc = Jsoup.parse(htmlFile, null);
			String datasetLabel = htmlFile.getName().substring(0, htmlFile.getName().length() - 5);
			return fromHtml(params, datasetLabel, doc, citedContent);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	public static <T extends Text> Dataset<T> fromHtml(DatasetParams<T> params, String label, Document doc, String citedContent){
		Timer t = new Timer();
		printer.print("Creating dataset from HTML (" + label + ") ... ");
		String citedTitle = doc.select(".dstPaperTitle").get(0).text();
		String[] citedAuthors = doc.select(".dstPaperAuthors").get(0).text().split(";");
		String mainAuthorLastName = citedAuthors[0].split(",")[0];
		
		List<CitingPaper<T>> citers = new ArrayList<CitingPaper<T>>();
		
		Elements citerElements = doc.select("table.srcPaper > tbody > tr");
		StringBuilder mergedExplicitCitations = new StringBuilder();
		printer.resetProgress();
		for(Element citer : citerElements){
			printer.progress();
			List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
			String citerTitle = citer.childNode(1).attr("title");
			int sentenceIndex = 0;
			for(int i = 3; i < citer.childNodeSize(); i+= 2){
				Node line = citer.childNode(i);
				String sentiment = getTypeFromClassAttr(line.attr("class"));
				String rawText = line.attr("title").split("\t")[1].trim().replaceAll(" +", " ");
				Sentence<T> sentence = new Sentence<T>(sentiment, TextFactory.createText(params.textParams, rawText), sentenceIndex);
				if(isStartOfReferencesSection(sentence.text.raw)){
					printer.println("'" + citerTitle + "' reached start of references at " + i + " / " + citer.childNodeSize());
					break;
				}
				sentences.add(sentence);
				if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
					mergedExplicitCitations.append(rawText + "\n");
				}
				sentenceIndex ++;
			}
			citers.add(new CitingPaper<T>(citerTitle, sentences));
		}
		
		T citedTitleText = TextFactory.createText(params.textParams, citedTitle);
		T mergedExplicitCitationsText = TextFactory.createText(params.textParams, mergedExplicitCitations.toString());
		Dataset<T> dataset = Dataset.withoutCitedData(label, mainAuthorLastName, citedTitleText, citers, mergedExplicitCitationsText);
		dataset.citedContent = TextFactory.createText(params.textParams, citedContent);
		int numSentences = citers.stream().map(c -> c.sentences.size()).reduce(0, (x,y) -> x+y);
		printer.println("[x]  (" + t.getSecString() + ")  {" + dataset.citers.size() + " citers, in total " + numSentences + " sentences}");
		if(params.withExtra){
			dataset = dataset.findExtra(params.authorProxyBoundary, params.numLexicalHooks, params.numAcronyms);
		}
		return dataset;
	}
	
	public static boolean isStartOfReferencesSection(String sentence){
		 return sentence.startsWith("\\d?\\d?\\.?\\w*(R|r)(EFERENCES|eferences)");
	}
	
	public static <T extends Text, T2 extends Text> Dataset<T> fromOtherRaw(TextParams<T> params, Dataset<T2> other){
		printer.print("Creating dataset from other raw dataset " + other.datasetLabel + " ... ");
		Timer t = new Timer();
		printer.resetProgress();
		List<CitingPaper<T>> citers = other.citers.stream().parallel() //TODO
			.map(citer2 -> {
				printer.progress();
				List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
				for(Sentence<T2> sentence2 : citer2.sentences){
					T text = TextFactory.createText(params, sentence2.text.raw);
//					System.out.print(".");
					Sentence<T> sentence = new Sentence<T>(sentence2.type, text, sentence2.sentenceIndex);
					sentences.add(sentence);
				}
				return new CitingPaper<T>(citer2.title, sentences);
			}).collect(Collectors.toCollection(ArrayList::new));
		T citedTitle = TextFactory.createText(params, other.citedTitle.raw);
		T mergedExplicitCitations = TextFactory.createText(params, other.mergedExplicitCitations.raw);
		T citedContent = TextFactory.createText(params, other.citedContent.raw);
		Dataset<T> dataset = Dataset.full(other.datasetLabel, other.citedMainAuthor, citedTitle, citers, citedContent, mergedExplicitCitations);
		printer.println("[x]  (" + t.getSecString() + ")");
		return dataset;
	}
}
