package com.studiovision.loungefm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.studiovision.loungefm.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class ActivityStart extends Activity {
	
	private final int SPLASH_DISPLAY_LENGHT = 3000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
	//	hideActionBar();
		
		final String PREFS_NAME = "MyPrefsFile";
		final Handler handler = new Handler();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		if (settings.getBoolean("my_first_time", true)) {
		    //the app is being launched for first time, do something        
		    
		    handler.postDelayed(new Runnable() {
	            @Override
	            public void run() {
	            	setContentView(R.layout.first_start);
	            }
	        }, 1500);
		             // first time task

		    // record the fact that the app has been started at least once
		    settings.edit().putBoolean("my_first_time", false).commit(); 
		}
	    
		
		new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(ActivityStart.this, MainActivity.class);
                ActivityStart.this.startActivity(mainIntent);
                ActivityStart.this.finish();
            }
        }, SPLASH_DISPLAY_LENGHT);
    }
	
	/*@TargetApi(11)
	private void hideActionBar() {
	    // Make sure we're running on Honeycomb or higher to use ActionBar APIs
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	        ActionBar actionBar = getActionBar();
	        actionBar.hide();
	    }
	}*/
}

