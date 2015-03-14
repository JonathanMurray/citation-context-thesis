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
		Dataset<T> dataset = fromHtmlFile(params, htmlFile);
		String text = Texts.readTextFile(citedContentTextFile);
		dataset.citedContent = TextFactory.getText(params.textParams, text);
		return dataset;
	}
	
	/**
	 * 
	 * Returns incomplete dataset. cited-content missing
	 */
	public static <T extends Text> Dataset<T> fromHtmlFile(DatasetParams<T> params, File htmlFile){
		Timer t = new Timer();
		Document doc;
		try {
			doc = Jsoup.parse(htmlFile, null);
			String citedTitle = doc.select(".dstPaperTitle").get(0).text();
			String[] citedAuthors = doc.select(".dstPaperAuthors").get(0).text().split(";");
			String mainAuthorLastName = citedAuthors[0].split(",")[0];
			
			List<CitingPaper<T>> citers = new ArrayList<CitingPaper<T>>();
			
			Elements citerElements = doc.select("table.srcPaper > tbody > tr");
			for(Element citer : citerElements){
				List<Sentence<T>> sentences = new ArrayList<Sentence<T>>();
				String citerTitle = citer.childNode(1).attr("title");
				for(int i = 3; i < citer.childNodeSize(); i+= 2){
					Node line = citer.childNode(i);
					String type = getTypeFromClassAttr(line.attr("class"));
					String rawText = line.attr("title").split("\t")[1].trim().replaceAll(" +", " ");
					
					Sentence<T> sentence = new Sentence<T>(type, TextFactory.getText(params.textParams, rawText));
					sentences.add(sentence);
				}
				citers.add(new CitingPaper<T>(citerTitle, sentences));
			}
			String datasetLabel = htmlFile.getName().substring(0, htmlFile.getName().length() - 5);
			System.out.println("  took " + t.getMillisString()  + "   ");
			Dataset<T> dataset = Dataset.withoutCitedData(datasetLabel, mainAuthorLastName, citedTitle, citers);
			if(params.isEnhanced){
				dataset = dataset.withExtra(params.authorProxyBoundary, params.numLexicalHooks);
			}
			return dataset;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
}
