package com.alphabetbloc.chvsettings.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;
import com.alphabetbloc.chvsettings.data.StringGenerator;

/**
 * Do NOT call on its own, should only be called with a repeating
 * wakelock alarm through the Device Admin Service (e.g. with SEND_SMS intent
 * extra). <br>
 * <br>
 * Service sends SMS, checks for it being sent, waits for it to be logged in the
 * outbox, and then deletes the SMS. Can be called more than once, will wait for
 * a default time before killing itself, but no longer than 6x the default
 * time after the last pending SMS has been registered with the service. Intent
 * requires default of smsType, smsLine, and smsMessage Intent Extras.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class SendSMSService extends Service {

	public static final String TAG = "SendSMSService";
	private Context mContext;
	private int mSentSMS;
	private int mPendingSMS;
	private int mDeletedSMS;
	private long mPendingTime;
	private long mDeleteTime;
	private static long mWait;
	private static long mStopTime;
	private static final long WAIT_FOR_DELETE = 1000 * 45;

	@Override
	public IBinder onBind(Intent intent) {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		mPendingSMS = 0;
		mSentSMS = 0;
		mDeletedSMS = 0;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int smsBroadcast = intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0);
		String phoneNumber = intent.getStringExtra(Constants.SMS_LINE);
		String message = intent.getStringExtra(Constants.SMS_MESSAGE);
		sendSMS(smsBroadcast, phoneNumber, message);
		return super.onStartCommand(intent, flags, startId);
	}

	private void sendSMS(int smsBroadcast, String phoneNumber, String message) {
		String body = "!Reply!: " + message;
		String SENT = "SMS_SENT";

		// SETUP RECEIVE
		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
		SMSSentReceiver sendingSMS = new SMSSentReceiver();
		sendingSMS.addSMS(smsBroadcast, phoneNumber, body);
		registerReceiver(sendingSMS, new IntentFilter(SENT));

		// SETUP DELETE
		SentObserver sentObserver = new SentObserver();
		ContentResolver contentResolver = mContext.getContentResolver();
		contentResolver.registerContentObserver(Uri.parse("content://sms/out"), true, sentObserver);

		// SEND SMS
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, body, sentPI, null);

		// SETUP STOP SERVICE
		mPendingSMS++;
		mPendingTime = System.currentTimeMillis();

		// If no SMS sent, kill service to allow for Wakeful Alarm resend
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mSentSMS == 0)
					stopSelf();
			}
		}, WAIT_FOR_DELETE);
	}

	public class SMSSentReceiver extends BroadcastReceiver {
		int sentSmsType;
		String sentNumber = "";
		String sentMessage = "";

		public void addSMS(int smsBroadcast, String phoneNumber, String message) {
			sentSmsType = smsBroadcast;
			sentNumber = phoneNumber;
			sentMessage = message;
		}

		@Override
		public void onReceive(Context ctxt, Intent intent) {
			int result = getResultCode();
			if (result == Activity.RESULT_OK) {
				// log a successfully sent message
				mSentSMS++;
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
				pref.edit().putString(String.valueOf(sentSmsType), sentMessage).commit();

				// If no SMS ever deleted after send, then kill service
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mDeletedSMS == 0)
							stopSelf();
					}
				}, WAIT_FOR_DELETE);
			}
		}
	}

	// Need observer b/c delay between receive and move to outbox
	class SentObserver extends ContentObserver {

		public SentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			deleteSMS();
			super.onChange(selfChange);
		}
	}

	public void deleteSMS() {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
		try {
			Uri uriSms = Uri.parse("content://sms/out");
			Cursor c = mContext.getContentResolver().query(uriSms, new String[] { "_id", "address", "body" }, "address =? ", new String[] { line }, null);

			if (c != null && c.moveToFirst()) {
				do {
					long id = c.getLong(c.getColumnIndex("_id"));
					String address = c.getString(c.getColumnIndex("address"));
					String body = c.getString(c.getColumnIndex("body"));

					if (body.contains("!Reply!:") && address.equals(line)) {
						mContext.getContentResolver().delete(Uri.parse("content://sms/" + id), null, null);
						mDeletedSMS++;
						mDeleteTime = System.currentTimeMillis();
					}
				} while (c.moveToNext());
			}

			c.close();
		} catch (Exception e) {
			Log.e(TAG, "Could not delete SMS from inbox: " + e.getMessage());
		}

		stopService();
	}

	private void stopService() {

		if (mPendingSMS <= mDeletedSMS) {
			stopSelf();
		} else {
			// Kill service after 30s inactivity (between pending and delete)
			// or else just kill after 5 minutes of sending SMS
			new Thread(new Runnable() {
				public void run() {
					mWait = Math.abs(mDeleteTime - mPendingTime);

					while ((mWait < WAIT_FOR_DELETE) || mStopTime < (WAIT_FOR_DELETE * 6)) {

						try {
							mWait = Math.abs(mDeleteTime - mPendingTime);
							mStopTime = System.currentTimeMillis() - mPendingTime;
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// Auto-generated catch block
							e.printStackTrace();
						}
					}
					stopSelf();
				}
			}).start();
		}
	}

	// SEND SMS METHOD
	// ---when the SMS has been delivered---
	// Delivery intent: could add later, but does not add much purpose
	// do via xml so service does not linger waiting for receiver to finish
	// http://stackoverflow.com/questions/5624470/enable-and-disable-a-broadcast-receiver
	// String DELIVERED = "SMS_DELIVERED";
	// PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new
	// Intent(DELIVERED), 0);
	// SMSDeliverReceiver deliveredSMS = new SMSDeliverReceiver();
	// deliveredSMS.addSMS(phoneNumber, body);
	// registerReceiver(deliveredSMS, new IntentFilter(DELIVERED));

}