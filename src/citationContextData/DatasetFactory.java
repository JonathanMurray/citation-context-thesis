package citationContextData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import util.Printer;
import util.Texts;
import util.Timer;

public class DatasetFactory {
	
	private static Printer printer = new Printer(true);
	
	private static String getTypeFromClassAttr(String classes){
		return classes.split(" ")[1];
	}
	
	public static <T extends Text> ArrayList<Dataset<T>> fromHtmlDir(DatasetParams<T> params, File dir){
		ArrayList<Dataset<T>> datasets = new ArrayList<Dataset<T>>();
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
			datasets.add(fromFiles(params, htmlFile, textFile));
		}
		printer.println(" [x]");
		return datasets;
	}
	
	public static <T extends Text> Dataset<T> fromFiles(DatasetParams<T> params, File htmlFile, File citedContentTextFile){
		String text = Texts.readTextFile(citedContentTextFile, 20);
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
		String citedTitle = doc.select(".dstPaperTitle").get(0).text();
		String[] citedAuthors = doc.select(".dstPaperAuthors").get(0).text().split(";");
		String mainAuthorLastName = citedAuthors[0].split(",")[0];
		
		List<CitingPaper<T>> citers = new ArrayList<CitingPaper<T>>();
		
		Elements citerElements = doc.select("table.srcPaper > tbody > tr");
		StringBuilder mergedExplicitCitations = new StringBuilder();
		for(Element citer : citerElements){
			List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
			String citerTitle = citer.childNode(1).attr("title");
			for(int i = 3; i < citer.childNodeSize(); i+= 2){
				Node line = citer.childNode(i);
				String sentiment = getTypeFromClassAttr(line.attr("class"));
				String rawText = line.attr("title").split("\t")[1].trim().replaceAll(" +", " ");
				Sentence<T> sentence = new Sentence<T>(sentiment, TextFactory.getText(params.textParams, rawText));
				boolean startOfReferencesSection = sentence.text.raw.startsWith("\\d?\\d?\\.? R(EFERENCES|eferences)");
				if(startOfReferencesSection){
					break;
				}
				sentences.add(sentence);
				if(sentence.type == SentenceType.EXPLICIT_REFERENCE){
					mergedExplicitCitations.append(rawText + "\n");
				}
			}
			citers.add(new CitingPaper<T>(citerTitle, sentences));
		}
		
		System.out.println("Creating dataset from HTML took " + t.getSecString()  + "   ");
		T citedTitleText = TextFactory.getText(params.textParams, citedTitle);
		T mergedExplicitCitationsText = TextFactory.getText(params.textParams, mergedExplicitCitations.toString());
		Dataset<T> dataset = Dataset.withoutCitedData(label, mainAuthorLastName, citedTitleText, citers, mergedExplicitCitationsText);
		dataset.citedContent = TextFactory.getText(params.textParams, citedContent);
		if(params.isEnhanced){
			dataset = dataset.findExtra(params.authorProxyBoundary, params.numLexicalHooks, params.numAcronyms);
		}
		return dataset;
	}
}
