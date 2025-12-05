package dev.tomle.phoenixlogin.model;

public class PlayerData {
    
    private final String playerName;
    private String passwordHash;
    private String lastIP;
    private long lastLogin;
    private long registrationDate;
    private boolean isAuthenticated;
    private long sessionExpiry;
    
    public PlayerData(String playerName) {
        this.playerName = playerName;
        this.isAuthenticated = false;
    }
    
    public PlayerData(String playerName, String passwordHash, String lastIP, long lastLogin, long registrationDate) {
        this.playerName = playerName;
        this.passwordHash = passwordHash;
        this.lastIP = lastIP;
        this.lastLogin = lastLogin;
        this.registrationDate = registrationDate;
        this.isAuthenticated = false;
    }
    
    // Getters
    public String getPlayerName() {
        return playerName;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getLastIP() {
        return lastIP;
    }
    
    public long getLastLogin() {
        return lastLogin;
    }
    
    public long getRegistrationDate() {
        return registrationDate;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public long getSessionExpiry() {
        return sessionExpiry;
    }
    
    // Setters
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }
    
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }
    
    public void setSessionExpiry(long sessionExpiry) {
        this.sessionExpiry = sessionExpiry;
    }
    
    // Utility methods
    public boolean isRegistered() {
        return passwordHash != null && !passwordHash.isEmpty();
    }
    
    public boolean hasValidSession() {
        if (sessionExpiry == 0) {
            return false;
        }
        return System.currentTimeMillis() < sessionExpiry;
    }
}
