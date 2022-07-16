package com.fengliuwan.venus.service;

import org.springframework.stereotype.Service;

public class RecommendationException extends RuntimeException {

    public RecommendationException(String errorMessage){
        super(errorMessage);
    }
}
