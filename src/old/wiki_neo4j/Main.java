package old.wiki_neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Main {
	
	public static void main(String[] args) {
		new Main();
	}
	
	private static enum Relations implements RelationshipType{
	    KNOWS
	}
	
	GraphDatabaseService graphDb;
	
	
	Node firstNode;
	Node secondNode;
	Relationship relationship;
	
//	public Main() {
//		System.out.println("starting db...");
//		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( "data/graph.db" );
//		System.out.println("Successfully started");
//		registerShutdownHook( graphDb );
//		
//		try ( Transaction tx = graphDb.beginTx() )
//		{
//		    
//			firstNode = graphDb.createNode();
//			firstNode.setProperty( "message", "Hello, " );
//			secondNode = graphDb.createNode();
//			secondNode.setProperty( "message", "World!" );
//
//			relationship = firstNode.createRelationshipTo( secondNode, Relations.KNOWS );
//			relationship.setProperty( "message", "brave Neo4j " );
//			
//			System.out.print( firstNode.getProperty( "message" ) );
//			System.out.print( relationship.getProperty( "message" ) );
//			System.out.print( secondNode.getProperty( "message" ) );
//			
//			// let's remove the data
//			System.out.println("deleting data...");
//			firstNode.getSingleRelationship( Relations.KNOWS, Direction.OUTGOING ).delete();
//			firstNode.delete();
//			secondNode.delete();
//			System.out.println("deleted.");
//			
//			tx.success();
//			
//			
//		}
//		System.out.println("shutting down...");
//		graphDb.shutdown();
//		System.out.println("shut down.");
//		
//		
//	    
//	    System.out.println("success");
//	}
	
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	
	public void test(){
		
	}
}
