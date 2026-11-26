package com.gestorcontactos.gestor_contactos.controller;

import com.gestorcontactos.gestor_contactos.dto.CreateUserRequest;
import com.gestorcontactos.gestor_contactos.dto.EditUserRequest;
import com.gestorcontactos.gestor_contactos.dto.ResetPasswordRequest;
import com.gestorcontactos.gestor_contactos.entity.Role;
import com.gestorcontactos.gestor_contactos.exception.UserManagementException;
import com.gestorcontactos.gestor_contactos.security.CustomUserDetails;
import com.gestorcontactos.gestor_contactos.service.UserService;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/usuarios")
    public String listUsers(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        model.addAttribute("users", userService.listUsers(currentUser.getId()));
        model.addAttribute("currentUserId", currentUser.getId());
        return "users/list";
    }

    @GetMapping("/usuarios/nuevo")
    public String createForm(Model model) {
        model.addAttribute("form", new CreateUserRequest());
        addRoles(model);
        return "users/create";
    }

    @PostMapping("/usuarios")
    public String createUser(@Valid @ModelAttribute("form") CreateUserRequest form, BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addRoles(model);
            return "users/create";
        }
        try {
            userService.createUser(form, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Usuario creado correctamente.");
            return "redirect:/usuarios";
        } catch (UserManagementException exception) {
            bindingResult.reject("userManagement", exception.getMessage());
            addRoles(model);
            return "users/create";
        }
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            var user = userService.getUser(id, currentUser.getId());
            model.addAttribute("user", user);
            model.addAttribute("form", new EditUserRequest(user.role()));
            addRoles(model);
            return "users/edit";
        } catch (UserManagementException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editUser(@PathVariable Long id, @Valid @ModelAttribute("form") EditUserRequest form,
            BindingResult bindingResult, @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("user", userService.getUser(id, currentUser.getId()));
                addRoles(model);
                return "users/edit";
            } catch (UserManagementException exception) {
                redirectAttributes.addFlashAttribute("error", exception.getMessage());
                return "redirect:/usuarios";
            }
        }
        try {
            userService.changeRole(id, form, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Rol actualizado correctamente.");
        } catch (UserManagementException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/activar")
    public String enableUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        return changeUserStatus(id, true, currentUser, redirectAttributes);
    }

    @PostMapping("/usuarios/{id}/desactivar")
    public String disableUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        return changeUserStatus(id, false, currentUser, redirectAttributes);
    }

    @GetMapping("/usuarios/{id}/restablecer-contrasena")
    public String resetPasswordForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            if (id.equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "El restablecimiento debe realizarse para otro usuario.");
                return "redirect:/usuarios";
            }
            model.addAttribute("user", userService.getUser(id, currentUser.getId()));
            model.addAttribute("form", new ResetPasswordRequest());
            return "users/reset-password";
        } catch (UserManagementException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/usuarios/{id}/restablecer-contrasena")
    public String resetPassword(@PathVariable Long id,
            @Valid @ModelAttribute("form") ResetPasswordRequest form, BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("user", userService.getUser(id, currentUser.getId()));
                return "users/reset-password";
            } catch (UserManagementException exception) {
                redirectAttributes.addFlashAttribute("error", exception.getMessage());
                return "redirect:/usuarios";
            }
        }
        try {
            userService.resetPassword(id, form, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Contraseña restablecida correctamente.");
        } catch (UserManagementException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/usuarios";
    }

    private String changeUserStatus(Long id, boolean enabled, CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        try {
            userService.changeEnabled(id, enabled, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", enabled
                    ? "Usuario activado correctamente."
                    : "Usuario desactivado correctamente.");
        } catch (UserManagementException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/usuarios";
    }

    private void addRoles(Model model) {
        model.addAttribute("roles", Arrays.asList(Role.values()));
    }
}
