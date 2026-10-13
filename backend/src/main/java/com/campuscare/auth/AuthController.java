package com.campuscare.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final StudentAccountRepository studentAccounts;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            StudentAccountRepository studentAccounts,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.studentAccounts = studentAccounts;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
        public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody StudentCredentialsRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String email = request.email().trim().toLowerCase();
        if (studentAccounts.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An account with this email already exists"));
        }
        try {
            studentAccounts.save(new StudentAccount(email, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An account with this email already exists"));
        }
        Authentication authentication = establishSession(email, request.password(), servletRequest, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userSummary(authentication));
    }

    @PostMapping("/student/login")
    public ResponseEntity<Map<String, String>> studentLogin(
            @Valid @RequestBody StudentCredentialsRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return authenticate(request.email().trim().toLowerCase(), request.password(), "STUDENT", servletRequest, servletResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return authenticate(request.username(), request.password(), "ADMIN", servletRequest, servletResponse);
    }

    @GetMapping("/me")
    public Map<String, String> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("Login required");
        }
        String role = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "STUDENT";
        return Map.of("email", authentication.getName(), "role", role);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, String>> authenticate(
            String username,
            String password,
            String requiredRole,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
                Authentication authentication = establishSession(username, password, servletRequest, servletResponse);
            boolean hasRequiredRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + requiredRole));
            if (!hasRequiredRole) {
                SecurityContextHolder.clearContext();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email or password"));
            }
                return ResponseEntity.ok(userSummary(authentication));
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
    }

    private Authentication establishSession(
            String username,
            String password,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return authentication;
    }

    private Map<String, String> userSummary(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "STUDENT";
        return Map.of("email", authentication.getName(), "role", role);
    }
}