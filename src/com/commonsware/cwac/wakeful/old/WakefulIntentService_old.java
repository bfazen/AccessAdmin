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
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

abstract public class WakefulIntentService_old extends IntentService {
	abstract protected void doWakefulWork(Intent intent);

	static final String NAME = "com.commonsware.cwac.wakeful.WakefulIntentService_old";
	static final String LAST_ALARM = "lastAlarm";
	private static volatile PowerManager.WakeLock lockStatic = null;

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
			lockStatic.setReferenceCounted(true);
		}

		return (lockStatic);
	}

	public static void sendWakefulWork(Context ctxt, Intent i) {
		getLock(ctxt.getApplicationContext()).acquire();
		ctxt.startService(i);
	}

	public static void sendWakefulWork(Context ctxt, Class<?> clsService) {
		sendWakefulWork(ctxt, new Intent(ctxt, clsService));
	}

	public static void scheduleAlarms(AlarmListener listener, Context ctxt) {
//		0. louis.fazen adjusting this method so it fits with the below method... by adding a default intent with no extras...
		Intent i=new Intent(ctxt, WakelockReceiver_old.class);
		scheduleAlarms(listener, ctxt, true, i);
	}
// 1. louis.fazen adding an intent to the alarm scheduling activity...
	public static void scheduleAlarms(AlarmListener listener, Context ctxt, boolean force, Intent i) {
		SharedPreferences prefs = ctxt.getSharedPreferences(NAME, 0);
		long lastAlarm = prefs.getLong(LAST_ALARM, 0);

		if (lastAlarm == 0 || force || (System.currentTimeMillis() > lastAlarm && System.currentTimeMillis() - lastAlarm > listener.getMaxAge())) {
			AlarmManager mgr = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
			// Intent i=new Intent(ctxt, WakelockWorkReceiver.class); //2. louis.fazen taking this from the calling activity rather than defining here...
			PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);

			listener.scheduleAlarms(mgr, pi, ctxt);
		}
	}

	public static void cancelAlarms(Context ctxt) {
		AlarmManager mgr = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ctxt, WakelockReceiver_old.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);

		mgr.cancel(pi);
	}

	public WakefulIntentService_old(String name) {
		super(name);
		setIntentRedelivery(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		PowerManager.WakeLock lock = getLock(this.getApplicationContext());

		if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
			lock.acquire();
		}

		super.onStartCommand(intent, flags, startId);

		return (START_REDELIVER_INTENT);
	}

	@Override
	final protected void onHandleIntent(Intent intent) {
		try {
			doWakefulWork(intent);
		} finally {
			PowerManager.WakeLock lock = getLock(this.getApplicationContext());

			if (lock.isHeld()) {
				lock.release();
			}
		}
	}

	public interface AlarmListener {
		void scheduleAlarms(AlarmManager mgr, PendingIntent pi, Context ctxt);
		//3. louis.fazen adding in an intent to sendWakefulWork as well:
		void sendWakefulWork(Context ctxt, Intent intent);

		long getMaxAge();

	}

	// public static void sendWakefulWork(Context ctxt, Class<?> clsService,
	// Intent incoming) {
	// Intent i = new Intent(ctxt, clsService);
	// //louis.fazen: not sure if this is going to work, because this is a
	// pending intent, holding an actual intent?
	// i.putExtra(Constants.SMS_BROADCAST,
	// incoming.getIntExtra(Constants.SMS_BROADCAST, 0));
	// i.putExtra(Constants.SMS_LINE,
	// incoming.getStringExtra(Constants.SMS_LINE));
	// i.putExtra(Constants.SMS_MESSAGE,
	// incoming.getStringExtra(Constants.SMS_MESSAGE));
	// sendWakefulWork(ctxt, i);
	// }
}
