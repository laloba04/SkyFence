package com.skyfence.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String role;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
    public void setUsername(String u) { this.username = u; }
    public void setPassword(String p) { this.password = p; }
    public void setRole(String r)     { this.role = r; }
}
