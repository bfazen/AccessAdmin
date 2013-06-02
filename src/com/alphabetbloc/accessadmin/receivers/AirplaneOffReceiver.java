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
 * Used only to turn airplane mode off during MessageHoldActivity if someone tries to turn airplane on.  Ensures that there is connectivity to the device during the message hold activity. 
 * This receiver monitors to see if the user toggles airplane mode on (via power button) and then turns
 * airplane mode back off. This is meant to increase the probability that the admin will have access to the phone during an update.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class AirplaneOffReceiver extends BroadcastReceiver {

	public AirplaneOffReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v("AirplaneOnReceiver", "Airplane Off Receiver is receiving!");
		Policy policy = new Policy(context);

		// if device is setup:
		if (policy.isDeviceSecured()) {

			// if airplane mode is on, then turn it off...
			boolean enabled = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			if (enabled) {
				Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
				Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				i.putExtra("state", 0);
				context.sendBroadcast(i);

			}
		}

	}
}
