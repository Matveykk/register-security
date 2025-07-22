package com.example.service;

import com.example.entity.LoginRequest;
import com.example.entity.RegisterRequest;
import com.example.entity.AuthResponse;
import com.example.jwt.JwtUtil;
import dao.UserDAO;
import entity.Token;
import entity.User;
import lombok.AllArgsConstructor;
import model.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public void register(RegisterRequest request) {
        if (userDAO.findByLogin(request.getLogin()).isPresent()) {
            throw new RuntimeException("Login already in use");
        }
        if (userDAO.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .login(request.getLogin())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.GUEST))
                .build();

        userDAO.save(user);
    }

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLogin(),
                        request.getPassword()
                )
        );

        User user = userDAO.findByLogin(request.getLogin())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        tokenService.revokeAllUserTokens(user);

        String accessToken = jwtUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getLogin(),
                        user.getPassword(),
                        user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList()
                )
        );

        String refreshToken = UUID.randomUUID().toString();

        tokenService.saveToken(user, accessToken);
        tokenService.saveToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        Token tokenEntity = tokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        User user = tokenEntity.getUser();

        tokenService.revokeAllUserTokens(user);

        String newAccessToken = jwtUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getLogin(),
                        user.getPassword(),
                        user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList()
                )
        );

        tokenService.saveToken(user, newAccessToken);

        return new AuthResponse(newAccessToken, refreshToken);
    }
}
