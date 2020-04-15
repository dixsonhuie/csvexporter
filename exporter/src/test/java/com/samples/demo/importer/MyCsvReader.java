package com.samples.demo.importer;


//import com.gigaspaces.utils.CsvReader;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.logging.Level;
import java.util.logging.Logger;

/*
   Stand-alone program that uses the CsvReader utility to read a CSV file and write objects to the space.
 */
public class MyCsvReader {

    private static Logger log = Logger.getLogger(MyCsvReader.class.getName());
    private GigaSpace gigaSpace;

    private static final String SPACE_NAME = "mySpace";

    {
        System.setProperty("com.gs.jini_lus.groups", "xap-15.2.0");
        System.setProperty("com.gs.jini_lus.locators", "127.0.0.1:4174");
    }

    public MyCsvReader() {
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(SPACE_NAME);
        gigaSpace = new GigaSpaceConfigurer(configurer).gigaSpace();

    }

    public void write() {
        try {
            Class clazz = Class.forName("com.samples.common.Data");
            //new CsvReader().read(Paths.get("/tmp/client/com.samples.common.Data.csv"), clazz).forEach(gigaSpace::write);
        }
        // catch (IOException ioe) { e.printStackTrace(); log.log(Level.SEVERE, ioe.getMessage(), ioe); }
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            log.log(Level.SEVERE, cnfe.getMessage(), cnfe);
        }

    }

    public static void main(String[] args) throws Exception{
        MyCsvReader example = new MyCsvReader();

        example.write();
    }

}