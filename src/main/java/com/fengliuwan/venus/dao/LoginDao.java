package com.fengliuwan.venus.dao;

import com.fengliuwan.venus.entity.db.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class LoginDao {

    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Verify if the given user Id and password are correct. Returns the user name when it passes
     * @param userId    username from client
     * @param password  user password from client
     * @return username if the username and password passes verification
     */
    public String verifyLogin(String userId, String password) { // password has been encrypted when passed in
        String name = "";
        Session session = null;

        //https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        try {
            session = sessionFactory.openSession();
            //try to retrieve user with provided userid from database as java object User
            User user = session.get(User.class, userId);
            //verify the user with userId exists and the password matches
            if (user != null && user.getPassword().equals(password)) {
                name = user.getFirstName();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("failed to open session to verify userid and password");
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return name;
    }
}
