package com.campus.auction.advice;

import com.campus.auction.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Ensures the HTTP response status in the header always matches Result.code.
 * Only fires for controller methods that return Result<T> directly
 * (not for ResponseEntity<Result<T>>, which already carries an explicit status).
 */
@RestControllerAdvice
public class ResultHttpStatusAdvice implements ResponseBodyAdvice<Result<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body,
                                     MethodParameter returnType,
                                     MediaType selectedContentType,
                                     Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     ServerHttpRequest request,
                                     ServerHttpResponse response) {
        if (body != null) {
            response.setStatusCode(org.springframework.http.HttpStatus.valueOf(body.code()));
        }
        return body;
    }
}
