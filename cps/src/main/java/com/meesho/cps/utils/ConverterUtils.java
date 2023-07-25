package com.meesho.cps.utils;

import com.meesho.ad.client.constants.FeedType;
import lombok.experimental.UtilityClass;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@UtilityClass
public class ConverterUtils {

    public static Map<FeedType, BigDecimal> getRealEstateBudgetUtilisedMap(Document source, List<FeedType> realEstates) {
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType realEstate : realEstates) {
            realEstateBudgetUtilisedMap.put(realEstate, getBigDecimalFromDecimal128(source.get(
                    FormattingUtils.getRealEstateBudgetUtilisedField(realEstate), Decimal128.class)));
        }
        return realEstateBudgetUtilisedMap;
    }

    public static void putRealEstateBudgetUtilised(Document document, Map<FeedType, BigDecimal> realEstateBudgetUtilised) {
        if(Objects.nonNull(realEstateBudgetUtilised)) {
            for(Map.Entry<FeedType, BigDecimal> entry : realEstateBudgetUtilised.entrySet()) {
                if(Objects.nonNull(entry.getValue())) {
                    document.put(FormattingUtils.getRealEstateBudgetUtilisedField(entry.getKey()), entry.getValue());
                }
            }
        }
    }

    public static  <K,V> void addDataToDocument(K key, V value, BiConsumer<K, V> consumer) {
        if(Objects.nonNull(value)) {
            consumer.accept(key, value);
        }
    }

    public static BigDecimal getBigDecimalFromDecimal128(Decimal128 decimal128) {
        return Objects.nonNull(decimal128) ? decimal128.bigDecimalValue() : BigDecimal.ZERO;
    }

}