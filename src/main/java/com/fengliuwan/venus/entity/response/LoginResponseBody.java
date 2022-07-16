package com.fengliuwan.venus.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponseBody {
    /** userid provided by front end for verification */
    @JsonProperty("user_id")
    private final String userId;

    /** first name of the user to return to front end*/
    @JsonProperty("name")
    private final String name;

    /**
     * constructor creates response body with provided userId and user's firstname
     * @param userId userId provided by front end for verfication
     * @param name  user's first name if the verification succeeds
     */
    public LoginResponseBody(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

}
