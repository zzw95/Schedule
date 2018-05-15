package schedule.v1;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class Importer {
	DatabaseUD db;
	GraphDatabaseService graphDb;
	KnowledgeBase kb;
	String excelFile;

	public Importer(String excelFile, DatabaseUD db, KnowledgeBase kb){
		this.db=db;
		this.graphDb=db.graphDb;
		this.kb=kb;
		this.excelFile=excelFile;
	}
	
	public void Run() throws Exception{
		ArrayList<ArrayList<String>> sheetTitles = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<ArrayList<String>>> sheetData = new ArrayList<ArrayList<ArrayList<String>>>();
		utils.ReadTable(excelFile, sheetTitles, sheetData);
		for(int i=0;i<sheetTitles.size();i++){
			ArrayList<String> titles = sheetTitles.get(i);
			ArrayList<ArrayList<String>> data = sheetData.get(i);
		
			System.out.println(titles);
			System.out.println(data);
			
			try (Transaction tx = graphDb.beginTx()) {			
				ParseTable(titles, data);
				tx.success();  
			}
		}
	}
	
	private OWLObjectProperty ParseTableClass(String entity, ArrayList<String> col, OWLClass primLabel, OWLDataProperty primLabelKeyProp, ArrayList<Node> primNodes, 
			ArrayList<OWLClass> prevLabels, OWLObjectProperty prevRelate, ArrayList<ArrayList<Node>> prevNodess, ArrayList<Relationship> prevRels) throws Exception{
		
		ArrayList<Node> prevNodes = new ArrayList<Node>(primNodes.size());
		OWLClass label = kb.factory.getOWLClass(":"+entity,kb.pm);
		OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
		OWLObjectProperty op = kb.GetRelateBetweenClasses(primLabel, label);
		prevRels.clear();
		if(op!=null){
			for(int j=0;j<col.size();j++){
				if(col.get(j)!=null){
					Node node;
					if(labelKeyProp.getIRI().getShortForm().equals("type")){
						//not unique
						node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
								primNodes.get(j), op.getIRI().getShortForm(), Direction.OUTGOING);
					}else{
						//unique, inverse functional
						node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
					}
					
					
					Relationship relate = db.GetRelationBetweenNodes(primNodes.get(j), node, op.getIRI().getShortForm());
					prevNodes.add(node);
					prevRels.add(relate);
				}else{
					prevNodes.add(null);
					prevRels.add(null);
				}
				
			}
			prevLabels.add(label);
			prevNodess.add(prevNodes);
			//prevRelate = op;
			return op;
		}
		op = kb.GetRelateBetweenClasses(label, primLabel);
		if(op!=null){
			for(int j=0;j<col.size();j++){
				if(col.get(j)!=null){
					Node node;
					if(labelKeyProp.getIRI().getShortForm().equals("type")){
						node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
								primNodes.get(j), op.getIRI().getShortForm(), Direction.INCOMING);
					}else{
						node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
					}			
					
					Relationship relate = db.GetRelationBetweenNodes(node, primNodes.get(j), op.getIRI().getShortForm());
					prevNodes.add(node);
					prevRels.add(relate);
				}else{
					prevNodes.add(null);
					prevRels.add(null);
				}
			}
			prevLabels.add(label);
			prevNodess.add(prevNodes);
			//prevRelate = op;
			return op;
		}
		for(int k=0; k<prevLabels.size(); k++){
			OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
			if(label.equals(prevLabel)){
				for(int j=0; j<col.size();j++){
					if(col.get(j)!=null){
						Node preNode = prevNodess.get(prevLabels.size()-1-k).get(j);
						preNode.setProperty(labelKeyProp.getIRI().getShortForm(), col.get(j));
					}
				}
				return prevRelate;
			}else{
				op = kb.GetRelateBetweenClasses(prevLabel, label);
				if(op!=null){
					for(int j=0;j<col.size();j++){
						if(col.get(j)!=null){
							Node node;
							if(labelKeyProp.getIRI().getShortForm().equals("type")){
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
										prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm(), Direction.OUTGOING);
							}else{
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
							}
							Relationship relate = db.GetRelationBetweenNodes(prevNodess.get(prevLabels.size()-1-k).get(j), node, op.getIRI().getShortForm());
							prevNodes.add(node);
							prevRels.add(relate);
						}else{
							prevNodes.add(null);
							prevRels.add(null);
						}
					}
					prevLabels.add(label);
					prevNodess.add(prevNodes);
					//prevRelate = op;
					return op;
				}
				op = kb.GetRelateBetweenClasses(label, prevLabel);
				if(op!=null){
					for(int j=0;j<col.size();j++){
						if(col.get(j)!=null){
							Node node;
							if(labelKeyProp.getIRI().getShortForm().equals("type")){
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
										prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm(), Direction.INCOMING);
							}else{
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
							}
							Relationship relate = db.GetRelationBetweenNodes(node, prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm());
							prevNodes.add(node);
							prevRels.add(relate);
						}else{
							prevNodes.add(null);
							prevRels.add(null);
						}
					}
					prevLabels.add(label);
					prevNodess.add(prevNodes);
					//prevRelate = op;
					return op;
				}
				throw new Exception(String.format("Cannot parse the table title %s.", entity));
			}
			
		}
		throw new Exception(String.format("Cannot parse the table title %s.", entity));
	}
	
	private OWLObjectProperty ParseTableObjectProperty(String entity, ArrayList<String> col, OWLClass primLabel, OWLDataProperty primLabelKeyProp, ArrayList<Node> primNodes, 
			ArrayList<OWLClass> prevLabels, OWLObjectProperty prevRelate, ArrayList<ArrayList<Node>> prevNodess, ArrayList<Relationship> prevRels) throws Exception{
		
		ArrayList<Node> prevNodes = new ArrayList<Node>(primNodes.size());
		OWLObjectProperty relate = kb.factory.getOWLObjectProperty(":"+entity,kb.pm);
		Set<OWLClass> domains = kb.GetObjectPropertyDomain(relate);
		Set<OWLClass> ranges = kb.GetObjectPropertyRange(relate);
		prevRels.clear();
		if(domains.contains(primLabel)){
			ranges = kb.GetObjectPropertyRangeOri(relate);
			if(ranges.size()==1){
				OWLClass label = ranges.iterator().next();
				OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
				for(int j=0;j<col.size();j++){
					if(col.get(j)!=null){
						Node node;
						if(labelKeyProp.getIRI().getShortForm().equals("type")){
							node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
									primNodes.get(j), entity, Direction.OUTGOING);
						}else{
							node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
						}
						Relationship rel = db.GetRelationBetweenNodes(primNodes.get(j), node, entity);
						prevNodes.add(node);
						prevRels.add(rel);
					}else{
						prevNodes.add(null);
						prevRels.add(null);
					}
				}
				prevLabels.add(label);
				prevNodess.add(prevNodes);
				//prevRelate = relate;
				return relate;
			}
			
			
		}else if(ranges.contains(primLabel)){
			domains = kb.GetObjectPropertyDomainOri(relate);
			if(domains.size()==1){
				OWLClass label = domains.iterator().next();
				OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
				for(int j=0; j<col.size();j++){
					if(col.get(j)!=null){
						Node node;
						if(labelKeyProp.getIRI().getShortForm().equals("type")){
							node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
								primNodes.get(j), entity, Direction.INCOMING);
						}else{
							node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
						}
						Relationship rel = db.GetRelationBetweenNodes(node, primNodes.get(j), entity);
						prevNodes.add(node);
						prevRels.add(rel);
					}else{
						prevNodes.add(null);
						prevRels.add(null);
					}
					
				}
				prevLabels.add(label);
				prevNodess.add(prevNodes);
				//prevRelate = relate;
				return relate;
			}
			
			
		}
		for(int k=0; k<prevLabels.size();k++){
			OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
			if(domains.contains(prevLabel)){
				ranges = kb.GetObjectPropertyRangeOri(relate);
				if(ranges.size()==1){
					OWLClass label = ranges.iterator().next();
					OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
					for(int j=0;j<col.size();j++){
						if(col.get(j)!=null){
							Node node;
							if(labelKeyProp.getIRI().getShortForm().equals("type")){
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
									prevNodess.get(prevLabels.size()-1-k).get(j), entity, Direction.OUTGOING);
							}else{
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
							}
							Relationship rel = db.GetRelationBetweenNodes(prevNodess.get(prevLabels.size()-1-k).get(j), node, entity);
							prevNodes.add(node);
							prevRels.add(rel);
						}else{
							prevNodes.add(null);
							prevRels.add(null);
						}
						
					}
					prevLabels.add(label);
					prevNodess.add(prevNodes);
					//prevRelate = relate;
					return relate;
				}
				
			}else if(ranges.contains(prevLabel)){
				domains = kb.GetObjectPropertyDomainOri(relate);
				if(domains.size()==1){
					OWLClass label = domains.iterator().next();
					OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
					for(int j=0;j<col.size();j++){
						if(col.get(j)!=null){
							Node node;
							if(labelKeyProp.getIRI().getShortForm().equals("type")){
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
									prevNodess.get(prevLabels.size()-1-k).get(j), entity, Direction.INCOMING);
							}else{
								node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
							}
							Relationship rel = db.GetRelationBetweenNodes(node, prevNodess.get(prevLabels.size()-1-k).get(j), entity);
							prevNodes.add(node);
							prevRels.add(rel);
						}else{
							prevNodes.add(null);
							prevRels.add(null);
						}
						
					}
					prevLabels.add(label);
					prevNodess.add(prevNodes);
					//prevRelate = relate;
					return relate;
				}
				
			}
		}
		throw new Exception(String.format("Cannot parse the table title %s.", entity));
	}

	public void ParseTable(ArrayList<String> titles, ArrayList<ArrayList<String>> data) throws Exception{
		
		// Get the primary label and the key property
		if(!kb.onto.containsClassInSignature(kb.pm.getIRI(":"+titles.get(0)))){
			throw new Exception("First table title should be a class!");
		}
		OWLClass primLabel = kb.factory.getOWLClass(":"+titles.get(0), kb.pm);
		//Set<OWLClass> primLabelSupers = kb.GetClassSuper(primLabel);
		OWLDataProperty primLabelKeyProp = kb.GetLabelKeyProp(primLabel);
		
		ArrayList<Node> primNodes = new ArrayList<Node> ();
		ArrayList<String> firstCol = data.get(0);
		for(String cell:firstCol){
			Node node = db.GetNodeByProp(primLabel.getIRI().getShortForm(), primLabelKeyProp.getIRI().getShortForm(), cell, kb.GetLabelSuper(primLabel));
			primNodes.add(node);
		}
		
		
		ArrayList<OWLClass> prevLabels = new ArrayList<OWLClass>();
		OWLObjectProperty prevRelate = null;
		ArrayList<ArrayList<Node>> prevNodess = new ArrayList<ArrayList<Node>>();
		ArrayList<Relationship> prevRels = new ArrayList<Relationship>(primNodes.size());

		for(int i=1;i<titles.size();i++){
			String entity = titles.get(i);
			ArrayList<String> col = data.get(i);
			
			if (kb.onto.containsClassInSignature(kb.pm.getIRI(":"+entity))){
				prevRelate = ParseTableClass(entity, col, primLabel, primLabelKeyProp, primNodes, 
						prevLabels, prevRelate, prevNodess, prevRels);
				
			}else if (kb.onto.containsObjectPropertyInSignature(kb.pm.getIRI(":"+entity))){
				prevRelate = ParseTableObjectProperty(entity, col, primLabel, primLabelKeyProp, primNodes, 
						prevLabels, prevRelate, prevNodess, prevRels);
				
			}else if (kb.onto.containsDataPropertyInSignature(kb.pm.getIRI(":"+entity))){
				
				ArrayList<Node> prevNodes = new ArrayList<Node>(primNodes.size());
				
				OWLDataProperty prop = kb.factory.getOWLDataProperty(":"+entity,kb.pm);
				Set<OWLClass> domains = kb.GetDataPropertyDomain(prop);
				String range = kb.GetDataPropertyRange(prop);
				if(domains.contains(primLabel)){
					for(int j=0;j<col.size();j++){
						if(col.get(j)!=null){
							if(range.equals("int")){
								Double v = Double.parseDouble(col.get(j));
								primNodes.get(j).setProperty(entity, v.intValue());
							}else if(range.equals("double")){
								primNodes.get(j).setProperty(entity, Double.parseDouble(col.get(j)));
							}else{
								primNodes.get(j).setProperty(entity, col.get(j));
							}
						}												
					}
					continue;
				}
				if(prevRelate!=null){
					Set<OWLDataProperty> relateProps = kb.GetRelateProps(prevRelate);
					if(relateProps!=null && relateProps.contains(prop)){
						for(int j=0;j<col.size();j++){
							if(col.get(j)!=null){
								if(range.equals("int")){
									Double v = Double.parseDouble(col.get(j));
									prevRels.get(j).setProperty(entity, v.intValue());
								}else if(range.equals("double")){
									prevRels.get(j).setProperty(entity, Double.parseDouble(col.get(j)));
								}else{
									prevRels.get(j).setProperty(entity, col.get(j));
								}
							}														
						}
						continue;
					}
				}
				
				boolean done=false;
				for(int k=0; k<prevLabels.size();k++){
					OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
					if(domains.contains(prevLabel)){
						for(int j=0;j<col.size();j++){
							if(col.get(j)!=null){
								if(range.equals("int")){
									Double v = Double.parseDouble(col.get(j));
									prevNodess.get(prevLabels.size()-1-k).get(j).setProperty(entity, v.intValue());
								}else if(range.equals("double")){
									prevNodess.get(prevLabels.size()-1-k).get(j).setProperty(entity, Double.parseDouble(col.get(j)));
								}else{
									prevNodess.get(prevLabels.size()-1-k).get(j).setProperty(entity, col.get(j));
								}
							}							
						}
						done=true;
						break;
					}
					
				}
				if(!done){
					domains = kb.GetDataPropertyDomainOri(prop);
					if(domains.size()==1){
						OWLClass label = domains.iterator().next();
						OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
						if(labelKeyProp.equals(prop)){
							ParseTableClass(label.getIRI().getShortForm(), col, primLabel, primLabelKeyProp, primNodes, 
									prevLabels, prevRelate, prevNodess, prevRels);
							continue;  
						}
					}
					
					i++;
					if(i==titles.size()){
						throw new Exception(String.format("Error parse the table title %s.", entity));   
					}
					String nextEntity = titles.get(i);
					ArrayList<String> nextCol = data.get(i);
					if (kb.onto.containsClassInSignature(kb.pm.getIRI(":"+nextEntity))){
						OWLClass label = kb.factory.getOWLClass(":"+nextEntity,kb.pm);
						if(!domains.contains(label)){
							throw new Exception(String.format("Error parse the table title %s.", entity));
						}
						OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
						OWLObjectProperty op = kb.GetRelateBetweenClasses(primLabel, label);
						if(op!=null){
							prevRels.clear();
							for(int j=0;j<col.size();j++){
								if(col.get(j)!=null){
									Node node;
									if(labelKeyProp.getIRI().getShortForm().equals("type")){
										node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
												primNodes.get(j), op.getIRI().getShortForm(), Direction.OUTGOING);
									}else{
										node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetLabelSuper(label));
									}
									Relationship relate = db.GetRelationBetweenNodes(primNodes.get(j), node, op.getIRI().getShortForm());
									prevNodes.add(node);
									prevRels.add(relate);
									if(range.equals("int")){
										Double v = Double.parseDouble(col.get(j));
										node.setProperty(entity, v.intValue());
									}else if(range.equals("double")){
										node.setProperty(entity, Double.parseDouble(col.get(j)));
									}else{
										node.setProperty(entity, col.get(j));
									}
								}else{
									prevNodes.add(null);
									prevRels.add(null);
								}
								
							}
							prevLabels.add(label);
							prevNodess.add(prevNodes);
							prevRelate = op;
							continue;
						}
						op = kb.GetRelateBetweenClasses(label, primLabel);
						if(op!=null){
							prevRels.clear();
							for(int j=0;j<col.size();j++){
								if(col.get(j)!=null){
									Node node;
									if(labelKeyProp.getIRI().getShortForm().equals("type")){
										node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
												primNodes.get(j), op.getIRI().getShortForm(), Direction.INCOMING);
									}else{
										node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetLabelSuper(label));
									}
									Relationship relate = db.GetRelationBetweenNodes(node, primNodes.get(j), op.getIRI().getShortForm());
									prevNodes.add(node);
									prevRels.add(relate);
									if(range.equals("int")){
										Double v = Double.parseDouble(col.get(j));
										node.setProperty(entity, v.intValue());
									}else if(range.equals("double")){
										node.setProperty(entity, Double.parseDouble(col.get(j)));
									}else{
										node.setProperty(entity, col.get(j));
									}
								}else{
									prevNodes.add(null);
									prevRels.add(null);
								}
								
							}
							prevLabels.add(label);
							prevNodess.add(prevNodes);
							prevRelate = op;
							continue;
						}
						boolean done1=false;
						for(int k=0; k<prevLabels.size(); k++){
							OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
							op = kb.GetRelateBetweenClasses(prevLabel, label);
							if(op!=null){
								prevRels.clear();
								for(int j=0;j<col.size();j++){
									if(col.get(j)!=null){
										Node node;
										if(labelKeyProp.getIRI().getShortForm().equals("type")){
											node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
												prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm(), Direction.OUTGOING);
										}else{
											node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), nextCol.get(j), kb.GetLabelSuper(label));
										}
										Relationship relate = db.GetRelationBetweenNodes(prevNodess.get(prevLabels.size()-1-k).get(j), node, op.getIRI().getShortForm());
										prevNodes.add(node);
										prevRels.add(relate);
										if(range.equals("int")){
											Double v = Double.parseDouble(col.get(j));
											node.setProperty(entity, v.intValue());
										}else if(range.equals("double")){
											node.setProperty(entity, Double.parseDouble(col.get(j)));
										}else{
											node.setProperty(entity, col.get(j));
										}
									}else{
										prevNodes.add(null);
										prevRels.add(null);
									}
									
								}
								prevLabels.add(label);
								prevNodess.add(prevNodes);
								prevRelate = op;
								break;
							}
							op = kb.GetRelateBetweenClasses(label, prevLabel);
							if(op!=null){
								prevRels.clear();
								for(int j=0;j<col.size();j++){
									if(col.get(j)!=null){
										Node node;
										if(labelKeyProp.getIRI().getShortForm().equals("type")){
											node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetDataPropertyRange(labelKeyProp), kb.GetLabelSuper(label), 
												prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm(), Direction.INCOMING);
										}else{
											node = db.GetNodeByProp(label.getIRI().getShortForm(), labelKeyProp.getIRI().getShortForm(), col.get(j), kb.GetLabelSuper(label));
										}
										Relationship relate = db.GetRelationBetweenNodes(node, prevNodess.get(prevLabels.size()-1-k).get(j), op.getIRI().getShortForm());
										prevNodes.add(node);
										prevRels.add(relate);
										if(range.equals("int")){
											Double v = Double.parseDouble(col.get(j));
											node.setProperty(entity, v.intValue());
										}else if(range.equals("double")){
											node.setProperty(entity, Double.parseDouble(col.get(j)));
										}else{
											node.setProperty(entity, col.get(j));
										}
									}else{
										prevNodes.add(null);
										prevRels.add(null);
									}
									
								}
								prevLabels.add(label);
								prevNodess.add(prevNodes);
								prevRelate = op;
								break;
							}
						}
						if(!done1){
							throw new Exception(String.format("Cannot parse the table title %s.", nextEntity));
						}else{
							continue;
						}				
					}
				}
				
				
			}else{
				throw new Exception(String.format("Wrong table title %s.", entity));
			}
		}
		
		
	}

}
