package com.example.demoapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //getSupportActionBar().hide();

        Thread t1=new Thread(){
            @Override
            public void run() {
                try {
                    sleep(3000);
                }
                catch (Exception e){
                    System.out.println(e);
                }
                finally {
                    Intent intent=new Intent(splash.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        t1.start();




    }
}