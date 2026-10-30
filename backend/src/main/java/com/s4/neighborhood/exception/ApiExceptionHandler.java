
package com.s4.neighborhood.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import com.s4.neighborhood.service.WorkspaceAlreadyExistsException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> invalidRequest(
            MethodArgumentNotValidException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );

        problem.setTitle("Invalid business profile");
        problem.setDetail(
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": "
                                + error.getDefaultMessage())
                        .findFirst()
                        .orElse("Check the submitted business information")
        );

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalidValue(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );
        problem.setDetail(exception.getMessage());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(WorkspaceAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> workspaceExists(WorkspaceAlreadyExistsException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setDetail(exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(
            NoSuchElementException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.NOT_FOUND
        );
        problem.setDetail(exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
