package com.meesho.cps.exception.handler;

import com.meesho.cps.data.response.ServiceResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Slf4j
@ControllerAdvice(annotations = RestController.class)
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        List<String> errors = new LinkedList<>();
        errors.add(ex.getMessage());
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(new ServiceResponse<>(null, errors, null), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        List<String> errors = new LinkedList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(error.getDefaultMessage());
        });
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(new ServiceResponse<>(null, errors, null), HttpStatus.BAD_REQUEST);
    }

}
