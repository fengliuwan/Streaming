package com.fengliuwan.venus.service;

/** for identify which module created exception */
public class TwitchException extends RuntimeException {

    public TwitchException(String errorMessage) {
        super(errorMessage);
    }
}
