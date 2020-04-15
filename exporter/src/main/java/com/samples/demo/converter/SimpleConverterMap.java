package com.samples.demo.converter;

import java.util.HashMap;
import java.util.Map;

public class SimpleConverterMap extends AbstractConverterMap implements java.io.Serializable {
    public static final long serialVersionUID = 0L;

    public SimpleConverterMap() {
        super();

        map.put("java.util.List<java.lang.String>", new SimpleListStringConverter());
    }

}