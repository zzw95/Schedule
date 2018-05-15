package schedule.v1;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class BGTest {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		PrintStream out = System.out;
		PrintStream log = new PrintStream("sys.log");
		System.setOut(log);
		
		DatabaseUD db = new DatabaseUD("G:\\Scheduling\\Test");
		GraphDatabaseService graphDb = db.graphDb;
		Bike bike = new Bike(db);
		
		
		
		
		KnowledgeBase kb = new KnowledgeBase(new File("G:\\Scheduling\\Schedule.owl"));
		
		File excelFile=new File("G:\\Scheduling\\import.xlsx");
		if(!excelFile.exists()){
			
		}else{
			try{
				ArrayList<ArrayList<String>> sheetTitles = new ArrayList<ArrayList<String>>();
				ArrayList<ArrayList<ArrayList<String>>> sheetData = new ArrayList<ArrayList<ArrayList<String>>>();
				utils.ReadTable("G:\\Scheduling\\import.xlsx", sheetTitles, sheetData);
				for(int i=0;i<sheetTitles.size();i++){
					ArrayList<String> titles = sheetTitles.get(i);
					ArrayList<ArrayList<String>> data = sheetData.get(i);
				
					System.out.println(titles);
					System.out.println(data);
					Importer importer = new Importer(db, kb);
					try (Transaction tx = graphDb.beginTx()) {			
						importer.ParseTable(titles, data);
						tx.success();  
					}
				}
			}catch (Exception e1){
			}// end catch
		}// end else
		
		
		try(Transaction tx = graphDb.beginTx()){
			
			bike.SetClock();
			
			bike.PreProcess();
			
			bike.DecomposeOrder();
			
			bike.GenerateJobs();
			
			bike.InitializeTimeConstraints();
			
			bike.Schedule();
			
			bike.Output("G:\\Scheduling\\output.xlsx");
			tx.success();
		}
		
		
		
		db.Shutdown();
		graphDb.shutdown();
		
		log.close();
		System.setOut(out);
		System.out.println("Finish!");
		

	}

}
