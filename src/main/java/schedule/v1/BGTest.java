package schedule.v1;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class BGTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		PrintStream out = System.out;
//		PrintStream log = new PrintStream("sys.log");
//		System.setOut(log);
//		
		DatabaseUD db = new DatabaseUD("G:\\Scheduling\\Test");
		GraphDatabaseService graphDb = db.graphDb;
		Bike bike = new Bike(db);	
		KnowledgeBase kb = new KnowledgeBase(new File("G:\\Scheduling\\Schedule.owl"));	
		
		File excelFile=new File("G:\\Scheduling\\import.xlsx");
		if(!excelFile.exists()){
			
		}else{
			try{
				Importer importer = new Importer("G:\\Scheduling\\import.xlsx", db, kb);
				importer.Run();
			}catch (Exception e1){
			}// end catch
		}// end else
		
		
		try(Transaction tx = graphDb.beginTx()){
			
			bike.SetClock();
			
			bike.PreProcess();
			
			bike.DecomposeOrder();
			
			bike.GenerateJobs();
			
			bike.InitializeTimeConstraints();
			
//			bike.Schedule();
//			
//			bike.Output("G:\\Scheduling\\output.xlsx");
			tx.success();
		}
		
		
		
		
		Exporter exporter = new Exporter("G:\\Scheduling\\export.xlsx",db,kb);
		exporter.Run();
		
		db.Shutdown();
		graphDb.shutdown();
//		
//		log.close();
//		System.setOut(out);
//		System.out.println("Finish!");
		

	}

}
