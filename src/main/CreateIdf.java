package main;

import java.io.File;

import util.Environment;
import util.Printer;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.NgramIdf;
import dataset.Text;

/**
 * Compute idf (inverse document frequency) values for the dataset and write
 * them to XML-files.
 * @author jonathan
 *
 */
public class CreateIdf {
	
	private final static String[] LABELS = new String[]{
			"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
			"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
			"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
	
	public static void main(String[] args) {
		Printer.printBigHeader("Create IDF-file");
		NgramIdf ngramIdf = new NgramIdf();
		NgramIdf skipgramIdf = new NgramIdf();
		for(int i = 0; i < LABELS.length; i++){
			String label = LABELS[i];
			Printer.printBigProgressHeader(i, LABELS.length);
			File datasetXmlFile = new File(Environment.resources(), "xml-datasets/" + label + "-with-ngrams.xml");
			Dataset<Text> dataset = DatasetXml.parseXmlFile(Text.class, datasetXmlFile, 0);
			ngramIdf.parseDataset(dataset, NgramIdf.Type.NGRAM);
			skipgramIdf.parseDataset(dataset, NgramIdf.Type.SKIPGRAM);
		}
		ngramIdf.writeXml(new File(Environment.resources(), "xml-datasets/ngram-frequencies.xml"), NgramIdf.DEFAULT_NGRAM_MIN_COUNT);
		skipgramIdf.writeXml(new File(Environment.resources(), "xml-datasets/skipgram-frequencies.xml"), NgramIdf.DEFAULT_SKIPGRAM_MIN_COUNT);
	}
}
