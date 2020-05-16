package ai.konduit.serving.pipeline.util;

import ai.konduit.serving.pipeline.api.data.Data;
import ai.konduit.serving.pipeline.api.data.ValueType;

import java.util.Arrays;
import java.util.List;

public class DataUtils {

    private DataUtils(){ }

    public static boolean listEquals(List<?> list1, List<?> list2, ValueType l1Type, ValueType l2Type){
        if(l1Type != l2Type)
            return false;

        if(list1.size() != list2.size()){
            return false;
        }

        if(l1Type == ValueType.BYTES){
            List<byte[]> lb1 = (List<byte[]>)list1;
            List<byte[]> lb2 = (List<byte[]>)list2;
            for( int i=0; i<lb1.size(); i++ ){
                byte[] b1 = lb1.get(i);
                byte[] b2 = lb2.get(i);
                if(b1.length != b2.length)
                    return false;
                if(!Arrays.equals(b1, b2))
                    return false;
            }
        } else if(l1Type == ValueType.LIST){
            throw new UnsupportedOperationException("Nested lists equality not yet implemented");
        } else {
            if(!list1.equals(list2))
                return false;
        }
        return true;
    }

    public static String inferField(Data d, ValueType vt, boolean allowLists, String errMultipleKeys, String errNoKeys){

        String field = null;
        for(String s : d.keys()){
            if(d.type(s) == vt){
                if(field == null) {
                    field = s;
                } else {
                    throw new IllegalStateException(String.format(errMultipleKeys, field, s));
                }
            } else if(allowLists & d.type(s) == ValueType.LIST && d.listType(s) == vt){
                if(field == null) {
                    field = s;
                } else {
                    throw new IllegalStateException(String.format(errMultipleKeys, field, s));
                }
            }
        }

        if(field == null)
            throw new IllegalStateException(errNoKeys);

        return field;
    }

}
