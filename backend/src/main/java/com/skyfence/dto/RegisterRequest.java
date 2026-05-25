package com.skyfence.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String role;
    private String email;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
    public String getEmail()    { return email; }
    public void setUsername(String u) { this.username = u; }
    public void setPassword(String p) { this.password = p; }
    public void setRole(String r)     { this.role = r; }
    public void setEmail(String e)    { this.email = e; }
}
