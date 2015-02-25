package conceptGraph;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import citationContextData.Citer;
import citationContextData.ContextDataSet;
import citationContextData.ContextHTML_Parser;

/**
 * Builds up a title-graph from the wiki-dumps at http://haselgrove.id.au/wikipedia.htm
 * @author jonathan
 *
 */
public class GraphCreator {
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
//		buildLinksAndSaveToFile("conceptGraphLower.ser");
		
		

	}
	
	

	public static void buildLinksAndSaveToFile(String fileName){
		String dir = "/home/jonathan/Documents/exjobb/data/wikipedia/";
		try {
			ConcurrentHashMap<String, List<String>> links = createDataFromFiles(dir + "links-simple-sorted.txt", dir + "titles-sorted.txt");
			saveLinksToFile(links, fileName);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private static void testSerialization(){
		try{
			GraphCreator gc = new GraphCreator();
			ConcurrentHashMap<String, List<String>> links = new ConcurrentHashMap<String, List<String>>();
			links.put("hej", Arrays.asList(new String[]{"hopp", "san", "sa"}));
			links.put("va", Arrays.asList(new String[]{"sa", "du"}));
			links.put("tom", Arrays.asList(new String[0]));
			links.put("a", Arrays.asList(new String[]{"b"}));
			
			saveLinksToFile(links, "test.ser");
			links = null;
			links = loadLinksFromFile("test.ser");
			
			links.entrySet().forEach(
				System.out::println
			);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static ConcurrentHashMap<String, List<String>> createDataFromFiles(String linksFilePath, String titlesFilePath) throws IOException, InterruptedException{
		return createDataFromFiles(new File(linksFilePath), new File(titlesFilePath));
	}
	
	public static ConcurrentHashMap<String, List<String>> createDataFromFiles(File linksFile, File titlesFile) throws IOException, InterruptedException{
		System.out.println("Creating data from files: " + linksFile.getPath() + ",  " + titlesFile.getPath());
		try(BufferedReader linksReader = new BufferedReader(new FileReader(linksFile))){
			try (BufferedReader titlesReader = new BufferedReader(new FileReader(titlesFile))) {
				return createDataFromReaders(linksReader, titlesReader);
			}
		}
	}
	
	public static ConcurrentHashMap<String, List<String>> createDataFromReaders(BufferedReader linksReader, BufferedReader titlesReader) throws IOException{
		
		List<String> titles = new ArrayList<String>();
		ConcurrentHashMap<String, List<String>> links = new ConcurrentHashMap<String, List<String>>();
		
		titlesReader.lines().forEach(titlesLine -> {
			titles.add(titlesLine.replace('_', ' ').toLowerCase());
		});
		
		
		System.out.println("Adding links");
		linksReader.lines().parallel().forEach(linksLine -> {
			String[] ids = linksLine.split("[ :]");
			String citer = titles.get(Integer.parseInt(ids[0])-1);
			List<String> cited = Arrays.stream(ids)
					.skip(1)
					.filter(s -> s.length() > 0)
					.map(s -> titles.get(Integer.parseInt(s)-1))
					.collect(Collectors.toCollection(ArrayList::new));
			if(links.size() % 100000 < 4){
				System.out.println(links.size());
			}
//			if(cited == null){
//				System.out.println(links.size() + ": NULL instead of empty list!");
//				cited = new ArrayList<String>();
//			}
			links.put(citer, cited);
			
		});
		return links;
	}
	
	public static void saveLinksToFile(ConcurrentHashMap<String, List<String>> links, String filepath) throws IOException{
		System.out.println("Saving links to file ...");
		System.out.println("keys: " + links.keySet().size());
		System.out.println("values: " + links.values().size());
		System.out.println("size: " + links.size());
		System.out.println("(There are " + links.entrySet().size() + " entries.)");
		
		try(ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)))){
			oos.writeInt(links.size());
			int i = 0; 
			for(Entry<String, List<String>> e : links.entrySet()){
				if(e.getValue() == null){
					throw new IllegalArgumentException();
				}
				oos.writeObject(e.getKey());
				oos.writeObject(e.getValue());
				i++;
				if(i % 100000 == 0){
					System.out.println(i);
				}
			}
			
			System.out.println("Wrote " + i + " entries.");
			System.out.println("now size: " + links.entrySet().size() + ", " + links.size());
		}
	}
	
	public static ConcurrentHashMap<String,List<String>> loadLinksFromFile(String filepath){
		System.out.println("Loading links from file ...");
		try(ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)))){
			int size = ois.readInt();
			System.out.println(size + " entries");
			ConcurrentHashMap<String,List<String>> links = new ConcurrentHashMap<String, List<String>>();
			for(int i = 0; i < size; i++){
				if(i % 100000 == 0){
					System.out.println(i);
				}
				String key = (String) ois.readObject();
				@SuppressWarnings("unchecked")
				List<String> val = (List<String>) ois.readObject();
				links.put(key, val);
			}
			System.out.println("Finished reading. links.size() == " + links.size());
			return links;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
}
