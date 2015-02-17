package wiki_neo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class NewDataCreator {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		NewDataCreator dc = new NewDataCreator("/opt/neo4j-community-2.1.6/data/graph.db");
		String dir = "/home/jonathan/Documents/exjobb/data/wikipedia/";
		dc.createDataFromFiles(dir + "links-simple-sorted.txt", dir + "titles-sorted.txt");
		
	}
	
	private ConcurrentHashMap<Integer, Node> nodes = new ConcurrentHashMap<Integer, Node>();
	private GraphDatabaseService graph;
	private String dbPath;
	
	private class CreateNode implements Runnable{

		private String title;
		private int index;
		
		public CreateNode(String title, int index){
			this.title = title;
			this.index = index;
		}
		
		@Override
		public void run() {
			System.out.println(".");
			Node node = graph.createNode(Labels.Concept);
			node.setProperty("title", title);
			System.out.print("-");
			System.out.flush();
			nodes.put(index, node);
		}
	}
	
	private class CreateRelations implements Runnable{
		
		private String linksLine;
		
		public CreateRelations(String linksLine){
			this.linksLine = linksLine;
		}
		
		public void run(){
			Scanner scanner = new Scanner(linksLine);
			String citerId = scanner.next();
			citerId = citerId.substring(0, citerId.length() - 1);
			Node citer = nodes.get(citerId);
			try(Transaction tx = graph.beginTx()){
				while (scanner.hasNext()) {
					int id = scanner.nextInt();
					citer.createRelationshipTo(nodes.get(id), Relations.CITES);
				}
				tx.success();
			}
		}
	}

	
	private void setupDb(String dbPath){
		
		
		BatchInserter inserter = BatchInserters.inserter(dbPath);
		
		
		System.out.print("Creating graph... ");
		Map<String, String> config = new HashMap<>();
		config.put( "neostore.nodestore.db.mapped_memory", "100M" );
		config.put( "neostore.relationstore.db.mapped_memory", "4G" );
		config.put( "neostore.propertystore.db.mapped_memory", "500M");
		config.put( "neostore.propertystore.db.strings.mapped_memory", "300M");
//		graph = BatchInserters.batchDatabase(dbPath, config);
		graph = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
		
		
		System.out.print("Creating index... ");
		IndexDefinition indexDefinition;
		try ( Transaction tx = graph.beginTx() ){
		    indexDefinition = graph.schema().indexFor(Labels.Concept).on("title").create();
		    tx.success();
		}
		
		try (Transaction tx = graph.beginTx()){
			graph.schema().awaitIndexOnline( indexDefinition, 60, TimeUnit.SECONDS );
			tx.success();
		}
		
		System.out.println("done.");
	}
	
	 	
	public NewDataCreator(String dbPath){
		this.dbPath = dbPath;
		clearData();
		setupDb(dbPath);
		registerShutdownHook(graph);
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
	
	public void createDataFromFiles(String linksFilePath, String titlesFilePath) throws IOException, InterruptedException{
		createDataFromFiles(new File(linksFilePath), new File(titlesFilePath));
	}
	
	public void createDataFromFiles(File linksFile, File titlesFile) throws IOException, InterruptedException{
		System.out.println("Creating data from files: " + linksFile.getPath() + ",  " + titlesFile.getPath());
		try(BufferedReader linksReader = new BufferedReader(new FileReader(linksFile))){
			try (BufferedReader titlesReader = new BufferedReader(new FileReader(titlesFile))) {
				createDataFromReaders(linksReader, titlesReader);
			}
		}
	}
	
	public void createDataFromReaders(BufferedReader linksReader, BufferedReader titlesReader) throws IOException, InterruptedException{
		String linksLine;
		String titlesLine;
		
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		int lineIndex = 0;
		Transaction tx = graph.beginTx();
		while((titlesLine = titlesReader.readLine()) != null){
			if(lineIndex % 200000 == 0){
				System.out.println(lineIndex);
			}
			if(lineIndex % 5000 == 0){
				System.out.print("[");
				exec.awaitTermination(60, TimeUnit.SECONDS);
				System.out.print("X] ");
				tx.success();
				tx.close();
				tx = graph.beginTx();
			}
			exec.submit(new CreateNode(titlesLine, lineIndex));
			lineIndex++;
		}
		exec.awaitTermination(60, TimeUnit.SECONDS);
		tx.success();
		tx.close();
		
		
		
		exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		int numRelations = 0;
		System.out.println("Adding links");
		tx = graph.beginTx();
		while ((linksLine = linksReader.readLine()) != null) {
			exec.submit(new CreateRelations(linksLine));
			if(numRelations % 500 == 0){
				System.out.print("." );
				exec.awaitTermination(60, TimeUnit.SECONDS);
				System.out.print("X] ");
				tx.success();
				tx.close();
				tx = graph.beginTx();
			}
			if(numRelations % 1000000 == 0){
				System.out.println(numRelations);
			}
			numRelations ++;
		}
		
		System.out.print("Awaiting termination of CreateRelations... ");
		exec.awaitTermination(10, TimeUnit.SECONDS);
		tx.success();
		tx.close();
		System.out.println("done.");
	}
	private static void registerShutdownHook(final GraphDatabaseService graph) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.print("Shutting down... ");
				graph.shutdown();
				System.out.println("done.");
			}
		});
	}

}
