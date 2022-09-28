package ai.konduit.serving.documentparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Extract based on divs where
 * the previous div's entry is
 * the name and the div below is the value.
 *
 */
public class DivRowBased implements TableExtractor {
    private String rowSelector;
    private List<String> fieldNames;
    private List<String> partialFieldNames;
    public DivRowBased(String rowSelector, List<String> fieldNames,List<String> partialFieldNames) {
        this.rowSelector = rowSelector;
        this.fieldNames = fieldNames;
        this.partialFieldNames = partialFieldNames;

    }

    @Override
    public List<Map<String, List<String>>> extract(String html, List<String> tableSeparators) {
        List<Map<String,List<String>>> ret = new ArrayList<>();

        Map<String,List<String>> currTable = new LinkedHashMap<>();
        Document parse = Jsoup.parse(html);
        Elements select = parse.select(rowSelector);
        StringBuilder fieldValue = new StringBuilder();
        String currFieldName = null;
        for(Element element : select) {
            String elementText = element.text().replace("&nbsp;"," ").replace("NBSP"," ");
            if(tableSeparators.contains(elementText)) {
                if(!currTable.isEmpty())
                    ret.add(currTable);

                addValueToList(currTable, fieldValue, currFieldName);


                currTable = new LinkedHashMap<>();

                currFieldName = null;
                fieldValue = new StringBuilder();
                continue;
            }

            if(fieldNames.contains(elementText)) {
                //first encountered field
                if(currFieldName == null) {
                    currFieldName = elementText;
                }  else { //next field
                    addValueToList(currTable, fieldValue, currFieldName);


                    fieldValue = new StringBuilder();
                    currFieldName = elementText;
                }
            } else if(currFieldName != null) { //currently in a key just append text
                fieldValue.append(elementText + " ");
            } else if(partialFieldNames != null && !partialFieldNames.isEmpty()) {
                for(String partialFieldName : partialFieldNames) {
                    if(elementText.contains(partialFieldName)) {
                        List<String> values;
                        if(currTable.containsKey(partialFieldName)) {
                            values = currTable.get(partialFieldName);
                        } else {
                            values = new ArrayList<>();
                            currTable.put(partialFieldName,values);
                        }

                        values.add(elementText.replace(partialFieldName,""));
                    }
                }
            }
        }

        //after everything is done, add last table if it's not empty
        if(!currTable.isEmpty()) {
            ret.add(currTable);
        }

        return ret;
    }

    private void addValueToList(Map<String, List<String>> currTable, StringBuilder fieldValue, String currFieldName) {
        List<String> valueList;
        if(currTable.containsKey(currFieldName)) {
            valueList = currTable.get(currFieldName);
        } else {
            valueList = new ArrayList<>();
        }

        valueList.add(fieldValue.toString().trim());
        currTable.put(currFieldName,valueList);
    }
}
