package com.example.entity;

import lombok.Data;
import model.Role;

import java.util.Set;

@Data
public class RegisterRequest {
    private String login;
    private String password;
    private String email;
    private Set<Role> roles;
}
