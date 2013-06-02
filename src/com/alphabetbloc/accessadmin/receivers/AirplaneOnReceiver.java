/**
 * 
 */
package com.alphabetbloc.accessadmin.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.alphabetbloc.accessadmin.data.Policy;

/**
 * Used only to turn airplane mode on during device installation if someone
 * turns airplane mode off. InitialSetupActivity turns airplane mode on to
 * prevent the user from skipping over InitialSetupActivity by receiving an SMS,
 * then pressing the home button to get out of app. This receiver monitors to
 * see if the user toggles airplane mode off (via power button) and then turns
 * airplane mode back on.This is meant to increase the probability that the user
 * will have to go through the entire InitialSetupActivity to secure the device
 * with Admin, password and provider ID)
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class AirplaneOnReceiver extends BroadcastReceiver {

	public AirplaneOnReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("AirplaneOnReceiver", "Airplane Receiver is receiving!");
		Policy policy = new Policy(context);

		// if device is not setup:
		if (!policy.isDeviceSecured()) {

			// if airplane mode is off, then turn it on...
			boolean enabled = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			if (!enabled) {
				Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
				Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				i.putExtra("state", 1);
				context.sendBroadcast(i);

			}
		}

	}
}
