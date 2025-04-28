package org.lareferencia.services.vufindbulkdownloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.opencsv.CSVWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SolrStreamWriter {

  private static final int PAGE_SIZE = 1000; // adjust for Solr

  public static void writeCsv(String solrServer, String query, int totalRecords, Map<String, String> fieldList,
      List<String> userFields,
      Map<String, List<String>> aggFields, String listSep, String nullMsg, List<String> noMsgFields, String outputFile)
      throws IOException {
    try (CSVWriter writer = new CSVWriter(
        new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8),
        ',',
        CSVWriter.NO_QUOTE_CHARACTER,
        CSVWriter.NO_ESCAPE_CHARACTER,
        System.lineSeparator())) {

      boolean firstPage = true;

      JSONParser parser = new JSONParser();
      int start = 0;

      while (start <= totalRecords + PAGE_SIZE) {
        String pageQuery = query + "&rows=" + PAGE_SIZE + "&start=" + start;
        URL url = new URL(solrServer + "/select?" + pageQuery + "&wt=json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          JSONObject response = (JSONObject) parser.parse(reader);
          JSONObject solrResponse = (JSONObject) response.get("response");
          List<JSONObject> docs = (List<JSONObject>) solrResponse.get("docs");
          System.out.println("docs size: " + start + docs.size());

          FileUtils fileUtils = new FileUtils();

          List<List<String>> records = new ArrayList<>();

          for (List<String> record : records) {
            String[] line = record.stream().toArray(String[]::new);
            writer.writeNext(line, false);
          }

          start = start + PAGE_SIZE;
        } catch (ParseException e) {
          throw new IOException("Error parsing Solr response", e);
        }
      }
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(outputFile + ".gz"))) {
        // GZIPOutputStream is now properly closed
      }
    }
  }

  public static void writeRis(String solrServer, String query, int totalRecords, String outputFile) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

      JSONParser parser = new JSONParser();
      int start = 0;

      while (start <= totalRecords + PAGE_SIZE) {
        String pageQuery = query + "&rows=" + PAGE_SIZE + "&start=" + start;
        URL url = new URL(solrServer + "/select?" + pageQuery + "&wt=json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          JSONObject response = (JSONObject) parser.parse(reader);
          JSONObject solrResponse = (JSONObject) response.get("response");
          JSONArray docs = (JSONArray) solrResponse.get("docs");

          for (Object docObj : docs) {
            JSONObject doc = (JSONObject) docObj;

            writer.write("TY  - JOUR\n"); // Example RIS type

            for (Object keyObj : doc.keySet()) {
              String key = keyObj.toString();
              Object value = doc.get(key);

              String risTag = mapFieldToRISTag(key); // you can customize mapping here
              if (value instanceof JSONArray) {
                for (Object item : (JSONArray) value) {
                  writer.write(risTag + "  - " + item.toString() + "\n");
                }
              } else if (value != null) {
                writer.write(risTag + "  - " + value.toString() + "\n");
              }
            }
            writer.write("ER  -\n\n"); // RIS end record
          }
          start = start + PAGE_SIZE;
        } catch (ParseException e) {
          throw new IOException("Error parsing Solr response", e);
        }
      }
    }
  }

  private static String mapFieldToRISTag(String fieldName) {
    switch (fieldName.toLowerCase()) {
      case "author":
      case "creator":
        return "AU";
      case "title":
        return "TI";
      case "abstract":
        return "AB";
      default:
        return "N1"; // Note field for unmapped fields
    }
  }
}
