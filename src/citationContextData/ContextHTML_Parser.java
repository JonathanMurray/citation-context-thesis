package citationContextData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

public class ContextHTML_Parser {
	
	public static final String dataDir = "/home/jonathan/Documents/exjobb/data/";
	public static final String corpusDir = dataDir + "teufel-citation-context-corpus/";
	

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		File f = Paths.get(corpusDir + "A92-1018.html").toFile();
		ContextHTML_Parser.parseHTML(f).writeToJson(Paths.get("test.json").toFile());
	}
	
	public static ContextDataSet parseHTML_Default(){
		final String dataDir = "/home/jonathan/Documents/exjobb/data/";
		String corpusDir = dataDir + "teufel-citation-context-corpus/";
		String filepath = corpusDir + "A92-1018.html";
		return parseHTML(Paths.get(filepath).toFile());
	}
	
	public static ContextDataSet parseHTML(File file){
		Document doc;
		try {
			doc = Jsoup.parse(file, null);
		
			String citedTitle = doc.select(".dstPaperTitle").get(0).text();
			String[] citedAuthors = doc.select(".dstPaperAuthors").get(0).text().split(";");
			String mainAuthorLastName = citedAuthors[0].split(",")[0];
			
			List<Citer> citers = new ArrayList<Citer>();
			
			Elements citerElements = doc.select("table.srcPaper > tbody > tr");
			for(Element citer : citerElements){
				List<Sentence> sentences = new ArrayList<Sentence>();
				String citerTitle = citer.childNode(1).attr("title");
				for(int i = 3; i < citer.childNodeSize(); i+= 2){
					Node line = citer.childNode(i);
					String type = getTypeFromClassAttr(line.attr("class"));
	//				System.out.println(line);
					String text = line.attr("title").split("\t")[1].trim().replaceAll(" +", " ");
					Sentence sentence = new Sentence(type, text);
					sentences.add(sentence);
	//				System.out.println(type + ": " + text);
				}
				citers.add(new Citer(citerTitle, sentences));
			}
			return new ContextDataSet(mainAuthorLastName, citedTitle, citers);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
	
	private static String getTypeFromClassAttr(String classes){
		return classes.split(" ")[1];
	}

	
}
