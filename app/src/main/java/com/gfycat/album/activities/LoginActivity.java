package com.gfycat.album.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.gfycat.album.R;
import com.gfycat.album.application.GfyApplication;
import com.gfycat.album.data.GfyPreferences;
import com.gfycat.album.dialog.GfyLoginDialog;
import com.gfycat.album.interfaces.RetrofitInterface;
import com.gfycat.album.models.GrantRequest;
import com.gfycat.album.models.GrantResponsePojo;
import javax.inject.Inject;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Michael Yoon Huh on 1/28/2017.
 */

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.login_field) EditText loginField;
    @BindView(R.id.password_field) EditText passwordField;
    @BindView(R.id.login_progress_bar) ProgressBar loginProgressBar;
    @BindView(R.id.activity_login_layout) RelativeLayout loginActivityLayout;

    @Inject Retrofit retrofitAdapter;

    @OnClick(R.id.login_button)
    public void loginButton() {

        // Disabled for now. Hardcoding credentials.
        // String username = loginField.getText().toString();
        // String password = passwordField.getText().toString();
        // loginUser(username, password);

        loginUser(getString(R.string.gfycat_test_username), getString(R.string.gfycat_test_password));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        unbinder = ButterKnife.bind(this);

        // Retrofit Dagger injection for this activity.
        ((GfyApplication) getApplication()).getApiComponent().inject(this);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    private void initView() {
        loginProgressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#ee00d4"), android.graphics.PorterDuff.Mode.SRC_ATOP);
    }

    private void loginUser(@NonNull final String username, @NonNull final String password) {

        Log.d(LOG_TAG, "loginUser(): Username: " + username);

        loginProgressBar.setVisibility(View.VISIBLE);
        RetrofitInterface loginRequest = retrofitAdapter.create(RetrofitInterface.class);
        Call<GrantResponsePojo> call = loginRequest.loginUser(new GrantRequest(username,
                password, getString(R.string.gfycat_client_id), getString(R.string.gfycat_client_secret)));
        call.enqueue(new Callback<GrantResponsePojo>() {

            @Override
            public void onResponse(Call<GrantResponsePojo> call, Response<GrantResponsePojo> response) {

                loginProgressBar.setVisibility(View.INVISIBLE);

                Log.d(LOG_TAG, "onResponse(): Response success: " + response.isSuccessful());
                Log.d(LOG_TAG, "onResponse(): Response message: " + response.message());

                if (response.isSuccessful()) {

                    GrantResponsePojo responsePojo = response.body();

                    SharedPreferences gfyPrefs = GfyPreferences.initializePreferences(LoginActivity.this);
                    GfyPreferences.setUserLoggedIn(true, gfyPrefs);
                    GfyPreferences.setAccessToken(responsePojo.getAccessToken(), gfyPrefs);
                    GfyPreferences.setRefreshToken(responsePojo.getRefreshToken(), gfyPrefs);

                    // TODO: Very insecure, remove when unneeded.
                    GfyPreferences.setLogin(username, gfyPrefs);
                    GfyPreferences.setPassword(password, gfyPrefs);

                    launchMainIntent();
                } else {
                    displayLoginResponseSnackbar(getString(R.string.login_error_message));
                }
            }

            @Override
            public void onFailure(Call<GrantResponsePojo> call, Throwable t) {
                loginProgressBar.setVisibility(View.INVISIBLE);
                displayLoginResponseSnackbar(getString(R.string.login_error_message));
            }
        });
    }

    private void displayLoginResponseSnackbar(String message) {
        Snackbar snackbar = Snackbar
                .make(loginActivityLayout, message, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loginButton();
                    }
                });
        snackbar.show();
    }

    private void launchMainIntent() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    /** DIALOG FRAGMENT METHODS ________________________________________________________________ **/

    private void displayLoginDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        GfyLoginDialog loginDialog = new GfyLoginDialog();
        loginDialog.show(fragmentManager, GfyLoginDialog.class.getSimpleName());
    }
}
