package com.example.user;

/**
 * Created by linyun on 14-6-25.
 */
public class User {

    private static User sUser;

    public String mUsername;
    public String mImage;

    private User() {
        mUsername = "the username";
        mImage = "the image";
    }

    public static User getInstance() {
        if (sUser == null)
            sUser = new User();
        return sUser;
    }
}
