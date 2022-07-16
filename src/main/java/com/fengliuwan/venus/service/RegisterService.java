package com.fengliuwan.venus.service;

import com.fengliuwan.venus.Util.Util;
import com.fengliuwan.venus.dao.RegisterDao;
import com.fengliuwan.venus.entity.db.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RegisterService {

    @Autowired
    private RegisterDao registerDao;

    public boolean register(User user) throws IOException {
        user.setPassword(Util.encryptPassword(user.getUserId(), user.getPassword()));
        return registerDao.register(user);
    }

}
