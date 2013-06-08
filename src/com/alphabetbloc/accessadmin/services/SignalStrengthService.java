package com.alphabetbloc.accessadmin.services;

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

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.activities.AdminLoginActivity;
import com.alphabetbloc.accessadmin.data.Constants;

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
	static final String NAME = "com.alphabetbloc.accessmrs.android.RefreshDataActivity";

	private NotificationManager mNM;
	private int NOTIFICATION = 1;
	private static int countN;
	private static int countS;
	private TelephonyManager mTelephonyManager;
	private PhoneStateListener mPhoneStateListener;
	private static final String TAG = SignalStrengthService.class.getSimpleName();
	public static final String REFRESH_BROADCAST = "com.alphabetbloc.accessmrs.services.SignalStrengthService";
	// CM7
	public static final String MOBILE_DATA_CHANGED = "com.alphabetbloc.android.telephony.MOBILE_DATA_CHANGED";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		showNotification();

		mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mPhoneStateListener = new PhoneStateListener() {
			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				int asu = signalStrength.getGsmSignalStrength();

				if (asu >= 7 && asu < 32) {
					if (dataNetworkAvailable())
						refreshClientsNow();
				} else if (asu < 1 || asu > 32 || countS++ > 8) {
					stopSelf();
				}
				if (Constants.DEBUG)
					Log.e(TAG, "asu=" + asu + " countN=" + countN + " countS=" + countS);
				super.onSignalStrengthsChanged(signalStrength);
			}

			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				if (Constants.DEBUG)
					Log.d(TAG, "Service State changed! New state = " + serviceState.getState());
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


	public boolean dataNetworkAvailable() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}


	private void refreshClientsNow() {

	}

	private void showNotification() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		CharSequence text = getText(R.string.app_name);
		Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AdminLoginActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
		mNM.notify(NOTIFICATION, notification);
	}

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
	
	@Override
	public void onDestroy() {
		if (Constants.DEBUG)
			Log.d(TAG, "Shutting down the Service" + TAG);
		mNM.cancel(NOTIFICATION);
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		super.onDestroy();
	}

}