package schedule.v1;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.emory.mathcs.backport.java.util.Collections;

public class Bike {
	GraphDatabaseService graphDb;
	DatabaseUD db;
	public Bike(DatabaseUD db){
		this.db=db;
		this.graphDb = db.graphDb;
	}
	
	public enum Labels implements Label {
		Product,
        Part,
        SalesPart,
        RawPart,
        ProcessedPart,
        Operation,
        ResourceGroup,
        Resource,
        SalesOrder,
        Order,
        RootOrder,
        LeafOrder,
        Purchase,
        Job,
        Clock
	}
	
	public enum RelationshipTypes implements RelationshipType {
	    ProducePart,
	    SupplyPart,
	    NeedPart,
	    RelyPart,
	    RelyOrder,
	    OpNext,
	    JobNext,
	    HaveRes,
	    HaveOrder,
	    HaveJob,
	    CorresOp,
	    AdoptRes,
	    AdoptOp,
	    CorresPart,
	    LoadJob
	}
	
	public void UpdateJobTimeBackward(Node job, Calendar calendar){
		//System.out.println("    Job "+job.getProperty("type")+":"+utils.FormatCalendar(calendar));
		job.setProperty("latestEndTime", utils.FormatCalendar(calendar));
		
		if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.INCOMING)>0){
			//update the calendar
			Calendar newCalendar = Calendar.getInstance();
			newCalendar.setTime(calendar.getTime());
			Double costMin = (Double)job.getSingleRelationship(Bike.RelationshipTypes.AdoptRes, Direction.OUTGOING).getProperty("costMin");	
			newCalendar.add(Calendar.MINUTE, -costMin.intValue());
			//Jobs Backward Traversal (reverse JobNext)
			ArrayList<Node> preJobs = db.GetRelateNodes(job, Bike.RelationshipTypes.JobNext, Direction.INCOMING);
			for(Node preJob:preJobs){
				UpdateJobTimeBackward(preJob, newCalendar);		
			}
		}						
	}
	
	public void UpdateOrderTimeBackward(Node order, Calendar calendar){
		//System.out.println("======Order "+order.getProperty("orderId")+":"+utils.FormatCalendar(calendar));
		order.setProperty("latestEndTime", utils.FormatCalendar(calendar));
		
		ArrayList<Node> jobs = db.GetRelateNodes(order, Bike.RelationshipTypes.HaveJob, Direction.OUTGOING);
		// Find the first and last job in this order, jobs are straightly sequential
		Node firstJob=null,lastJob=null;	
		if(jobs.size()==1){
			firstJob=lastJob=jobs.get(0);
		}else{
			for(Node job :jobs){
				if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.OUTGOING)==0){
					lastJob = job;
				}else if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.INCOMING)==0){
					firstJob = job;
				}
			}
		}
		UpdateJobTimeBackward(lastJob, calendar);
		

		if(order.getDegree(Bike.RelationshipTypes.RelyOrder, Direction.OUTGOING)>0){
			//update the calendar
			Calendar newCalendar = utils.ParseCalenderYMDHM   (firstJob.getProperty("latestEndTime").toString());
			Double costMin = (Double)firstJob.getSingleRelationship(Bike.RelationshipTypes.AdoptRes, Direction.OUTGOING).getProperty("costMin");
			//System.out.println(costMin);
			newCalendar.add(Calendar.MINUTE, -costMin.intValue());
			
			//Orders Backward Traversal (RelyPart)
			ArrayList<Node> preOrders = db.GetRelateNodes(order, Bike.RelationshipTypes.RelyOrder, Direction.OUTGOING);
			for(Node preOrder:preOrders){
				UpdateOrderTimeBackward(preOrder, newCalendar);
			}
		}
	}
	
	public void UpdateJobTimeForward(Node job, Calendar calendar){
		//System.out.println("    Job "+job.getProperty("type")+":"+utils.FormatCalendar(calendar));
		// earliestStartTime should choose later value
		String time = utils.FormatCalendar(calendar);
		if(job.hasProperty("earliestStartTime") && time.compareTo((String)job.getProperty("earliestStartTime"))<0){
			// Do not update earliestStartTime of job, return
			return;
		}
		job.setProperty("earliestStartTime", time);

		
		if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.OUTGOING)>0){
			//update the calendar
			Calendar newCalendar = Calendar.getInstance();
			newCalendar.setTime(calendar.getTime());
			Double costMin = (Double)job.getSingleRelationship(Bike.RelationshipTypes.AdoptRes, Direction.OUTGOING).getProperty("costMin");	
			newCalendar.add(Calendar.MINUTE, costMin.intValue());
			
			//Jobs Forward Traversal (JobNext)
			ArrayList<Node> postJobs = db.GetRelateNodes(job, Bike.RelationshipTypes.JobNext, Direction.OUTGOING);
			for(Node postJob:postJobs){
				UpdateJobTimeForward(postJob, newCalendar);		
			}
		}						
	}
	
	public void UpdateOrderTimeForward(Node order, Calendar calendar){
		//System.out.println("======Order "+order.getProperty("orderId")+":"+utils.FormatCalendar(calendar));
		// earliestStartTime should choose later value
		String time = utils.FormatCalendar(calendar);
		if(order.hasProperty("earliestStartTime") && time.compareTo((String)order.getProperty("earliestStartTime"))<0){
			// Do not update earliestStartTime of order, return
			return;
		}
		order.setProperty("earliestStartTime", time);

		ArrayList<Node> jobs = db.GetRelateNodes(order, Bike.RelationshipTypes.HaveJob, Direction.OUTGOING);
		// Find the first and last job in this order, jobs are straightly sequential
		Node firstJob=null,lastJob=null;	
		if(jobs.size()==1){
			firstJob=lastJob=jobs.get(0);
		}else{
			for(Node job :jobs){
				if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.OUTGOING)==0){
					lastJob = job;
				}else if(job.getDegree(Bike.RelationshipTypes.JobNext, Direction.INCOMING)==0){
					firstJob = job;
				}
			}
		}
		UpdateJobTimeForward(firstJob, calendar);
		

		if(order.getDegree(Bike.RelationshipTypes.RelyOrder, Direction.INCOMING)>0){
			//update the calendar
			Calendar newCalendar = utils.ParseCalenderYMDHM(lastJob.getProperty("earliestStartTime").toString());
			Double costMin = (Double)lastJob.getSingleRelationship(Bike.RelationshipTypes.AdoptRes, Direction.OUTGOING).getProperty("costMin");
			//System.out.println(costMin);
			newCalendar.add(Calendar.MINUTE, costMin.intValue());
			
			//Orders Forward Traversal (reverse RelyPart)
			ArrayList<Node> postOrders = db.GetRelateNodes(order, Bike.RelationshipTypes.RelyOrder, Direction.INCOMING);
			for(Node postOrder:postOrders){
				UpdateOrderTimeForward(postOrder, newCalendar);
			}
		}
	}
	
	
	public void DecomposeOrder(){
		// Create root orders
		String cql1 = "match(so:SalesOrder)-[pp:ProducePart]->(p:Part) "
				+ "merge (so)-[:HaveOrder]->(o:Order:RootOrder)-[:ProducePart{partNum:pp.partNum}]->(p)"
				+ " on create set o.orderId= so.salesOrderId +\"-0-\"+id(o) on create set o.status=\"Rest\"";
		this.db.RunCQL(cql1);
		
		// Decompose orders
		String cql2 = "match(so:SalesOrder)-[pp:ProducePart]->(p:Part) "
				+ "match path=(p)-[:RelyPart*1..]->(p1:ProcessedPart)"
				+ "merge (so)-[:HaveOrder]->(o:Order)-[:ProducePart]->(p1) on create set o.orderId=so.salesOrderId+\"-\"+length(path)+\"-\"+id(o) "
				+ "on create set o.status=\"Rest\"";
		this.db.RunCQL(cql2);
		
		// Generate the precedence of orders and the quantity of products (Iteration)
		String cql3 = "match(o1:Order)-[pp1:ProducePart]->(p1:Part) where not exists(pp1.partNum) "
				+ "match (o1)<-[:HaveOrder]-(so:SalesOrder)-[:HaveOrder]-(o2)-[pp2:ProducePart]->(p2)-[rp:RelyPart]->(p1) "
				+ "where exists(pp2.partNum) "
				+ "set pp1.partNum=pp2.partNum*rp.partNum "
				+ "merge (o2)-[:RelyOrder]->(o1) ";
		boolean update;
		do{
			update=this.db.RunCQL(cql3);
		}while(update);
		
		// Create leaf orders
		String cql4 = "match (o:Order) where not o:LeafOrder and not (o)-[:RelyOrder]->() set o:LeafOrder set o.status=\"Loading\"";
		this.db.RunCQL(cql4);
		
	}
	
	public void GenerateJobs(){
		// Generate operation jobs
		String cql1 = "match (o:Order)-[pp:ProducePart]->(p:Part)-[:AdoptOp]->(op:Operation)-[ar:AdoptRes]->(res) "
				+ "merge (o)-[:HaveJob]->(job:Job{type:op.type})-[:CorresOp]->(op) on create set job.status=\"Rest\""
				+ "merge (job)-[:AdoptRes{costMin:ar.costMin*pp.partNum}]->(res)";
		this.db.RunCQL(cql1);
		
		
		// Generate the precedence of jobs
		String cql2 = "match (job1:Job)-[:CorresOp]->(op1)-[:OpNext]->(op2)<-[:CorresOp]-(job2:Job) "
				+ "match (job1)<-[:HaveJob]-(o)-[:HaveJob]->(job2) "
				+ "merge (job1)-[:JobNext]->(job2)";
		this.db.RunCQL(cql2);
		
		// Change the loading status
		String cql3 = "match (job:Job) where job.status=\"Rest\" and not ()-[:JobNext]->(job) set job.status=\"Loading\"";
		this.db.RunCQL(cql3);
	}
	
	public void UpdateJobStatus(Node job){
		job.setProperty("status", "Loaded");
		String timestamp = (String)job.getProperty("endTime");
		Calendar calendar = utils.ParseCalenderYMDHM(timestamp);
		ArrayList<Node> nextJobs = this.db.GetRelateNodes(job,Bike.RelationshipTypes.JobNext, Direction.OUTGOING);
		if(nextJobs.isEmpty()){
			// if all the jobs of this order are loaded, the order is finished.
			Node order = this.db.GetRelateNodes(job, Bike.RelationshipTypes.HaveJob, Direction.INCOMING).get(0);
		    ArrayList<Node> orderJobs = this.db.GetRelateNodes(order, Bike.RelationshipTypes.HaveJob, Direction.OUTGOING);
		    boolean orderDone = true;
		    for(Node orderJob:orderJobs){
		    	String status = (String)orderJob.getProperty("status");
		    	if(!status.contentEquals("Loaded")){
		    		orderDone = false;
		    		break;
		    	}
		    }
		    if(orderDone){
		    	order.setProperty("status", "Loaded");
		    	System.out.println(String.format("Order %s has Finished! %s\n", order.getProperty("orderId"), order.getProperty("status")));
		    	ArrayList<Node> nextOrders = this.db.GetRelateNodes(order, Bike.RelationshipTypes.RelyOrder, Direction.INCOMING);
		    	if(nextOrders.isEmpty()){
		    		
		    	}else{
		    		for(Node nextOrder:nextOrders){
		    			// change the status 
		    			String oldStatus = (String)nextOrder.getProperty("status");
		    			ArrayList<Node> preOrders = this.db.GetRelateNodes(nextOrder, Bike.RelationshipTypes.RelyOrder, Direction.OUTGOING);
		    			boolean allLoaded = true;
		    			for(Node preOrder:preOrders){
		    				String preOrderStatus = (String)preOrder.getProperty("status");
		    				if(!preOrderStatus.contentEquals("Loaded")){
		    					allLoaded = false;
		    					break;
		    				}
		    			}
		    			if(allLoaded){
		    				if(oldStatus.contentEquals("Loaded")){
		    					System.out.println(String.format("Update Order %s Status Error!", nextOrder.getProperty("orderId")));
		    					for(Node preOrder:preOrders){
		    						System.out.println(String.format("--Error Order %s, Status %s", preOrder.getProperty("orderId"), preOrder.getProperty("status")));
		    					}
		    				}
		    				nextOrder.setProperty("status", "Loading");
			    			System.out.println(String.format("Order %s, status %s -> %s", nextOrder.getProperty("orderId"), oldStatus, nextOrder.getProperty("status") ));
		    			}else{
		    				System.out.println(String.format("Order %s, status %s cannot be changed.", nextOrder.getProperty("orderId"), oldStatus));
		    				for(Node preOrder:preOrders){
	    						System.out.println(String.format("--Pre Order %s, Status %s", preOrder.getProperty("orderId"), preOrder.getProperty("status")));
	    					}
		    			}
		    			
		    			// update the earlierstStartTime
		    			UpdateOrderTimeForward(nextOrder, calendar);
		    		}
		    	}
		    }
		}else{
			for(Node nextJob:nextJobs){
				// change the status    
				nextJob.setProperty("status", "Loading");
				// update the earlierstStartTime
				UpdateJobTimeForward(nextJob, calendar);
			}
		}
	}
	
	public void PreProcess(){
		// Set Part sub labels ProcessedPart, RawPart
		String cql1 = "match (p:Part) where (p)-[:AdoptOp]->() set p:ProcessedPart";
		this.db.RunCQL(cql1);
		String cql2 = "match (p:Part) where not (p)-[:AdoptOp]->() set p:RawPart";
		this.db.RunCQL(cql2);
		
		// Generate the Relation RelyPart
		String cql3 = "match (p1:Part)-[:AdoptOp]->(op:Operation)-[np:NeedPart]->(p2:Part) merge (p1)-[:RelyPart{partNum:np.partNum}]->(p2)";
		this.db.RunCQL(cql3);
		
		// Generate the precedence of Operations according to opNo ( Relation OpNext ) 
		String cql4 = "match (p:Part)-[:AdoptOp]-> (op1:Operation)  "
				+ "match (p)-[:AdoptOp]->(op2:Operation) where op2.opNo-op1.opNo=10 create (op1)-[:OpNext]->(op2)";
		this.db.RunCQL(cql4);
	}
	
	public void InitializeTimeConstraints(){
		
		ResourceIterator<Node> rootOrderNodes = graphDb.findNodes(Bike.Labels.RootOrder);
		while(rootOrderNodes.hasNext()){
			Node rootOrder = (Node)rootOrderNodes.next();		
			Node salesOrder = rootOrder.getSingleRelationship(Bike.RelationshipTypes.HaveOrder, Direction.INCOMING).getOtherNode(rootOrder);
			Calendar calendar = utils.ParseCalenderYMD(salesOrder.getProperty("dueDate").toString());
			
			System.out.println(salesOrder.getProperty("salesOrderId")+":"+utils.FormatCalendar(calendar));
			UpdateOrderTimeBackward(rootOrder, calendar); 
		}
		
		ResourceIterator<Node> leafOrderNodes = graphDb.findNodes(Bike.Labels.LeafOrder);
		while(leafOrderNodes.hasNext()){
			Node leafOrder = (Node)leafOrderNodes.next();
			Node salesOrder = leafOrder.getSingleRelationship(Bike.RelationshipTypes.HaveOrder, Direction.INCOMING).getOtherNode(leafOrder);
			Calendar calendar = utils.ParseCalenderYMD(salesOrder.getProperty("arrivalDate").toString());
			
			System.out.println(salesOrder.getProperty("salesOrderId")+":"+utils.FormatCalendar(calendar));
			UpdateOrderTimeForward(leafOrder, calendar);
			System.out.println();  
		}

	}
	
	public void SetClock(){
		Node clockNode = graphDb.createNode(Bike.Labels.Clock);
		clockNode.setProperty("timestamp", "2000-01-01 00:00");
		db.RunCQL("match (r:Resource) set r.timestamp=\"2000-01-01 00:00\"");
	}
	
	public void Schedule(){
		
		Node sysClockNode = graphDb.findNodes(Bike.Labels.Clock).next();
		String sysClock = (String)sysClockNode.getProperty("timestamp");
		
		ArrayList<String> resClocks=new ArrayList<String>();
		ArrayList<Node> resNodes = new ArrayList<Node>();
		ResourceIterator<Node> ress = graphDb.findNodes(Bike.Labels.Resource);
		while(ress.hasNext()){
			Node resNode = ress.next();
			resNodes.add(resNode);
			String clock = (String)resNode.getProperty("timestamp");
			if(!resClocks.contains(clock)){
				resClocks.add(clock);
			}
		}
		
		while(!resClocks.isEmpty()){
			
			Collections.sort(resClocks);
			System.out.println(resClocks);
			sysClock = resClocks.get(0);
			System.out.println(String.format("System Clock %s\n", sysClock));
			resClocks.remove(0);
			sysClockNode.setProperty("timestamp", sysClock);
			
			boolean loaded=false;
			for(Node loadRes:resNodes){
				String  clock= (String)loadRes.getProperty("timestamp");
				if(clock.compareTo(sysClock)<0){
					loadRes.setProperty("timestamp", sysClock);
					clock = sysClock;
					System.out.println(String.format("Res %s, clock %s -> %s", loadRes.getProperty("name"), clock, sysClock));
				}else if (clock.compareTo(sysClock)>0){
					break;
				}
				// else clock.compareTo(sysClock)==0
				
				//Load the job to the resource
				String resName = (String)loadRes.getProperty("name");
				String jobQuery = String.format("match (res:Resource)<-[:HaveRes]-(rg) where res.name=\"%s\" "
						+ "match (o:Order) where o.status=\"Loading\" "
						+ "match (o)-[:HaveJob]->(job)-[:AdoptRes]->(rg) where job.status=\"Loading\" and job.earliestStartTime<=res.timestamp "
						+ "return job order by job.latestEndTime, job.earliestStartTime", resName);
				ArrayList<Node> jobNodes = db.GetCQLNodes(jobQuery);
				
				System.out.println(String.format("------%s\n", resName));
				if(jobNodes.isEmpty()){
					continue;
				}
				System.out.println(graphDb.execute(jobQuery).resultAsString());
				
				
				Node loadJob = jobNodes.get(0);
				loadJob.setProperty("startTime", clock);
				Double cost = (Double)loadJob.getSingleRelationship(Bike.RelationshipTypes.AdoptRes, Direction.OUTGOING).getProperty("costMin");
				Calendar startCalendar = utils.ParseCalenderYMDHM(clock);
				Calendar endCalendar = Calendar.getInstance();
				endCalendar.setTime(startCalendar.getTime());
				endCalendar.add(Calendar.MINUTE, cost.intValue());
				
				loadJob.setProperty("endTime", utils.FormatCalendar(endCalendar));
				resClocks.add(utils.FormatCalendar(endCalendar));
				
				loadRes.createRelationshipTo(loadJob, Bike.RelationshipTypes.LoadJob);
				loadRes.setProperty("timestamp", utils.FormatCalendar(endCalendar));
				
				//update status and time constraints
				UpdateJobStatus(loadJob); 	
				loaded = true;
				
				Node loadOrder = loadJob.getSingleRelationship(Bike.RelationshipTypes.HaveJob, Direction.INCOMING).getOtherNode(loadJob);
				System.out.println(String.format("Res %s:start %s, end %s, job %s, order %s\n", resName, clock, utils.FormatCalendar(endCalendar), loadJob.getProperty("type"), loadOrder.getProperty("orderId")));

			}//end for
			
		}
		
		System.out.println(graphDb.execute("match(ro:RootOrder) where ro.status=\"Loaded\" return ro.orderId").resultAsString());
	}
	
	public void Output(String excelFileName) throws IOException{
		FileOutputStream fileOutputStream = new FileOutputStream(excelFileName);
        XSSFWorkbook workbook= new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Schedule");
        XSSFCellStyle style = workbook.createCellStyle();
        // 设置格式
        XSSFFont font = workbook.createFont();  
        font.setFontName("等线");
        font.setFontHeight(11);
        style.setFont(font);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		XSSFRow titleRow = sheet.createRow(0);
		Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("Job");
		titleCell.setCellStyle(style);
		titleCell = titleRow.createCell(1);
		titleCell.setCellValue("Resource");
		titleCell.setCellStyle(style);
		titleCell = titleRow.createCell(2);
		titleCell.setCellValue("startTime");
		titleCell.setCellStyle(style);
		titleCell = titleRow.createCell(3);
		titleCell.setCellValue("endTime");
		titleCell.setCellStyle(style);
		int rowNum=1;
		
		String cql = "match (o:Order)-[:HaveJob]->(job:Job)<-[:LoadJob]-(res:Resource) return o.orderId as order, job.type as job, res.name as res, job.startTime as st, job.endTime as et order by job.startTime, job.endTime";
		try(Result result = graphDb.execute(cql)){
			//System.out.println(result.resultAsString());
			while (result.hasNext()){	
				Map<String, Object> row = result.next();
		        for ( String key : result.columns() ){
		            System.out.printf( "%s = %s%n", key, row.get( key ) );
		        }
		        XSSFRow xrow = sheet.createRow(rowNum++);
		        
		        Cell cell = xrow.createCell(0);
		        cell.setCellValue(String.format("Job %s, Order %s", row.get("job"), row.get("order")));
		        cell.setCellStyle(style);
		        
		        cell = xrow.createCell(1);
		        cell.setCellValue((String)row.get("res"));
		        cell.setCellStyle(style);
		        
		        cell = xrow.createCell(2);
		        cell.setCellValue((String)row.get("st"));
		        cell.setCellStyle(style);
		        
		        cell = xrow.createCell(3);
		        cell.setCellValue((String)row.get("et"));
		        cell.setCellStyle(style);
		        
			}
		}
		
		workbook.write(fileOutputStream);
	}

}
