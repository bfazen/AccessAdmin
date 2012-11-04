package com.alphabetbloc.accessadmin.activities;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;

public class SetUserPassword extends DeviceHoldActivity {

	private static final int SET_PASSWORD = 0;
	public static final String SUGGEST_RESET_PASSWORD = "suggest_reset_password";
	// private static final String TAG = SetUserPassword.class.getSimpleName();
	public static final String INITIAL_SETUP = "initial_setup";
	private Button mExitBtn;
	private Button mPwdBtn;
	private boolean mForceResetPwd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// If First Run, then run the setup Wizard
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean newInstall = prefs.getBoolean(Constants.NEW_INSTALL, true);
		if (newInstall) {

			Intent i = new Intent(this, InitialSetupActivity.class);
			startActivity(i);
			finish();
		} else {

			// Detect if password needs to be reset (hide exit button)
			mForceResetPwd = getIntent().getBooleanExtra(SUGGEST_RESET_PASSWORD, false);
			Policy policy = new Policy(this);
			if (!policy.isActivePasswordSufficient())
				mForceResetPwd = true;

			boolean initialSetup = getIntent().getBooleanExtra(INITIAL_SETUP, false);
			if (initialSetup)
				startAirplaneMode();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshView();
	}

	// Initialize policy viewing screen.
	private void refreshView() {
		Policy policy = new Policy(this);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.setup_password);
		TextView setupMessage = (TextView) findViewById(R.id.setup_message);
		ImageView exclaim = (ImageView) findViewById(R.id.exclamation);
		exclaim.setVisibility(View.VISIBLE);

		// Minimum Password Length
		TextView pwdLength = (TextView) findViewById(R.id.policy_password_length);
		pwdLength.setText(String.valueOf(policy.getPasswordLength()));

		// Password Type/Quality
		int pwdType = policy.getPasswordQuality();
		TextView pwdTypeText = (TextView) findViewById(R.id.policy_password_quality);
		pwdTypeText.setText(getResources().getStringArray(R.array.password_types)[pwdType]);

		// Password Case
		TextView pwdLock = (TextView) findViewById(R.id.policy_password_lockout);
		pwdLock.setText(String.valueOf(policy.getMaxFailedPwd()));

		mPwdBtn = (Button) findViewById(R.id.setup_action_btn);
		mPwdBtn.setText(R.string.change_password);
		mPwdBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
				startActivityForResult(intent, SET_PASSWORD);
			}
		});

		mExitBtn = (Button) findViewById(R.id.exit);

		if (!policy.isActivePasswordSufficient() || mForceResetPwd) {
			// Launches password set-up screen in Settings.
			setupMessage.setText(R.string.password_not_allowed);
			exclaim.setVisibility(View.VISIBLE);
			mExitBtn.setVisibility(View.GONE);
			Intent data = new Intent();
			setResult(RESULT_CANCELED, data);
		} else {
			// Can exit
			setupMessage.setText(R.string.password_allowed);
			exclaim.setVisibility(View.GONE);
			mExitBtn.setText(R.string.exit);
			mExitBtn.setVisibility(View.VISIBLE);
			Intent data = new Intent();
			setResult(RESULT_OK, data);
			mExitBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			});
			stopAirplaneMode();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showMenu = prefs.getBoolean(Constants.SHOW_MENU, false);
		Policy policy = new Policy(this);
		if (!policy.isAdminActive() || !showMenu || mForceResetPwd || !policy.isActivePasswordSufficient())
			return false;
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Policy policy = new Policy(this);

		if (policy.isActivePasswordSufficient() && mForceResetPwd)
			mForceResetPwd = false;

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
