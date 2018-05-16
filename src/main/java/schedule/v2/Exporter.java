package schedule.v2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class Exporter {
	DatabaseUD db;
	GraphDatabaseService graphDb;
	KnowledgeBase kb;
	String excelFile;

	public Exporter(String excelFile, DatabaseUD db, KnowledgeBase kb){
		this.db=db;
		this.graphDb=db.graphDb;
		this.kb=kb;
		this.excelFile = excelFile;
	}
	
	public void Run() throws Exception{
		FileInputStream fileInputStream = new FileInputStream(excelFile);
        XSSFWorkbook workbook= new XSSFWorkbook(fileInputStream);
        fileInputStream.close();
        for(int sheetNum=0;sheetNum<workbook.getNumberOfSheets();sheetNum++){
	        ArrayList<String> titles = new ArrayList<String>();
	        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
	        XSSFSheet sheet=workbook.getSheetAt(sheetNum);	      
	        XSSFRow titleRow=sheet.getRow(0);
	        for(Cell cell:titleRow){
	        	titles.add(cell.getStringCellValue());
	        }
	        System.out.println(titles);
	        String cql = ParseTable(titles);
	        System.out.println(cql);
	        try(Transaction tx = graphDb.beginTx()){
	        	Result result = graphDb.execute(cql);
	        	int rowNum=0;
	 	        while(result.hasNext()){
	 	        	Map<String, Object> rowMap = result.next();
	 	        	rowNum++;
	 	        	XSSFRow row = sheet.createRow(rowNum);
	 	        	int cellNum=0;
	 	        	//System.out.println(result.columns());
	 		        for ( String key : result.columns()){
	 		            Object value = rowMap.get(key);
	 		            XSSFCell cell = row.createCell(cellNum);
	 		            if(value==null){
	 		            	
	 		            }else if(value instanceof Integer){
	 		            	cell.setCellValue((int)value);
	 		            }else if(value instanceof Double){
	 		            	cell.setCellValue((Double)value);
	 		            }else{
	 		            	cell.setCellValue(value.toString());
	 		            }
	 		            cellNum++;
	 		        }
	 	        }
	        }
	       
	        
	     }

		
		FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
		workbook.write(fileOutputStream);
		fileOutputStream.flush();
        fileOutputStream.close();
        workbook.close();
	}
	
	
	public String ParseTable(ArrayList<String> titles) throws Exception{
		String matchCQL="";
		String returnCQL="";
		int vix=0;
		
		// Get the primary label and the key property
		if(!kb.onto.containsClassInSignature(kb.pm.getIRI(":"+titles.get(0)))){
			throw new Exception(String.format("First table title %s should be a class!", titles.get(0)));
		}
		OWLClass primLabel = kb.factory.getOWLClass(":"+titles.get(0), kb.pm);
		String primLabelVarName = primLabel.getIRI().getShortForm().toLowerCase()+vix;
		OWLDataProperty primLabelKeyProp = kb.GetLabelKeyProp(primLabel);
		
		matchCQL +=String.format("match(%s:%s) ", primLabelVarName, primLabel.getIRI().getShortForm()); 
		returnCQL += String.format("return %s.%s ", primLabelVarName, primLabelKeyProp.getIRI().getShortForm());
		//returnCQL += String.format("return %s.%s as %s", primLabelVarName, primLabelKeyProp.getIRI().getShortForm(), titles.get(0));
		
		ArrayList<OWLClass> prevLabels = new ArrayList<OWLClass>();
		ArrayList<String> prevLabelsVarNames = new ArrayList<String>();
		OWLObjectProperty prevRelate = null;
		String prevRelateVarName="";
		
		for(int i=1; i<titles.size(); i++){
			vix++;
			String entity = titles.get(i);		
			if (kb.onto.containsClassInSignature(kb.pm.getIRI(":"+entity))){
				OWLClass label = kb.factory.getOWLClass(":"+entity,kb.pm);
				String labelVarName = label.getIRI().getShortForm().toLowerCase()+vix;
				prevLabels.add(label);
				prevLabelsVarNames.add(labelVarName);
				OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
				returnCQL +=", "+ labelVarName+"."+labelKeyProp.getIRI().getShortForm();
				//returnCQL +=", "+ labelVarName+"."+labelKeyProp.getIRI().getShortForm()+" as "+entity;
				OWLObjectProperty op = kb.GetRelateBetweenClasses(primLabel, label);
				if(op!=null){
					String relateVarName = op.getIRI().getShortForm().toLowerCase() +vix;
					if(i==1){
						matchCQL += String.format("-[%s:%s]-> (%s:%s) ", relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
					}else{
						matchCQL += String.format("\noptional match (%s)-[%s:%s]-> (%s:%s) ", 
								primLabelVarName, relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
					}
					prevRelate = op;
					prevRelateVarName = relateVarName;	
					continue;
				}
				op = kb.GetRelateBetweenClasses(label, primLabel);
				if(op!=null){
					String relateVarName = op.getIRI().getShortForm().toLowerCase() +vix;
					if(i==1){
						matchCQL += String.format("<-[%s:%s]- (%s:%s) ", relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
					}else{
						matchCQL += String.format("\noptional match (%s:%s)-[%s:%s]-> (%s) ", 
								labelVarName, label.getIRI().getShortForm(), relateVarName, op.getIRI().getShortForm(), primLabelVarName);
					}
					prevRelate = op;
					prevRelateVarName = relateVarName;	
					continue;
				}
				boolean done =false;
				for(int k=0; k<prevLabels.size(); k++){
					OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
					op = kb.GetRelateBetweenClasses(prevLabel, label);
					if(op!=null){
						String relateVarName = op.getIRI().getShortForm().toLowerCase() +vix;
						if(k==0){
							matchCQL += String.format("-[%s:%s]-> (%s:%s) ", relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}else{
							matchCQL += String.format("\noptional match (%s)-[%s:%s]-> (%s:%s) ", 
									prevLabelsVarNames.get(prevLabels.size()-1-k), relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}
						prevRelate = op;
						prevRelateVarName = relateVarName;
						done=true;
						break;
					}

					op = kb.GetRelateBetweenClasses(label, prevLabel);
					if(op!=null){
						String relateVarName = op.getIRI().getShortForm().toLowerCase() +vix;
						if(k==0){
							matchCQL += String.format("<-[%s:%s]- (%s:%s) ", relateVarName, op.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}else{
							matchCQL += String.format("\noptional match (%s:%s)-[%s:%s]-> (%s) ", 
									labelVarName, label.getIRI().getShortForm(), relateVarName, op.getIRI().getShortForm(), prevLabelsVarNames.get(prevLabels.size()-1-k));
						}
						prevRelate = op;
						prevRelateVarName = relateVarName;	
						done=true;
						break;
					}	
				}
				if(done){
					continue;
				}else{
					throw new Exception(String.format("Cannot parse the table title %s.", entity));
				}
				
			}else if (kb.onto.containsObjectPropertyInSignature(kb.pm.getIRI(":"+entity))){
				OWLObjectProperty relate = kb.factory.getOWLObjectProperty(":"+entity,kb.pm);
				String relateVarName = relate.getIRI().getShortForm().toLowerCase()+vix;
				prevRelate = relate;
				prevRelateVarName = relateVarName;	
				Set<OWLClass> domains = kb.GetObjectPropertyDomain(relate);
				Set<OWLClass> ranges = kb.GetObjectPropertyRange(relate);
				
				if(domains.contains(primLabel)){
					ranges = kb.GetObjectPropertyRangeOri(relate);
					if(ranges.size()==1){
						OWLClass label = ranges.iterator().next();
						OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
						String labelVarName = label.getIRI().getShortForm().toLowerCase()+vix;
						returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm();
						//returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm()+" as "+entity;
						if(i==1){
							matchCQL += String.format("-[%s:%s]-> (%s:%s) ", relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}else{
							matchCQL += String.format("\noptional match (%s)-[%s:%s]-> (%s:%s) ", 
									primLabelVarName, relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}
						prevLabels.add(label);
						prevLabelsVarNames.add(labelVarName);
						continue;
					}
				}else if(ranges.contains(primLabel)){
					domains = kb.GetObjectPropertyDomainOri(relate);
					if(domains.size()==1){
						OWLClass label = domains.iterator().next();
						OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
						String labelVarName = label.getIRI().getShortForm().toLowerCase()+vix;
						returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm();
						//returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm()+" as "+entity;
						if(i==1){
							matchCQL += String.format("<-[%s:%s]- (%s:%s) ", relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
						}else{
							matchCQL += String.format("\noptional match (%s:%s)-[%s:%s]-> (%s) ", 
									labelVarName, label.getIRI().getShortForm(), relateVarName, relate.getIRI().getShortForm(),primLabelVarName);
						}
						prevLabels.add(label);
						prevLabelsVarNames.add(labelVarName);
						continue;
					}
				}
				boolean done=false;
				for(int k=0; k<prevLabels.size();k++){
					OWLClass prevLabel = prevLabels.get(prevLabels.size()-1-k);
					if(domains.contains(prevLabel)){
						ranges = kb.GetObjectPropertyRangeOri(relate);
						if(ranges.size()==1){
							OWLClass label = ranges.iterator().next();
							OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
							String labelVarName = label.getIRI().getShortForm().toLowerCase()+vix;
							returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm();
							//returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm()+" as "+entity;
							if(k==0){
								matchCQL += String.format("-[%s:%s]-> (%s:%s) ", relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
							}else{
								matchCQL += String.format("\noptional match (%s)-[%s:%s]-> (%s:%s) ", 
										prevLabelsVarNames.get(prevLabels.size()-1-k), relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
							}
							prevLabels.add(label);
							prevLabelsVarNames.add(labelVarName);
							done=true;
							break;
						}
						
					}else if(ranges.contains(prevLabel)){
						domains = kb.GetObjectPropertyDomainOri(relate);
						if(domains.size()==1){
							OWLClass label = domains.iterator().next();
							OWLDataProperty labelKeyProp = kb.GetLabelKeyProp(label);
							String labelVarName = label.getIRI().getShortForm().toLowerCase()+vix;
							returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm();
							//returnCQL +=", "+labelVarName+"."+labelKeyProp.getIRI().getShortForm()+" as "+entity;
							if(k==0){
								matchCQL += String.format("<-[%s:%s]- (%s:%s) ", relateVarName, relate.getIRI().getShortForm(), labelVarName, label.getIRI().getShortForm());
							}else{
								matchCQL += String.format("\noptional match (%s:%s)-[%s:%s]-> (%s) ", 
										labelVarName, label.getIRI().getShortForm(), relateVarName, relate.getIRI().getShortForm(),prevLabelsVarNames.get(prevLabels.size()-1-k));
							}
							prevLabels.add(label);
							prevLabelsVarNames.add(labelVarName);
							done=true;
							break;
						}
					}
				}
				if(done){
					continue;
				}else{
					throw new Exception(String.format("Cannot parse the table title %s.", entity));
				}
				
			}else if (kb.onto.containsDataPropertyInSignature(kb.pm.getIRI(":"+entity))){
				OWLDataProperty prop = kb.factory.getOWLDataProperty(":"+entity,kb.pm);
				Set<OWLClass> domains = kb.GetDataPropertyDomain(prop);
				String range = kb.GetDataPropertyRange(prop);
				if(domains.contains(primLabel)){
					returnCQL += ", "+primLabelVarName+"."+prop.getIRI().getShortForm();
					//returnCQL += ", "+primLabelVarName+"."+prop.getIRI().getShortForm()+" as "+entity;
					continue;
				}
				if(prevRelate!=null){
					Set<OWLDataProperty> relateProps = kb.GetRelateProps(prevRelate);
					if(relateProps!=null && relateProps.contains(prop)){
						returnCQL += ", "+prevRelateVarName + "."+prop.getIRI().getShortForm();
						//returnCQL += ", "+prevRelateVarName + "."+prop.getIRI().getShortForm()+" as "+entity;
						continue;
					}
				}
				if(!prevLabels.isEmpty()){
					OWLClass prevLabel = prevLabels.get(prevLabels.size()-1);
					if(domains.contains(prevLabel)){
						returnCQL +=", "+prevLabelsVarNames.get(prevLabels.size()-1)+ "."+prop.getIRI().getShortForm();
						//returnCQL +=", "+prevLabelsVarNames.get(prevLabels.size()-1)+ "."+prop.getIRI().getShortForm()+" as "+entity;
						continue;
					}
				}
				throw new Exception(String.format("Cannot parse the table title %s.", entity));
			}
		}		
		return matchCQL +"\n"+returnCQL;		
	}

}
