package com.alphabetbloc.chvsettings.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.activities.AdminLoginActivity;

/**
 * 
 * @author Louis Fazen (louis.fazen@gmail.com) (excerpts from curioustechizen
 *         from stackoverflow)
 * 
 *         This checks the signal strength, data connectivity and user activity
 *         before refreshing the patient list as background service.
 */
public class SignalStrengthService extends Service {

	private static volatile PowerManager.WakeLock lockStatic = null;
	static final String NAME = "com.alphabetbloc.clinic.android.RefreshDataActivity";

	private NotificationManager mNM;
	private int NOTIFICATION = 1;
	private static int countN;
	private static int countS;
	private TelephonyManager mTelephonyManager;
	private PhoneStateListener mPhoneStateListener;
	private static final String TAG = "SignalStrengthService";
	public static final String REFRESH_BROADCAST = "com.alphabetbloc.clinic.services.SignalStrengthService";
	// CM7
	public static final String MOBILE_DATA_CHANGED = "com.alphabetbloc.android.telephony.MOBILE_DATA_CHANGED";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		createWakeLock();
		showNotification();

		mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mPhoneStateListener = new PhoneStateListener() {
			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				int asu = signalStrength.getGsmSignalStrength();

				if (asu >= 7 && asu < 32) {
					if (networkAvailable())
						refreshClientsNow();
					else if (countN++ > 5)
						updateService();
				} else if (asu < 1 || asu > 32 || countS++ > 8)
					stopSelf();

				Log.e(TAG, "asu=" + asu + " countN=" + countN + " countS=" + countS);
				super.onSignalStrengthsChanged(signalStrength);
			}

			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				Log.d("louis.fazen", "Service State changed! New state = " + serviceState.getState());
				super.onServiceStateChanged(serviceState);
			}
		};
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
		return super.onStartCommand(intent, flags, startId);
	}

	private void showNotification() {

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		CharSequence text = getText(R.string.app_name);
		Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AdminLoginActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
		mNM.notify(NOTIFICATION, notification);

		// if (notification != null) {
		// startForeground(NOTIFICATION, notification);
		// Log.e(TAG, "SignalStrengthService Started in Foreground");
		// }
	}

	private boolean networkAvailable() {
		boolean dataNetwork = false;
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetworkInfo != null)
			dataNetwork = true;
		return dataNetwork;
	}

	// TODO! Update the service connection!!!
	private void updateService() {
		// if 2G, then update to 3G?
		int nt = mTelephonyManager.getNetworkType();
		if (nt < 3)
			Log.d(TAG, "network type =" + nt);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		Intent launchIntent = new Intent(MOBILE_DATA_CHANGED);
		// sendBroadcast(launchIntent);
		PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, launchIntent, 0);
		Log.e(TAG, "Sending a broadcast intent to change the network!");
		/*
		 * Intent launchIntent = new Intent(); launchIntent.setClass(context,
		 * SettingsAppWidgetProvider.class);
		 * launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		 * launchIntent.setData(Uri.parse("custom:" + buttonId)); PendingIntent
		 * pi = PendingIntent.getBroadcast(getApplicationContext(), 0,
		 * launchIntent, 0);
		 */

		countS = 0;
		countN = 0;

		/*
		 * int iconLevel = -1; if (asu <= 2 || asu == 99) iconLevel = 0; // 0 or
		 * 99 = no signal else if (asu >= 12) iconLevel = 4; // very good signal
		 * else if (asu >= 8) iconLevel = 3; // good signal else if (asu >= 5)
		 * iconLevel = 2; // poor signal else iconLevel = 1; // <5 is very poor
		 * signal
		 */

		/*
		 * switch (nt) { case 1: return GPRS; case 2: return EDGE; case 3:
		 * return UMTS; case 8: return HSDPA; case 9: return HSUPA; case 10:
		 * return HSPA; default: return UNKNOWN; }
		 */

	}

	private void refreshClientsNow() {

	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Shutting down the Service" + TAG);
		mNM.cancel(NOTIFICATION);
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		countS = 0;
		countN = 0;

		// then call:
		if (lockStatic.isHeld()) {
			lockStatic.release();
			Log.e("louis.fazen", "Called lockStatic.release()=" + lockStatic.toString());
		}
		super.onDestroy();
	}

	private void createWakeLock() {
		// first call:
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
			lockStatic.setReferenceCounted(true);
			lockStatic.acquire();

			Log.e("louis.fazen", "lockStatic.acquire()=" + lockStatic.toString());

			// PowerManager pm = (PowerManager)
			// getSystemService(Context.POWER_SERVICE);
			// lockStatic =
			// mgr.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,
			// "bbbb");
			// lockStatic.acquire();

			// may need:
			// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
			// WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
			// WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		}

	}

	// TODO! NO LONGER NEEDED IN CLINIC.... we dont need this method anymore,
	// because sync is
	// automated?!
	// private boolean isSyncNeeded(SyncResult syncResult) {
	//
	// // establish threshold for syncing (i.e. do not sync continuously)
	// long recentDownload = Db.open().fetchMostRecentDownload();
	// long timeSinceRefresh = System.currentTimeMillis() - recentDownload;
	// SharedPreferences prefs =
	// PreferenceManager.getDefaultSharedPreferences(App.getApp());
	// String maxRefreshSeconds =
	// prefs.getString(App.getApp().getString(R.string.key_max_refresh_seconds),
	// App.getApp().getString(R.string.default_max_refresh_seconds));
	// long maxRefreshMs = 1000L * Long.valueOf(maxRefreshSeconds);
	//
	// Log.e(TAG, "Minutes since last refresh: " + timeSinceRefresh / (1000 *
	// 60));
	// if (timeSinceRefresh < maxRefreshMs) {
	//
	// long timeToNextSync = maxRefreshMs - timeSinceRefresh;
	// syncResult.delayUntil = timeToNextSync;
	// Log.e(TAG, "Synced recently... lets delay the sync until ! timetosync=" +
	// timeToNextSync);
	// return false;
	//
	// } else {
	// return true;
	// }
	//
	// }

}