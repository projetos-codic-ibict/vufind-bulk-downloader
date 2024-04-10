package org.lareferencia.services.vufindbulkdownloader;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.list.FixedSizeList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.opencsv.CSVWriter;

public class FileUtils {
	
	private Map<String, Integer> columnIndexes = new HashMap<String, Integer>();
	
	// Check whether a field is the first element in an aggregated column
	private boolean isAggFieldHead (Map<String, List<String>> aggFields, String field){
		
		for (List<String> fields : aggFields.values()){
			if (fields.get(0).equals(field)) return true;
		}
		
		return false;
	}
	
	// Get the name of the aggregated column where a field is the head
	private String getAggFieldName (Map<String, List<String>> aggFields, String field){
		
		for (String aggField : aggFields.keySet()){
			if (aggFields.get(aggField).get(0).equals(field)) return aggField;
		}
		
		return null;
	}
	
	// Convert a list to a formatted string which will fill a single cell in the CSV
	private String listToString (List<String> list, String sep, String nullMsg){
		
		String strList = "";
		boolean empty = true;
		boolean showNullMsg = true;
		
		// Test if list only contains empty values...
		for (String item : list){
			if (!item.equals("")){
				empty = false;
				break;
			}
		}
		
		// ... or only the null message
		for (String item : list){
			if (!item.equals(nullMsg)){
				showNullMsg = false;
				break;
			}
		}
		
		if (showNullMsg) {
			strList = nullMsg;
		}
		else if (!empty){
			for (String item : list){
				strList += item + sep;
			}
			strList = strList.substring(0, strList.length() - sep.length()); //remove last separator
		}	
		
		return strList;
	}
	
	// Convert a JSON response into a CSV-ready structure
	@SuppressWarnings("unchecked")
	public List<List<String>> JSONtoCSV (String json, Map<String, String> fieldList, List<String> userFields, 
			Map<String, List<String>> aggFields, String listSep, String nullMsg, List<String> noMsgFields){
		
		List<List<String>> csv = new ArrayList<List<String>>();
		Set<String> fields = fieldList.keySet();	
		//Add header
		List<String> header = new ArrayList<String>();
		int colIndex = 0;
		
		for (String field : fields){
			if (userFields.contains(field)){
				String label = fieldList.get(field);
				if (!label.equals("null")){
					
					header.add(label);
					columnIndexes.put(label, colIndex++);
				}
				else{
					if (isAggFieldHead(aggFields, field)){ // the first field in an aggregated column, set aggregating field name as header
						label = getAggFieldName(aggFields, field);
						header.add(label);
						columnIndexes.put(label, colIndex++);
					}
				}
			}
		}
		csv.add(header);
		
		//Get all field values
		try {
            JSONParser parser = new JSONParser();
            Object resultObject = parser.parse(json);

            if (resultObject instanceof JSONObject) {
                JSONObject object = (JSONObject) resultObject;
                JSONObject response = (JSONObject) object.get("response");
                List<JSONObject> docs = (List<JSONObject>) response.get("docs"); 
                
                for (JSONObject doc : docs){
                	List<String> line = FixedSizeList.decorate(Arrays.asList(new String[header.size()]));
                	Map<String, String> toAggregate = new HashMap<String, String>();
	                
                	for (String field : fields){
                		if (userFields.contains(field)){ //user selected this field for export
                			Object attribute = doc.get(field);
                			String label  = fieldList.get(field);
                			int index = label.equals("null") ? -1 : columnIndexes.get(fieldList.get(field));

                			if (attribute == null){ //doc has no such field
                				if (label.equals("null")){ //empty field to be aggregated, save for later retrieval
                					if (!noMsgFields.contains(field)) {
                						toAggregate.put(field, nullMsg);
                					}
                					else {
                						toAggregate.put(field, "");
                					}
                				}
                				else{
                					if (!noMsgFields.contains(field)) { //a custom message should be shown instead of a blank
                						line.set(index, nullMsg);
                					}
                					else {
                						line.set(index, "");
                					}	
                				}	
                			}
                			else{
                				if (attribute instanceof JSONArray){ //field contains a list
                					JSONArray items = (JSONArray) attribute;
                					List<String> jsonList = new ArrayList<String>();
                					
                					for (Object item : items){
                						jsonList.add((String) item);
                					}  
                					
                					if (label.equals("null")){ //list field to be aggregated, save for later retrieval
                						toAggregate.put(field, listToString(jsonList, listSep, nullMsg));
                					}
                					else{
                						line.set(index, listToString(jsonList, listSep, nullMsg));
                					}	
                				}
                				else{
                					if (label.equals("null")){ //single-value field to be aggregated, save for later retrieval
                    					toAggregate.put(field, (String) attribute);
                    				}
                					else{
                						line.set(index, (String) attribute);
                					}	
                				}	
                			}	
                		}
                	}
                	
                	// Process aggregated fields
                	for (String aggField : aggFields.keySet()){
                		int index = columnIndexes.get(aggField);
                		List<String> items = new ArrayList<String>();
                		
                		for (String entry : aggFields.get(aggField)){
                			items.add(toAggregate.get(entry));
                		}
                		line.set(index, listToString(items, listSep, nullMsg));
                	}
					
                	csv.add(line);
                }
            }

        } 
        catch (Exception e) {
        	e.printStackTrace();;
        }
		
		return csv;
	}

	private String formatArrays(JSONArray receivedArray) {
		StringBuilder formattedArray = new StringBuilder();
		if(receivedArray == null){
			return "-";
		}
		for (Object authorObj : receivedArray) {
			String author = (String) authorObj;
			formattedArray.append(author).append("; ");
		}
		if (formattedArray.length() > 0) {
			formattedArray.setLength(formattedArray.length() - 2);
		}
		return formattedArray.toString();
	}

	@SuppressWarnings({ "unchecked", "unlikely-arg-type" })
	public String JSONtoRIS (String json, Map<String, String> fieldListRIS, List<String> userFields, 
			 String listSep, String nullMsg, List<String> noMsgFields){
				StringBuilder ris = new StringBuilder();
				
				try {
					JSONParser parser = new JSONParser();
					Object resultObject = parser.parse(json);
					if (resultObject instanceof JSONObject) {
						JSONObject object = (JSONObject) resultObject;
						JSONObject response = (JSONObject) object.get("response");
						JSONArray docs = (JSONArray) response.get("docs");
						
							for (Object docObject : docs) {
								JSONObject doc = (JSONObject) docObject;
								
								
								String TI = (String) doc.get("title");

								
								
								JSONArray PYArray = (JSONArray) doc.get("publishDate");
								String PY = formatArrays(PYArray);
								JSONArray TYArray =  (JSONArray) doc.get("format");
								String TY = formatArrays(TYArray);
								
								String ISSN = (String) doc.get("dc.identifier.issn.pt_BR.fl_str_mv");
								String ISBN = (String) doc.get("dc.identifier.isbn.pt_BR.fl_str_mv");

								JSONArray LAArray = (JSONArray) doc.get("dc.language.iso.fl_str_mv");
								String LA = formatArrays(LAArray);
								
								String AB = (String) doc.get("description");
								JSONArray DIArray = (JSONArray) doc.get("identifier_str_mv");
								String DI = formatArrays(DIArray);
								
								JSONArray URArray = (JSONArray) doc.get("dc.identifier.uri.fl_str_mv");
								String UR = formatArrays(URArray);
								String JO = (String) doc.get("reponame_str");
											
								
								ris.append("TY  - ").append(TY).append("\n");

								JSONArray AUArray = (JSONArray) doc.get("author_facet");
								
								int autoNumber = AUArray.size();
								for(int e = 0;e < autoNumber;e++){
									ris.append("AU  - ").append(AUArray.get(e)).append("\n");
								}

								ris.append("PY  - ").append(PY).append("\n");
								ris.append("TI  - ").append(TI).append("\n");
								ris.append("LA  - ").append(LA).append("\n");
								if(ISSN != null){
									ris.append("SN  - ").append(ISSN).append("\n");
								}else if (ISBN != null) {
									ris.append("SN  - ").append(ISBN).append("\n");
								} else{
									ris.append("SN  - ").append("-").append("\n");
								}
								if(AB != null){
									ris.append("AB  - ").append(AB).append("\n");
								}else{
									ris.append("AB  - ").append("-").append("\n");
								}
								
								ris.append("DI  - ").append(DI).append("\n");
								ris.append("UR  - ").append(UR).append("\n");
								ris.append("JO  - ").append(JO).append("\n");
								ris.append("ER  - \n\n");
							}
						
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return ris.toString();

			}

	
	// Create a zip file containing the CSV file
	private void compressFile (String inputfile){
		File file = new File(inputfile);
		File compressed = new File(inputfile.replace(".csv", ".zip"));
		
		try{
			ZipOutputStream writer = new ZipOutputStream(new FileOutputStream(compressed));
			ZipEntry entry = new ZipEntry(file.getName());
			writer.putNextEntry(entry);
			Files.copy(file.toPath(), writer);
			writer.closeEntry();
			writer.close();
		}
		catch(IOException e){  
			e.printStackTrace();	
		}
	}


	
	// Write the CSV content to a file
	public void saveCSVFile(List<List<String>> records, char sep, String outputfile, String encoding, boolean compress){
		
		try {
			Charset charset = encoding.equals("UTF-8") ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputfile + ".csv"), charset));
	        CSVWriter csvWriter = new CSVWriter(writer, sep, CSVWriter.DEFAULT_QUOTE_CHARACTER, 
	        		CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
	        
	        for (List<String> record : records) {
	        	String[] line = record.stream().toArray(String[]::new);
	            csvWriter.writeNext(line, false);
	        }
	        csvWriter.close();
	        writer.close(); 
	        
	        if (compress){
	        	compressFile(outputfile + ".csv");
	        	Files.delete(Paths.get(outputfile + ".csv")); //remove CSV file
	        }  
	    }
		catch(IOException e){  
			e.printStackTrace();
		}
	}


	public void saveRISFile(String content, String fileName, String directory) {
        try {
            FileWriter writer = new FileWriter(fileName + ".ris");
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
