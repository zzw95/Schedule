package schedule.v1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import nu.xom.jaxen.function.StringFunction;

public class KnowledgeBase {
	
	
	OWLOntologyManager manager; 
    OWLOntology onto; 
    //An interface for creating entities, class expressions and axioms.
    OWLDataFactory factory ; 
    //A prefix manager than can provide prefixes for prefix names.
    PrefixManager pm;
    
    OWLReasoner reasoner;

	
	public KnowledgeBase(File ontoFile) {
		// TODO Auto-generated constructor stub
		try {
    		manager = OWLManager.createOWLOntologyManager(); 
    		factory = manager.getOWLDataFactory();
			onto=manager.loadOntologyFromOntologyDocument(ontoFile);
			IRI inputIri=onto.getOntologyID().getOntologyIRI().get();
	    	pm=new DefaultPrefixManager(null,null,inputIri.toString()+"#");
	    	
	    	OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance(); 
	        this.reasoner=reasonerFactory.createNonBufferingReasoner(this.onto, new SimpleConfiguration());

	 		
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Set<String> ReadEntities(){
		Set<String> entities = new HashSet<String>();
		for(OWLClass label :onto.getClassesInSignature()){
			String labelName = label.getIRI().getShortForm();
			if(!labelName.contains("R_") && !labelName.equals("Relationship") && !labelName.equals("RelationProperty")){
				entities.add(labelName);
			}
		}
		for(OWLObjectProperty relat :onto.getObjectPropertiesInSignature()){
			entities.add(relat.getIRI().getShortForm());
		}
		for(OWLDataProperty prop :onto.getDataPropertiesInSignature()){
			entities.add(prop.getIRI().getShortForm());
		}
		return entities;
	}
	
	public String QueryEntity(String entity) throws Exception{
		if (onto.containsClassInSignature(pm.getIRI(":"+entity))){
			return QueryLabel(entity);
		}else if (onto.containsObjectPropertyInSignature(pm.getIRI(":"+entity))){
			return QueryRelat(entity);
			
		}else if (onto.containsDataPropertyInSignature(pm.getIRI(":"+entity))){
			return QueryProp(entity);
		}else{
			
		}
		return "";
	}
	
	public String QueryLabel(String entity) throws Exception{
		String info=String.format("----------Label [%s]\n", entity);
		OWLClass label = factory.getOWLClass(":"+entity, pm);
		
		Set<OWLClass> superLabels = GetClassSuper(label);
		if(!superLabels.isEmpty()){
			String sls="";
			for(OWLClass superLabel:superLabels){
				sls += String.format("%s, ", superLabel.getIRI().getShortForm());
			}
			sls = sls.substring(0, sls.length()-2);
			info = info + String.format("Label [%s] has Super Label [%s]\n", entity, sls);
		}
		
		Set<OWLClass> subLabels = GetClassSub(label);
		if(!subLabels.isEmpty()){
			String sls="";
			for(OWLClass subLabel:subLabels){
				sls += String.format("%s, ", subLabel.getIRI().getShortForm());
			}
			sls = sls.substring(0, sls.length()-2);
			info = info + String.format("Label [%s] has Sub Label [%s]\n", entity, sls);
		}
		
		Set<OWLDataProperty> props = GetClassDataProperties(label);
//		for(OWLClass superLabel: superLabels){
//			props.addAll(GetClassDataProperties(superLabel));
//		}
		for(OWLDataProperty prop:props){
			info = info +String.format("Label [%s] has property [%s], %s type\n", entity, prop.getIRI().getShortForm(), GetDataPropertyRange(prop));
		}
		
		Set<OWLObjectProperty> domainRelats = GetClassDomainObjectProperties(label);
//		for(OWLClass superLabel: superLabels){
//			domainRelats.addAll(GetClassDomainObjectProperties(superLabel));
//		}
		for(OWLObjectProperty relat:domainRelats){
			String range="";
			for(OWLClass rangeLabel: GetObjectPropertyRange(relat)){
				range += String.format("%s, ", rangeLabel.getIRI().getShortForm());
			}
			range=range.substring(0, range.length()-2);
			info = info + String.format("Label [%s] - Relation [%s] -> Label [%s]\n", entity, relat.getIRI().getShortForm(), range);
		}
		
		Set<OWLObjectProperty> rangeRelats = GetClassRangeObjectProperties(label);
//		for(OWLClass subLabel: subLabels){
//			rangeRelats.addAll(GetClassRangeObjectProperties(subLabel));
//		}
		for(OWLObjectProperty relat:rangeRelats){
			String domain="";
			for(OWLClass domainLabel: GetObjectPropertyDomain(relat)){
				domain += String.format("%s, ", domainLabel.getIRI().getShortForm());
			}
			domain=domain.substring(0, domain.length()-2);
			info = info +String.format("Label [%s] - Relation [%s] -> Label [%s]\n", domain, relat.getIRI().getShortForm(), entity);
		}
		
		return info;
	}
	
	public String QueryRelat(String entity) throws Exception{
		String info =String.format("----------Relation [%s]\n", entity);
		OWLObjectProperty relat = factory.getOWLObjectProperty(":"+entity, pm);
		Set<OWLClass> domainLabels = GetObjectPropertyDomain(relat);
		
		String domain="";
		for(OWLClass domainLabel: GetObjectPropertyDomain(relat)){
			domain += String.format("%s, ", domainLabel.getIRI().getShortForm());
		}
		domain=domain.substring(0, domain.length()-2);
		
		String range="";
		for(OWLClass rangeLabel: GetObjectPropertyRange(relat)){
			range += String.format("%s, ", rangeLabel.getIRI().getShortForm());
		}
		range=range.substring(0, range.length()-2);
		
		info = info +String.format("Label [%s] - Relation [%s] -> Label [%s]\n", domain, entity, range);
		
		if(onto.containsClassInSignature(pm.getIRI(":"+"R_"+entity))){
			OWLClass rl = factory.getOWLClass(":R_"+entity, pm);
			Set<OWLDataProperty> props = GetClassDataProperties(rl);
			for(OWLDataProperty prop:props){
				info = info + String.format("Relation [%s] has property [%s], [%s] type\n", entity, prop.getIRI().getShortForm(), GetDataPropertyRange(prop));
			}
		}
		
		return info;
	}
	
	public String QueryProp(String entity) throws Exception{
		String info =String.format("----------Property [%s]\n", entity);
		OWLDataProperty prop = factory.getOWLDataProperty(":"+entity, pm);
		String domain="";
		for(OWLClass domainLabel: GetDataPropertyDomain(prop)){
			domain += String.format("%s, ", domainLabel.getIRI().getShortForm());
		}
		domain=domain.substring(0, domain.length()-2);
		if(domain.contains("R_")){
			domain.replaceAll("R_", "");
			info = info+String.format("Relation [%s] has property [%s], [%s] type\n", domain, entity, GetDataPropertyRange(prop));
		}else{
			info = info+String.format("Label [%s] has property [%s], [%s] type\n", domain, entity, GetDataPropertyRange(prop));
		}
		
		return info;
	}
	
	public Set<OWLClass> GetObjectPropertyDomain(OWLObjectProperty op) throws Exception{
		Set<OWLClass> domainClasses = new HashSet<OWLClass>();
		Set<OWLObjectPropertyDomainAxiom> domainAxioms = onto.getObjectPropertyDomainAxioms(op);
		if(domainAxioms.isEmpty()){
			throw new Exception(op.getIRI().getShortForm() + " Object Property Domain Error!");
		}
		OWLObjectPropertyDomainAxiom domainAxiom=domainAxioms.iterator().next();
		OWLClassExpression cexp = domainAxiom.getDomain();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				domainClasses.add(exp.asOWLClass());
				domainClasses.addAll(GetClassSub(exp.asOWLClass()));
			}
				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			domainClasses.add(cexp.asOWLClass());	
			domainClasses.addAll(GetClassSub(cexp.asOWLClass()));
		}else{
			throw new Exception(op.getIRI().getShortForm()+ " Oject Property Domain Error!");
		}
		return domainClasses;
	}
	
	public Set<OWLClass> GetObjectPropertyDomainOri(OWLObjectProperty op) throws Exception{
		Set<OWLClass> domainClasses = new HashSet<OWLClass>();
		Set<OWLObjectPropertyDomainAxiom> domainAxioms = onto.getObjectPropertyDomainAxioms(op);
		if(domainAxioms.isEmpty()){
			throw new Exception(op.getIRI().getShortForm() + " Object Property Domain Error!");
		}
		OWLObjectPropertyDomainAxiom domainAxiom=domainAxioms.iterator().next();
		OWLClassExpression cexp = domainAxiom.getDomain();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				domainClasses.add(exp.asOWLClass());
			}
				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			domainClasses.add(cexp.asOWLClass());	
		}else{
			throw new Exception(op.getIRI().getShortForm()+ " Oject Property Domain Error!");
		}
		return domainClasses;
	}
	
	public Set<OWLClass> GetObjectPropertyRange(OWLObjectProperty op) throws Exception{
		Set<OWLClass> rangeClasses = new HashSet<OWLClass>();
		Set<OWLObjectPropertyRangeAxiom> rangeAxioms = onto.getObjectPropertyRangeAxioms(op);
		if(rangeAxioms.isEmpty()){
			throw new Exception(op.getIRI().getShortForm() + " Object Property Range Error!");
		}
		OWLObjectPropertyRangeAxiom rangeAxiom=rangeAxioms.iterator().next();
		OWLClassExpression cexp = rangeAxiom.getRange();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				rangeClasses.add(exp.asOWLClass());
				rangeClasses.addAll(GetClassSub(exp.asOWLClass()));
			}
				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			rangeClasses.add(cexp.asOWLClass());
			rangeClasses.addAll(GetClassSub(cexp.asOWLClass()));
		}else{
			throw new Exception(op.getIRI().getShortForm()+ " Oject Property Range Error!");
		}
		return rangeClasses;
	}
	public Set<OWLClass> GetObjectPropertyRangeOri(OWLObjectProperty op) throws Exception{
		Set<OWLClass> rangeClasses = new HashSet<OWLClass>();
		Set<OWLObjectPropertyRangeAxiom> rangeAxioms = onto.getObjectPropertyRangeAxioms(op);
		if(rangeAxioms.isEmpty()){
			throw new Exception(op.getIRI().getShortForm() + " Object Property Range Error!");
		}
		OWLObjectPropertyRangeAxiom rangeAxiom=rangeAxioms.iterator().next();
		OWLClassExpression cexp = rangeAxiom.getRange();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				rangeClasses.add(exp.asOWLClass());
			}
				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			rangeClasses.add(cexp.asOWLClass());
		}else{
			throw new Exception(op.getIRI().getShortForm()+ " Oject Property Range Error!");
		}
		return rangeClasses;
	}
	
	public Set<OWLClass> GetDataPropertyDomain(OWLDataProperty dp) throws Exception{
		Set<OWLClass> domainClasses = new HashSet<OWLClass>();
		Set<OWLDataPropertyDomainAxiom> domainAxioms = onto.getDataPropertyDomainAxioms(dp);
		if(domainAxioms.isEmpty()){
			throw new Exception(dp.getIRI().getShortForm() + " Data Property Domain Error!");
		}
		OWLDataPropertyDomainAxiom domainAxiom=domainAxioms.iterator().next();
		OWLClassExpression cexp = domainAxiom.getDomain();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				domainClasses.add(exp.asOWLClass());
				domainClasses.addAll(GetClassSub(exp.asOWLClass()));
			}				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			domainClasses.add(cexp.asOWLClass());	
			domainClasses.addAll(GetClassSub(cexp.asOWLClass()));
		}else{
			throw new Exception(dp.getIRI().getShortForm()+ " Oject Property Domain Error!");
		}
		return domainClasses;
	}
	
	public Set<OWLClass> GetDataPropertyDomainOri(OWLDataProperty dp) throws Exception{
		Set<OWLClass> domainClasses = new HashSet<OWLClass>();
		Set<OWLDataPropertyDomainAxiom> domainAxioms = onto.getDataPropertyDomainAxioms(dp);
		if(domainAxioms.isEmpty()){
			throw new Exception(dp.getIRI().getShortForm() + " Data Property Domain Error!");
		}
		OWLDataPropertyDomainAxiom domainAxiom=domainAxioms.iterator().next();
		OWLClassExpression cexp = domainAxiom.getDomain();
		ClassExpressionType type = cexp.getClassExpressionType();
		if(type==ClassExpressionType.OBJECT_UNION_OF){
			Set<OWLClassExpression> exps = cexp.asDisjunctSet();			
			for(OWLClassExpression exp: exps){
				domainClasses.add(exp.asOWLClass());
			}				
		}else if(type==ClassExpressionType.OWL_CLASS){			
			domainClasses.add(cexp.asOWLClass());	
		}else{
			throw new Exception(dp.getIRI().getShortForm()+ " Oject Property Domain Error!");
		}
		return domainClasses;
	}
	
	public String GetDataPropertyRange(OWLDataProperty dp){
		Set<OWLDataPropertyRangeAxiom> rangeAxioms = onto.getDataPropertyRangeAxioms(dp);
		if(rangeAxioms.isEmpty()){
			return "string";
		}
		String range = rangeAxioms.iterator().next().getRange().asOWLDatatype().getIRI().getShortForm();
		return range;
	}
	
	public Set<OWLDataProperty> GetClassDataProperties(OWLClass c) throws Exception{
		Set<OWLDataProperty> dataProps = new HashSet<OWLDataProperty>();
		Set<OWLDataProperty> dprops = onto.getDataPropertiesInSignature();
		for(OWLDataProperty prop:dprops){
			Set<OWLClass> domains = GetDataPropertyDomain(prop);
			if(domains.contains(c)){
				dataProps.add(prop);
			}
		}
		return dataProps;
	}
	
	public Set<OWLObjectProperty> GetClassDomainObjectProperties(OWLClass c) throws Exception{
		Set<OWLObjectProperty> objectProps=new HashSet<OWLObjectProperty>();
		Set<OWLObjectProperty> oprops = onto.getObjectPropertiesInSignature();
		for(OWLObjectProperty prop:oprops){
			Set<OWLClass> domains = GetObjectPropertyDomain(prop);
			if(domains.contains(c)){
				objectProps.add(prop);
			}
		}
		return objectProps;
	}
	
	public Set<OWLObjectProperty> GetClassRangeObjectProperties(OWLClass c) throws Exception{
		Set<OWLObjectProperty> objectProps=new HashSet<OWLObjectProperty>();
		Set<OWLObjectProperty> oprops = onto.getObjectPropertiesInSignature();
		for(OWLObjectProperty prop:oprops){
			Set<OWLClass> ranges = GetObjectPropertyRange(prop);
			if(ranges.contains(c)){
				objectProps.add(prop);
			}
		}
		return objectProps;
	}
	
	public Set<OWLClass> GetClassSuper(OWLClass c){
		Set<OWLClass> scs = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom>  axioms = onto.getSubClassAxiomsForSubClass(c);
		for(OWLSubClassOfAxiom axiom: axioms){
			scs.add(axiom.getSuperClass().asOWLClass());
		}
		return scs;
	}
	
	public Set<String> GetLabelSuper(OWLClass c){
		Set<String> scs = new HashSet<String>();
		Set<OWLSubClassOfAxiom>  axioms = onto.getSubClassAxiomsForSubClass(c);
		for(OWLSubClassOfAxiom axiom: axioms){
			scs.add(axiom.getSuperClass().asOWLClass().getIRI().getShortForm());
		}
		return scs;
	}
	
	public Set<OWLClass> GetClassSub(OWLClass c){
		Set<OWLClass> scs = new HashSet<OWLClass>();
		Set<OWLSubClassOfAxiom>  axioms = onto.getSubClassAxiomsForSuperClass(c);
		for(OWLSubClassOfAxiom axiom: axioms){
			scs.add(axiom.getSubClass().asOWLClass());
		}
		return scs;
	}
	public OWLObjectProperty GetRelateBetweenClasses(OWLClass c1, OWLClass c2) throws Exception{
		Set<OWLObjectProperty> oprops = onto.getObjectPropertiesInSignature();
		for(OWLObjectProperty prop:oprops){
			Set<OWLClass> domains = GetObjectPropertyDomain(prop);
			if(!domains.contains(c1)){
				continue;
			}
			
			Set<OWLClass> ranges = GetObjectPropertyRange(prop);
			if(ranges.contains(c2)){
				return prop;
			}			
		}
		return null;
	}
	
	public String GetClassDataPropertyRange(OWLClass c,OWLDataProperty dp) throws Exception{
		Set<OWLClass> domains = GetDataPropertyDomain(dp);
		if(!domains.contains(c)){
			Set<OWLClass> scs = GetClassSuper(c);
			boolean contain = false;
			for(OWLClass sc:scs){
				if(domains.contains(sc)){
					contain=true;
					break;
				}
			}
			if(!contain){
				return null;
			}
		}
		
		Set<OWLDataPropertyRangeAxiom> rangeAxioms = onto.getDataPropertyRangeAxioms(dp);
		if(rangeAxioms.isEmpty()){
			return "string";
		}
		String range = rangeAxioms.iterator().next().getRange().asOWLDatatype().getIRI().getShortForm();
		return range;
	}
	
	public OWLDataProperty GetLabelKeyProp(OWLClass label) throws Exception{
		Set<OWLClass> superLabels = GetClassSuper(label);
		if(!superLabels.isEmpty()){
			return GetLabelKeyProp(superLabels.iterator().next());
		}
		String labelName = label.getIRI().getShortForm();
		OWLDataProperty labelKeyProp = null;
		if(onto.containsDataPropertyInSignature(pm.getIRI(":"+ labelName.substring(0, 1).toLowerCase()+labelName.substring(1)+"Id"))){
			labelKeyProp = factory.getOWLDataProperty(labelName.substring(0, 1).toLowerCase()+labelName.substring(1)+"Id",pm);
			Set<OWLClass> domains = GetDataPropertyDomain(labelKeyProp);
			if(domains.contains(label)){
				return labelKeyProp;
			}
		}
		if(onto.containsDataPropertyInSignature(pm.getIRI(":name"))){
			labelKeyProp = factory.getOWLDataProperty(":name",pm);
			Set<OWLClass> domains = GetDataPropertyDomain(labelKeyProp);
//			if(!domains.contains(label)){
//				for(OWLClass superLabel: primLabelSupers){
//					if(domains.contains(superLabel)){
//						return labelKeyProp;
//					}
//				}
//
//			}
			if(domains.contains(label)){
				return labelKeyProp;
			}
		}
		if(onto.containsDataPropertyInSignature(pm.getIRI(":type"))){
			labelKeyProp = factory.getOWLDataProperty(":type",pm);
			Set<OWLClass> domains = GetDataPropertyDomain(labelKeyProp);
			if(domains.contains(label)){
				return labelKeyProp;
			}
		}
		throw new Exception(String.format("Label %s doesn't have key property.", labelName));

	}
	
	public Set<OWLDataProperty> GetRelateProps(OWLObjectProperty relate) throws Exception{
		if(onto.containsClassInSignature(pm.getIRI(":"+"R_"+relate.getIRI().getShortForm()))){
			OWLClass rl = factory.getOWLClass(":R_"+relate.getIRI().getShortForm(), pm);
			Set<OWLDataProperty> props = GetClassDataProperties(rl);
			return props;
		}
		return null;
	}
	
	public void SetNodeSuperLabel(Set<OWLClass> superLabels, Node node){
		for(OWLClass superLabel:superLabels){
			node.addLabel(Label.label(superLabel.getIRI().getShortForm()));
		}
	}
	
	
}
