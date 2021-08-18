package com.meesho.cps.filters;

import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author shubham.aggarwal
 * 06/08/21
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationFilter extends GenericFilterBean {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getRequestURI().startsWith(Constants.API.BASE_PATH) &&
                !httpServletRequest.getRequestURI()
                        .equals(Constants.API.BASE_PATH + Constants.API.HEALTH_CHECK_ENDPOINT)) {
            String authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            if (isAuthorized(authToken)) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid auth token");
            }
        }
    }

    private Boolean isAuthorized(String authToken) {
        return applicationProperties.getAuthTokens().contains(authToken);
    }

}
