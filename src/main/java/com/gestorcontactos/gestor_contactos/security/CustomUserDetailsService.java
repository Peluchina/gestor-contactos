package com.gestorcontactos.gestor_contactos.security;

import com.gestorcontactos.gestor_contactos.entity.User;
import com.gestorcontactos.gestor_contactos.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuario o contraseña incorrectos";

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));
        return new CustomUserDetails(user);
    }
}
