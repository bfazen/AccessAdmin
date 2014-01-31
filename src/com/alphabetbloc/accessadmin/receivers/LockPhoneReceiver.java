/*
 * Copyright (C) 2012 Louis Fazen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
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

		// 1. Show LOCK message PERMANENTLY. Can ONLY be reset by Admin Code.
		Intent holdI = new Intent(context, DeviceAdminService.class);
		holdI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.HOLD_DEVICE_LOCKED);
		WakefulIntentService.sendWakefulWork(context, holdI);

		// 2. Lock the phone with random password and present password prompt
		Intent passwordI = new Intent(context, DeviceAdminService.class);
		passwordI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.LOCK_RANDOM_PWD);
		WakefulIntentService.sendWakefulWork(context, passwordI);

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
