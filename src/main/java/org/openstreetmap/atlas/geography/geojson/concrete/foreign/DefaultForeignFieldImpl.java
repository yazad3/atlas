package org.openstreetmap.atlas.geography.geojson.concrete.foreign;

import java.util.Map;

/**
 * @author Yazad Khambata
 */
public class DefaultForeignFieldImpl implements ForeignField {
    private Map<String, Object> valuesAsMap;

    @Override
    public Object get(final String key) {
        return valuesAsMap.get(key);
    }

    @Override
    public <T> T get(final String key, final Class<T> valueClass) {
        return (T) valuesAsMap.get(key);
    }

    @Override
    public void put(final String key, final String value) {
        valuesAsMap.put(key, value);
    }
}
