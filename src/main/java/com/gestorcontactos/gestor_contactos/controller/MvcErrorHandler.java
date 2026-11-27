package com.gestorcontactos.gestor_contactos.controller;

import com.gestorcontactos.gestor_contactos.exception.ContactConflictException;
import com.gestorcontactos.gestor_contactos.exception.ContactNotFoundException;
import com.gestorcontactos.gestor_contactos.exception.UserConflictException;
import com.gestorcontactos.gestor_contactos.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@ControllerAdvice(annotations = Controller.class)
public class MvcErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(MvcErrorHandler.class);

    @ExceptionHandler({ContactNotFoundException.class, UserNotFoundException.class, NoResourceFoundException.class})
    public ModelAndView handleNotFound(Exception exception) {
        return errorPage(HttpStatus.NOT_FOUND, "error/404");
    }

    @ExceptionHandler({ContactConflictException.class, UserConflictException.class})
    public ModelAndView handleConflict(Exception exception) {
        return errorPage(HttpStatus.CONFLICT, "error/409");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, MethodArgumentNotValidException.class, BindException.class,
            HttpMessageNotReadableException.class})
    public ModelAndView handleBadRequest(Exception exception) {
        return errorPage(HttpStatus.BAD_REQUEST, "error/400");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return errorPage(HttpStatus.PAYLOAD_TOO_LARGE, "error/413");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception) throws Exception {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof AccessDeniedException) {
                throw exception;
            }
            if (cause instanceof MaxUploadSizeExceededException) {
                return errorPage(HttpStatus.PAYLOAD_TOO_LARGE, "error/413");
            }
            cause = cause.getCause();
        }
        logger.error("Error inesperado al procesar una solicitud MVC.", exception);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "error/500");
    }

    private ModelAndView errorPage(HttpStatus status, String viewName) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        return modelAndView;
    }
}
