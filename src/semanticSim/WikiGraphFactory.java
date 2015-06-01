package semanticSim;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import util.Printer;


/**
 * Builds up a title-graph from the wiki-dumps at http://haselgrove.id.au/wikipedia.htm
 * @author jonathan
 *
 */
public class WikiGraphFactory {
	
	private static Printer printer = new Printer(true);
	
	public static QuickWikiGraph quickWikiGraph(String titlesSortedPath, String linksSortedPath){
		return new QuickWikiGraph(new File(titlesSortedPath), new File(linksSortedPath)); 
	}
	
	@SuppressWarnings("unchecked")
	public static PreBuiltWikiGraph loadWikiGraph(File linksFile, File indicesFile, boolean allowStopwordConcepts){
		try{
			TIntObjectHashMap<TIntArrayList> links;
			TObjectIntHashMap<String> indices;
			try(ObjectInputStream linksIn = new ObjectInputStream(new FileInputStream(linksFile))){
				printer.print("Loading wiki links from " + linksFile + " ... ");
				links = (TIntObjectHashMap<TIntArrayList>) linksIn.readObject();
				printer.println("[x]");
			}
			try(ObjectInputStream indicesIn = new ObjectInputStream(new FileInputStream(indicesFile))){
				printer.print("Loading wiki indices from " + indicesFile + " ... ");
				indices = (TObjectIntHashMap<String>) indicesIn.readObject();
				printer.println("[x]");
			}
			return new PreBuiltWikiGraph(links, indices, allowStopwordConcepts);
		}catch(IOException | ClassNotFoundException e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public static void readDataBuildLinksAndSaveToFile(String toIndexPath, String linksPath, boolean onlySingleWords){
		File dir = new File("/home/jonathan/Documents/exjobb/data/wikipedia/");
		try {
			Structures structures = readDataAndBuildLinks(new File(dir,"links-simple-sorted.txt"), new File(dir, "titles-sorted.txt"), onlySingleWords);
			try(ObjectOutputStream toIndexStream = new ObjectOutputStream(new FileOutputStream(new File(toIndexPath)))){
				toIndexStream.writeObject(structures.phraseToIndex);
			}
			try(ObjectOutputStream linksStream = new ObjectOutputStream(new FileOutputStream(new File(linksPath)))){
				linksStream.writeObject(structures.links);
			}
			printer.print("Saved graph to files.");
		} catch (IOException | InterruptedException e) {
			
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Structures readDataAndBuildLinks(File linksFile, File titlesFile, boolean onlySingleWordIndices) throws IOException, InterruptedException{
		printer.println("Creating data from files: " + linksFile.getPath() + ",  " + titlesFile.getPath());
		try(BufferedReader linksReader = new BufferedReader(new FileReader(linksFile))){
			try (BufferedReader titlesReader = new BufferedReader(new FileReader(titlesFile))) {
				return readDataAndBuildLinks(linksReader, titlesReader, onlySingleWordIndices);
			}
		}
	}
	
	public static Structures readDataAndBuildLinks(BufferedReader linksReader, BufferedReader titlesReader, boolean onlySingleWords) throws IOException{
		TObjectIntHashMap<String> phraseToIndex = new TObjectIntHashMap<String>();
		TIntObjectHashMap<TIntArrayList> links = new TIntObjectHashMap<TIntArrayList>(); 
		TIntHashSet oneWordIndices = new TIntHashSet();
		printer.println("Adding titles...");
		{
			Iterator<String> titleLines = titlesReader.lines().iterator();
			int i = 0;
			while(titleLines.hasNext()){
				i++;
				String title = titleLines.next();
				if(onlySingleWords){
					if(title.contains("_")){
						continue;
					}
					oneWordIndices.add(i);
				}
				String phrase = title.replace('_', ' ').toLowerCase();
				phraseToIndex.put(phrase, i);
			}
		}
		
		printer.println("Adding links...");
		
		//NOTE!
		//lines().parallel is extremely slow when running for a long time, due to garbage collection.
		//Crashes with "gc limit exceeded" after being almost stuck for several minutes at around 4'800'000
		Iterator<String> linksLines = linksReader.lines().iterator();
		printer.resetProgress();
		while(linksLines.hasNext()){
			String linksLine = linksLines.next();
			printer.progress(100000);
			int pos = linksLine.indexOf(' ') + 1;
			int citer = Integer.parseInt(linksLine.substring(0, pos-2));
			if(onlySingleWords && !oneWordIndices.contains(citer)){
				continue;
			}
			TIntArrayList allCited = new TIntArrayList();
	        int end;
	        while ((end = linksLine.indexOf(' ', pos)) >= 0) {
	        	int cited = Integer.parseInt(linksLine.substring(pos, end));
	        	
	            pos = end + 1;
	            if(!onlySingleWords || oneWordIndices.contains(cited)){
	            	allCited.add(cited);
	        	}
	            
	        }
	        links.put(citer, allCited);
		}
		return new Structures(phraseToIndex, links);
	}
	
	private static class Structures{
		TObjectIntHashMap<String> phraseToIndex;
		TIntObjectHashMap<TIntArrayList> links;
		
		public Structures(TObjectIntHashMap<String> phraseToIndex, TIntObjectHashMap<TIntArrayList> links) {
			this.phraseToIndex = phraseToIndex;
			this.links = links;
		}
	}
}
