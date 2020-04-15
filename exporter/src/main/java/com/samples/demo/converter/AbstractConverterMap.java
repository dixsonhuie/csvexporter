package com.samples.demo.converter;

import java.util.HashMap;
import java.util.Map;

public class AbstractConverterMap implements java.io.Serializable {

    public static final long serialVersionUID = 0L;

    // return a map where:
    //     key can be field type (genericType) or field name
    //     value is the converter to be used
    Map<String, IConverter> map = new HashMap<String, IConverter>();

    public Map<String, IConverter> getConverterMap() {
        return map;
    }

}
