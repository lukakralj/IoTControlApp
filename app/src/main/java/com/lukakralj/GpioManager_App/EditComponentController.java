package com.lukakralj.GpioManager_App;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.lukakralj.GpioManager_App.backend.GpioComponent;
import com.lukakralj.GpioManager_App.backend.RequestCode;
import com.lukakralj.GpioManager_App.backend.ServerConnection;
import com.lukakralj.GpioManager_App.backend.logger.Level;
import com.lukakralj.GpioManager_App.backend.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class EditComponentController extends AppCompatActivity {

    private EditText compNameInput;
    private EditText compPinInput;
    private Switch compTypeInput;
    private EditText compDescriptionInput;
    private Button cancelButton;
    private Button createButton;
    private TextView editComMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_component);

        compNameInput = (EditText) findViewById(R.id.compNameInput);
        compPinInput = (EditText) findViewById(R.id.compPinInput);
        compTypeInput = (Switch) findViewById(R.id.compTypeInput);
        compDescriptionInput = (EditText) findViewById(R.id.compDescriptionInput);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        createButton = (Button) findViewById(R.id.createButton);
        editComMessage = (TextView) findViewById(R.id.editCompMessage);
        editComMessage.setText("");

        cancelButton.setOnClickListener(v -> onBackPressed());
        createButton.setOnClickListener(this::updateComponent);


        Intent i = getIntent();
        GpioComponent component = (GpioComponent) i.getSerializableExtra("editComp");
        if (component == null) {
            // No component to edit. Go to components screen.
            Intent intent = new Intent(this, ComponentsScreenController.class);
            startActivity(intent);
        }

        compNameInput.setText(component.getName());
        compPinInput.setText(component.getPhysicalPin());
        compDescriptionInput.setText(component.getDescription());

        if (component.getDirection().equals("out")) {
            compTypeInput.setChecked(false);
            compTypeInput.setText(R.string.outPin);
        }
        else {
            compTypeInput.setChecked(true);
            compTypeInput.setText(R.string.inPin);
        }

        compTypeInput.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                compTypeInput.setText(R.string.inPin);
            }
            else {
                compTypeInput.setText(R.string.outPin);
            }
        });

        // TODO: add connect and disconnect events
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

    private boolean verifyData() {
        if (compNameInput.getText().toString().length() == 0) {
            editComMessage.setText(R.string.emptyName);
            return false;
        }

        if (compPinInput.getText().toString().length() == 0) {
            editComMessage.setText(R.string.emptyPinNo);
            return false;
        }

        int pinNo = 0;
        try {
            pinNo = Integer.parseInt(compPinInput.getText().toString());
            // valid range 24-34 inclusive
            if (pinNo < 24 || pinNo > 34) {
                throw new NumberFormatException("Pin number out of range.");
            }
        }
        catch (NumberFormatException e) {
            editComMessage.setText(R.string.invalidPinNo);
            return false;
        }
        editComMessage.setText("");
        return true;
    }

    private void updateUnsuccessful() {
        editComMessage.setText(R.string.updateFailed);
    }

    private void updateSuccessful() {
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(getApplicationContext(), R.string.updateOK, duration);
        toast.show();

        Intent intent = new Intent(this, ComponentsScreenController.class);
        startActivity(intent);
    }

    private void updateComponent(View v) {
        if (!verifyData()) {
            return;
        }
        JSONObject extraData = new JSONObject();
        try {
            extraData.put("name", compNameInput.getText().toString());
            extraData.put("physicalPin", Integer.parseInt(compPinInput.getText().toString()));
            extraData.put("direction", compTypeInput.getText().toString().equals(getText(R.string.outPin).toString()) ? "out" : "in");
            extraData.put("description", compDescriptionInput.getText().toString());
        }
        catch (JSONException e) {
            Logger.log(e.getMessage(), Level.ERROR);
            e.printStackTrace();
            return;
        }
        ServerConnection.getInstance().scheduleRequest(RequestCode.UPDATE_COMPONENT, extraData, data -> {
            try {
                Logger.log("data: " + data.toString(), Level.DEBUG);
                if (data.getString("status").equals("OK")) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(this::updateSuccessful);
                }
                else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(this::updateUnsuccessful);
                }
            }
            catch (JSONException e) {
                Logger.log(e.getMessage(), Level.ERROR);
                e.printStackTrace();
            }
        });
    }
}
