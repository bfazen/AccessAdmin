package com.alphabetbloc.chvsettings.activities;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;
import com.alphabetbloc.chvsettings.data.Policy;
import com.alphabetbloc.chvsettings.data.StringGenerator;
import com.alphabetbloc.chvsettings.receivers.DeviceAdmin;

/**
 * Activity that steps through the process of enabling the Device Policy,
 * setting up a password that follows the policy, and establishing a provider ID
 * for Clinic for CHWs in a secure way. Uses full screen view and airplane mode
 * to prevent leaving activity via a call or SMS and interrupting the activity
 * via notifications bar. Also leads the user to setup an account on the device
 * if they wish.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class InitialSetupActivity extends DeviceHoldActivity {
	// private static final String TAG = "InitialSetUpService";
	private static final int START = 0;
	private static final int SET_ADMIN = 1;
	private static final int SET_PWD = 2;
	private static final int SETUP_CLINIC = 3;
	private static final String TAG = InitialSetupActivity.class.getSimpleName();
	private int mStep;
	private Context mContext;
	private TextView mInstructionText;
	private TextView mStepText;
	private Button mButton;
	private int mRequestCode;
	private Policy mPolicy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "InitialSetupActivity is called");
		mContext = this;
		mPolicy = new Policy(mContext);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		settings.edit().putBoolean(Constants.NEW_INSTALL, false).commit();

		startAirplaneMode();
		initializeSIM();
		createUniqueDeviceAdminCode();
		saveDefaultDeviceAdminPwd();
		saveDefaultReportingLine();
		initializeView();
		super.onCreate(savedInstanceState);
	}

	private void initializeView() {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.initial_setup);
		mInstructionText = (TextView) findViewById(R.id.instruction);
		mButton = (Button) findViewById(R.id.next_button);
		mStepText = (TextView) findViewById(R.id.step);

		mButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (mStep) {
				case SET_ADMIN:
					activateDeviceAdmin();
					break;
				case SET_PWD:
					setPassword();
					break;
				case SETUP_CLINIC:
					setupClinic();
					break;
				default:
					break;
				}

			}

		});

		mRequestCode = START;
		stepForward();
	}

	private void stepForward() {
		switch (mRequestCode) {
		case START:
			mInstructionText.setText(R.string.initial_instructions);
			mStep = SET_ADMIN;
			mStepText.setVisibility(View.GONE);
			mButton.setText(R.string.start);
			break;
		case SET_ADMIN:
			mPolicy.initializeDefaultPolicy();
			mInstructionText.setText(R.string.pwd_instructions);
			mButton.setText(R.string.set_pwd);
			mStep = SET_PWD;
			mStepText.setText(String.valueOf(SET_PWD));
			mStepText.setVisibility(View.VISIBLE);
			break;
		case SET_PWD:
			if (mPolicy.isDeviceSecured()) {
				stopAirplaneMode();
				mInstructionText.setText(R.string.access_mrs_instructions);
				mButton.setText(R.string.setup_access_mrs);
				mStep = SETUP_CLINIC;
				mStepText.setText(String.valueOf(SETUP_CLINIC));
			} else {
				mInstructionText.setText(R.string.setup_error);
				mStep = SET_ADMIN;
				mStepText.setVisibility(View.GONE);
				mButton.setText(R.string.start);
			}
			break;
		default:
			break;
		}
	}

	private void stepBack() {
		switch (mRequestCode) {
		case SET_ADMIN:
			mInstructionText.setText(R.string.admin_requirement);
			mButton.setText(R.string.set_admin);
			mStep = SET_ADMIN;
			mStepText.setText(String.valueOf(SET_ADMIN));
			mStepText.setVisibility(View.VISIBLE);
			break;
		case SET_PWD:
			mInstructionText.setText(R.string.pwd_requirement);
			mButton.setText(R.string.set_pwd);
			mStep = SET_PWD;
			mStepText.setText(String.valueOf(SET_PWD));
			break;
		default:
			break;
		}
	}

	private void initializeSIM() {
		// Setup the SIM information (this will act as the registered SIM code
		// for the device)
		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String line = tm.getLine1Number();
		String serial = tm.getSimSerialNumber();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		settings.edit().putString(Constants.SIM_SERIAL, serial).commit();
		settings.edit().putString(Constants.SIM_LINE, line).commit();
	}

	/**
	 * Creates a unique device code that must be used to do Device Admin work
	 * via SMS.
	 */
	private void createUniqueDeviceAdminCode() {
		// not secure, just more secure.
		// could be more secure if the user supplied the encryption key, but
		// that may be unecessary, given still have root priv
		String rAlphaNum = (new StringGenerator(15)).getRandomAlphaNumericString();
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		prefs.edit().putString(Constants.UNIQUE_DEVICE_ID, rAlphaNum).commit();
	}

	private void saveDefaultDeviceAdminPwd() {
		// could always use this default with getString(), but put in here so as
		// to have check for null
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		prefs.edit().putString(Constants.ADMIN_PASSWORD, Constants.DEFAULT_ADMIN_PASSWORD).commit();
	}

	private void saveDefaultReportingLine() {
		// could always use this default with getString(), but put in here so as
		// to have check for null
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		prefs.edit().putString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE).commit();
	}

	private void activateDeviceAdmin() {
		Intent initAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		ComponentName deviceAdmin = new ComponentName(InitialSetupActivity.this, DeviceAdmin.class);
		initAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
		initAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please set up a device administrator to ensure the security of this device.");
		startActivityForResult(initAdmin, SET_ADMIN);
	}

	private void setPassword() {
		Intent i = new Intent(mContext, SetUserPassword.class);
		i.putExtra(SetUserPassword.INITIAL_SETUP, true);
		startActivityForResult(i, SET_PWD);
	}

	private void setupClinic() {
		if (isClinicInstalled()) {
			Log.e(TAG, "InitialSetupActivity is calling setupClinic");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			prefs.edit().putBoolean(Constants.SHOW_MENU, true).commit();
			Intent i = new Intent();
			i.setComponent(new ComponentName("com.alphabetbloc.clinic", "com.alphabetbloc.clinic.ui.admin.ClinicLauncherActivity"));
			i.putExtra("device_admin_setup", true);
			startActivityForResult(i, SETUP_CLINIC);
		}
		finish();
	}

	private boolean isClinicInstalled() {
		Log.e(TAG, "isClinicInstalled");
		try {
			getPackageManager().getPackageInfo("com.alphabetbloc.clinic", PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Clinic is not installed, so skipping setup.");
			return false;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mRequestCode = requestCode;
		if (resultCode == RESULT_OK)
			stepForward();
		else
			stepBack();
	}

	// /Override which buttons to allow through DeviceHold by not consuming
	// TouchEvent
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.equals(mButton)) {
			return false;
		}
		return true;

	}
}