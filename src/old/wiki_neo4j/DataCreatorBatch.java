package old.wiki_neo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class DataCreatorBatch {
	
	public static void main(String[] args) throws IOException {
		DataCreatorBatch dc = new DataCreatorBatch("/opt/neo4j-community-2.1.6/data/graph.db");
		String dir = "/home/jonathan/Documents/exjobb/data/wikipedia/";
		dc.createDataFromFiles(dir + "links-simple-sorted.txt", dir + "titles-sorted.txt");
	}
	
	private BatchInserter inserter ;
	private String dbPath;
	private IndexDefinition index;
	
	public DataCreatorBatch(String dbPath){
		this.dbPath = dbPath;
		clearData();
		setupDb(dbPath);
	}
	
	public void clearData(){
		System.out.print("Clearing old data (" + dbPath + ")...");
		deleteRecursive(new File(dbPath));
		System.out.println("done.");
	}
	
	private void deleteRecursive(File f){
		if(f.isDirectory()){
			for(File child : f.listFiles()){
				deleteRecursive(child);
			}
		}
		f.delete();
	}
	
	private void setupDb(String dbPath){
		System.out.print("Creating graph... ");
		Map<String, String> config = new HashMap<>();
		config.put( "neostore.nodestore.db.mapped_memory", "100M" );
		config.put( "neostore.relationstore.db.mapped_memory", "6G" );
		config.put( "neostore.propertystore.db.mapped_memory", "500M");
		config.put( "neostore.propertystore.db.strings.mapped_memory", "300M");
		inserter = BatchInserters.inserter(dbPath, config);
		registerShutdownHook(inserter);
	}
	
	public void createDataFromFiles(String linksFilePath, String titlesFilePath) throws IOException{
		createDataFromFiles(new File(linksFilePath), new File(titlesFilePath));
	}
	
	public void createDataFromFiles(File linksFile, File titlesFile) throws IOException{
		System.out.println("Creating data from files: " + linksFile.getPath() + ",  " + titlesFile.getPath());
		try(BufferedReader linksReader = new BufferedReader(new FileReader(linksFile))){
			try (BufferedReader titlesReader = new BufferedReader(new FileReader(titlesFile))) {
				createDataFromReaders(linksReader, titlesReader);
			}
		}
	}
	
	public void createDataFromReaders(BufferedReader linksReader, BufferedReader titlesReader) throws IOException{
		Scanner scanner;
		String linksLine;
		String titlesLine;
		
		int id = 1;
		System.out.println("CREATING NODES...");
		while((titlesLine = titlesReader.readLine()) != null){
			if(id % 200000 == 0){
				System.out.println(id);
			}
			inserter.createNode(id, MapUtil.map("title", titlesLine), Labels.Concept);
			id++;
		}
		
		System.out.println("CREATING RELATIONS...");
		int lineIndex = 0;
		final int innerLoop = 50000;
		readRelations: 
			while (true) {
				System.out.println(lineIndex);
				for(int i = 0; i < innerLoop; i++){
					linksLine = linksReader.readLine();
					if(linksLine == null){
						break readRelations;
					}
					scanner = new Scanner(linksLine);
					scanner.useDelimiter("(: | )");
					int citerId = scanner.nextInt();
					while(scanner.hasNext()) {
						int citedId = scanner.nextInt();
						inserter.createRelationship(citerId, citedId, Relations.CITES, null);
						
					}
				}
				lineIndex += innerLoop;
			}
		
	}
	
	private void registerShutdownHook(final BatchInserter inserter) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.print("Shutting down... ");
				IndexCreator indexCreator = inserter.createDeferredSchemaIndex(Labels.Concept);
				indexCreator = indexCreator.on("title");
				index = indexCreator.create();
				inserter.shutdown();
				System.out.println("done.");
			}
		});
	}
}
