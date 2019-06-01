package com.example.runapp.utils;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Keyboard {


    static public void hide(Activity activity) {
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //InputMethodManager imm = (InputMethodManager) activity.getSystemService(MainActivity.INPUT_METHOD_SERVICE);
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(activity.getApplicationContext().INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();

        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }



}
