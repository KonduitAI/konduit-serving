package ai.konduit.serving.pipeline.impl.data.wrappers;

import ai.konduit.serving.pipeline.impl.data.Value;
import ai.konduit.serving.pipeline.impl.data.ValueType;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ListValue<T> implements Value<List<T>> {

    private List<T> values;
    private ValueType elementType;

    @Override
    public ValueType type() {
        return ValueType.LIST;
    }

    public ValueType elementType() { return elementType; }

    @Override
    public List<T> get() {
        return values;
    }

    @Override
    public void set(List<T> value) {
        throw new IllegalStateException("Use set(List<T>,ValueType) for lists");
    }

    // @Override
    public void set(List<T> value, ValueType elementType) {
        this.values = values;
    }
}
