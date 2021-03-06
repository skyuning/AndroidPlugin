package com.example.user;

import android.content.Context;

/**
 * Created by linyun on 14-6-25.
 */
public class User {

    private static User sUser;

    public static User getInstance(Context context) {
        if (sUser == null)
            sUser = new User(context);
        return sUser;
    }

    private String mUsername;

    private User(Context context) {
        mUsername = context.getString(R.string.username);
    }

    public String getUsername() {
        return mUsername;
    }
}
