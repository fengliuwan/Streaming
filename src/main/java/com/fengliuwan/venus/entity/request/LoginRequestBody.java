package com.fengliuwan.venus.entity.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * For store the login information sent from frontend
 */
public class LoginRequestBody {

    private final String userId;
    private final String password;

    // two different ways for mapping Json string to object, see Login Response Body
    @JsonCreator
    public LoginRequestBody(@JsonProperty("user_id") String userId, @JsonProperty("password") String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

}
