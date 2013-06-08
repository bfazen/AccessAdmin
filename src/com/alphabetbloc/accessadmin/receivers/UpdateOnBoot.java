package com.alphabetbloc.accessadmin.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.accessadmin.activities.InitialSetupActivity;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.alphabetbloc.accessadmin.services.SendSMSService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Checks for a new install, a change in the SIM code, and device security
 * before passing on the intent to the appropriate activity or service. Also
 * launches the UpdateClockService.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class UpdateOnBoot extends BroadcastReceiver {

	private static final String TAG = UpdateOnBoot.class.getSimpleName();
	private Context mContext;

	public UpdateOnBoot() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;

		if (Constants.BOOT_COMPLETED.equals(intent.getAction())) {
			if (Constants.DEBUG)
				Log.v(TAG, "Boot Receiver is receiving!");

			// Always verify the System Time
			updateSystemClock();

			// Always check security...
			Policy policy = new Policy(mContext);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			boolean newInstall = settings.getBoolean(Constants.NEW_INSTALL, true);
			boolean phoneLocked = settings.getBoolean(Constants.SIM_ERROR_PHONE_LOCKED, false);

			if (newInstall) {
				// Setup Initial Security
				Intent i = new Intent(mContext, InitialSetupActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);

			} else if (phoneLocked) {
				// Send SMS confirmation that data has been wiped
				final SharedPreferences encPrefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
				String line = encPrefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
				Intent smsI = new Intent(mContext, SendSMSService.class);
				smsI.putExtra(Constants.SMS_LINE, line);
				smsI.putExtra(Constants.SMS_MESSAGE, "This device is booting again after being locked. Will wipe data SOON.");
				mContext.startService(smsI);

				// Hold the device in a perpetual locked state. THIS IS
				// PERMANENT, as ALARM needs to be cancelled by admin
				Intent i = new Intent(context, DeviceAdminService.class);
				i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.HOLD_DEVICE_LOCKED);
				WakefulIntentService.sendWakefulWork(context, i);

			} else if (policy.isAdminActive()) {
				// Check on Security

				// 1. Check on simChange
				if (isSimChanged()) {
					Intent i = new Intent(mContext, DeviceAdminService.class);
					i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.VERIFY_SIM);
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

	private void updateSystemClock() {
		// always ask user to verify the clock...
		Intent timeIntent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
		timeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(timeIntent);

		// set an alarm for 5 minutes to verify the time
		Intent i = new Intent(mContext, UpdateClockReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, 0);
		AlarmManager aM = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

		aM.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + (AlarmManager.INTERVAL_FIFTEEN_MINUTES / 2) , AlarmManager.INTERVAL_HOUR, pi);
	}

	
	private boolean isSimChanged() {
		boolean simChanged = false;

		// Get Current SIM
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String registeredSimSerial = settings.getString(Constants.SIM_SERIAL, null);
		String registeredSimLine = settings.getString(Constants.SIM_LINE, null);
		if (Constants.DEBUG)
			Log.e(TAG, "Registered SIM: \n\t REGISTERED SIM LINE=\'" + registeredSimLine + "\' \n\t REGISTERED SIM SERIAL\'" + registeredSimSerial + "\'");

		// TODO Feature: Need to include the Preference For SIM LOCK so that you
		// can turn on and off SIM Lock at anytime and reset the registered SIM
		if (registeredSimLine == null || registeredSimSerial == null) {
			if (Constants.DEBUG)
				Log.e(TAG, "Registered SIM has been lost! Requesting Initial Security Setup! \n\t REGISTERED SIM LINE: " + registeredSimLine + " \n\t REGISTERED SIM SERIAL: " + registeredSimSerial);
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

		if (currentSimLine == null || currentSimSerial == null || currentSimSerial.equals("")) {
			if (Constants.DEBUG)
				Log.w(TAG, "SIM has been taken out of phone or is not registering with device \n\t CURRENT SIM LINE: " + currentSimLine + " \n\t CURRENT SIM SERIAL: " + currentSimSerial);
			simChanged = true;
		} else if (!currentSimLine.equals(registeredSimLine) || !currentSimSerial.equals(registeredSimSerial)) {
			if (Constants.DEBUG)
				Log.w(TAG, "SIM has been changed from the initial registered SIM \n\t CURRENT SIM LINE: " + currentSimLine + " DOES NOT MATCH.  \n\t CURRENT SIM SERIAL: " + currentSimSerial + " DOES NOT MATCH.");
			simChanged = true;
		}
		if (Constants.DEBUG)
			Log.e(TAG, "simChanged=" + simChanged);

		return simChanged;
	}

}
