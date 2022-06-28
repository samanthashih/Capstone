package com.example.meloday20;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.parse.ParseUser;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SplashActivity extends AppCompatActivity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkIfLoggedIn();
            }
        },3000);
    }

    private void routeToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void routeToLoginActivity() {
        Intent intent = new Intent(this, SpotifyLoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkIfLoggedIn() {
        try {
            ParseUser currentUser = ParseUser.getCurrentUser();
            String accessToken = currentUser.getString("accessToken");
            SpotifyService spotify = SpotifyServiceSingleton.getInstance(accessToken);
            spotify.getMe(new Callback<UserPrivate>() {
                @Override
                public void success(UserPrivate userPrivate, Response response) {
                    routeToMainActivity();
                }
                @Override
                public void failure(RetrofitError error) {
                    routeToLoginActivity();
                }
            });
        }
        catch(Exception e) {
            routeToLoginActivity();
        }
    }

}