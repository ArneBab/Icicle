package ca.louisbourque.freenetassistant;

import android.app.Activity;
import android.os.Bundle;



public class SettingsActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setResult(Activity.RESULT_OK);
    }
}