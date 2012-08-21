/***
  Copyright (c) 2009-11 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.wakeful.old;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class WakelockListener_old implements WakefulIntentService_old.AlarmListener {

	public void scheduleAlarms(AlarmManager mgr, PendingIntent pi, Context ctxt) {

		Log.e("louis.fazen", "scheduling alarms... ");
		mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() +1000, AlarmManager.INTERVAL_HOUR, pi);

	}

	//0. louis.fazen is adding an intent here...
	public void sendWakefulWork(Context ctxt, Intent i) {
		// and passing the intent to do the work...
		Log.e("louis.fazen", "sendWakefulWork is called");
		WakefulIntentService_old.sendWakefulWork(ctxt, i);
	}

	public long getMaxAge() {
		// if interval between alarms is > maxAge, then alarm probably was reset
		// by force-stopping application in settings,
		// so we need to reestablish the Alarm. This logic is taken care of by
		// WakefulIntentService_old scheduleAlarms()
		// Here we only set the maxAge paramater, suggested to be 2xInterval by
		// CommonsWare
		return (AlarmManager.INTERVAL_HOUR * 2);
	}
}
