package com.samples.demo.converter;

import com.samples.demo.exporter.CsvUtil;

import java.util.ArrayList;
import java.util.List;

/*
   This class is used to demonstrates how to output the field as String,
   in case the default toString behavior is not desired.
 */
public class SimpleListStringConverter implements IConverter, java.io.Serializable {

    public static final long serialVersionUID = 0L;

    @Override
    public String convert(Object obj) {
        if (obj == null ) return CsvUtil.NULL_AS_STRING;
        List<String> list = (List) obj;
        if (list.size() == 0) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append(CsvUtil.QUOTE_CHAR);
        int count = 0;
        for (String s : list) {
            if (count > 0) {
                sb.append(CsvUtil.CSV_DELIMITER);
            }
            sb.append(s);
            count++;
        }
        sb.append(CsvUtil.QUOTE_CHAR);
        return sb.toString();
    }


    public static void main(String[] args) {
        SimpleListStringConverter example = new SimpleListStringConverter();

        List<String> list = new ArrayList();

        list.add("One");
        //list.add("2");
        //list.add("three");

        System.out.println(example.convert(null));
        System.out.println(list.toString());

    }
}