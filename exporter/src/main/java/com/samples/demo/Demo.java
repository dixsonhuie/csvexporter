package com.samples.demo;


import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;
import com.samples.demo.converter.AbstractConverterMap;
import com.samples.demo.converter.SimpleConverterMap;
import com.samples.demo.exporter.CsvExport;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoContext;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.Task;
import org.openspaces.core.executor.TaskGigaSpace;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Demo implements java.io.Serializable {

    public static final long serialVersionUID = 0L;

    private static Logger log = Logger.getLogger(Demo.class.getName());

    // run on client-side
    public static void runClientSide(String spaceName, String exportBaseDir, AbstractConverterMap converterMap) {
        GigaSpace space = getRemoteProxy(spaceName);
        export(space, exportBaseDir, converterMap, "client");
    }

    // runs on server-side using DistributedTask API
    public static void runServerSide(String spaceName, String exportBaseDir, AbstractConverterMap converterMap) {
        GigaSpace proxy = getRemoteProxy(spaceName);

        // get the number of partitions
        Admin admin = new AdminFactory().createAdmin();
        admin.getGridServiceAgents().waitForAtLeastOne();

        Space space = admin.getSpaces().waitFor(spaceName, 10, TimeUnit.SECONDS);
        int numPartitions = space.getNumberOfInstances();

        AsyncFuture<Serializable>[] futures = new AsyncFuture[numPartitions];

        for (int i=0; i < numPartitions; i++ ) {
            int finalPartitionId = i + 1;

            futures[i] = proxy.execute(new Task<Serializable>() {
                // need to pass in instance/partition id
                // instanceId needs to effectively final
                int instanceId = finalPartitionId;

                @TaskGigaSpace
                private transient GigaSpace gigaSpace;

                @Override
                public Serializable execute() throws Exception {

                    log.log(Level.INFO, "In task execute, instanceId is: " + instanceId);
                    export(gigaSpace, exportBaseDir, converterMap, String.valueOf(instanceId));
                    return null;
                }

            }, finalPartitionId); // finalPartitionId also used for routing
            futures[i].setListener(new AsyncFutureListener<Serializable>() {
                int instanceId = finalPartitionId;
                @Override
                public void onResult(AsyncResult<Serializable> asyncResult) {
                    if (asyncResult.getException() != null) {
                       Exception e = asyncResult.getException();
                       e.printStackTrace();
                       log.log(Level.SEVERE, "Task for partition: " + instanceId + " has errors.");
                       log.log(Level.SEVERE, e.getMessage(), e);
                    } else {
                        log.log(Level.INFO, "Task for partition: " + instanceId + " is done.");
                    }
                }
            });
        }
    }

    public static void export(GigaSpace space, String exportDir, AbstractConverterMap converterMap, String instanceId) {

        CsvExport exporter = new CsvExport(space, exportDir, converterMap, instanceId);
        exporter.export();
    }

    // helper class to the get remote proxy
    private static GigaSpace getRemoteProxy(String spaceName) {
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(spaceName);
        GigaSpace proxy = new GigaSpaceConfigurer(configurer).gigaSpace();
        return proxy;
    }

    public static void printHelpAndExit() {
        String indent = "          ";
        System.out.println("This program will export data from a space. Below are the arguments supported: ");
        System.out.println("  --runClientSide");
        System.out.println(indent  + "run this program on the client side.");
        System.out.println("  --runServerSide");
        System.out.println(indent  + "run this program on the server side.");
        System.out.println("  --spaceName=<SPACE NAME>");
        System.out.println(indent + "the name of the space to connect to.");
        System.out.println("  --exportBaseDir=</PATH/TO/EXPORT/DIR>");
        System.out.println(indent + "the parent directory where the exported .csv files are written. Default is /tmp");
        System.out.println("  --help");
        System.out.println(indent + "this help. Output the command line options.");
        System.exit(-1);
    }

    public static void main(String[] args) {
        boolean runClientSide = false;
        boolean runServerSide = false;
        String spaceName = "";
        String exportBaseDir = "/tmp";
        try {
            for (String s : args) {
                if ("--runClientSide".equalsIgnoreCase(s)) {
                    runClientSide = true;
                } else if ("--runServerSide".equalsIgnoreCase(s)) {
                    runServerSide = true;
                } else if (s.startsWith("--spaceName=")) {
                    String[] sArray = s.split("=");
                    spaceName = sArray[1];
                } else if (s.startsWith("--exportBaseDir=")) {
                    String[] sArray = s.split("=");
                    exportBaseDir = sArray[1];
                } else if ("--help".equalsIgnoreCase(s)) {
                    printHelpAndExit();
                }

            }

            AbstractConverterMap converterMap = new SimpleConverterMap();

            if (runClientSide == true) {
                runClientSide(spaceName, exportBaseDir, converterMap);
            } else if (runServerSide == true) {
                runServerSide(spaceName, exportBaseDir, converterMap);
            } else {
                printHelpAndExit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
;