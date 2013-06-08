/**
 * 
 */
package com.alphabetbloc.accessadmin.receivers;

import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.services.UpdateClockService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Starts service to check the time set, and update the clock.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class UpdateClockReceiver extends BroadcastReceiver {

	private static final String TAG = UpdateClockReceiver.class.getSimpleName();

	public UpdateClockReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Constants.DEBUG)
			Log.v(TAG, "Received Alarm. Starting the Update Clock Service.");
		
		context.startService(new Intent(context, UpdateClockService.class));

	}
}
