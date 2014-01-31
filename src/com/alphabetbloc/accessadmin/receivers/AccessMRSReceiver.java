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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.alphabetbloc.accessadmin.services.SendSMSService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Receives confirmation from AccessMRS after all client data has been wiped.
 * Starts service to do factory reset or to hold the device depending on what
 * the original intent was.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class AccessMRSReceiver extends BroadcastReceiver {

	private static final String TAG = AccessMRSReceiver.class.getSimpleName();

	public AccessMRSReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Constants.WIPE_DATA_COMPLETE)) {

			// Cancel existing alarms
			WakefulIntentService.cancelAlarms(context);

			// Continue to perform factory reset if necessary
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean resetDevice = prefs.getBoolean(Constants.PERFORM_FACTORY_RESET, false);
			if (resetDevice) {
				Intent i = new Intent(context, DeviceAdminService.class);
				i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.FACTORY_RESET);
				WakefulIntentService.sendWakefulWork(context, i);

			} else {
				// Send SMS confirmation that data has been wiped
				final SharedPreferences encPrefs = new EncryptedPreferences(context, context.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
				String line = encPrefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
				Intent smsI = new Intent(context, SendSMSService.class);
				smsI.putExtra(Constants.SMS_LINE, line);
				smsI.putExtra(Constants.SMS_MESSAGE, "Patient Data Successfully Deleted.");
				context.startService(smsI);

				// Show Hold Screen permanently- THIS IS PERMANENT, as ALARM
				// needs to be cancelled by admin
				Intent holdI = new Intent(context, DeviceAdminService.class);
				holdI.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.HOLD_DEVICE_LOCKED);
				WakefulIntentService.sendWakefulWork(context, holdI);
			}

			// Record the data has been wiped successfully
			if (Constants.DEBUG)
				Log.e(TAG, "Recieved AccessMRS Notice that Data has been Successfully Wiped");

		}
	}

}
