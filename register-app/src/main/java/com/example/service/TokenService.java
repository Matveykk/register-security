package com.example.service;

import dao.TokenDAO;
import entity.Token;
import entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TokenService {

    private final TokenDAO tokenDAO;

    public TokenService(TokenDAO tokenDAO) {
        this.tokenDAO = tokenDAO;
    }

    public void saveToken(User user, String jwt) {
        Token token = Token.builder()
                .token(jwt)
                .user(user)
                .expired(false)
                .revoked(false)
                .build();
        tokenDAO.save(token);
    }

    public void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenDAO.findAllValidTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;

        for (Token token : validTokens) {
            token.setExpired(true);
            token.setRevoked(true);
        }
        tokenDAO.saveAll(validTokens);
    }

    public boolean isTokenValid(String jwt) {
        return tokenDAO.findByToken(jwt)
                .map(token -> !token.isExpired() && !token.isRevoked())
                .orElse(false);
    }

    public Optional<Token> findByToken(String token) {
        return tokenDAO.findByToken(token);
    }
}
