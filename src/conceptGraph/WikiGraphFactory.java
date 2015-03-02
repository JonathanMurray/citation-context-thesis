package conceptGraph;

import gnu.trove.list.array.TIntArrayList;

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
	
	private static Printer printer = new Printer(false);
	
	public static WikiGraph loadConceptGraph(String linksPath, String indicesPath){
		try{
			HashMap<Integer, TIntArrayList> links = WikiGraphFactory.deserializeLinks(linksPath);
			HashMap<String, Integer> indices = WikiGraphFactory.deserializeIndices(indicesPath);
			return new WikiGraph(links, indices);
		}catch(IOException | ClassNotFoundException e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public static  HashMap<Integer, TIntArrayList> deserializeLinks(String filepath) throws FileNotFoundException, IOException, ClassNotFoundException{
		printer.println("Loading map from file ...");
		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)))){
			int size = ois.readInt();
			HashMap<Integer, TIntArrayList> map = new HashMap<Integer, TIntArrayList>();
			for(int i = 0; i < size; i++){
				Integer key = ois.readInt();
				TIntArrayList value = (TIntArrayList) ois.readObject();
				map.put(key, value);
				printer.printProgress(i, 500000, 20);
			}
			printer.println("Load successful. Read " + map.size() + " entries.");
			return map;
		}
	}
	
	public static HashMap<String, Integer> deserializeIndices(String filepath){
		printer.println("Loading map<string,int> from file ...");
		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)))){
			int size = ois.readInt();
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			for(int i = 0; i < size; i++){
				String key = (String)ois.readObject();
				int value = ois.readInt();
				map.put(key, value);
				printer.printProgress(i, 500000, 20);
			}
			printer.println("Load successful. Read " + map.size() + " entries.");
			return map;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void buildLinksAndSaveToFile(String toPhrasePath, String toIndexPath, String linksPath){
		String dir = "/home/jonathan/Documents/exjobb/data/wikipedia/";
		try {
			Structures structures = createDataFromFiles(dir + "links-simple-sorted.txt", dir + "titles-sorted.txt");
			serialize(structures.phraseToIndex, toIndexPath, WikiGraphFactory::writeString, WikiGraphFactory::writeInt);
			serialize(structures.links, linksPath, WikiGraphFactory::writeInt, WikiGraphFactory::writeObject);
		} catch (IOException | InterruptedException e) {
			
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private static void writeInt(int i, ObjectOutputStream oos){
		try {
			oos.writeInt(i);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeString(String s, ObjectOutputStream oos){
		try {
			oos.writeObject(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void writeObject(Object o, ObjectOutputStream oos){
		try {
			oos.writeObject(o);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Structures createDataFromFiles(String linksFilePath, String titlesFilePath) throws IOException, InterruptedException{
		return createDataFromFiles(new File(linksFilePath), new File(titlesFilePath));
	}
	
	public static Structures createDataFromFiles(File linksFile, File titlesFile) throws IOException, InterruptedException{
		printer.println("Creating data from files: " + linksFile.getPath() + ",  " + titlesFile.getPath());
		try(BufferedReader linksReader = new BufferedReader(new FileReader(linksFile))){
			try (BufferedReader titlesReader = new BufferedReader(new FileReader(titlesFile))) {
				return createDataFromReaders(linksReader, titlesReader);
			}
		}
	}
	
	public static Structures createDataFromReaders(BufferedReader linksReader, BufferedReader titlesReader) throws IOException{
		ConcurrentHashMap<String,Integer> phraseToIndex = new ConcurrentHashMap<String,Integer>();
		ConcurrentHashMap<Integer, TIntArrayList> links = new ConcurrentHashMap<Integer, TIntArrayList>(); 
		
		printer.println("Adding titles...");
		{
			Iterator<String> titleLines = titlesReader.lines().iterator();
			int i = 1;
			while(titleLines.hasNext()){
				String phrase = titleLines.next().replace('_', ' ').toLowerCase();
				phraseToIndex.put(phrase, i);
			}
		}
		
		printer.println("Adding links...");
		
		//NOTE!
		//lines().parallel is extremely slow when running for a long time, due to garbage collection.
		//Crashes with "gc limit exceeded" after being almost stuck for several minutes at around 4'800'000
		linksReader.lines().forEach(linksLine -> {
			printer.printProgress(links.size(), 100000, 10);
			int pos = linksLine.indexOf(' ') + 1;
			int citer = Integer.parseInt(linksLine.substring(0, pos-2));
			TIntArrayList cited = new TIntArrayList();
	        int end;
	        while ((end = linksLine.indexOf(' ', pos)) >= 0) {
	            cited.add(Integer.parseInt(linksLine.substring(pos, end)));
	            pos = end + 1;
	        }
	        links.put(citer, cited);
		});
		return new Structures(phraseToIndex, links);
	}
	
	

	public static <K,V> void serialize(ConcurrentHashMap<K, V> map, String filepath, BiConsumer<K, ObjectOutputStream> serializeKey, BiConsumer<V, ObjectOutputStream> serializeValue) throws FileNotFoundException, IOException{
		printer.println("Saving map to file ...");
		
		try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)))){
			oos.writeInt(map.size());
			int i = 0; 
			for(Entry<K, V> e : map.entrySet()){
				serializeKey.accept(e.getKey(), oos);
				serializeValue.accept(e.getValue(), oos);
				i++;
				printer.printProgress(i, 100000, 10);
			}
			printer.println("Save successful. Wrote " + i + " entries.");
		}
	}
	
	private static class Structures{
		public Structures(ConcurrentHashMap<String, Integer> phraseToIndex,
				ConcurrentHashMap<Integer, TIntArrayList> links) {
			super();
			this.phraseToIndex = phraseToIndex;
			this.links = links;
		}
		ConcurrentHashMap<String, Integer> phraseToIndex;
		ConcurrentHashMap<Integer, TIntArrayList> links;
	}
}
