package com.alphabetbloc.accessadmin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.accessadmin.activities.InitialSetupActivity;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.alphabetbloc.accessadmin.services.UpdateClockService;

/**
 * Checks for a new install, a change in the SIM code, and device security
 * before passing on the intent to the appropriate activity or service. Also
 * launches the UpdateClockService.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class UpdateOnBoot extends BroadcastReceiver {

	private static final String TAG = "UpdateOnBoot";
	private Context mContext;
	private int mSecurityCode;

	public UpdateOnBoot() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;

		if (Constants.BOOT_COMPLETED.equals(intent.getAction())) {
			// always check clock...
			mContext.startService(new Intent(mContext, UpdateClockService.class));
			Log.v("BootReceiver", "Boot Receiver is receiving!");
			// check security...
			Policy policy = new Policy(mContext);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			boolean newInstall = settings.getBoolean(Constants.NEW_INSTALL, true);

			if (newInstall) {
				// Setup Initial Security
				Intent i = new Intent(mContext, InitialSetupActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);

			} else if (policy.isAdminActive()) {

				// Check on Security

				// 1. Check on simChange
				if (isSimChanged()) {
					// TODO! This should be made into a SharedPreference so does
					// not need to be active
					int simChangeCount = logNewSimChange();

					// Send new SIM to DeviceAdmin (assumes airplane mode off)
					if (simChangeCount < 5)
						mSecurityCode = Constants.SEND_SIM;

					// Phone booted with unregistered SIM 5 times in <3 weeks,
					// so we assume Lost or Stolen:
					else
						mSecurityCode = Constants.WIPE_ODK_DATA;

					Intent i = new Intent(mContext, DeviceAdminService.class);
					i.putExtra(Constants.DEVICE_ADMIN_WORK, mSecurityCode);
					mContext.startService(i);
				}

				// 2. Check on Device Security
				if (!policy.isDeviceSecured()) {
					Intent i = new Intent(context, InitialSetupActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
				}

			}

		}

	}

	private boolean isSimChanged() {
		boolean simChanged = false;

		// Get Current SIM
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String registeredSimSerial = settings.getString(Constants.SIM_SERIAL, null);
		String registeredSimLine = settings.getString(Constants.SIM_LINE, null);
		if (registeredSimLine == null || registeredSimSerial == null) {
			if(Constants.DEBUG) Log.e(TAG, "Registered SIM has been lost! Requesting Initial Security Setup! \n\t REGISTERED SIM LINE: " + registeredSimLine + " \n\t REGISTERED SIM SERIAL: " + registeredSimSerial);
			settings.edit().putBoolean(Constants.NEW_INSTALL, true).commit();
			// Setup Initial Security
			Intent i = new Intent(mContext, InitialSetupActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(i);
			return simChanged;
		}

		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String currentSimSerial = tm.getSimSerialNumber();
		String currentSimLine = tm.getLine1Number();

		if (currentSimLine == null || currentSimSerial == null) {
			if(Constants.DEBUG) Log.w(TAG, "SIM has been taken out of phone or is not registering with device \n\t CURRENT SIM LINE: " + currentSimLine + " \n\t CURRENT SIM SERIAL: " + currentSimSerial);
			simChanged = true;
		}
		if (!currentSimLine.equals(registeredSimLine) || !currentSimSerial.equals(registeredSimSerial)) {
			if(Constants.DEBUG)Log.w(TAG, "SIM has been changed from the initial registered SIM \n\t CURRENT SIM LINE: " + currentSimLine + " DOES NOT MATCH.  \n\t CURRENT SIM SERIAL: " + currentSimSerial + " DOES NOT MATCH.");
			simChanged = true;
		}
		return simChanged;
	}

	private int logNewSimChange() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		Long lastSimChange = settings.getLong(Constants.LAST_SIM_CHANGE, 0);
		int simChangeCount = settings.getInt(Constants.SIM_CHANGE_COUNT, 0);
		Long now = System.currentTimeMillis();

		settings.edit().putLong(Constants.LAST_SIM_CHANGE, now).commit();

		Long deltaSimChange = now - lastSimChange;
		int week = 1000 * 60 * 60 * 24 * 7;
		if (deltaSimChange < week) {
			// count if booted with unregsitered SIM in last week
			simChangeCount++;
		} else {
			simChangeCount = 0;
		}

		settings.edit().putInt(Constants.SIM_CHANGE_COUNT, simChangeCount).commit();

		return simChangeCount;
	}

}
