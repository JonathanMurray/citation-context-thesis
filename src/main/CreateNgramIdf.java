package main;

import java.io.File;

import util.Environment;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.NgramIdf;
import dataset.Text;

public class CreateNgramIdf {
	public static void main(String[] args) {
		
		NgramIdf ngramIdf = new NgramIdf();
		String[] labels = new String[]{
		"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
		"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
		"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"};
		for(String label : labels){
			File datasetXmlFile = new File(Environment.resources(), "xml-datasets/" + label + "-with-ngrams.xml");
			Dataset<Text> dataset = DatasetXml.parseXmlFile(Text.class, datasetXmlFile, 0);
			ngramIdf.parseDataset(dataset);
		}

		ngramIdf.writeXml(new File(Environment.resources(), "xml-datasets/ngram-frequencies.xml"));
	}
}
