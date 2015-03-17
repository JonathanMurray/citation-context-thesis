package citationContextData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Json {
	/**
	 * Writes a json structure of the whole data set
	 * @param file
	 */
	public static <T extends Text> void writeToJson(Dataset<T> dataset, File file){
		try(FileWriter writer = new FileWriter(file)){
			writer.write("{\n");
			writer.write("\"citedMainAuthor\": \"" + dataset.citedMainAuthor + "\",\n");
			writer.write("\"citedTitle\": \"" + dataset.citedTitle + "\",\n");
			writer.write("\"citers\": [\n");
			StringBuilder citersStr = new StringBuilder();
			for(CitingPaper<T> citer : dataset.citers.subList(0, 2)){
				citersStr.append("{\n");
				citersStr.append("\"title\": \"" + citer.title.replace('\n', ' ') + "\",\n");
				citersStr.append("\"sentences\": [\n");
				StringBuilder sentencesStr = new StringBuilder();
				for(Sentence<T> sentence : citer.sentences.subList(0, 2)){
					sentencesStr.append("{\n");
					sentencesStr.append("\"type\": \"" + sentence.sentiment + "\",\n");
					sentencesStr.append("\"text\": \"" + sentence.text.raw + "\"},\n");
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
