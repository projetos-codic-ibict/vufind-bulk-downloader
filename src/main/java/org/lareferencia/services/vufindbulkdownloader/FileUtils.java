package org.lareferencia.services.vufindbulkdownloader;

import java.io.BufferedWriter;
import java.io.File;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.list.FixedSizeList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.opencsv.CSVWriter;

public class FileUtils {

	private Map<String, Integer> columnIndexes = new HashMap<String, Integer>();

	// Check whether a field is the first element in an aggregated column
	private boolean isAggFieldHead(Map<String, List<String>> aggFields, String field) {

		for (List<String> fields : aggFields.values()) {
			if (fields.get(0).equals(field))
				return true;
		}

		return false;
	}

	// Get the name of the aggregated column where a field is the head
	private String getAggFieldName(Map<String, List<String>> aggFields, String field) {

		for (String aggField : aggFields.keySet()) {
			if (aggFields.get(aggField).get(0).equals(field))
				return aggField;
		}

		return null;
	}

	// Convert a list to a formatted string which will fill a single cell in the CSV
	private String listToString(List<String> list, String sep, String nullMsg) {

		String strList = "";
		boolean empty = true;
		boolean showNullMsg = true;

		// Test if list only contains empty values...
		for (String item : list) {
			if (!item.equals("")) {
				empty = false;
				break;
			}
		}

		// ... or only the null message
		for (String item : list) {
			if (!item.equals(nullMsg)) {
				showNullMsg = false;
				break;
			}
		}

		if (showNullMsg) {
			strList = nullMsg;
		} else if (!empty) {
			for (String item : list) {
				strList += item + sep;
			}
			strList = strList.substring(0, strList.length() - sep.length()); // remove last separator
		}

		return strList;
	}

	// Convert a JSON response into a CSV-ready structure
	@SuppressWarnings("unchecked")
	public List<List<String>> JSONtoCSV(String json, Map<String, String> fieldList, List<String> userFields,
			Map<String, List<String>> aggFields, String listSep, String nullMsg, List<String> noMsgFields,
			String serverIp, String hostBase) {

		List<List<String>> csv = new ArrayList<List<String>>();
		Set<String> fields = fieldList.keySet();
		// Add header
		List<String> header = new ArrayList<String>();
		int colIndex = 0;

		for (String field : fields) {
			if (userFields.contains(field)) {
				String label = fieldList.get(field);
				if (!label.equals("null")) {

					header.add(label);
					columnIndexes.put(label, colIndex++);
				} else {
					if (isAggFieldHead(aggFields, field)) { // the first field in an aggregated column, set aggregating
															// field name
															// as header
						label = getAggFieldName(aggFields, field);
						header.add(label);
						columnIndexes.put(label, colIndex++);
					}
				}
			}
		}
		csv.add(header);

		// Get all field values
		try {
			JSONParser parser = new JSONParser();
			Object resultObject = parser.parse(json);

			if (resultObject instanceof JSONObject) {
				JSONObject object = (JSONObject) resultObject;
				JSONObject response = (JSONObject) object.get("response");
				List<JSONObject> docs = (List<JSONObject>) response.get("docs");

				for (JSONObject doc : docs) {
					List<String> line = FixedSizeList.decorate(Arrays.asList(new String[header.size()]));
					Map<String, String> toAggregate = new HashMap<String, String>();

					for (String field : fields) {
						if (userFields.contains(field)) { // user selected this field for export
							Object attribute = doc.get(field);
							String label = fieldList.get(field);
							int index = label.equals("null") ? -1 : columnIndexes.get(fieldList.get(field));

							if (attribute == null) { // doc has no such field
								if (label.equals("null")) { // empty field to be aggregated, save for later retrieval
									if (!noMsgFields.contains(field)) {
										toAggregate.put(field, nullMsg);
									} else {
										toAggregate.put(field, "");
									}
								} else {
									if (!noMsgFields.contains(field)) { // a custom message should be shown instead of a
																		// blank
										line.set(index, nullMsg);
									} else {
										line.set(index, "");
									}
								}
							} else {
								if (attribute instanceof JSONArray) { // field contains a list
									JSONArray items = (JSONArray) attribute;
									List<String> jsonList = new ArrayList<String>();

									for (Object item : items) {
										jsonList.add((String) item);
									}

									if (label.equals("null")) { // list field to be aggregated, save for later retrieval
										toAggregate.put(field, listToString(jsonList, listSep, nullMsg));
									} else {
										line.set(index, listToString(jsonList, listSep, nullMsg));
									}
								} else {
									if (label.equals("null")) { // single-value field to be aggregated, save for later
																// retrieval
										toAggregate.put(field, (String) attribute);
									} else {
										line.set(index, (String) attribute);
									}
								}
							}
						}
					}

					//  para retornar no arquivo de exportação a url completa
					for (String field : fields) {
						if ("search.uniqueid".equals(field)) { 
							String idValue = (String) doc.get(field);  

							if (idValue != null) {
								String fullUrl = hostBase + "/Record/" + idValue;
								//String fullUrl = serverIp + "/vufind/Record/" + idValue;
								int index = columnIndexes.get(fieldList.get(field)); 
								line.set(index, fullUrl); 

								//System.out.println("URL Completa para 'search.uniqueid': " + fullUrl);
							} else {
								int index = columnIndexes.get(fieldList.get(field));
								line.set(index, nullMsg);
							}
						}
					}

					// Process aggregated fields
					for (String aggField : aggFields.keySet()) {
						int index = columnIndexes.get(aggField);
						List<String> items = new ArrayList<String>();

						for (String entry : aggFields.get(aggField)) {
							items.add(toAggregate.get(entry));
						}
						line.set(index, listToString(items, listSep, nullMsg));
					}

					csv.add(line);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			;
		}
		return csv;
	}

	private String formatArrays(JSONArray receivedArray) {
		StringBuilder formattedArray = new StringBuilder();
		if (receivedArray == null) {
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
	public String JSONtoRIS(String json, Map<String, String> fieldListRIS) {

		StringBuilder ris = new StringBuilder();

		try {

			JSONParser parser = new JSONParser();
			Object resultObject = parser.parse(json);
			JSONObject object = (JSONObject) resultObject;
			JSONObject response = (JSONObject) object.get("response");
			JSONArray docs = (JSONArray) response.get("docs");

			String separator = "  - ";

			for (int i = 0; i < docs.size(); i++) {
				JSONObject doc = (JSONObject) docs.get(i);
				for (Map.Entry<String, String> entry : fieldListRIS.entrySet()) {
					String key = entry.getKey().toUpperCase();
					String valor = entry.getValue();

					if (doc instanceof JSONObject) {
						if (doc.get(valor) instanceof JSONArray) {
							JSONArray arrayValor = (JSONArray) doc.get(valor);

							if (key.equals("AU")) {
								int autorNumber = arrayValor.size();
								for (int e = 0; e < autorNumber; e++) {
									ris.append(key).append(separator).append(arrayValor.get(e)).append("\n");
								}
							} else {
								if (formatArrays(arrayValor) == null) {
									ris.append(key).append(separator).append("").append("\n");
								} else {
									ris.append(key).append(separator).append(formatArrays(arrayValor)).append("\n");
								}
							}
						} else {
							if (doc.get(valor) == null) {
								ris.append(key).append(separator).append("").append("\n");
							} else {
								ris.append(key).append(separator).append(doc.get(valor)).append("\n");
							}
						}

					}
				}
				ris.append("ER").append(separator).append("\n\n");
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ris.toString();

	}

	// Create a zip file containing the CSV file
	private void compressFile(String inputfile, String type) {
		File file = new File(inputfile);
		File compressed = new File(inputfile.replace(type, ".zip"));

		try {
			ZipOutputStream writer = new ZipOutputStream(new FileOutputStream(compressed));
			ZipEntry entry = new ZipEntry(file.getName());
			writer.putNextEntry(entry);
			Files.copy(file.toPath(), writer);
			writer.closeEntry();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * private void compressFileRIS (String inputfile){
	 * System.out.println("---------------");
	 * System.out.println(inputfile);
	 * File file = new File(inputfile);
	 * File compressed = new File(inputfile.replace(".ris", ".zip"));
	 * 
	 * try{
	 * ZipOutputStream writer = new ZipOutputStream(new
	 * FileOutputStream(compressed));
	 * ZipEntry entry = new ZipEntry(file.getName());
	 * writer.putNextEntry(entry);
	 * Files.copy(file.toPath(), writer);
	 * writer.closeEntry();
	 * writer.close();
	 * }
	 * catch(IOException e){
	 * e.printStackTrace();
	 * }
	 * }
	 */

	// Write the CSV content to a file
	public void saveCSVFile(List<List<String>> records, char sep, String outputfile, String encoding,
			boolean compress) {

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

			if (compress) {
				compressFile(outputfile + ".csv", ".csv");
				Files.delete(Paths.get(outputfile + ".csv")); // remove CSV file
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveRISFile(String content, String fileName, String directory, boolean compress) {
		try {
			FileWriter writer = new FileWriter(fileName + ".ris");
			writer.write(content);
			writer.close();
			if (compress) {
				compressFile(fileName + ".ris", ".ris");
				Files.delete(Paths.get(fileName + ".ris")); // remove CSV file
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
