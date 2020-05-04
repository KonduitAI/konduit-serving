package ai.konduit.serving.pipeline.impl.data.wrappers;

import ai.konduit.serving.pipeline.impl.data.Value;
import ai.konduit.serving.pipeline.impl.data.ValueType;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ListValue<T> implements Value<List<T>> {

    private List<T> values;

    @Override
    public ValueType type() {
        return ValueType.LIST;
    }

    @Override
    public List<T> get() {
        return values;
    }

    @Override
    public void set(List<T> value) {
        this.values = values;
    }
}
