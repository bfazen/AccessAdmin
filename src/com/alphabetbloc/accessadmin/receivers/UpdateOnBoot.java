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
				Intent i = new Intent(context, InitialSetupActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);

			} else if (policy.isAdminActive()) {
				// Check on Security
				if (SimChanged()) {
					Intent i = new Intent(mContext, DeviceAdminService.class);
					i.putExtra(Constants.DEVICE_ADMIN_WORK, mSecurityCode);
					mContext.startService(i);
				}
				if (!policy.isDeviceSecured()) {
					Intent i = new Intent(context, InitialSetupActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
				}

			}

		}

	}

	private boolean SimChanged() {
		boolean simChanged = false;

		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String registeredSimSerial = settings.getString(Constants.SIM_SERIAL, "");
		String registeredSimLine = settings.getString(Constants.SIM_LINE, "");
		String currentSimSerial = tm.getSimSerialNumber();
		String currentSimLine = tm.getLine1Number();

		if (!currentSimLine.equals(registeredSimLine) || !currentSimSerial.equals(registeredSimSerial)) {
			// SIM changed!
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

			if (simChangeCount < 3) {
				// Send new SIM to DeviceAdmin (assumes airplane mode off)
				mSecurityCode = Constants.SEND_SIM;
			} else {
				// Phone booted with unregistered SIM 3 times in <3 weeks, so we
				// assume Lost or Stolen:
				mSecurityCode = Constants.WIPE_ODK_DATA;
			}

			simChanged = true;
			Log.w(TAG, "SIM has been changed from the initial registered SIM");
		}

		return simChanged;
	}

}
