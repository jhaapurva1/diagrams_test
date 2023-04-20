package com.meesho.cps.db.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.meesho.cps.constants.MessageIntent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BaseLocalCache contains operations with access local cache.
 * */
public abstract class BaseLocalCache<K, V> {

    private final Cache<K, V> storageCache;

    public BaseLocalCache(Cache<K, V> paramCache) {
        this.storageCache = paramCache;
    }

    public abstract MessageIntent getMessageIntentType();

    public abstract List<K> transformEvent(String data);

    public void putAll(Map<K, V> paramMap) {
        storageCache.putAll(paramMap);
    }

    public void put(K key, V value) {
        storageCache.put(key, value);
    }

    public V get(K key) {
        V value = storageCache.getIfPresent(key);

        if(Objects.isNull(value)) {
            Map<K, V> mapValueForMissingKey = backFill(Collections.singletonList(key));
            return mapValueForMissingKey.get(key);
        }

        return value;

    }

    public Map<K, V> getAll(List<K> keys) {
        Map<K, V> mapValuesForPresentKeys = storageCache.getAllPresent(keys);

        Map<K, V> outputValuesMap = new HashMap<>();
        outputValuesMap.putAll(mapValuesForPresentKeys);

        List<K> missingKeys = keys.stream().filter(k -> !mapValuesForPresentKeys.containsKey(k)).collect(Collectors.toList());

        if(!missingKeys.isEmpty()){
            Map<K, V> mapValuesForMissingKeys = backFill(missingKeys);
            outputValuesMap.putAll(mapValuesForMissingKeys);
        }

        return outputValuesMap;
    }

    public void remove(K key) {
        storageCache.invalidate(key);
    }

    public void removeAll(List<K> paramKeys) {
        storageCache.invalidateAll(paramKeys);
    }

    //Returned map will be used to directly populate local cache from data source
    public abstract Map<K, V> fetchRecordsFromDataSource(List<K> keySet);

    public Map<K, V> backFill(List<K> paramKeys) {
        Map<K, V> fetchRecordsMap = fetchRecordsFromDataSource(paramKeys);
        putAll(fetchRecordsMap);

        return fetchRecordsMap;
    }

}
