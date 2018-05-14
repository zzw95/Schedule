package neo4jtest;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import static org.neo4j.driver.v1.Values.parameters;

public class DriverAPITest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		StatementResult result = session.run( "MATCH (n:Resource) RETURN n.name as res LIMIT 25",
		        parameters() );
		while ( result.hasNext() )
		{
		    Record record = result.next();
		    System.out.println(record.get("res").asString());
		}

		session.close();
		driver.close();


	}


}
