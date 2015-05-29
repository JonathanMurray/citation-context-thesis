package main;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import util.Environment;
import dataset.CitingPaper;
import dataset.Dataset;
import dataset.DatasetXml;
import dataset.SentenceType;
import dataset.Text;

/**
 * Measure the gaps between citations in the dataset. This simply
 * serves as an analysis of the dataset
 * @author jonathan
 *
 */
public class ContextGap {
	public static void main(String[] args) throws ClassNotFoundException {
		List<String> labels = Arrays.asList(new String[]{
				"D07-1031", "J96-2004", "N06-1020", "P04-1015", "P05-1045", "W02-1011", "W06-1615",
				"A92-1018", "J90-1003", "N03-1003", "P04-1035", "P07-1033", "W04-1013", "C98-2122", 
				"J93-1007", "N04-1035", "P02-1053", "P04-1041", "P90-1034", "W05-0909"});
		String resourcesDir = Environment.resources();
		File XML_DIR = new File(resourcesDir, "xml-datasets");
		TIntIntHashMap gaps = new TIntIntHashMap();
		for(String label : labels){
			final int MAX_CITERS = 0;
			Dataset<Text> dataset = DatasetXml.parseXmlFile(
					Text.class,
					new File(XML_DIR, label + ".xml"), 
					MAX_CITERS);
			for(CitingPaper<Text> citer : dataset.citers){
				for(int i = 0; i < citer.sentences.size(); i++){
					if(citer.sentences.get(i).type != SentenceType.IMPLICIT_REFERENCE){
						continue;
					}
					int minDist = Integer.MAX_VALUE;
					for(int dist = 1; dist < citer.sentences.size(); dist++){
						if(i - dist >= 0 && citer.sentences.get(i-dist).type != SentenceType.NOT_REFERENCE){
							minDist = dist;
							break;
						}
						if(i + dist < citer.sentences.size() && citer.sentences.get(i+dist).type != SentenceType.NOT_REFERENCE){
							minDist = dist;
							break;
						}
					}
					if(minDist == Integer.MAX_VALUE){
						gaps.adjustOrPutValue(0, 1, 1); //There was only 1 citation in citer. Set gap to 0
					}else{
						gaps.adjustOrPutValue(minDist-1, 1, 1); //gap is minDist - 1
					}
				}
			}
		}
		
		System.out.println("Gaps:");
		System.out.println(gaps);
		System.out.println("map size: " + gaps.size());
		int totalImplicit = 0;
		for(int numImplicit : gaps.values()){
			totalImplicit += numImplicit;
		}
		System.out.println("Total implicit citations: " + totalImplicit);
	}
	
}
