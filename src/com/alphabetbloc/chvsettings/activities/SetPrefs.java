package com.alphabetbloc.chvsettings.activities;

import com.alphabetbloc.chvsettings.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

/**
 * @author Louis Fazen (louis.fazen@gmail.com)
 */
public class SetPrefs extends Activity  {
    /** Called when the activity is first created. */
	private Button collectButton;
	private Button clinicButton;
	private Button adwButton;
	private Button ushahidiButton;
	private SharedPreferences.Editor editor;
	private ToggleButton collectMenuToggle;
	private ToggleButton collectLogToggle;
	private ToggleButton ushahidiToggle;
	private ToggleButton clinicToggle;
	private ToggleButton adwToggle;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     
        setContentView(R.layout.editprefs);     
        SharedPreferences settings = getSharedPreferences("ChwSettings", MODE_PRIVATE);
        editor = settings.edit();

        adwButton = (Button) findViewById(R.id.adwButton);
        adwButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			 Intent i = new Intent(Intent.ACTION_VIEW);     
    	         i.setComponent(new ComponentName("com.android.launcher","com.android.launcher.MyLauncherSettings"));
    		     startActivity(i);
    		}
    	});
        
        collectButton = (Button) findViewById(R.id.collectButton);
        collectButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			 Intent i = new Intent(Intent.ACTION_VIEW);     
    		     i.setComponent(new ComponentName("org.odk.collect.android","org.odk.collect.android.preferences.PreferencesActivity"));
    		     startActivity(i);
    		}
    	});
        
        clinicButton = (Button) findViewById(R.id.clinicButton);
        clinicButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			 Intent i = new Intent(Intent.ACTION_VIEW);     
    	         i.setComponent(new ComponentName("org.odk.clinic.android","org.odk.clinic.android.activities.PreferencesActivity"));
    		     startActivity(i);
    		}
    	});
        
        
        ushahidiButton = (Button) findViewById(R.id.ushahidiButton);
        ushahidiButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			 Intent i = new Intent(Intent.ACTION_VIEW);     
    	         i.setComponent(new ComponentName("com.ushahidi.android.app","com.ushahidi.android.app.Settings"));
    		     startActivity(i);
    		}
    	});
        
        
        
//        check boxes / toggle buttons
        ushahidiToggle = (ToggleButton) findViewById(R.id.ushahidi_checkbox);
 
        if (settings.getBoolean("UshahidiMenuEnabled", false)){
        	ushahidiToggle.setChecked(true);
        
        }
       
        ushahidiToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {


			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        Intent i = new Intent(Intent.ACTION_VIEW); 
		        i.setComponent(new ComponentName("com.ushahidi.android.app","com.ushahidi.android.app.ViewMenuPreference")); 
				if (ushahidiToggle.isChecked()) {
		        	ushahidiToggle.setChecked(true);
	    		     i.putExtra("ShowMenu", true);
	    		
	    		        editor.putBoolean("UshahidiMenuEnabled", true);
	    		        
		         }
		        else{
		        	ushahidiToggle.setChecked(false);
		        	i.putExtra("ShowMenu", false);
		        	editor.putBoolean("UshahidiMenuEnabled", false);
		    
		        }
		        editor.commit();
		        startActivity(i);
		  
				
			}
		});
        
        clinicToggle = (ToggleButton) findViewById(R.id.clinic_checkbox);
        if (settings.getBoolean("ClinicMenuEnabled", false)){
        	clinicToggle.setChecked(true);
        }
       
        clinicToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {


			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        Intent i = new Intent(Intent.ACTION_VIEW); 
		        i.setComponent(new ComponentName("org.odk.clinic.android","org.odk.clinic.android.activities.ViewMenuPreference"));
				if (clinicToggle.isChecked()) {
		        	clinicToggle.setChecked(true);
	    		     i.putExtra("ShowMenu", true);
	    		
	    		     editor.putBoolean("ClinicMenuEnabled", true);
	    		        
		         }
		        else{
		        	clinicToggle.setChecked(false);
		        	i.putExtra("ShowMenu", false);
		        	editor.putBoolean("ClinicMenuEnabled", false);
		       
		        }
		        editor.commit();
		        startActivity(i);
		    
				
			}
		});
        
        collectMenuToggle = (ToggleButton) findViewById(R.id.collect_checkbox);
        if (settings.getBoolean("CollectMenuEnabled", false)){
        	collectMenuToggle.setChecked(true);
        }
       
        collectMenuToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {


			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        Intent i = new Intent("com.android.launcher.ViewMenuPreference.ACTION"); 
		        i.setComponent(new ComponentName("com.android.launcher.","com.android.launcher.ViewMenuPreference"));
				if (collectLogToggle.isChecked()) {
		        	collectLogToggle.setChecked(true);
	    		     i.putExtra("LogActivities", true);
	    		     i.setAction("com.android.launcher.ViewMenuPreference.ACTION");
	    		     editor.putBoolean("CollectLogEnabled", true);
	    		        
		         }
		        else{
		        	collectLogToggle.setChecked(false);
		        	i.putExtra("LogActivities", false);
		        	i.addCategory("com.android.launcher.ViewMenuPreference.CATEGORY");
		        	editor.putBoolean("CollectLogEnabled", false);
		        	
		        }
		
				
			}
		});
        
        
        collectLogToggle = (ToggleButton) findViewById(R.id.collect_log_checkbox);
        if (settings.getBoolean("CollectLogEnabled", false)){
        	collectLogToggle.setChecked(true);
        }
       
        collectLogToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {


			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        Intent i = new Intent(Intent.ACTION_VIEW); 
		        
		        
		        i.setComponent(new ComponentName("com.android.launcher.","com.android.launcher.ViewMenuPreference"));
				if (collectLogToggle.isChecked()) {
		        	collectLogToggle.setChecked(true);
	    		     i.putExtra("LogActivities", true);
	    		     i.setAction("com.android.launcher.ViewMenuPreference.ACTION");
	    		     editor.putBoolean("CollectLogEnabled", true);
	    		        
		         }
		        else{
		        	collectLogToggle.setChecked(false);
		        	i.putExtra("LogActivities", false);
		        	i.addCategory("com.alphabetbloc.clinic.android.ViewMenuPreference.CATEGORY");
		        	editor.putBoolean("CollectLogEnabled", false);
		        	
		        }
		        editor.commit();
		        startActivity(i);
				
			}
		});
        
        
        
        
        
        adwToggle = (ToggleButton) findViewById(R.id.adw_checkbox);
        if (settings.getBoolean("AdwMenuEnabled", false)){
        	adwToggle.setChecked(true);
        }
       
        adwToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {


			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        Intent i = new Intent(Intent.ACTION_VIEW); 
		        i.setComponent(new ComponentName("com.android.launcher","com.android.launcher.ViewMenuPreference"));
				if (adwToggle.isChecked()) {
		        	adwToggle.setChecked(true);
	    		     i.putExtra("ShowMenu", true);
	    		     i.setAction("com.alphabetbloc.clinic.android.ViewMenuPreference.ACTION");
	    		     editor.putBoolean("AdwMenuEnabled", true);
	    		        
		         }
		        else{
		        	adwToggle.setChecked(false);
		        	i.putExtra("ShowMenu", false);
		        	editor.putBoolean("AdwMenuEnabled", false);
		
		        }
		        editor.commit();
		        startActivity(i);
	
				
			}
		});
        
  
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}


}