package org.buildingsmart;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import fi.ni.rdf.Namespace;
import org.buildingsmart.vo.AttributeVO;
import org.buildingsmart.vo.EntityVO;
import org.buildingsmart.vo.InverseVO;
import org.buildingsmart.vo.NamedIndividualVO;
import org.buildingsmart.vo.PrimaryTypeVO;
import org.buildingsmart.vo.PropertyVO;
import org.buildingsmart.vo.TypeVO;

/*
 * ExpressReader reads EXPRESS file11 specification of the IFC files and creates 
 * an internal representation of it.
 * 
 * The usage:
 * ExpressReader er = new ExpressReader(file name);
 * 
 *  - getEntities() - gives map of Entities in IFC
 *  - getTypes()    - gives map of Types in IFC
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

public class ExpressReader {

	private String expressSchemaName;
	private String expressFile;
	
	private static BufferedWriter logger = null;
	
	private Map<String, EntityVO> entities = new HashMap<String, EntityVO>();
	private Map<String, TypeVO> types = new HashMap<String, TypeVO>();
	private List<NamedIndividualVO> enumIndividuals = new ArrayList<NamedIndividualVO>();;
	//private Map<String, String> interfaces = new HashMap<String, String>();
	private Map<String, AttributeVO> attributes = new HashMap<String, AttributeVO>();
	private Map<String, PropertyVO> properties = new HashMap<String, PropertyVO>();	
	private Map<String, Set<String>> siblings = new HashMap<String, Set<String>>();
	//private Map<String, String> interface_aliases = new HashMap<String, String>();
	
	private ArrayList<TypeVO> selectTypesToExpand_temp = new ArrayList<TypeVO>(); 
	
	public ExpressReader(String expressSchemaName, String fileName) {
		Namespace.IFC = "http://buildingsmart.org/ontology/"+expressSchemaName+"#";
		this.expressSchemaName = expressSchemaName; 
		this.expressFile = fileName;	
		
		//prepare dirs
		File theDir = new File("src\\org\\buildingsmart\\"+expressSchemaName);
		if (!theDir.exists())
			theDir.mkdir();
//		File theDirInt = new File("src\\org\\buildingsmart\\"+expressSchemaName+"\\interfaces");
//		if (!theDirInt.exists())
//			theDirInt.mkdir();
	}

	public static void main(String[] args) throws IOException {		
		
        try {
            //create a temporary file
            String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            File logFile = new File("out//log_" + timeLog + ".txt");

            // This will output the full path where the file will be written to...
            System.out.println(logFile.getCanonicalPath());

            logger = new BufferedWriter(new FileWriter(logFile));            
        
		ExpressReader er = new ExpressReader("IFC4RC4","samples\\IFC4RC4.exp");//"samples\\"+outputschema+".exp");
		er.readSpec();
		er.buildExpressStructure();
		er.rearrangeAttributes();
		er.rearrangeProperties();
		er.unpackSelectTypes();
//		er.printIFCClassesInLog();
		
//		JAVAWriter jw = new JAVAWriter(er.expressSchemaName, er.entities, er.interfaces, er.types, er.interface_aliases);
//		jw.outputJavaClasses();

		OWLWriter ow = new OWLWriter(er.expressSchemaName, er.entities, er.types, er.siblings, er.enumIndividuals, er.properties);
//		ow.outputRDFS();
		ow.outputOWL();
		//FileWriter fw = new FileWriter("out\\"+outputschema+".n3");
		
		//BufferedWriter out = new BufferedWriter(fw);
		//er.outputRDFS(out);
		//er.outputOWL(out);
		
		
//		try {
//			out.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		out.close();		

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                logger.close();
            } catch (Exception e) {
            }
        }
	}
	
	private void rearrangeProperties() {
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = pairs.getValue();
			for (int n = 0; n < evo.getAttributes().size(); n++) {
				AttributeVO attr = evo.getAttributes().get(n);
				TypeVO type = attr.getType();
				String type_primaryType = attr.getType().getPrimarytype();
				String type_name = attr.getType().getName();
				
				PropertyVO prop = new PropertyVO();
				prop.setName(attr.getName());
				prop.setOriginalName(attr.getOriginalName());
				prop.setDomain(attr.getDomain());
				prop.setList(attr.isList());
				prop.setRange(type_name);
				prop.setMinCardinality(attr.getMinCard());
				prop.setMaxCardinality(attr.getMaxCard());
				
				//TODO: check whether we do not have a list of selects
				if(type_primaryType.equalsIgnoreCase("enumeration")) prop.setType(PropertyVO.propertyType.TypeVO);
				else if (type_primaryType.equalsIgnoreCase("select")){
					prop.setType(PropertyVO.propertyType.Select);
					prop.setSelectEntities(type.getSelect_entities());					
				}
				else if (type_primaryType.equalsIgnoreCase("class")) prop.setType(PropertyVO.propertyType.EntityVO);					
				else if(PrimaryTypeVO.getPrimaryTypeVO(type_primaryType) != null) prop.setType(PropertyVO.propertyType.TypeVO);
				else {prop.setType(PropertyVO.propertyType.TypeVO);
				//System.out.println("Found an alternative range type : "+ type_primaryType);
				}
				
				properties.put(prop.getName(),prop);				
			}
			
			//inverses TODO
			for (int n = 0; n < evo.getInverses().size(); n++) {
				InverseVO inv = evo.getInverses().get(n);
				String property = formatProperty(inv.getName());			
				System.out.println("parsing inverse property : " + property);
				
				PropertyVO t = properties.get(property);
				if (t == null) {
					System.out.println("Making new property : "+property);
					t = new PropertyVO();
					t.setName(property); // not the correct name? Inverse property?
					t.setList(true); //TODO not always a list
					//t.setIdentity(true); //former isIdentity property
					t.setRange(evo.getInverses().get(n).getClassRange());
					t.setDomain(evo);
					PropertyVO inverseOfInv = properties.get(inv.getInverseOfProperty());
					if(inverseOfInv!=null){
						t.setInverseProp(inverseOfInv);
						inverseOfInv.setInverseProp(t);
					}
					else{
						System.out.println("Warning: inverses not added for " + t.getName());
					}
					//t.addIfcClass(evo.getName());
					//property, true, true, evo
						//	.getInverses().get(n).getIfc_class());
					properties.put(property, t);
				}
			}
		}		
	}
	
	private void rearrangeAttributes() throws IOException{
		logger.write("Generating list of attributes for : " + attributes.size() + "attributes \r\n");
		ArrayList<String> doublegeneratedattributes = new ArrayList<String>(); 
		HashMap<String,AttributeVO> alreadygeneratedattributes = new HashMap<String, AttributeVO>();
		Iterator<Entry<String, EntityVO>> iter = entities.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, EntityVO> pairs = iter.next();
			EntityVO evo = pairs.getValue();
			for (int n = 0; n < evo.getAttributes().size(); n++) {
				AttributeVO attr = evo.getAttributes().get(n);
				attr.setDomain(evo);
				
				if(!doublegeneratedattributes.contains(attr.getName())){
					if(alreadygeneratedattributes.containsKey(attr.getName())){
						doublegeneratedattributes.add(attr.getName());
						AttributeVO firstattr = (AttributeVO)alreadygeneratedattributes.get(attr.getName());
						firstattr.setOriginalName(firstattr.getName());
						firstattr.setName(firstattr.getDomain().getName() + "_" + firstattr.getName());
						attr.setOriginalName(attr.getName());
						attr.setName(attr.getDomain().getName() + "_" + attr.getName());
					}
					else
					{
						//no name change
						alreadygeneratedattributes.put(attr.getName(), attr);
						attr.setOriginalName(attr.getName());
					}
				}
				else
				{
					attr.setOriginalName(attr.getName());
					attr.setName(evo.getName() + "_" + attr.getName());
					//enumIndividuals.add(new NamedIndividualVO(vo.getName(), vo.getName() + "_" + vo.getEnum_entities().get(n), vo.getEnum_entities().get(n)));
				}
				//write outcome
				//logger.write(evo.getName() +" - " + attr.getName() + " - " + attr.getType().getName()  + " --- isSet: " + attr.isSet() + " - isList: " + attr.isList() + "\r\n");
			}			
		}
//		Iterator<Entry<String, EntityVO>> iter1 = entities.entrySet().iterator();
//		while (iter1.hasNext()) {
//			Entry<String, EntityVO> pairs = iter1.next();
//			EntityVO evo = pairs.getValue();
//			for (int n = 0; n < evo.getAttributes().size(); n++) {
//				AttributeVO attr = evo.getAttributes().get(n);
//				logger.write(evo.getName() +" - " + attr.getName() + " - " + attr.getType().getName()  + " --- isSet: " + attr.isSet() + " - isList: " + attr.isList() + "\r\n");
//			}			
//		}
	}
	
	private void unpackSelectTypes(){
		//if a select type is referred to by another select type -> replace it by the elements of the latter!
		for (Map.Entry<String, TypeVO> entry : types.entrySet()) {
			TypeVO vo = entry.getValue();
			for (int n = 0; n < vo.getSelect_entities().size(); n++) {
				String selectEnt = vo.getSelect_entities().get(n);				
				for(int i = 0; i<selectTypesToExpand_temp.size();i++){
					if(selectTypesToExpand_temp.get(i).getName().equalsIgnoreCase(selectEnt)){
//						System.out.println("selecttype found in temp : " + selectEnt);
						vo.getSelect_entities().remove(n);
						for(String sel : selectTypesToExpand_temp.get(i).getSelect_entities())
							vo.getSelect_entities().add(sel);
//						current_type.setPrimarytype(formatClassName(txt));
//						System.out.println("setPrimarytype"+txt);
//						interface_aliases.put(current_type.getName(),
//								vo.getName());
////						vo.getSelect_entities().remove(n);
//						current_type = vo;
//						break;
					}
				}
			}
		}
	}
	
	//CONVERTING
	private void readSpec() {		
		try {
			//parsing file
			FileInputStream fstream = new FileInputStream(expressFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			try {
				String strLine;
				while ((strLine = br.readLine()) != null) {
					if (strLine.length() > 0) {
						parse_level(strLine);
					}
				}
			} finally {
				br.close();
			}
		}
		catch (FileNotFoundException fe) {
		    System.err.println("The IFC Express file "+expressFile+" is missing.");
		    System.exit(1);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("ended parsing the EXPRESS file");
	}
	
	private void buildExpressStructure() throws IOException{
		generateNamedIndividuals();
		generate_derived_attribute_list();
		generate_derived_inverse_list();	
		iterate();
	}
	
	private void generateNamedIndividuals() throws IOException{
		logger.write("Generating named individuals" + "\r\n");
		ArrayList<String> doublegeneratednamedindividuals = new ArrayList<String>(); 
		HashMap<String,NamedIndividualVO> alreadygeneratednamedindividuals = new HashMap<String, NamedIndividualVO>();
		for (Map.Entry<String, TypeVO> entry : types.entrySet()) {
			TypeVO vo = entry.getValue();
			logger.write("found : " + vo.toString() + "\r\n");
			// List of all TYPE:SELECT -entries
			for (int n = 0; n < vo.getSelect_entities().size(); n++) {
				EntityVO evo = entities.get(vo.getSelect_entities().get(n));
				if (evo != null) {
					logger.write("EntityVO element found for " + vo.getName() + " : " + evo.getName() + "\r\n");
//					evo.getInterfaces().add(vo.getName());
//					interfaces.put(formatClassName(vo.getName()), vo.getName());
				}
				else{
					logger.write("No entityVO element found for " + vo.getSelect_entities().get(n) + " ( " + vo.getName() + " ) --> the SELECT type refers to another Type" + "\r\n");
				}
			}
			for(int n = 0;  n < vo.getEnum_entities().size(); n++) {
				if(!doublegeneratednamedindividuals.contains(vo.getEnum_entities().get(n))){
					if(alreadygeneratednamedindividuals.containsKey(vo.getEnum_entities().get(n))){
						doublegeneratednamedindividuals.add(vo.getEnum_entities().get(n));
						NamedIndividualVO firstind = (NamedIndividualVO)alreadygeneratednamedindividuals.get(vo.getEnum_entities().get(n));
						firstind.setNamedIndividual(firstind.getEnumName()+"_"+firstind.getOriginalNameOfIndividual());
						enumIndividuals.add(new NamedIndividualVO(vo.getName(), vo.getName() + "_" + vo.getEnum_entities().get(n), vo.getEnum_entities().get(n)));
					}
					else
					{
						NamedIndividualVO ind = new NamedIndividualVO(vo.getName(), vo.getEnum_entities().get(n));
						enumIndividuals.add(ind);
						alreadygeneratednamedindividuals.put(vo.getEnum_entities().get(n), ind);
					}
				}
				else
					enumIndividuals.add(new NamedIndividualVO(vo.getName(), vo.getName() + "_" + vo.getEnum_entities().get(n), vo.getEnum_entities().get(n)));
			}
		}
		logger.write("Named individuals generated" + "\r\n" + "\r\n");
	}
	
	private void generate_derived_attribute_list() throws IOException {
		logger.write("Generating derived attribute list" + "\r\n");
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = pairs.getValue();
			logger.write("found : " + evo.toString() + "\r\n");
			add_attribute_entries(evo, evo);
		}
		logger.write("derived attribute list generated" + "\r\n" + "\r\n");
	}
	
	private void add_attribute_entries(EntityVO evo, EntityVO top) throws IOException {
		if (evo.getSuperclass() != null) {
			EntityVO sup = entities.get(formatClassName(evo.getSuperclass()));
			if (sup != null)
				add_attribute_entries(sup, top);
		}

		for (int n = 0; n < evo.getAttributes().size(); n++) {
			attributes.put(top.getName() + "#"
					+ evo.getAttributes().get(n).getName(), evo.getAttributes()
					.get(n));
			logger.write("added attribute : " + top.getName() + "#"
					+ evo.getAttributes().get(n).getName() + " - " + evo.getAttributes()
					.get(n) + "\r\n");
			top.getDerived_attribute_list().add(evo.getAttributes().get(n));
		}
	}
		
	private void generate_derived_inverse_list() throws IOException {
		logger.write("Generating derived inverse list" + "\r\n");
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = (EntityVO) pairs.getValue();
			logger.write("found : " + evo.toString() + "\r\n");
			add_inverse_entries(evo, evo);
		}
		logger.write("derived inverse list generated" + "\r\n" + "\r\n");
	}
	
	private void add_inverse_entries(EntityVO evo, EntityVO top) throws IOException {
		if (evo.getSuperclass() != null) {
			EntityVO sup = entities.get(evo.getSuperclass());
			if (sup != null)
				add_inverse_entries(sup, top);
		}

		for (int n = 0; n < evo.getInverses().size(); n++) {
			AttributeVO avo = attributes.get(evo.getInverses().get(n)
					.getClassRange()
					+ "#" + evo.getInverses().get(n).getInverseOfProperty());
			logger.write("found attributeVO : " + avo.toString() + "\r\n");
			boolean unique = false;
			if (avo != null) {
				InverseVO ivo = evo.getInverses().get(n);
				logger.write("found inverseVO : " + ivo.toString() + "\r\n");
				if (ivo.isOne_valued())
					if (!avo.isSet())
						avo.setOne2One(true);
				if (avo.isUnique())
					unique = true;
				avo.setReverse_pointer(true);
				avo.setPoints_from(evo.getInverses().get(n));
			} 
//				else
//				System.err.println("reverse not found:"
//						+ evo.getInverses().get(n).getIfc_class() + "#"
//						+ evo.getInverses().get(n).getProperty());
			evo.getInverses().get(n).setUnique(unique);
			top.getDerived_inverse_list().add(evo.getInverses().get(n));
		}
	}
	
	private void iterate() throws IOException {
		logger.write("iterating through all entities" + "\r\n");
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = pairs.getValue();
			if (evo.getSuperclass() != null) {
				logger.write(pairs.getKey() + " s:" + evo.getSuperclass() + "\r\n");
				EntityVO sup = entities.get(evo.getName());
				if (sup != null)
					printEntity(sup);
			} else
				logger.write(pairs.getKey() + "\r\n");

			printEntity(evo);
		}
		logger.write("iterated through all entities" + "\r\n" + "\r\n");
	}	
	
	private void printEntity(EntityVO evo) throws IOException {
		if (evo.getSuperclass() != null) {
			EntityVO sup = entities.get(evo.getSuperclass().toLowerCase());
			if (sup != null)
				printEntity(sup);
		}

		for (int n = 0; n < evo.getAttributes().size(); n++) {
			logger.write("   " + evo.getName() + " , "
					+ evo.getAttributes().get(n) + "\r\n");
		}
	}
	
	private void printIFCClassesInLog() throws IOException {
		logger.write("iterating through all IFC Classes" + "\r\n");
		
		Iterator<Entry<String, EntityVO>> it = entities.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, EntityVO> pairs = it.next();
			EntityVO evo = pairs.getValue();
			if (evo.getSuperclass() != null)
				logger.write(pairs.getKey() + " s:" + evo.getSuperclass() + "\r\n");
			else
				logger.write(pairs.getKey() + "\r\n");

			for (int n = 0; n < evo.getDerived_attribute_list().size(); n++) {
				if (evo.getDerived_attribute_list().get(n).isReverse_pointer())
					logger.write(" "
							+ evo.getDerived_attribute_list().get(n).getType()
									.getName()
							+ ":"
							+ evo.getDerived_attribute_list().get(n).getType()
									.getPrimarytype() + "   "
							+ evo.getDerived_attribute_list().get(n).getName()
							+ " CAN BE REVERSED" + "\r\n");
				else
					logger.write(" "
							+ evo.getDerived_attribute_list().get(n).getType()
									.getName()
							+ ":"
							+ evo.getDerived_attribute_list().get(n).getType()
									.getPrimarytype() + "   "
							+ evo.getDerived_attribute_list().get(n).getName() + "\r\n");
			}
			for (int n = 0; n < evo.getDerived_inverse_list().size(); n++) {
				logger.write(" i:"
						+ evo.getDerived_inverse_list().get(n).getName() + "\r\n");
			}

			logger.write("iterated through all IFC Classes" + "\r\n" + "\r\n");
		}
	}
		
	//FORMATTING and BUILDING
	static public String formatClassName(String s) {
		if (s == null)
			return null;
		return filter_extras(s).toUpperCase();
	}

	static public String formatProperty(String s) {
		if (s == null)
			return null;

		StringBuffer sb = new StringBuffer();
		sb.append(Character.toLowerCase(s.charAt(0)));
		sb.append(s.substring(1));
		return sb.toString();
	}

	private boolean isSubClassOf_this(EntityVO evo, String superclass_name) {
		if (evo.getName().equalsIgnoreCase(superclass_name))
			return true;
		if (evo.getSuperclass() != null) {
			EntityVO sup = entities.get(evo.getSuperclass());
			if (sup != null) {
				if (isSubClassOf_this(sup, superclass_name))
					return true;
			}
		}
		return false;
	}

	public boolean isSubClassOf(String class_name, String superclass_name) {
		return isSubClassOf_this(entities.get(class_name), superclass_name);
	}

	
	//STATE_MACHINE FOR PARSING EXPRESS FILES
	private static final int INIT_STATE = 0;
	private static final int TYPE_STATE = 1;
	private static final int TYPE_SWITCH = 101;
	private static final int TYPE_SELECT = 102;
	private static final int TYPE_ENUMERATION = 103;
	private static final int TYPE_ENUMERATION_OF = 104;
	private static final int TYPE_LIST = 105;

	private static final int ENTITY_STATE = 2;
	private static final int ENTITY_READY = 201;
	private static final int ENTITY_SUBTYPE_STATE = 3;
	private static final int ENTITY_SUBTYPE_OF_STATE = 4;
	private static final int ENTITY_UNIQUE = 50;
	private static final int ENTITY_UNIQUE_TYPE = 51;
//	private static final int ENTITY_ATTRIBUTE = 6;
	private static final int ENTITY_WHERE = 7;
	private static final int ENTITY_DERIVE = 8;
	private static final int ENTITY_SUPERTYPE = 90;
	private static final int ENTITY_SUPERTYPE_OF_ONEOF = 91;
	private static final int ENTITY_NAME_STATE = 11;
	private static final int ENTITY_INVERSE_STATE = 111;
	private static final int ENTITY_INVERSE_SET_OF = 112;
	private static final int ENTITY_INVERSE_FOR = 113;

	private int state = INIT_STATE;
	private EntityVO current_entity = null;
	private String tmp_inverse_name;
	private String tmp_inverse_classnamerange;
	private String tmp_inverse_inverseprop;
	private boolean tmp_inverse_is_one_valued = false;

	private String tmp_entity_name;
	private String tmp_entity_type;
	private TypeVO current_type;
	private Set<String> current_sibling_set;

	private boolean is_set = false;
	private boolean is_list = false;
	private int tmp_mincard = -1; //cardinality of the targeted list, not of the property
	private int tmp_maxcard = -1; //cardinality of the targeted list, not of the property
	private boolean is_optional = false;

	private void state_machine(String txt) {
		
		switch (state) {
		case INIT_STATE:
			if (txt.equalsIgnoreCase("TYPE"))
				state = TYPE_STATE;
			if (txt.equalsIgnoreCase("ENTITY"))
				state = ENTITY_NAME_STATE;
			if (txt.equalsIgnoreCase("FUNCTION")){
				break;
			}
			if (txt.equalsIgnoreCase("RULE")){
				break;
			}
			if (txt.equalsIgnoreCase("END_SCHEMA;"))
				break;
			break;

		//1. TYPE
		case TYPE_STATE:
			if (txt.endsWith("=")) {
				state = TYPE_SWITCH;
			} else {
				String txt_t = formatClassName(txt);
				TypeVO t = types.get(txt_t);
				if (t == null) {
					current_type = new TypeVO(txt);
					types.put(txt_t, current_type);
				}
			}
			break;

		case TYPE_SWITCH:
			if (txt.equalsIgnoreCase("SELECT")) {
				state = TYPE_SELECT;
				selectTypesToExpand_temp.add(current_type);
				current_type.setPrimarytype(formatClassName(txt));

				//check if we are not missing anything here -> this was commented without checking consequences
				
//				ArrayList<String> doublegeneratedattributes = new ArrayList<String>(); 
//				HashMap<String,AttributeVO> alreadygeneratedattributes = new HashMap<String, AttributeVO>();
//				for (Map.Entry<String, TypeVO> entry : types.entrySet()) {
//					TypeVO vo = entry.getValue();
//					// List of all TYPE:SELECT -entries
//					for (int n = 0; n < vo.getSelect_entities().size(); n++) {
//						if (current_type.getName().equalsIgnoreCase(
//								vo.getSelect_entities().get(n))) {
//							//We encounter a case where a select is mentioned within another select
//							System.out.println("SELECT type "+ current_type.getName() +" referenced by "+vo.getSelect_entities().get(n));
//							current_type.setPrimarytype(formatClassName(txt));
//							System.out.println("setPrimarytype"+txt);
//							interface_aliases.put(current_type.getName(),
//									vo.getName());
////							vo.getSelect_entities().remove(n);
//							current_type = vo;
//							break;
//						}
//					}
//				}

			} else if (txt.equalsIgnoreCase("ENUMERATION")) {
				state = TYPE_ENUMERATION;
			}
			else if(isAllUpper(txt)){
				if(txt.startsWith("ARRAY") || txt.startsWith("SET") || txt.startsWith("LIST"))
					state = TYPE_LIST;
				else{
					// primarytypes like REAL/INTEGER/STRING/...
					new PrimaryTypeVO(formatClassName(txt));
					state = INIT_STATE;
				}
				txt = formatClassName(txt);
			}
			else {
				// references to TypeVOs
				if(txt.endsWith(";")) txt = txt.substring(0,txt.length()-1);
				state = INIT_STATE;
			}
			current_type.setPrimarytype(txt);
			break;
			
		case TYPE_LIST:
			//LIST [3:4] OF INTEGER
			//ARRAY [1:2] OF REAL;
			//SET [1:?] OF IfcPropertySetDefinition;
//			if (txt.endsWith("]")&&txt.startsWith("[")){
//				//[3:4] or similar parsed
//				String[] tempCards = txt.split(":");
//				String mincard = txt.split(":")[0].substring(1);
//				String maxcard = txt.split(":")[1].substring(0, tempCards[1].length()-1);
//				int minc = -1;
//				int maxc = -1;
//				if(!mincard.equalsIgnoreCase("?"))
//					minc = Integer.parseInt(mincard);
//				if(!maxcard.equalsIgnoreCase("?"))
//					maxc = Integer.parseInt(maxcard);
//				current_type.setListCardinalities(new int[]{minc,maxc});
//				current_type.setListCardinalities(new int[]{(int)tempCards[0].substring(1), (int)tempCards[1].substring(0, tempCards[1].length()-1)});
//			}
//			else 
			if (!txt.endsWith(";")) {
				if (current_type != null)
					current_type.setPrimarytype(current_type.getPrimarytype() + " " + txt);
			} else {
				if (current_type != null)
					current_type.setPrimarytype(current_type.getPrimarytype() + " " + txt);
				state = INIT_STATE;
			}
			break;

		case TYPE_SELECT:
			if (txt.endsWith(";")) {
				String txt_t = filter_extras(txt);//formatClassName(txt);
				if (current_type != null)
					current_type.getSelect_entities().add(txt_t);
				state = INIT_STATE;
			} else {
				String txt_t = filter_extras(txt);//formatClassName(txt);
				if (current_type != null)
					current_type.getSelect_entities().add(txt_t);
			}
			break;

		case TYPE_ENUMERATION:
			if (txt.equals("OF")) {
				state = TYPE_ENUMERATION_OF;
			}
			break;

		case TYPE_ENUMERATION_OF:
			if (txt.endsWith(";")) {
				String txt_t = formatClassName(txt);
				if (current_type != null)
					current_type.getEnum_entities().add(txt_t);
				state = INIT_STATE;
			} else {
				String txt_t = formatClassName(txt);
				if (current_type != null)
					current_type.getEnum_entities().add(txt_t);
			}
			break;

		//2. ENTITY
		case ENTITY_NAME_STATE:
			// replaces all non-letter characters with nothing
			String org_name = txt;
			if (org_name.endsWith(";"))
				org_name = org_name.substring(0, org_name.length() - 1);
			String entity_name = ExpressReader.formatClassName(org_name);
			current_entity = entities.get(entity_name);
			if (current_entity == null) {
				current_entity = new EntityVO(org_name);
				entities.put(entity_name, current_entity);
			}
			state = ENTITY_STATE;
			break;

		case ENTITY_STATE:
			is_set = false;
			is_list = false;
			if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.equalsIgnoreCase("SUPERTYPE")) {
				state = ENTITY_SUPERTYPE;
			} else if (txt.equalsIgnoreCase("ABSTRACT")) {
				current_entity.setAbstractSuperclass(true);
				state = ENTITY_SUPERTYPE;
			} else if (txt.equalsIgnoreCase("INVERSE")) {
				state = ENTITY_INVERSE_STATE;
//			} else if (txt.equalsIgnoreCase(":")) {
//				state = ENTITY_ATTRIBUTE;
			} else if (txt.equalsIgnoreCase("UNIQUE")) {
				state = ENTITY_UNIQUE;
			} else if (txt.equalsIgnoreCase("WHERE")) {
				state = ENTITY_WHERE;
			} else if (txt.equalsIgnoreCase("DERIVE")) {
				state = ENTITY_DERIVE;
			} else if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else {
				tmp_entity_name = ExpressReader.formatProperty(txt);
				state = ENTITY_READY;
			}
			break;


		//2.1 PROPERTIES	
		case ENTITY_READY:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("OPTIONAL")) {
				is_optional = true;
			} else if (txt.equalsIgnoreCase("SET")) {
				is_set = true;
			} else if (txt.equalsIgnoreCase("LIST")) {
				is_set = true;
				is_list = true;
			} else if(txt.endsWith("]")&&txt.startsWith("[")){
//				//[3:4] or similar parsed
				String[] tempCards = txt.split(":");
				String mincard = txt.split(":")[0].substring(1);
				String maxcard = txt.split(":")[1].substring(0, tempCards[1].length()-1);
				if(!mincard.equalsIgnoreCase("?"))
					tmp_mincard = Integer.parseInt(mincard);
				if(!maxcard.equalsIgnoreCase("?"))
					tmp_maxcard = Integer.parseInt(maxcard);
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.contains(";")) {
				tmp_entity_type = ExpressReader.formatClassName(txt.substring(
						0, txt.length() - 1));
				TypeVO type = types.get(tmp_entity_type);
				if (type == null) {
					type = new TypeVO(txt.substring(0, txt.length() - 1),
							"CLASS");
				}
				current_entity.getAttributes()
						.add(new AttributeVO(tmp_entity_name, type, is_set,
								is_list,tmp_mincard,tmp_maxcard,is_optional));
				state = ENTITY_STATE;
			}
			break;	
			
		//2.2 SUBTYPE
		case ENTITY_SUBTYPE_STATE:
			if (txt.equalsIgnoreCase("OF"))
				state = ENTITY_SUBTYPE_OF_STATE;
			else
				state = ENTITY_STATE;
			break;
			
		case ENTITY_SUBTYPE_OF_STATE:
			current_entity.setSuperclass(filter_extras(txt));

			state = ENTITY_STATE;
			break;	
			
		//2.3 SUPERTYPE
		case ENTITY_SUPERTYPE:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.equalsIgnoreCase("(ONEOF")) {
				state = ENTITY_SUPERTYPE_OF_ONEOF;
				current_sibling_set = new HashSet<String>();
			} else {
				if (txt.contains(";"))
					state = ENTITY_STATE;
			}
			break;

		case ENTITY_SUPERTYPE_OF_ONEOF:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else {
				if (txt.contains(";")){
					current_entity.setSubClassList(current_sibling_set);
					state = ENTITY_STATE;					
				}
				if (txt.contains(")")){
					current_entity.setSubClassList(current_sibling_set);
					state = ENTITY_STATE;
				}
				String sibstr = filter_extras(txt);
				current_sibling_set.add(sibstr);
				@SuppressWarnings("rawtypes")
				Set s = this.siblings.get(sibstr);
				if (s != null)
					System.err.println("DUPLICATE: " + sibstr);
				else
					this.siblings.put(sibstr, current_sibling_set);
			}
			break;
			
		//2.4 INVERSE
		case ENTITY_INVERSE_STATE:
			is_set = false;
			if (txt.equalsIgnoreCase("WHERE")) {
				state = ENTITY_WHERE;
			}
			else if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.equalsIgnoreCase(":")) { 
				// the name of the inverse attribute
				state = ENTITY_INVERSE_SET_OF;
			} else
				tmp_inverse_name = ExpressReader.formatProperty(txt);
			break;

		case ENTITY_INVERSE_SET_OF:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.equalsIgnoreCase("SET")) {
				is_set = true;
			} else if (txt.equalsIgnoreCase("FOR")) {
				state = ENTITY_INVERSE_FOR;
			} else {
				if (txt.equals("[0:1]"))
					tmp_inverse_is_one_valued = true;
				if (txt.equals("[1:1]"))
					tmp_inverse_is_one_valued = true;

				tmp_inverse_classnamerange = txt;// ExpressReader.formatClassName(txt);
			}
			break;
			
		case ENTITY_INVERSE_FOR:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else if (txt.contains(";")) {

				tmp_inverse_inverseprop = ExpressReader.formatProperty(txt
						.substring(0, txt.length() - 1));
				current_entity.getInverses().add(
						new InverseVO(tmp_inverse_name, tmp_inverse_classnamerange,
								tmp_inverse_inverseprop, is_set,
								tmp_inverse_is_one_valued));
				state = ENTITY_INVERSE_STATE;
			}
			break;
				
		//2.5 UNIQUE RESTRICTIONS
		case ENTITY_UNIQUE:
			if (txt.equals(":"))
				state = ENTITY_UNIQUE_TYPE;
			else if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("WHERE")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			}
			break;

		case ENTITY_UNIQUE_TYPE:
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("WHERE")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			} else {
				if (!txt.contains(",")) {
					String unique_attribute = ExpressReader.formatProperty(txt
							.substring(0, txt.length() - 1));
					// System.out.println("A "+current_entity.getName()+"."+unique_attribute);

					for (int j = 0; j < current_entity.getAttributes().size(); j++) {
						AttributeVO ao = current_entity.getAttributes().get(j);
						if (ao.getName().equals(unique_attribute)) {
							ao.setUnique(true);
						}
					}
				}
				state = ENTITY_UNIQUE;
			}
			break;
					
//		case ENTITY_ATTRIBUTE:
//			if (txt.equalsIgnoreCase("END_ENTITY;")) {
//				state = INIT_STATE;
//			} else {
//				if (txt.contains(";"))
//					state = ENTITY_STATE;
//			}
//			break;
		
		//2.6 UNHANLDED WHERE AND DERIVE LINES	
		case ENTITY_WHERE:
			//not parsed
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			}
			break;
			
		case ENTITY_DERIVE:
			//not parsed
			if (txt.equalsIgnoreCase("END_ENTITY;")) {
				state = INIT_STATE;
			} else if (txt.equalsIgnoreCase("SUBTYPE")) {
				state = ENTITY_SUBTYPE_STATE;
			}
			break;

		default:
			// Do nothing
		}
	}

	static public String filter_extras(String txt) {
		StringBuffer sb = new StringBuffer();
		for (int n = 0; n < txt.length(); n++) {
			char ch = txt.charAt(n);
			switch (ch) {
			case '(':
				break;
			case ';':
				break;
			case ',':
				break;
			case ')':
				break;
			default:
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private void parse_level(String txt) {
		StringTokenizer st = new StringTokenizer(txt);
		while (st.hasMoreTokens()) {
			state_machine(st.nextToken());
		}
	}
	
	public static boolean isAllUpper(String s) {
	    for(char c : s.toCharArray()) {
	       if(Character.isLetter(c) && Character.isLowerCase(c)) {
	           return false;
	        }
	    }
	    return true;
	}
	
	//ACCESSORS	
	public Map<String, TypeVO> getTypes() {
		return types;
	}

	public void setTypes(Map<String, TypeVO> types) {
		this.types = types;
	}

	public Map<String, EntityVO> getEntities() {
		return entities;
	}

	public void setEntities(Map<String, EntityVO> entities) {
		this.entities = entities;
	}
}
