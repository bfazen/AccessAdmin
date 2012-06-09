package com.alphabetbloc.chvsettings.activities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import com.alphabetbloc.chvsettings.R;

import com.alphabetbloc.chvsettings.receivers.UpdateOnCoverage;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class PrefsPassword extends Activity {

	private static final String TAG = "PrefsPassword";
	private EditText password;
	private Button btnSubmit;
	Context context;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		addListenerOnButton();
		
		
	}
	

		
		public void addListenerOnButton() {

			password = (EditText) findViewById(R.id.txtPassword);
			btnSubmit = (Button) findViewById(R.id.btnSubmit);
			
			btnSubmit.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
		
/* Code for testing the UpdateClockService:					  
					Intent intent = new Intent("com.alphabetbloc.chvsettings.services.UpdateClockService");
			        startService(intent);
			        Log.e(TAG, "starting Update Clock Service");
//					 ComponentName comp = new ComponentName("com.alpahabetbloc.chvsettings.services", UpdateClockService.class.getName());
//			         context.startService(new Intent().setComponent(comp));
//			          Intent serviceIntent = new Intent("com.alphabetbloc.chvsettings.services.UpdateClockService");
//			          Intent serviceIntent = new Intent(context, UpdateTimeService.class); 
//			          context.startService(serviceIntent);

//		testing a new intent
//		 Calendar cal = Calendar.getInstance();
//		 cal.add(Calendar.MINUTE, 1);
//    	 Intent intent = new Intent(context, UpdateOnCoverage.class);
//    	 intent.putExtra("alarm_message", "O'Doyle Rules!");
//    	 // In reality, you would want to have a static variable for the request code instead of 192837
//    	 PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//    	 AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
//    	 am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
			          
	*/		          
			         
			         
					String pwd = md5(password.getText().toString());
					String defaultpwd = md5(getString(R.string.default_pwd));
					int i = pwd.compareTo(defaultpwd);

					if (i == 0){
						Intent intenta = new Intent(v.getContext(), SetPrefs.class);
						  startActivity(intenta);

					}
					else
						Toast.makeText(PrefsPassword.this, "Incorrect Password: " + password.getText(),
								Toast.LENGTH_SHORT).show();
						
						
				}

			});
		
		
		

	}
	
	
	private String md5(String in) {
	    MessageDigest digest;
	    try {
	        digest = MessageDigest.getInstance("MD5");
	        digest.reset();
	        digest.update(in.getBytes());
	        byte[] a = digest.digest();
	        int len = a.length;
	        StringBuilder sb = new StringBuilder(len << 1);
	        for (int i = 0; i < len; i++) {
	            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
	            sb.append(Character.forDigit(a[i] & 0x0f, 16));
	        }
	        return sb.toString();
	    } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
	    return null;
	}

	
	
}