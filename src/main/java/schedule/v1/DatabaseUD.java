package schedule.v1;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryStatistics;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.semanticweb.owlapi.model.OWLClass;

public class DatabaseUD {
	public GraphDatabaseService graphDb;
	
	public DatabaseUD(String path){
		this.graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(path));
		System.out.println("Database Load!");
	}
	
	public void Shutdown()

    {
      Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		});
      System.out.println("Database Shutdown!");
    }
	
	public ArrayList<Node> GetRelateNodes(Node startNode, RelationshipType relation, Direction dir){
		ArrayList<Node> nodes = new ArrayList<Node>();
// 		try(Transaction tx= graphDb.beginTx()){	
 			if (startNode.hasRelationship(relation, dir)){
 				Iterable<Relationship> rss = startNode.getRelationships(relation, dir);
 				for(Relationship rs:rss){
 					Node node = rs.getOtherNode(startNode);
 					nodes.add(node);
 				}
 			}
// 			tx.success();
// 		}
 		return nodes;
	}
	public boolean RunCQL(String query){
		boolean update =false;
//		try(Transaction tx = this.graphDb.beginTx()){		
			 try ( Result result = graphDb.execute( query) )
			 {
				 QueryStatistics qs=result.getQueryStatistics();
				 if(qs.containsUpdates()){
					 update=true;
				 }
			 }
//			 tx.success();
//		}
		return update;
	}
	
	public ArrayList<Node> GetCQLNodes(String query){
		ArrayList<Node> nodes = new ArrayList<Node>();
//		try(Transaction tx = this.graphDb.beginTx()){		
			 try ( Result result = graphDb.execute( query) )
			 {
				 String key = result.columns().get(0);
			     while ( result.hasNext() )
			     {			    	 
			         Node node = (Node)result.next().get(key);
			         nodes.add(node);
			     }
			 }
//			 tx.success();
//		}
		return nodes; 
	}
	
	public Node GetNodeByProp(String labelName, String key, String value, Set<String> superLabels){
		Node node = graphDb.findNode(Label.label(labelName), key, value);
		if(node==null){
			for(String superLabel:superLabels){
				node = graphDb.findNode(Label.label(superLabel), key, value);
				if(node!=null){
					break;
				}
			}
			if(node==null){
				node = graphDb.createNode(Label.label(labelName));
				node.setProperty(key, value);
				for(String superLabel:superLabels){
					node.addLabel(Label.label(superLabel));
				}
			}else{
				node.addLabel(Label.label(labelName));
			}
			
		}
		return node;
	}
	
	public Node GetNodeByProp(String labelName, String key, String value, String range, Set<String> superLabels, Node relateNode, String relateName, Direction dir){
		//Node node = graphDb.findNodes(Label.label(labelName), key, value);
		ArrayList<Node> nodes = GetRelateNodes(relateNode, RelationshipType.withName(relateName), dir);
		if(!nodes.isEmpty()){
			for(Node node:nodes){
				if(node.hasProperty(key)){
					if(range.equals("int")){
						if( ((Long)node.getProperty(key) - Integer.parseInt(value) )==0){
							return node;
						}
					}
					else if(range.equals("double")){
						if( ((Long)node.getProperty(key) - Double.parseDouble(value) )==0){
							return node;
						}
					}
					else{
						if(((String)node.getProperty(key)).equals(value)){
							return node;
						}
					}
				}
			}
		}
			
		Node node = graphDb.createNode(Label.label(labelName));
		node.setProperty(key, value);
		for(String superLabel:superLabels){
			node.addLabel(Label.label(superLabel));
		}
		return node;
	}
	
	public Relationship GetRelationBetweenNodes(Node node1, Node node2, String relateName){
		 Relationship relate = null;
		 if(!node1.hasRelationship(RelationshipType.withName(relateName), Direction.OUTGOING)){
			 relate = node1.createRelationshipTo(node2, RelationshipType.withName(relateName));
		 }else{
			 Iterable<Relationship> rss = node1.getRelationships(RelationshipType.withName(relateName), Direction.OUTGOING);
			for(Relationship rs:rss){
				if(rs.getOtherNode(node1).equals(node2)){
					relate = rs;
					break;
				}		
			}
			if(relate==null){
				relate = node1.createRelationshipTo(node2, RelationshipType.withName(relateName));
			}
		 }
		 return relate;
	}

}
