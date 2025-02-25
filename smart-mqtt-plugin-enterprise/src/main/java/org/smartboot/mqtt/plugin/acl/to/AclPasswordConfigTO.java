package org.smartboot.mqtt.plugin.acl.to;

public class AclPasswordConfigTO {
    private String username;

    private String password;

    private String alg;

    private String saltType;

    private String salt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getSaltType() {
        return saltType;
    }

    public void setSaltType(String saltType) {
        this.saltType = saltType;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
