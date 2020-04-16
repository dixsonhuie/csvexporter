package com.samples.demo.exporter;


import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.query.QueryResultType;
import com.j_spaces.core.client.SQLQuery;
import com.samples.demo.converter.AbstractConverterMap;
import com.samples.demo.converter.IConverter;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.Spaces;
import org.openspaces.core.GigaSpace;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvExport implements java.io.Serializable {
    // this gets called from Task API, therefore it should be serializable
    public static final long serialVersionUID = 0L;

    private final String exportBaseDir;

    private static Logger log = Logger.getLogger(CsvExport.class.getName());

    // key is fully qualified class name, value is sorted array of sorted field names
    HashMap<String, String[]> nameMap = new HashMap<String, String[]>();

    // key is fully qualified class name, value is array of type names, in same order as field names above
    HashMap<String, String[]> typeMap = new HashMap<String, String[]>();

    // same as HashMap nameMap, typeMap but stored as TreeMap
    TreeMap<String, String[]> sortedNameMap = new TreeMap<>();
    TreeMap<String, String[]> sortedTypeMap = new TreeMap<>();

    private GigaSpace gigaSpace;
    private Admin admin;

    final Map<String, IConverter> converterMap;

    // used to identify the partition, or when running from client is 'client'
    private final String instanceId;


    public CsvExport(GigaSpace space, String exportBaseDir, AbstractConverterMap converterMap, String instanceId) {
        this.gigaSpace = space;
        this.exportBaseDir = exportBaseDir;
        this.converterMap = converterMap.getConverterMap();
        this.instanceId = instanceId;

        admin = new AdminFactory().createAdmin();
        admin.getGridServiceAgents().waitForAtLeastOne();
    }


    public void export() {
        getTypeInformation();
        log.log(Level.INFO, "About to export partition id: " + instanceId);

        // output class and field names
        for(Map.Entry<String,String[]> entry : sortedNameMap.entrySet()) {
            String className = entry.getKey();
            String[] fieldNames = entry.getValue();
            if( fieldNames == null ) continue;
            log.log(Level.INFO, className + " => ");
            int i = 0;
            for (String s: fieldNames) {
                log.log(Level.INFO, "field " + i + ": " + fieldNames[i]);
                i++;
            }
            exportClass(className, fieldNames);
            log.log(Level.INFO, "Completed export of partition id: " + instanceId);
        }

    }

    public void exportClass(String className, String[] fieldNames) {
        SQLQuery query = new SQLQuery<SpaceDocument>(className, "", QueryResultType.DOCUMENT);
        SpaceDocument[] results = (SpaceDocument[]) gigaSpace.readMultiple(query);
        writeToCsv(results, className);
    }

    private String createCommaSeparatedString(String[] sArray) {
        StringBuffer sb = new StringBuffer();

        for (int i=0; i < sArray.length; i++) {
            if( i!= 0) {
                sb.append(CsvUtil.CSV_DELIMITER);
            }
            sb.append(sArray[i]);
        }
        return sb.toString();
    }

    private void writeMetadataToCsv(String className, String[] fieldNames, String[] typeNames, String exportDir) {
        String sFieldNames = createCommaSeparatedString(fieldNames);
        String sTypeNames  = createCommaSeparatedString(typeNames);

        List<String> lines = Arrays.asList(sFieldNames, sTypeNames);
        Path file = Paths.get(exportDir + File.separator + className + ".meta.csv");
        try {
            Files.write(file, lines, CsvUtil.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void writeToCsv(SpaceDocument[] results, String className) {

        String[] fieldNames = sortedNameMap.get(className);
        String[] typeNames = sortedTypeMap.get(className);

        String exportDirname = null;

        try {

            log.log(Level.INFO, "Checking if directory exists...");
            // may need to create a sub-directory. The sub-directory will be the instanceId / partition id
            // if running from client side, a sub-directory 'client' will be created.
            exportDirname = exportBaseDir + File.separator + instanceId;
            File exportDir = new File(exportDirname);
            if (!exportDir.exists()) {
                exportDir.mkdir();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage(), e);
            return;
        }
        writeMetadataToCsv(className, fieldNames, typeNames, exportDirname);
        try (
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportDirname + File.separator + className + ".csv"), CsvUtil.S_CHARSET));
        ) {
            StringBuffer oneLine = new StringBuffer();

            // print column header
            int index = 0;
            for (String field: fieldNames) {
                if (index != 0) {
                    oneLine.append(CsvUtil.CSV_DELIMITER);
                }
                oneLine.append(field);
                index ++;
            }
            bw.write(oneLine.toString());
            bw.newLine();

            // print each document
            for (SpaceDocument document: results) {
                oneLine = new StringBuffer();

                index = 0;
                for (String field : fieldNames) {
                    if (index != 0) {
                        oneLine.append(CsvUtil.CSV_DELIMITER);
                    }

                    String type = typeNames[index];
                    String sValue = null;
                    Object value = document.getProperty(field);
                    if( value == null ) {
                        sValue = CsvUtil.NULL_AS_STRING;
                    }
                    else if (converterMap.containsKey(type)) {
                        sValue = ((IConverter) converterMap.get(field)).convert(value);
                    } else if (converterMap.containsKey(field)) {
                        sValue = ((IConverter) converterMap.get(type)).convert(value);
                    } else {
                        sValue = value.toString();
                    }
                    oneLine.append(sValue);
                    index ++;
                }
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
        }
        //catch (UnsupportedEncodingException e) { e.printStackTrace(); log.log(Level.SEVERE, e.getMessage(), e); }
        //catch (FileNotFoundException e){ e.printStackTrace(); log.log(Level.SEVERE, e.getMessage(), e); }
        catch (IOException e){ e.printStackTrace(); log.log(Level.SEVERE, e.getMessage(), e); }
    }

    // get type information for each class
    public void getTypeInformation() {
        Spaces spaces = admin.getSpaces();

        Space space = spaces.waitFor(gigaSpace.getName(), 10000, TimeUnit.MILLISECONDS);

        SpaceInstance[] spaceInstances = space.getInstances();

        for (SpaceInstance spaceInstance: spaceInstances){
            String[] strings = spaceInstance.getRuntimeDetails().getClassNames();

            for (String className : strings){
                try {
                    addClassEntry(className);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        sortBykey();
    }

    private void addClassEntry(String className) throws ClassNotFoundException {

        Field[] fields = Class.forName(className).getDeclaredFields();

        if (fields.length == 0) {
            typeMap.put(className, null);
            nameMap.put(className, null);
            return;
        }

        // key field name, value field type
        HashMap<String,String> hm = new HashMap<>();

        for( Field field: fields ) {
            String key = field.getName();
            //String value  = field.getType().toString();
            String value = field.getGenericType().getTypeName();
            if (key.equalsIgnoreCase("serialVersionUID")) {
                continue;
            }
            hm.put(key, value);
        }

        // get sorted keys
        Set keyset = hm.keySet();
        List keyList = new ArrayList( keyset );
        Collections.sort(keyList);

        log.log(Level.INFO, "fields: " + keyList);
        Iterator iter = keyList.iterator();

        String[] propertyType = new String[keyList.size()];
        String[] propertyName = new String[keyList.size()];

        // place in array sorted by name
        int idx = 0;
        while(iter.hasNext()) {
            String fieldName = (String) iter.next();
            propertyName[idx] = fieldName;
            propertyType[idx] = hm.get(fieldName);
            idx ++;
        }

        typeMap.put(className, propertyType);
        nameMap.put(className, propertyName);

    }
    private void sortBykey()
    {
        sortedTypeMap.putAll(typeMap);
        sortedNameMap.putAll(nameMap);
    }

    public static void main(String[] args) throws Exception {

    }

}
