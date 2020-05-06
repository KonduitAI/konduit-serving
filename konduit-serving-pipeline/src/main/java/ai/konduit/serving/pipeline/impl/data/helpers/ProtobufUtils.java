package ai.konduit.serving.pipeline.impl.data.helpers;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.impl.data.JData;
import ai.konduit.serving.pipeline.impl.data.Value;
import ai.konduit.serving.pipeline.impl.data.ValueType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProtobufUtils {

    public static generated.Data.DataMap convertJavaToProtobufData(Map<String,Value> dataMap) {

        Map<String, generated.Data.DataScheme> pbItemsMap = new HashMap<>();
        Iterator<Map.Entry<String, Value>> iterator = dataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            generated.Data.DataScheme item = null;

            if (value.type().equals(ValueType.STRING)) {
                item = generated.Data.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.BOOLEAN)) {
                item = generated.Data.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.INT64)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.DOUBLE)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.DOUBLE.ordinal()).
                        build();
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            pbItemsMap.put(nextItem.getKey(), item);
        }
        generated.Data.DataMap pbDataMap = generated.Data.DataMap.newBuilder().
                putAllMapItems(pbItemsMap).
                build();
        return pbDataMap;
    }

    public static Data convertProtobufToData(generated.Data.DataMap dataMap) {
        JData retData = new JData();
        Map<String, generated.Data.DataScheme> schemeMap = dataMap.getMapItemsMap();
        Iterator<Map.Entry<String, generated.Data.DataScheme>> iterator =
                schemeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, generated.Data.DataScheme> entry = iterator.next();
            generated.Data.DataScheme item = entry.getValue();
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.STRING.ordinal()) {
                retData.put(entry.getKey(), item.getSValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.BOOLEAN.ordinal()) {
                retData.put(entry.getKey(), item.getBoolValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.INT64.ordinal()) {
                retData.put(entry.getKey(), item.getIValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.DOUBLE.ordinal()) {
                retData.put(entry.getKey(), item.getDoubleValue());
            }
            if (item.getTypeValue() == generated.Data.DataScheme.ValueType.LIST.ordinal()) {
                // TODO
            }
        }
        return retData;
    }

    /*public static Map<String, generated.Data.DataScheme> javaMapToPbMap() {
        Map<String, generated.Data.DataScheme> newItemsMap = Collections.emptyMap();
        Iterator<Map.Entry<String, Value>> iterator = getDataMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Value> nextItem = iterator.next();
            Value value = nextItem.getValue();

            generated.Data.DataScheme item = null;

            if (value.type().equals(ValueType.STRING)) {
                item = generated.Data.DataScheme.newBuilder().
                        setSValue((String) nextItem.getValue().get()).
                        setTypeValue(ValueType.STRING.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.BOOLEAN)) {
                item = generated.Data.DataScheme.newBuilder().
                        setBoolValue((Boolean) nextItem.getValue().get()).
                        setTypeValue(ValueType.BOOLEAN.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.INT64)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.INT64.ordinal()).
                        build();
            } else if (value.type().equals(ValueType.DOUBLE)) {
                item = generated.Data.DataScheme.newBuilder().
                        setIValue((Long) nextItem.getValue().get()).
                        setTypeValue(ValueType.DOUBLE.ordinal()).
                        build();
            }
            if (item == null) {
                throw new IllegalStateException("JData.write failed");
            }
            newItemsMap.put(nextItem.getKey(), item);
        }
        return newItemsMap;
    }*/


}
