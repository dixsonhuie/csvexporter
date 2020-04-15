package com.samples.demo.feeder;

import com.samples.demo.common.Data;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.ArrayList;
import java.util.List;

public class Feeder {

    private GigaSpace gigaSpace;

    private static final String SPACE_NAME = "mySpace";

    {
        /*
        System.setProperty("com.gs.jini_lus.groups", "xap-15.2.0");
        System.setProperty("com.gs.jini_lus.locators", "127.0.0.1:4174");
         */
    }

    public Feeder() {
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(SPACE_NAME);
        gigaSpace = new GigaSpaceConfigurer(configurer).gigaSpace();

    }
    public void write() {
        List<String> list = new ArrayList<String>();
        list.add("Abigail");
        list.add("Becky");
        list.add("Camille");

        Data data = new Data();
        data.setId(1);
        data.setMessage("1 - one");
        data.setNames(list);
        data.setProcessed(Boolean.FALSE);
        gigaSpace.write(data);

        list = new ArrayList<String>();
        list.add("David");
        list.add("Edward");
        list.add("Franklin");

        data = new Data();
        data.setId(2);
        data.setMessage("2 - two");
        data.setNames(list);
        data.setProcessed(Boolean.FALSE);
        gigaSpace.write(data);

        list = null;

        data = new Data();
        data.setId(3);
        data.setMessage(null);
        data.setNames(list);
        data.setProcessed(Boolean.FALSE);
        gigaSpace.write(data);

    }

    public static void main(String[] args) {
        Feeder feeder = new Feeder();
        feeder.write();
    }
}
