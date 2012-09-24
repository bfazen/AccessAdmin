package com.alphabetbloc.chvsettings.activities;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.Policy;

public class SetUserPassword extends DeviceHoldActivity {

	private static final int SET_PASSWORD = 0;
	private Policy mPolicy;
	private Button mExitBtn;
	private Button mPwdBtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean newInstall = settings.getBoolean(Constants.NEW_INSTALL, true);
		if (newInstall) {
			launchInitialSetupActivity();
		} else {
			mPolicy = new Policy(this);
//			if (!mPolicy.isDeviceSecured()) {
//				Log.e("SetUserPassword", "Something went drastically wrong!  Lost device setup after install!");
//				launchInitialSetupActivity();
//			}
			//Device Admin is successfully setup, so continue
			refreshView();
		}

	}

	private void launchInitialSetupActivity() {
		Intent i = new Intent(this, InitialSetupActivity.class);
		startActivity(i);
		finish();
	}

	// Initialize policy viewing screen.
	private void refreshView() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.setup_password);
		TextView setupMessage = (TextView) findViewById(R.id.setup_message);
		ImageView exclaim = (ImageView) findViewById(R.id.exclamation);
		exclaim.setVisibility(View.VISIBLE);

		// Minimum Password Length
		TextView pwdLength = (TextView) findViewById(R.id.policy_password_length);
		pwdLength.setText(String.valueOf(mPolicy.getPasswordLength()));

		// Password Type/Quality
		int pwdType = mPolicy.getPasswordQuality();
		TextView pwdTypeText = (TextView) findViewById(R.id.policy_password_quality);
		pwdTypeText.setText(getResources().getStringArray(R.array.password_types)[pwdType]);

		// Password Case
		TextView pwdLock = (TextView) findViewById(R.id.policy_password_lockout);
		pwdLock.setText(String.valueOf(mPolicy.getMaxFailedPwd()));

		mPwdBtn = (Button) findViewById(R.id.setup_action_btn);
		mPwdBtn.setText(R.string.change_password);
		mPwdBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
				startActivityForResult(intent, SET_PASSWORD);
			}
		});

		if (!mPolicy.isActivePasswordSufficient()) {
			// Launches password set-up screen in Settings.
			exclaim.setVisibility(View.VISIBLE);
			setupMessage.setText(R.string.password_not_allowed);
			Intent data = new Intent();
			setResult(RESULT_CANCELED, data);
		} else {
			// Can exit
			setupMessage.setText(R.string.password_allowed);
			exclaim.setVisibility(View.GONE);
			mExitBtn = (Button) findViewById(R.id.exit);
			mExitBtn.setText(R.string.exit);
			mExitBtn.setVisibility(View.VISIBLE);
			Intent data = new Intent();
			setResult(RESULT_OK, data);
			mExitBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			});
		}
	}

	// /Override which buttons to allow through DeviceHold by setting
	// mRetainView to false and not consuming TouchEvent
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.equals(mExitBtn) || v.equals(mPwdBtn)) {
			return false;
		}
		return true;
	}

}
