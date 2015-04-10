package old.wiki_neo4j;

import java.io.IOException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class DataReader {

	public static void main(String[] args) throws IOException {
		DataReader dr = new DataReader("/opt/neo4j-community-2.1.6/data/graph.db");
	}
	
	public DataReader(String dbPath){
		setupDb(dbPath);
	}
	
	private GraphDatabaseService graph;
	
	private void setupDb(String dbPath){
		graph = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
		try(Transaction tx = graph.beginTx()){
			ResourceIterable<Node> nodes = graph.findNodesByLabelAndProperty(Labels.Concept, "title", "!!!");
			Node n = nodes.iterator().next();
			System.out.println(n.getProperty("title"));
			for(Relationship in : n.getRelationships(Direction.INCOMING)){
				String title = (String) in.getStartNode().getProperty("title");
				System.out.println(title);
			}
			tx.success();
		}
	}
	
}
