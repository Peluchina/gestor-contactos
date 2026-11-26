package com.gestorcontactos.gestor_contactos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialAdministratorInitializer implements ApplicationRunner {

    private static final System.Logger LOGGER = System.getLogger(InitialAdministratorInitializer.class.getName());

    private final UserService userService;
    private final String username;
    private final String password;

    public InitialAdministratorInitializer(UserService userService,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.password:}") String password) {
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.initializeFirstAdministrator(username, password)) {
            LOGGER.log(System.Logger.Level.INFO, "Se creó el administrador inicial configurado por entorno.");
        }
    }
}
