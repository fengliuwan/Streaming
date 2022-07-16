package com.fengliuwan.venus.service;

import com.fengliuwan.venus.Util.Util;
import com.fengliuwan.venus.dao.LoginDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LoginService {

    @Autowired
    private LoginDao loginDao;

    /**
     *
     * @param userId userId from client to verify
     * @param password  password from client to verify
     * @return  username of the given userId and password if they match an existing user
     * @throws IOException
     */
    public String verifyLogin(String userId, String password) throws IOException {
        // get encrypted password  for verification, otherwise will always fail to match userId and password
        password = Util.encryptPassword(userId, password);
        return loginDao.verifyLogin(userId, password);
    }
}
