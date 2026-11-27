package com.gestorcontactos.gestor_contactos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.gestorcontactos.gestor_contactos.exception.ContactConflictException;
import com.gestorcontactos.gestor_contactos.exception.ContactNotFoundException;
import com.gestorcontactos.gestor_contactos.exception.UserConflictException;
import com.gestorcontactos.gestor_contactos.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class MvcErrorHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorScenarioController())
                .setControllerAdvice(new MvcErrorHandler())
                .build();
    }

    @Test
    void mapsContactNotFoundTo404() throws Exception {
        mockMvc.perform(get("/errors/contact-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void mapsUserNotFoundTo404() throws Exception {
        mockMvc.perform(get("/errors/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void mapsContactConflictTo409() throws Exception {
        mockMvc.perform(get("/errors/contact-conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"));
    }

    @Test
    void mapsUserConflictTo409() throws Exception {
        mockMvc.perform(get("/errors/user-conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"));
    }

    @Test
    void mapsUnexpectedExceptionToSafe500() throws Exception {
        mockMvc.perform(get("/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"));
    }

    @Test
    void mapsUnboundRequestValueTo400() throws Exception {
        mockMvc.perform(get("/errors/type-mismatch/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
    }

    @Test
    void preserves413ForMultipartFailures() throws Exception {
        mockMvc.perform(get("/errors/multipart"))
                .andExpect(status().is(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(view().name("error/413"));
    }

    @Controller
    static class ErrorScenarioController {

        @GetMapping("/errors/contact-not-found")
        String contactNotFound() {
            throw new ContactNotFoundException("internal contact detail");
        }

        @GetMapping("/errors/user-not-found")
        String userNotFound() {
            throw new UserNotFoundException("internal user detail");
        }

        @GetMapping("/errors/contact-conflict")
        String contactConflict() {
            throw new ContactConflictException("internal contact conflict");
        }

        @GetMapping("/errors/user-conflict")
        String userConflict() {
            throw new UserConflictException("internal user conflict");
        }

        @GetMapping("/errors/unexpected")
        String unexpected() {
            throw new IllegalStateException("SQL and internal details");
        }

        @GetMapping("/errors/multipart")
        String multipart() {
            throw new MaxUploadSizeExceededException(1L);
        }

        @GetMapping("/errors/type-mismatch/{id}")
        String typeMismatch(@org.springframework.web.bind.annotation.PathVariable Long id) {
            return id.toString();
        }
    }
}
