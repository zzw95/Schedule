package neo4jtest;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
public class APITest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File("Data/Test"));
		System.out.println("Database Load!");
		try (Transaction tx = graphDb.beginTx()) {
			// Perform DB operations
			Node steve = graphDb.createNode(Labels.USER);
			steve.setProperty("name", "Steve");
			Node linda = graphDb.createNode(Labels.USER);
			linda.setProperty("name", "Linda");
			
			steve.createRelationshipTo( linda, RelationshipTypes.IS_FRIEND_OF );
			
			System.out.println("created node name is" + steve.getProperty("name"));

			tx.success();
		}
		
		String query ="match (n:USER) return n.name as name";
		Map<String, Object >parameters = new HashMap<String, Object>();
	
		 try ( Result result = graphDb.execute( query, parameters ) )
		 {
		     while ( result.hasNext() )
		     {
		         Map<String, Object> row = result.next();
		         for ( String key : result.columns() )
		         {
		             System.out.printf( "%s = %s%n", key, row.get( key ) );
		         }
		     }
		 }
		 
		 registerShutdownHook(graphDb);
		 System.out.println("Database Shutdown!");

	}
	
	public enum Labels implements Label {
        USER,
        MOVIE;
	}
	
	public enum RelationshipTypes implements RelationshipType {
	    IS_FRIEND_OF,
	    HAS_SEEN;
	}


	private static void registerShutdownHook(final GraphDatabaseService graphDb){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		});
	}

}
