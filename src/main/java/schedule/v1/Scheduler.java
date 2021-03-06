package schedule.v1;

import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class Scheduler {
	GraphDatabaseService graphDb;
	DatabaseUD db;
	Bike bike;
	String outputExcel;
	public Scheduler(DatabaseUD db, String outputExcel){
		this.db = db;
		this.outputExcel = outputExcel;
		graphDb = db.graphDb;
		bike=new Bike(db);
	}
	
	public void Generate() throws IOException{
		try(Transaction tx = graphDb.beginTx()){
			
			bike.SetClock();
			
			bike.PreProcess();
			
			bike.DecomposeOrder();
			
			bike.GenerateJobs();
			
			bike.InitializeTimeConstraints();
			
			bike.Schedule();
			
			//bike.Output(outputExcel);
			tx.success();
		}
	}
	

}
