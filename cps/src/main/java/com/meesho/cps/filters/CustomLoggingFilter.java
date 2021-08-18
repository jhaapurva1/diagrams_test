package com.meesho.cps.filters;

import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.enums.CommonConstants;
import com.meesho.commons.enums.Country;
import com.meesho.commons.enums.Language;
import com.meesho.cps.config.ApplicationProperties;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 06/08/21
 */
@Slf4j
@Component
public class CustomLoggingFilter extends AbstractRequestLoggingFilter {

    @Autowired
    private ApplicationProperties applicationProperties;

    public CustomLoggingFilter() {
        setIncludeClientInfo(false);
        setIncludeQueryString(true);
        setIncludeHeaders(false);
        setIncludePayload(true);
        setMaxPayloadLength(2500);
    }

    @Override
    protected void beforeRequest(HttpServletRequest httpServletRequest, String message) {
        String lbToken = httpServletRequest.getHeader(Constants.LB_TOKEN);
        lbToken = Objects.nonNull(lbToken) ? lbToken : UUID.randomUUID().toString();

        MDC.put(Constants.GUID, lbToken);

        String uri = httpServletRequest.getRequestURI();

        if (!uri.contains("/health")) {
            String countryCode = httpServletRequest.getHeader(CommonConstants.COUNTRY_HEADER);
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            String languageCode = httpServletRequest.getHeader(CommonConstants.COUNTRY_LANGUAGE_HEADER);
            if (Objects.isNull(languageCode)) {
                languageCode = Language.EN.toString();
            }
            MDC.put(CommonConstants.COUNTRY_LANGUAGE_HEADER, languageCode);
        }
    }

    @Override
    public void afterRequest(HttpServletRequest request, String message) {
        String uri = request.getRequestURI();
        boolean logEnabled = !Utils.matchString(applicationProperties.getLogDisabledPaths(), uri);
        if (logEnabled) {
            Map<String, String> headersToLog = new HashMap<>();
            headersToLog.put(CommonConstants.APP_VERSION_CODE, request.getHeader(CommonConstants.APP_VERSION_CODE));
            headersToLog.put(CommonConstants.APP_USER_ID, request.getHeader(CommonConstants.APP_USER_ID));
            log.info("{} ,headers {}", message, headersToLog);
        }
        MDC.clear();
    }

}
