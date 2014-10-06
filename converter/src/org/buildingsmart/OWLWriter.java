package org.buildingsmart;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.buildingsmart.vo.AttributeVO;
import org.buildingsmart.vo.EntityVO;
import org.buildingsmart.vo.NamedIndividualVO;
import org.buildingsmart.vo.PrimaryTypeVO;
import org.buildingsmart.vo.PropertyVO;
import org.buildingsmart.vo.TypeVO;

import softhema.system.toolkits.ToolkitString;
import fi.ni.rdf.Namespace;

/*
 * OWLWriter writes .n3 files representing OWL ontologies, thereby relying on the in-memory EXPRESS model that is parsed by the ExpressReader class.
 * 
 * The usage:
 * OWLWriter ow = new OWLWriter(expressSchemaName, entities, types, siblings);
 * 
 *  - outputOWL() - writes the OWL ontology in N3 files in appropriate 'schema' package
 *  - outputRDFS() - writes the RDFS ontology in N3 files
 *   
 * @author Jyrki Oraskari
 * @author of modifications Pieter Pauwels (pipauwel.pauwels@ugent.be / pipauwel@gmail.com)
 */

/*
 * The GNU Affero General Public License
 * 
 * Copyright (c) 2014 Jyrki Oraskari (original)
 * Copyright (c) 2014 Pieter Pauwels (modifications - pipauwel.pauwels@ugent.be / pipauwel@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 */

public class OWLWriter {

	private String expressSchemaName;
	
	private Map<String, EntityVO> entities = new HashMap<String, EntityVO>();
	private Map<String, PropertyVO> properties = new HashMap<String, PropertyVO>();	
	private Map<String, TypeVO> types = new HashMap<String, TypeVO>();
	private Map<String, Set<String>> siblings = new HashMap<String, Set<String>>();
	private List<NamedIndividualVO> enumIndividuals= new ArrayList<NamedIndividualVO>();
//	private Map<String, TypeVO> types = new HashMap<String, TypeVO>();
//	private Map<String, String> interfaces = new HashMap<String, String>();
//	private Map<String, String> interface_aliases = new HashMap<String, String>();
	
	static final String simpleobject_property = "ifc:<_NAME_>\r\n"
				+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"
				+ "\trdfs:range ifc:<_RANGE_> ;\r\n"
				+ "\ta owl:ObjectProperty .\r\n\r\n";
	
	static final String simpleobject_property_list = "ifc:<_NAME_>\r\n"
			+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"			
			+ "\trdfs:range [ \r\n"
			+ "\t\ta owl:Class ; \r\n"
			+ "\t\trdfs:subClassOf olo:OrderedList ; \r\n"
			+ "\t\trdfs:subClassOf [ \r\n"
			+ "\t\t\ta owl:Restriction, owl:Class ; \r\n"
			+ "\t\t\towl:allValuesFrom [ \r\n"
			+ "\t\t\t\ta rdfs:Datatype ;\r\n"
			+ "\t\t\t\towl:oneOf (<_LISTRANGEVALUES_>)\r\n"//"1"^^xsd:int "2"^^xsd:int
			+ "\t\t\t] ;\r\n"
			+ "\t\t\towl:onProperty olo:length \r\n"	
			+ "\t\t] ;\r\n"
			+ "\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\ta owl:Restriction ; \r\n"			
			+ "\t\t\towl:allValuesFrom [ \r\n"			
			+ "\t\t\t\ta owl:Class ; \r\n"			
			+ "\t\t\t\trdfs:subClassOf olo:Slot ; \r\n"	
			+ "\t\t\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\t\t\ta owl:Restriction ; \r\n"
			+ "\t\t\t\t\ta owl:allValuesFrom ifc:<_LISTVALUE_> ; \r\n"
			+ "\t\t\t\t\ta owl:onProperty olo:item \r\n"
			+ "\t\t\t\t] ; \r\n"	
			+ "\t\t\t] ; \r\n"				
			+ "\t\t\towl:onProperty olo:slot \r\n"	
			+ "\t\t] ; \r\n"			
			+ "\t] ; \r\n"			
			+ "\ta owl:ObjectProperty .\r\n\r\n";
	
	static final String simpleobject_property_list_unbounded = "ifc:<_NAME_>\r\n"
			+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"			
			+ "\trdfs:range [ \r\n"
			+ "\t\ta owl:Class ; \r\n"
			+ "\t\trdfs:subClassOf olo:OrderedList ; \r\n"
			+ "\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\ta owl:Restriction ; \r\n"			
			+ "\t\t\towl:allValuesFrom [ \r\n"			
			+ "\t\t\t\ta owl:Class ; \r\n"			
			+ "\t\t\t\trdfs:subClassOf olo:Slot ; \r\n"	
			+ "\t\t\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\t\t\ta owl:Restriction ; \r\n"
			+ "\t\t\t\t\ta owl:allValuesFrom ifc:<_LISTVALUE_> ; \r\n"
			+ "\t\t\t\t\ta owl:onProperty olo:item \r\n"
			+ "\t\t\t\t] ; \r\n"	
			+ "\t\t\t] ; \r\n"				
			+ "\t\t\towl:onProperty olo:slot \r\n"	
			+ "\t\t] ; \r\n"			
			+ "\t] ; \r\n"			
			+ "\ta owl:ObjectProperty .\r\n\r\n";
	
	static final String select_property = "ifc:<_NAME_>\r\n"
			+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"
			+ "\trdfs:range [ \r\n"
			+ "\t\ta owl:Class ; \r\n"
			+ "\t\ta owl:unionOf ( <_SELECTVALUES_> ) ; \r\n"
			+ "\t] ;\r\n"
			+ "\ta owl:ObjectProperty .\r\n\r\n";
	
	static final String select_property_list = "ifc:<_NAME_>\r\n"
			+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"			
			+ "\trdfs:range [ \r\n"
			+ "\t\ta owl:Class ; \r\n"
			+ "\t\trdfs:subClassOf olo:OrderedList ; \r\n"
			+ "\t\trdfs:subClassOf [ \r\n"
			+ "\t\t\ta owl:Restriction, owl:Class ; \r\n"
			+ "\t\t\towl:allValuesFrom [ \r\n"
			+ "\t\t\t\ta rdfs:Datatype ;\r\n"
			+ "\t\t\t\towl:oneOf (<_LISTRANGEVALUES_>)\r\n"//"1"^^xsd:int "2"^^xsd:int
			+ "\t\t\t] ;\r\n"
			+ "\t\t\towl:onProperty olo:length \r\n"	
			+ "\t\t] ;\r\n"
			+ "\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\ta owl:Restriction ; \r\n"			
			+ "\t\t\towl:allValuesFrom [ \r\n"			
			+ "\t\t\t\ta owl:Class ; \r\n"			
			+ "\t\t\t\trdfs:subClassOf olo:Slot ; \r\n"	
			+ "\t\t\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\t\t\ta owl:Restriction ; \r\n"
			+ "\t\t\t\t\ta owl:allValuesFrom [ \r\n"
			+ "\t\t\t\t\t\ta owl:Class ; \r\n"
			+ "\t\t\t\t\t\ta owl:unionOf ( <_SELECTVALUES_> ) ; \r\n"
			+ "\t\t\t\t\t] ; \r\n"
			+ "\t\t\t\t\ta owl:onProperty olo:item \r\n"
			+ "\t\t\t\t] ; \r\n"	
			+ "\t\t\t] ; \r\n"				
			+ "\t\t\towl:onProperty olo:slot \r\n"	
			+ "\t\t] ; \r\n"			
			+ "\t] ; \r\n"			
			+ "\ta owl:ObjectProperty .\r\n\r\n";
	
	static final String select_property_list_unbounded = "ifc:<_NAME_>\r\n"
			+ "\trdfs:domain ifc:<_DOMAIN_> ;\r\n"			
			+ "\trdfs:range [ \r\n"
			+ "\t\ta owl:Class ; \r\n"
			+ "\t\trdfs:subClassOf olo:OrderedList ; \r\n"
			+ "\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\ta owl:Restriction ; \r\n"			
			+ "\t\t\towl:allValuesFrom [ \r\n"			
			+ "\t\t\t\ta owl:Class ; \r\n"			
			+ "\t\t\t\trdfs:subClassOf olo:Slot ; \r\n"	
			+ "\t\t\t\trdfs:subClassOf [ \r\n"	
			+ "\t\t\t\t\ta owl:Restriction ; \r\n"
			+ "\t\t\t\t\ta owl:allValuesFrom [ \r\n"
			+ "\t\t\t\t\t\ta owl:Class ; \r\n"
			+ "\t\t\t\t\t\ta owl:unionOf ( <_SELECTVALUES_> ) ; \r\n"
			+ "\t\t\t\t\t] ; \r\n"
			+ "\t\t\t\t\ta owl:onProperty olo:item \r\n"
			+ "\t\t\t\t] ; \r\n"	
			+ "\t\t\t] ; \r\n"				
			+ "\t\t\towl:onProperty olo:slot \r\n"	
			+ "\t\t] ; \r\n"			
			+ "\t] ; \r\n"			
			+ "\ta owl:ObjectProperty .\r\n\r\n";
			
	
	public OWLWriter() {
		// UNUSED
	}
	
	public OWLWriter(String expressSchemaName, Map<String, EntityVO> entities, Map<String, TypeVO> types, Map<String, Set<String>> siblings, List<NamedIndividualVO> enumIndividuals, Map<String, PropertyVO> properties) {
		this.expressSchemaName = expressSchemaName;
		this.entities = entities;
		this.types = types;
		this.siblings = siblings;
		this.enumIndividuals = enumIndividuals;
		this.properties = properties;
	}

	public void outputOWL() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"out\\"+expressSchemaName + "_owl.n3"));
			out.write("@prefix ifc: <" + Namespace.IFC + "> .\r\n");
			out.write(getOwl_header());
			
			writePrimaryTypes(out);
			writeNamedIndividuals(out);
			
			Iterator<Entry<String, TypeVO>> it_type = types.entrySet().iterator();
			writeTypesToOWL(it_type,out);
			
			Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
			writeEntitiesToOWL(it,out);
			
			out.write("# start writing properties\r\n");
			for (Map.Entry<String, PropertyVO> entry : properties.entrySet()) {
				PropertyVO property = entry.getValue();
				outputOWLproperty(out, property);
			}
			
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputRDFS() {
		//UNUSED
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					"out\\"+expressSchemaName + "_rdfs.n3"));
			out.write("@prefix rdf:  <" + Namespace.RDF + "> .\r\n");
			out.write("@prefix rdfs: <" + Namespace.RDFS + "> .\r\n");
			out.write("@prefix owl: <" + Namespace.OWL + "> .\r\n");
			out.write("@prefix ifc: <" + Namespace.IFC + "> .\r\n");
			out.write("@prefix xsd: <" + Namespace.XSD + "> .\r\n");
			out.write("\r\n");
			while (it.hasNext()) {
				Entry<String, EntityVO> pairs = it.next();
				EntityVO evo = pairs.getValue();

				if (evo.getSuperclass() == null)
					out.write("ifc:" + evo.getName() + " a rdfs:Class .\r\n");
				else
					out.write("ifc:" + evo.getName()
							+ " a rdfs:Class; rdfs:subClassOf ifc:"
							+ evo.getSuperclass() + " .\r\n");

				for (int n = 0; n < evo.getAttributes().size(); n++) {
					String property = evo.getAttributes().get(n).getName()
							.toLowerCase();// formatProperty(evo.getAttributes().get(n).getName());
					PropertyVO t = properties.get(property);
					if (t == null) {
						properties.put(property, null);
						out.write("ifc:" + property + " a rdf:Property.\r\n");
					}

					out.write("ifc:" + property + " rdfs:domain ifc:"
							+ pairs.getValue().getName() + ".\r\n");
				}

				for (int n = 0; n < evo.getInverses().size(); n++) {
					String property = ExpressReader.formatProperty(evo.getInverses().get(n)
							.getName());
					PropertyVO t = properties.get(property);
					if (t == null) {

						properties.put(property, null);
						out.write("ifc:" + property + " a rdf:Property.\r\n");
					}

					out.write("ifc:" + property + " rdfs:domain ifc:"
							+ pairs.getValue().getName() + ".\r\n");
				}

			}
			out.write("ifc:has_linenumber a rdf:Property.\r\n");
			out.write("ifc:graph_deep a rdf:Property.\r\n");
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void outputOWLproperty(BufferedWriter out, PropertyVO property) {
		try {
			if (property.isList()) {
				if (property.getType() == PropertyVO.propertyType.TypeVO
						|| property.getType() == PropertyVO.propertyType.EntityVO) {
					String tmp;

					if(property.getMaxCardinality()!=-1)
						tmp = simpleobject_property_list;
					else tmp = simpleobject_property_list_unbounded;
					
					tmp = ToolkitString.strReplaceLike(tmp, "<_NAME_>",
							property.getName());
					tmp = ToolkitString.strReplaceLike(tmp, "<_DOMAIN_>",
							property.getDomain().getName());
					if(property.getMaxCardinality()!=-1)
						tmp = ToolkitString.strReplaceLike(tmp,
								"<_LISTRANGEVALUES_>",
								"\"" + property.getMinCardinality() + "\"" +
								"^^xsd:int " +
								"\"" + property.getMaxCardinality() + "\"" +
								"^^xsd:int");
					tmp = ToolkitString.strReplaceLike(tmp, "<_LISTVALUE_>",
							property.getRange());
					out.write(tmp);

					return;
				} else if (property.getType() == PropertyVO.propertyType.Select) {
					String tmp;

					if(property.getMaxCardinality()!=-1)
						tmp = select_property_list;
					else tmp = select_property_list_unbounded;
					
					tmp = ToolkitString.strReplaceLike(tmp, "<_NAME_>",
							property.getName());
					tmp = ToolkitString.strReplaceLike(tmp, "<_DOMAIN_>",
							property.getDomain().getName());

					if(property.getMaxCardinality()!=-1)
						tmp = ToolkitString.strReplaceLike(tmp,
								"<_LISTRANGEVALUES_>",
								"\"" + property.getMinCardinality() + "\"" +
								"^^xsd:int " +
								"\"" + property.getMaxCardinality() + "\"" +
								"^^xsd:int");
									
					if (property.getSelectEntities() != null || property.getSelectEntities().size() > 0) {
						// numbers
						String sv = "";
						for (String s : property.getSelectEntities()) { 
							sv += "ifc:" + s + ", ";
						}
						if(sv.length()>2){
						sv = sv.substring(0, sv.length() - 2);
						tmp = ToolkitString.strReplaceLike(tmp,
								"<_SELECTVALUES_>", sv);
						}
						else
						tmp = ToolkitString
								.strReplaceLike(tmp, "<_SELECTVALUES_>",
										"Something went wrong here : " + property.getName() + " - " + property.getDomain().getName());
					} else
						tmp = ToolkitString
								.strReplaceLike(tmp, "<_SELECTVALUES_>",
										"Something went wrong here : " + property.getName() + " - " + property.getDomain().getName());
					out.write(tmp);
					return;
				}
			} else {
				if (property.getType() == PropertyVO.propertyType.TypeVO
						|| property.getType() == PropertyVO.propertyType.EntityVO) {
					String tmp;

					tmp = simpleobject_property;
					tmp = ToolkitString.strReplaceLike(tmp, "<_NAME_>",
							property.getName());
					tmp = ToolkitString.strReplaceLike(tmp, "<_DOMAIN_>",
							property.getDomain().getName());
					tmp = ToolkitString.strReplaceLike(tmp, "<_RANGE_>",
							property.getRange());
					out.write(tmp);

					return;
				} else if (property.getType() == PropertyVO.propertyType.Select) {
					String tmp;

					tmp = select_property;
					tmp = ToolkitString.strReplaceLike(tmp, "<_NAME_>",
							property.getName());
					tmp = ToolkitString.strReplaceLike(tmp, "<_DOMAIN_>",
							property.getDomain().getName());
					
					if (property.getSelectEntities() != null || property.getSelectEntities().size() > 0) {
						String sv = "";
						for (String s : property.getSelectEntities()) {
							sv += "ifc:"+s + ", ";
						}
						if(sv.length()>=2){
						sv = sv.substring(0, sv.length() - 2);
						tmp = ToolkitString.strReplaceLike(tmp,
								"<_SELECTVALUES_>", sv);
						}
						else {
							tmp = ToolkitString
									.strReplaceLike(tmp, "<_SELECTVALUES_>",
											"Something went wrong there");
						}
					} else
						tmp = ToolkitString
								.strReplaceLike(tmp, "<_SELECTVALUES_>",
										"Something went wrong there");

					out.write(tmp);
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writePrimaryTypes(BufferedWriter out) throws IOException{
		for(PrimaryTypeVO pt : PrimaryTypeVO.getListOfPrimaryTypes()){
			out.write("ifc:"+pt.getPTypeName() + "\r\n");
			out.write("\ta owl:Class ;" + "\r\n");
			out.write("\trdfs:subClassOf [ " + "\r\n");
			out.write("\t\ta owl:Restriction ;" + "\r\n");
			out.write("\t\towl:allValuesFrom xsd:"+pt.getXSDType()+" ;" + "\r\n");
			out.write("\t\towl:onProperty ifc:hasContent" + "\r\n");
			out.write("\t\t] ." + "\r\n"+ "\r\n");
		}
	}
	
	private void writeNamedIndividuals(BufferedWriter out) throws IOException{
		for(NamedIndividualVO ni : enumIndividuals){
			out.write("ifc:" + ni.getNamedIndividual() + "\r\n");
			out.write("\ta owl:NamedIndividual ;" + "\r\n");
			out.write("\trdfs:label \""+ni.getOriginalNameOfIndividual()+"\"^^xsd:string ." + "\r\n" + "\r\n");
		}
	}
	
	private void writeEntitiesToOWL(Iterator<Entry<String, EntityVO>> it, BufferedWriter out) throws IOException{
		out.write("# start writing entity classes\r\n");
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = pairs.getValue();
			// Generate the disjoint set:
			StringBuffer sibtxt = new StringBuffer();
			Set<String> sibling_set = this.siblings.get(evo.getName());
			if (sibling_set != null) {
				Iterator<String> sib_it = sibling_set.iterator();
				int ii = 0;
				while (sib_it.hasNext()) {
					String sib = sib_it.next().toString();
					if (!sib.equalsIgnoreCase(evo.getName())) {
						if (ii > 0)
							sibtxt.append(",");
						sibtxt.append(" ifc:");
						sibtxt.append(sib);
						ii++;
					}
				}
			}

			//Write classes
			out.write("ifc:" + evo.getName() + "\r\n");
			out.write("\ta owl:Class ;\r\n");
			if (evo.getSuperclass() != null)
				out.write("\trdfs:subClassOf ifc:" + evo.getSuperclass());			

			//Writing abstractness
			if(evo.isAbstractSuperclass()){
				out.write(" ;\r\n");
				out.write("\towl:equivalentClass [" + "\r\n");
				out.write("\t\ta owl:Class ;" + "\r\n");
				out.write("\t\towl:unionOf ( ");
				Set<String> l = evo.getSubClassList();
				for (Iterator<String> lit = l.iterator(); lit.hasNext(); ) {
					String x = lit.next();
					if ( ! lit.hasNext())
						out.write("ifc:" + x);
					else
						out.write("ifc:" + x + ", ");
				}
				out.write(" ) ." + "\r\n");
				out.write("\t\t]");
			}

			//Writing disjointness
			if (sibtxt.length() > 0)
			{
				out.write(" ;\r\n");
				out.write("\towl:disjointWith " + sibtxt.toString());
			}

			//Writing properties
			for (int n = 0; n < evo.getAttributes().size(); n++) {
				
				AttributeVO attr = evo.getAttributes().get(n);
				if(!attr.isList() && !attr.isSet()){			
					out.write(" ;\r\n");
					out.write("\tifc:subClassOf [" + "\r\n");
					out.write("\t\ta owl:Restriction ; " + "\r\n");
					out.write("\t\towl:allValuesFrom ifc:"+attr.getType().getName() + " ; \r\n");
					out.write("\t\towl:onProperty ifc:"+attr.getName() + "\r\n");
					out.write("\t]");
				}
				else {		
					out.write(" ;\r\n");
					out.write("# TODO: list or set property found " + attr.getName() + "\r\n");
				}
				
				if(attr.isUnique()){
					//this is ignored
				}
				
				if(!attr.isOptional()){
					//required property -> cardinality restrictions 1-1
					
					out.write(" ;\r\n");
					out.write("\t" + "rdfs:subClassOf [" + "\r\n");
					out.write("\t\t" + "a owl:Restriction ;" + "\r\n");
					out.write("\t\t"
							+ "owl:maxCardinality \"1\"^^xsd:int ;"
							+ "\r\n");
					out.write("\t\t" + "owl:onProperty "
							+ attr.getName() + "\r\n");
					out.write("\t\t" + "] ;" + "\r\n");
					out.write("\t" + "rdfs:subClassOf " + "\r\n");
					out.write("\t\t" + "[ a owl:Restriction ;" + "\r\n");
					out.write("\t\t"
							+ "owl:minCardinality \"1\"^^xsd:int ;"
							+ "\r\n");
					out.write("\t\t" + "owl:onProperty "
							+ attr.getName() + "\r\n");
					out.write("\t\t" + "] ");
				}
				//copied
			}
			
			out.write(" .\r\n");

//			out.write("# writing inverses\r\n");
//			for (int n = 0; n < evo.getInverses().size(); n++) {
//
//				out.write("# inverse found\r\n");
//				
//				String property = ExpressReader.formatProperty(evo.getInverses().get(n)
//						.getName());
//				PropertyVO t = properties.get(property);
//				if (t == null) {
//					t = new PropertyVO(property, true, true, evo
//							.getInverses().get(n).getIfc_class());
//					properties.put(property, t);
//				}
//				t.addIfcClass(evo.getName());
//			}

			out.write("\r\n");
//			out.write("# printed an entity\r\n\r\n");
		}
	}
	
	private void writeTypesToOWL(Iterator<Entry<String, TypeVO>> it, BufferedWriter out) throws IOException{
		while (it.hasNext()) {
			Entry<String, TypeVO> pairs = it.next();
			TypeVO tvo = pairs.getValue();
			
			if(tvo.getPrimarytype().equalsIgnoreCase("ENUMERATION")){
				
				//out.write("#TYPE " + tvo.getName() + " - " + tvo.getPrimarytype() + "\r\n");
				out.write("ifc:" + tvo.getName() + "\r\n");
				out.write("\ta owl:Class ;" + "\r\n");//-> owl:Class
				out.write("\towl:oneOf (");
				for(int i = 0; i<tvo.getEnum_entities().size();i++){
					if(i!=tvo.getEnum_entities().size()-1)
						out.write("ifc:" + getNamedIndividual(tvo.getEnum_entities().get(i), tvo.getName()).getNamedIndividual() + ", ");//-> owl:oneOf (uniquely named individuals + label property)		
					else
						out.write("ifc:" + getNamedIndividual(tvo.getEnum_entities().get(i), tvo.getName()).getNamedIndividual());
				}
				out.write(") .\r\n\r\n");
			}
			
			else if(tvo.getPrimarytype().equalsIgnoreCase("SELECT")){
				//ignored here			
			}
			else {
				String type = tvo.getPrimarytype();
				if (type.startsWith("ARRAY") || type.startsWith("SET") || type.startsWith("LIST")){
					//list
					String startIndex = type.substring(type.indexOf('[')+1, type.indexOf('[')+2);
					String endIndex = type.substring(type.indexOf(']')-1, type.indexOf(']'));
					String[] cList = type.split(" ");
					String content = cList[cList.length-1];
					
					out.write("ifc:"+tvo.getName()+ "\r\n");
					out.write("\ta owl:Class ;"+ "\r\n");
					out.write("\trdfs:subClassOf olo:OrderedList ;"+ "\r\n");
					if(!endIndex.equalsIgnoreCase("?")){
						out.write("\trdfs:subClassOf ["+ "\r\n");
						out.write("\t\ta owl:Restriction, owl:Class ;"+ "\r\n");
						out.write("\t\towl:allValuesFrom [ "+ "\r\n");
						out.write("\t\t\ta rdfs:Datatype ;"+ "\r\n");
						out.write("\t\t\towl:oneOf (\""+startIndex+"\"^^xsd:int \""+endIndex+"\"^^xsd:int)"+ "\r\n");
						out.write("\t\t\t] ;"+ "\r\n");
						out.write("\t\towl:onProperty olo:length ;"+ "\r\n");
					}
					out.write("\trdfs:subClassOf ["+ "\r\n");
					out.write("\t\ta owl:Restriction ;"+ "\r\n");
					out.write("\t\towl:allValuesFrom ifc:"+tvo.getName()+"_Slot ;"+ "\r\n");
					out.write("\t\towl:onProperty olo:slot"+ "\r\n");
					out.write("\t] ."+ "\r\n"+ "\r\n");
					
					out.write("ifc:"+tvo.getName()+"_Slot a owl:Class ;"+ "\r\n");
					out.write("\trdfs:subClassOf olo:Slot ;"+ "\r\n");
					out.write("\trdfs:subClassOf ["+ "\r\n");
					out.write("\t\ta owl:Restriction ;"+ "\r\n");
					out.write("\t\towl:allValuesFrom ifc:"+content+ " ;\r\n");
					out.write("\t\towl:onProperty olo:item"+ "\r\n");
					out.write("\t\t] ."+ "\r\n"+ "\r\n");
				}
				else if(ExpressReader.isAllUpper(type)){
					//primaryType
					out.write("ifc:"+tvo.getName() + "\r\n");
					out.write("\ta owl:Class ;" + "\r\n");
					out.write("\trdfs:subClassOf [ " +"\r\n");
					out.write("\t\ta owl:Restriction ;" + "\r\n");
					out.write("\t\towl:allValuesFrom ifc:"+tvo.getPrimarytype()+" ;" + "\r\n");
					out.write("\t\towl:onProperty ifc:hasContent" + "\r\n");
					out.write("\t\t] ." + "\r\n" + "\r\n");
				}
				else{
					//typeVO
					out.write("ifc:"+tvo.getName() + "\r\n");
					out.write("\ta owl:Class ;" + "\r\n");
					out.write("\trdfs:subClassOf [ " +"\r\n");
					out.write("\t\ta owl:Restriction ;" + "\r\n");
					out.write("\t\towl:allValuesFrom ifc:"+tvo.getPrimarytype()+" ;" + "\r\n");
					out.write("\t\towl:onProperty ifc:hasContent" + "\r\n");
					out.write("\t\t] ." + "\r\n" + "\r\n");
				}
			}
		}
	}
	
	private NamedIndividualVO getNamedIndividual(String originalIndividualName, String enumName){
		for(NamedIndividualVO ni : enumIndividuals){
			if(ni.getOriginalNameOfIndividual() == originalIndividualName && ni.getEnumName() == enumName){
				return ni;
			}
		}
		return null;		
	}
	
	private String getOwl_header(){
		String s = "";
		s += "@prefix xsd: <" + Namespace.XSD + "> .\r\n"
				+ "@prefix owl: <" + Namespace.OWL + "> .\r\n"
				+ "@prefix rdfs: <" + Namespace.RDFS + "> .\r\n"
				+ "@prefix list: <" + Namespace.LIST + "> .\r\n"
				+ "@prefix dce: <" + Namespace.DCE + "> .\r\n"
				+ "@prefix dct: <" + Namespace.DCT + "> .\r\n"
				+ "@prefix rdf: <" + Namespace.RDF + "> .\r\n"
				+ "\r\n" + "ifc:\r\n" + "	a owl:Thing ;\r\n"
				+ "	a owl:Ontology ;\r\n" + "	dce:title \"\"\""+expressSchemaName +"\"\"\"@en ;\r\n"
				+ "	dce:format \"\"\"OWL Full\"\"\"@en ;\r\n"
				+ " 	dce:identifier \"\"\"ifc\"\"\"@en ;\r\n"
				+ "	dce:language \"\"\"English\"\"\"@en .\r\n" + "\r\n";
		return s;
	}
}
