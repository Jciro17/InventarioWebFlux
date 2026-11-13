package co.edu.uco.inventario.controller;

import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class MessageClientExceptionHandler {

    @ExceptionHandler(MessageClientResponseException.class)
    public ResponseEntity<byte[]> response(MessageClientResponseException error) {
        if (error.getCause() instanceof WebClientResponseException upstream) {
            var headers = new HttpHeaders();
            var contentType = upstream.getHeaders().getContentType();
            if (contentType != null) {
                headers.setContentType(contentType);
            }
            return ResponseEntity.status(error.getStatusCode())
                    .headers(headers)
                    .body(upstream.getResponseBodyAsByteArray());
        }
        return ResponseEntity.status(error.getStatusCode()).build();
    }

    @ExceptionHandler(MessageClientTransportException.class)
    public ProblemDetail transport(MessageClientTransportException error) {
        var status = error.getCause() instanceof TimeoutException
                ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        return ProblemDetail.forStatusAndDetail(status, error.getMessage());
    }
}
