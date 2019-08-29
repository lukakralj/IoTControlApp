package com.lukakralj.GpioManager_App;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import com.lukakralj.GpioManager_App.backend.RequestCode;
import com.lukakralj.GpioManager_App.backend.ServerConnection;
import com.lukakralj.GpioManager_App.backend.logger.Level;
import com.lukakralj.GpioManager_App.backend.logger.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the entry point of the application. It requires the user to login
 * with predefined username and password.
 */
public class LoginScreenController extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView loginMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: where to put this?
        if (!Logger.isStarted()) {
            Logger.startLogger();
        }
        ServerConnection.getInstance();

        setContentView(R.layout.activity_login_screen);

        usernameInput = (EditText) findViewById(R.id.usernameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);
        loginMessage = (TextView) findViewById(R.id.loginMessage);
        resetAll();

        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(this::handleLoginButton);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.loginToolbar);
        setSupportActionBar(myToolbar);

        ServerConnection.getInstance().subscribeOnConnectEvent(this.getClass(), () -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(this::enableAll);
            checkTokenAndRedirect();
        });

        ServerConnection.getInstance().subscribeOnDisconnectEvent(this.getClass(), () -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(this::disableAll);
        });

        if (!ServerConnection.getInstance().isConnected()) {
            disableAll();
        }

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String url = prefs.getString("url", null);
        if (url != null && !ServerConnection.getInstance().isConnected()) {
            ServerConnection.reconnect(url);
        }

        checkTokenAndRedirect();
    }

    private void checkTokenAndRedirect() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String token = prefs.getString("accessToken", null);
        if (token != null && ServerConnection.getInstance().isConnected()) {
            ServerConnection.getInstance().setAccessToken(token);

            ServerConnection.getInstance().scheduleRequest(RequestCode.REFRESH_TOKEN, null, data -> {
                try {
                    if (data.getString("status").equals("OK")) {
                        // Token is valid.
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(this::startHomeActivity);
                    }
                    else {
                        ServerConnection.getInstance().setAccessToken(null);
                    }
                }
                catch (JSONException e) {
                    Logger.log(e.getMessage(), Level.ERROR);
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.base_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.configureURLlogin) {
            // open url config activity
            showUrlDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void showUrlDialog() {
        Intent intent = new Intent(this, ConfigureURLController.class);
        startActivity(intent);
    }


    /**
     * Disable back button.
     */
    @Override
    public void onBackPressed() {
        // do nothing...
    }

    /**
     * Decide what happens when a Login button is clicked.
     *
     * @param v
     */
    private void handleLoginButton(View v) {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.equals("")) {
            loginMessage.setText(getString(R.string.emptyUsernameMsg));
        }
        else if (password.equals("")) {
            loginMessage.setText(getString(R.string.emptyPasswordMsg));
        }
        else {
            verifyUser(username, password);
        }
    }

    /**
     * Defines what happens when credentials are invalid.
     */
    private void invalidCredentials() {
        resetAll();
        loginMessage.setText(getString(R.string.invalidCredentialsMsg));
    }

    /**
     * This method verifies user. It send a login request.
     *
     * @param username Username the user entered.
     * @param password Password the user entered.
     */
    private void verifyUser(String username, String password) {
        JSONObject extraData = new JSONObject();
        try {
            extraData.put("username", username);
            extraData.put("password", password);
        }
        catch (JSONException e) {
            Logger.log(e.getMessage(), Level.ERROR);
e.printStackTrace();
            invalidCredentials();
            return;
        }
        ServerConnection.getInstance().scheduleRequest(RequestCode.LOGIN, extraData, data -> {
            try {
                Logger.log("data: " + data.toString(), Level.DEBUG);
                if (data.getString("status").equals("OK")) {
                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("accessToken", data.getString("accessToken"));
                    editor.apply();
                    startHomeActivity();
                }
                else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(this::invalidCredentials);
                }
            }
            catch (JSONException e) {
                Logger.log(e.getMessage(), Level.ERROR);
e.printStackTrace();
            }
        });
    }

    /**
     * Clears all input boxes and messages.
     */
    private void resetAll() {
        usernameInput.setText("");
        passwordInput.setText("");
        loginMessage.setText("");
    }

    private void enableAll() {
        loginMessage.setText("");
        usernameInput.setEnabled(true);
        passwordInput.setEnabled(true);
        loginButton.setEnabled(true);
    }

    private void disableAll() {
        loginMessage.setText(R.string.waitingConnection);
        usernameInput.setEnabled(false);
        passwordInput.setEnabled(false);
        loginButton.setEnabled(false);
    }

    /**
     * This method opens a new activity upon successful login.
     */
    private void startHomeActivity() {
        resetAll();
        Intent intent = new Intent(this, HomeScreenController.class);
        startActivity(intent);
    }
}
