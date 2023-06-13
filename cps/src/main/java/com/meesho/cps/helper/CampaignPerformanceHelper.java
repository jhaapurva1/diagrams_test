package com.meesho.cps.helper;

import com.meesho.ads.lib.utils.EncodingUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceHelper {

    @Autowired
    ApplicationProperties applicationProperties;

    public LocalDate getLocalDateForDailyCampaignFromLocalDateTime(LocalDateTime eventTime) {
        if (beforeResetTimeOfDailyBudgetForCampaign(eventTime)) {
            eventTime = eventTime.minusDays(1);
        }
        return eventTime.toLocalDate();
    }

    public boolean beforeResetTimeOfDailyBudgetForCampaign(LocalDateTime eventTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DailyBudgetConstants.TIME_FORMAT);
        LocalTime resetTime;
        try {
            resetTime = LocalTime.parse(applicationProperties.getDailyBudgetResetTime(), formatter);
        } catch (DateTimeParseException ex) {
            log.error("Error in parsing reset time for daily budget");
            resetTime = LocalTime.MIN;
        }
        return eventTime.toLocalTime().isBefore(resetTime);
    }

    public String decodeCursor(String encodedCursor) {
        String cursor = null;
        try {
            if (!encodedCursor.isEmpty()) {
                cursor = EncodingUtils.decodeFromBase64(encodedCursor, String.class);
            }
        } catch (Exception e) {
            log.error("Exception parsing cursor: {}, using default value.", encodedCursor);
        }
        return cursor;
    }

    public String encodeCursor(String decodedCursor) {
        String cursor = null;
        try {
            if(!decodedCursor.isEmpty()) {
                cursor = EncodingUtils.encodeToBase64(decodedCursor);
            }
        } catch (Exception e) {
            log.error("Exception encoding cursor: {}, using default value", decodedCursor);
        }
        return cursor;
    }

}
