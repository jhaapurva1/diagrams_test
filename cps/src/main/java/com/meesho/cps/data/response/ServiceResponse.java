package com.meesho.cps.data.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse<T> {

    @JsonProperty("data")
    private T data;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("error_details")
    private List<ErrorDetails> errorDetails;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetails {

        @JsonProperty("message")
        private String message;

        @JsonProperty("error_code")
        private String errorCode;

    }

}
