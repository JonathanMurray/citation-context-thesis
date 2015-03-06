package conceptGraph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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
	
//	public static PreBuiltWikiGraph buildWikiGraph(String linksPath, String indicesPath){
//		try{
//			
//			TIntObjectHashMap<TIntArrayList> links = WikiGraphFactory.deserializeLinks(linksPath);
//			TObjectIntHashMap<String> indices = WikiGraphFactory.deserializeIndices(indicesPath);
//			return new PreBuiltWikiGraph(links, indices);
//		}catch(IOException | ClassNotFoundException e){
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}
	
	public static PreBuiltWikiGraph loadWikiGraph(File linksFile, File indicesFile, double similarityMultiplier, boolean allowStopwordConcepts){
		try{
			printer.print("Loading links from " + linksFile + " ... ");
			TIntObjectHashMap<TIntArrayList> links = (TIntObjectHashMap<TIntArrayList>) new ObjectInputStream(new FileInputStream(linksFile)).readObject();
			printer.println("DONE.");
			printer.print("Loading indices from " + indicesFile + " ... ");
			TObjectIntHashMap<String> indices = (TObjectIntHashMap<String>) new ObjectInputStream(new FileInputStream(indicesFile)).readObject();
			printer.println("DONE.");
			return new PreBuiltWikiGraph(links, indices, similarityMultiplier, allowStopwordConcepts);
		}catch(IOException | ClassNotFoundException e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
//	public static  HashMap<Integer, TIntArrayList> deserializeLinks(String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{
//		
//		
//		
//		printer.println("Loading map<int, intlist> from file ...");
//		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)))){
//			int size = ois.readInt();
//			HashMap<Integer, TIntArrayList> map = new HashMap<Integer, TIntArrayList>();
//			for(int i = 0; i < size; i++){
//				Integer key = ois.readInt();
//				TIntArrayList value = (TIntArrayList) ois.readObject();
//				map.put(key, value);
//				printer.printProgress(i, 500000, 20);
//			}
//			printer.println("Load successful. Read " + map.size() + " entries.");
//			return map;
//		}
//	}
//	
//	public static HashMap<String, Integer> deserializeIndices(String filepath){
//		printer.println("Loading map<string,int> from file ...");
//		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)))){
//			int size = ois.readInt();
//			HashMap<String, Integer> map = new HashMap<String, Integer>();
//			for(int i = 0; i < size; i++){
//				String key = (String)ois.readObject();
//				int value = ois.readInt();
//				map.put(key, value);
//				printer.printProgress(i, 500000, 20);
//			}
//			printer.println("Load successful. Read " + map.size() + " entries.");
//			return map;
//		} catch (IOException | ClassNotFoundException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}

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
			
//			serialize(structures.phraseToIndex, toIndexPath, WikiGraphFactory::writeString, WikiGraphFactory::writeInt);
//			serialize(structures.links, linksPath, WikiGraphFactory::writeInt, WikiGraphFactory::writeObject);
		} catch (IOException | InterruptedException e) {
			
			e.printStackTrace();
			System.exit(0);
		}
	}
	
//	private static void writeInt(int i, ObjectOutputStream oos){
//		try {
//			oos.writeInt(i);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	private static void writeString(String s, ObjectOutputStream oos){
//		try {
//			oos.writeObject(s);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	private static void writeObject(Object o, ObjectOutputStream oos){
//		try {
//			oos.writeObject(o);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

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
		int progress = 0;
		while(linksLines.hasNext()){
			progress++;
			String linksLine = linksLines.next();
			printer.printProgress(progress, 100000, 10);
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
	
	

//	public static <K,V> void serialize(ConcurrentHashMap<K, V> map, String filepath, BiConsumer<K, ObjectOutputStream> serializeKey, BiConsumer<V, ObjectOutputStream> serializeValue) throws FileNotFoundException, IOException{
//		printer.println("Saving map to file ...");
//		
//		try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)))){
//			oos.writeInt(map.size());
//			int i = 0; 
//			for(Entry<K, V> e : map.entrySet()){
//				serializeKey.accept(e.getKey(), oos);
//				serializeValue.accept(e.getValue(), oos);
//				i++;
//				printer.printProgress(i, 100000, 10);
//			}
//			printer.println("Save successful. Wrote " + i + " entries.");
//		}
//	}
	
	private static class Structures{
		
		TObjectIntHashMap<String> phraseToIndex;
		TIntObjectHashMap<TIntArrayList> links;
		
		public Structures(TObjectIntHashMap<String> phraseToIndex, TIntObjectHashMap<TIntArrayList> links) {
			this.phraseToIndex = phraseToIndex;
			this.links = links;
		}
		
	}
}
