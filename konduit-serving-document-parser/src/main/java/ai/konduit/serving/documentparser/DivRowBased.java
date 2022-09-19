package ai.konduit.serving.documentparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<Map<String, String>> extract(String html, List<String> tableSeparators) {
        List<Map<String,String>> ret = new ArrayList<>();

        Map<String,String> currTable = new HashMap<>();
        Document parse = Jsoup.parse(html);
        Elements select = parse.select(rowSelector);
        StringBuilder fieldValue = new StringBuilder();
        String currFieldName = null;
        for(Element element : select) {
            String elementText = element.text().replace("&nbsp;"," ");
            if(tableSeparators.contains(elementText)) {
                if(!currTable.isEmpty())
                    ret.add(currTable);
                currTable = new HashMap<>();

                currFieldName = null;
                fieldValue = new StringBuilder();
                continue;
            }

            if(fieldNames.contains(elementText)) {
                //first encountered field
                if(currFieldName == null) {
                    currFieldName = elementText;
                }  else { //next field
                    currTable.put(currFieldName,fieldValue.toString());
                    fieldValue = new StringBuilder();
                    currFieldName = elementText;
                }
            } else if(currFieldName != null) { //currently in a key just append text
                fieldValue.append(elementText + " ");
            } else if(partialFieldNames != null && !partialFieldNames.isEmpty()) {
                for(String partialFieldName : partialFieldNames) {
                    if(elementText.contains(partialFieldName)) {
                        currTable.put(partialFieldName,elementText.replace(partialFieldName,""));
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
}
