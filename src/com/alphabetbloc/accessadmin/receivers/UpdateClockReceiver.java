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
