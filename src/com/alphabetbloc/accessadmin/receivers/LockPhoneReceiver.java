/**
 * 
 */
package com.alphabetbloc.accessadmin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Locks phone after SIM has been changed. Receives an alarm that phone has not
 * been turned off after 5 minutes of an alarm requesting SIM Change.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class LockPhoneReceiver extends BroadcastReceiver {

	private static final String TAG = LockPhoneReceiver.class.getSimpleName();

	public LockPhoneReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		// 1. Lock the phone with random password and present password prompt
		Intent passwordI = new Intent(context, DeviceAdminService.class);
		passwordI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.LOCK_RANDOM_PWD);
		WakefulIntentService.sendWakefulWork(context, passwordI);

		// 2. Show LOCK message PERMANENTLY. Can ONLY be reset by Admin Code.
		Intent holdI = new Intent(context, DeviceAdminService.class);
		holdI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.HOLD_DEVICE_LOCKED);
		WakefulIntentService.sendWakefulWork(context, holdI);

		// 3. Check if phone booted wrong SIM too many times and may be stolen
		boolean wipeData = intent.getBooleanExtra(Constants.SIM_ERROR_WIPE_DATA, false);
		if (wipeData) {
			Intent wipeI = new Intent(context, DeviceAdminService.class);
			wipeI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.WIPE_ODK_DATA);
			WakefulIntentService.sendWakefulWork(context, wipeI);
		}

		if (Constants.DEBUG)
			Log.e(TAG, "Recieved Notice to Lock Phone after too many SIM Changes with with WipeData=" + wipeData);
	}

}
