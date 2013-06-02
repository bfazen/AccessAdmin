package com.alphabetbloc.accessadmin.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class AdminLoginActivity extends SherlockActivity {

	private static final String TAG = "AdminLoginActivity";
	private EditText password;
	private Button btnSubmit;
	private String mAdminPassword;
	private TextView mInstructionText;
	private Context mContext;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.admin_login);
		mContext = this;

		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		mAdminPassword = prefs.getString(Constants.ADMIN_PASSWORD, null);
		if (mAdminPassword == null)
			prefs.edit().putString(Constants.ADMIN_PASSWORD, Constants.DEFAULT_ADMIN_PASSWORD).commit();
		password = (EditText) findViewById(R.id.text_password);
		mInstructionText = (TextView) findViewById(R.id.instruction);
		mInstructionText.setText(R.string.admin_password);
		btnSubmit = (Button) findViewById(R.id.submit_button);

		btnSubmit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String pwd = password.getText().toString();
				if (pwd != null) {
					checkPassword(pwd);
				}
			}
		});
	}

	private void checkPassword(String pwd) {
		int i = pwd.compareTo(mAdminPassword);
		password.setText("");
		if (i == 0) {
			resetErrorCount();
			Intent intenta = new Intent(mContext, AdminSettingsActivity.class);
			startActivity(intenta);
			finish();
		} else {
			int errorCount = countError();
			Toast.makeText(AdminLoginActivity.this, "Incorrect Password: " + pwd + " Attempt Number:" + String.valueOf(errorCount), Toast.LENGTH_SHORT).show();

			if (errorCount > 5)
				sendSMS(errorCount);

			if (errorCount > 5 && errorCount < 10) {
				Toast.makeText(AdminLoginActivity.this, "WARNING! You have had too many incorrect password attempts. Device will lock repeated failed attempts.", Toast.LENGTH_SHORT).show();
			} else if (errorCount >= 10 && errorCount < 20) {
				Toast.makeText(AdminLoginActivity.this, "You have had too many incorrect passwords. Device is now locked.", Toast.LENGTH_SHORT).show();
				lockDevice(errorCount);
			} else if (errorCount >= 20) {
				wipeDevice();
			}
		}
	}

	private void resetErrorCount() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		sharedPref.edit().putInt(Constants.ADMIN_PWD_COUNT, 0).commit();
	}

	private int countError() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		Long lastPwdAttempt = sharedPref.getLong(Constants.LAST_ADMIN_PWD_ATTEMPT, 0);
		int adminPwdCount = sharedPref.getInt(Constants.ADMIN_PWD_COUNT, 0);
		Long now = System.currentTimeMillis();
		Long deltaPwdAttempt = now - lastPwdAttempt;
		int day = 1000 * 60 * 60 * 24;

		if (deltaPwdAttempt < day) {
			// count if last admin password attempt was today
			adminPwdCount++;
		} else if (deltaPwdAttempt > (7 * day)) {
			adminPwdCount = 1;
		}

		sharedPref.edit().putLong(Constants.LAST_ADMIN_PWD_ATTEMPT, now).commit();
		sharedPref.edit().putInt(Constants.ADMIN_PWD_COUNT, adminPwdCount).commit();

		return adminPwdCount;
	}

	private void sendSMS(int errors) {
		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = tm.getDeviceId();
		String warningText = "";
		if (errors < 10)
			warningText = " WARNING: ";
		else if (errors >= 10 && errors < 20)
			warningText = " FINAL WARNING ABOUT TO WIPE DEVICE: ";
		else if (errors >= 20)
			warningText = "WIPING DEVICE: ";

		String message = " IMEI: " + imei + warningText + String.valueOf(errors) + " failed attempts at logging in as admin.";
		Log.e(TAG, message);
		Intent intent = new Intent(mContext, DeviceAdminService.class);
		intent.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.SEND_SMS);
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
		intent.putExtra(Constants.SMS_LINE, line);
		intent.putExtra(Constants.SMS_MESSAGE, message);
		WakefulIntentService.sendWakefulWork(mContext, intent);
	}

	private void lockDevice(int errors) {
		Intent intent = new Intent(mContext, DeviceAdminService.class);
		intent.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.LOCK_SCREEN);
		WakefulIntentService.sendWakefulWork(mContext, intent);
	}

	private void wipeDevice() {
		Intent intent = new Intent(mContext, DeviceAdminService.class);
		intent.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.WIPE_DATA);
		WakefulIntentService.sendWakefulWork(mContext, intent);
	}

}