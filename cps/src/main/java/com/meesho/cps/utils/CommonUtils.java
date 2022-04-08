package com.meesho.cps.utils;

import com.meesho.ads.lib.data.internal.IngestionProcessedMetadata;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
public class CommonUtils {

    public static IngestionProcessedMetadata getDefaultRedshitProcessedMetadata() {
        return IngestionProcessedMetadata.builder().processedDataSize(0).lastEntryCreatedAt(null).build();
    }

    public static boolean shouldQueryMonthWiseIndex(LocalDate startDate, LocalDate endDate) {
        LocalDate lastDateOfStartingMonth = startDate.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate firstDateOfEndingMonth = endDate.with(TemporalAdjusters.firstDayOfMonth());
        if (ChronoUnit.MONTHS.between(lastDateOfStartingMonth, firstDateOfEndingMonth) > 0) {
            return true;
        }
        return false;
    }
}
