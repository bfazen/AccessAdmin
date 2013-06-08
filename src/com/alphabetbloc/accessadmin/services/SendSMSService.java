package com.alphabetbloc.accessadmin.services;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;

/**
 * Do NOT call on its own, should only be called with a repeating wakelock alarm
 * through the Device Admin Service (e.g. with SEND_SMS intent extra). <br>
 * <br>
 * Service sends SMS, checks for it being sent, waits for it to be logged in the
 * outbox, and then deletes the SMS. Can be called more than once, will wait for
 * a default time before killing itself, but no longer than 6x the default time
 * after the last pending SMS has been registered with the service. Intent
 * requires default of smsType, smsLine, and smsMessage Intent Extras.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class SendSMSService extends Service {

	public static final String TAG = "SendSMSService";
	private static final String SMS_SENT = "SMS_SENT";

	private Context mContext;
	private ArrayList<SMS> mPendingSms = new ArrayList<SMS>();
	private SMSSentReceiver mSmsSentReceiver;
	private ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(5);
	private SentOutboxObserver mSentObserver = null;
	private ContentResolver mContentResolver = null;

	private SMS mCurrentSms;
	private boolean mSentSms;
	private boolean mDeletedSms;
	private int mMessageCount;
	private int mDeleteCount;

	@Override
	public IBinder onBind(Intent intent) {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		mMessageCount = 0;
		mDeleteCount = 0;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mMessageCount++;
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		if (intent != null) {

			int broadcast = intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0);
			String phoneNumber = intent.getStringExtra(Constants.SMS_LINE);
			String message = intent.getStringExtra(Constants.SMS_MESSAGE);
			if (phoneNumber == null)
				phoneNumber = prefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
			if (message == null)
				message = "Message has been lost";
			SMS sms = new SMS(mMessageCount, broadcast, phoneNumber, message);
			mPendingSms.add(sms);
			if (Constants.DEBUG)
				Log.v(TAG, "SendSMSService.onStartCommand Called with " + "\n\t MESSAGE= " + message + "\n\t TO= \'" + phoneNumber + "\'" + "\n\t PENDING SMS SIZE=" + mPendingSms.size() + "\n\t MESSAGE COUNT=" + mMessageCount);

			if (mMessageCount == 1) {
				setupReceivers();
				sendNextSms();
				if (Constants.DEBUG)
					Log.v(TAG, "SendSMSService Setting up receivers and sending First SMS");
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void setupReceivers() {

		// Receive notification when message sends
		if (mSmsSentReceiver == null) {
			mSmsSentReceiver = new SMSSentReceiver();
			registerReceiver(mSmsSentReceiver, new IntentFilter(SMS_SENT));
		}

		// Receive notification when/if message is entered into SMS 'Sent Box'
		// N.B. Deals with few AOSP mods that receive all outgoing SMS in outbox
		// SMS sent programmatically usually do not enter SMS app's SENT.db
		if (mSentObserver == null) {
			mSentObserver = new SentOutboxObserver();
			mContentResolver = mContext.getContentResolver();
			mContentResolver.registerContentObserver(Uri.parse("content://sms"), true, mSentObserver);
		}
	}

	public void sendNextSms() {
		mCurrentSms = mPendingSms.get(0);
		mSentSms = false;
		mDeletedSms = false;

		mSmsSentReceiver.addSMS(mCurrentSms);

		// Send Sms
		SmsManager smsManager = SmsManager.getDefault();
		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
		smsManager.sendTextMessage(mCurrentSms.getNumber(), null, mCurrentSms.getMessage(), sentPI, null);
		if (Constants.DEBUG)
			Log.v(TAG, "SENDING NEW SMS!: \n\t NUMBER =" + mCurrentSms.getNumber() + "\n\t MESSAGE=" + mCurrentSms.getMessage());

		// Monitor Delivery
		updateSmsDelivery();
	}

	private void updateSmsDelivery() {

		mExecutor.schedule(new Runnable() {
			int count = 0;

			public void run() {
				boolean completedSms = mSentSms & mDeletedSms; // don't
																// short-circuit

				if (Constants.DEBUG)
					Log.v(TAG, "updateSmsDelivery with: \n\t SmsSent =" + mSentSms + "\n\t SmsDeleted=" + mDeletedSms);

				if (!completedSms && count < 10) { // TODO CHANGE THIS BACK TO
													// <60
					mExecutor.schedule(this, 3000, TimeUnit.MILLISECONDS);
					if (Constants.DEBUG)
						Log.v(TAG, "Wait another 3 seconds. Current cycle count=" + count);
					count++;
				} else {
					// Log errors
					if (Constants.DEBUG) {
						if (!mSentSms)
							Log.e(TAG, "Timed out after 3 minutes of waiting for SMS to send");
						else if (!mDeletedSms)
							Log.e(TAG, "Timed out after 3 minutes of waiting for SMS to delete");
						else
							Log.i(TAG, "SMS has been successfully sent and deleted from outbox");
					}

					// delete the SMS if haven't done so
					if (!mSentSms && mPendingSms.size() > 0) {
						if (Constants.DEBUG)
							Log.e(TAG, "COULD NOT SEND SMS!! Removing SMS 0 from mPendingSms: \n\t SMS MESSAGE=" + mPendingSms.get(0).getMessage());
						mPendingSms.remove(0);
					}

					if (Constants.DEBUG)
						Log.v(TAG, "SMS messages that are still pending=" + mPendingSms.size());

					// send next SMS, if there is one
					if (mPendingSms.size() > 0)
						sendNextSms();
					else
						stopService();

				}

			}
		}, 0, TimeUnit.MILLISECONDS);
	}

	public class SMSSentReceiver extends BroadcastReceiver {
		SMS sms = null;

		public void addSMS(SMS currentSms) {
			sms = currentSms;
		}

		@Override
		public void onReceive(Context ctxt, Intent intent) {
			int result = getResultCode();
			if (result == Activity.RESULT_OK) {
				// log last successfully sent message
				mSentSms = true;
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
				pref.edit().putString(String.valueOf(sms.getBroadcast()), sms.getMessage()).commit();

				for (SMS currentSms : mPendingSms) {
					if (sms.getId() == currentSms.getId()) {
						int oldtotal = mPendingSms.size();
						mPendingSms.remove(currentSms);
						if (Constants.DEBUG)
							Log.v(TAG, "Removed an SMS from the Pending List  (1/" + oldtotal + ") total. Removed SMS had \n\t ID=" + sms.getId() + "\n\t MESSAGE=" + sms.getMessage() + "\n\t Number of SMS that are still on pending list=" + mPendingSms.size());
						break;
					}
				}
			}
		}
	}

	// Need observer b/c delay between receive and move to outbox
	class SentOutboxObserver extends ContentObserver {

		public SentOutboxObserver() {
			super(null);
			if (Constants.DEBUG)
				Log.v(TAG, "New SentOutbox Observer Created!");
		}

		@Override
		public void onChange(boolean selfChange) {
			if (Constants.DEBUG)
				Log.i(TAG, "Change to SMS Outbox. Calling deleteSmsFromOutbox to delete single SMS");
			deleteSmsFromOutbox(false);
			super.onChange(selfChange);
		}
	}

	public void deleteSmsFromOutbox(boolean deleteAll) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = null;
		String message = null;

		if (deleteAll) {
			line = prefs.getString(Constants.SMS_REPLY_LINE, "");
			message = "!Reply!:";
		} else {
			line = mCurrentSms.getNumber();
			message = mCurrentSms.getMessage();
		}

		if (Constants.DEBUG)
			Log.v(TAG, "Calling deleteSmsFromOutbox with: \n\t DELETE-ALL=" + deleteAll + "\n\t LINE=" + line + "\n\t MESSAGE=" + message);

		try {
			Uri uriSms = Uri.parse("content://sms");
			Cursor c = mContext.getContentResolver().query(uriSms, new String[] { "_id", "address", "body" }, "address =? ", new String[] { line }, null);

			if (c != null && c.moveToFirst()) {
				do {
					long id = c.getLong(c.getColumnIndex("_id"));
					String address = c.getString(c.getColumnIndex("address"));
					String body = c.getString(c.getColumnIndex("body"));

					if (Constants.DEBUG)
						Log.v(TAG, "Found SMS in the Outbox: \n\t ID=" + id + "\n\t ADDRESS=" + address + "\n\t BODY=" + body);

					if (body.contains(message) && address.equalsIgnoreCase(line)) {
						int rows = mContext.getContentResolver().delete(Uri.parse("content://sms/" + id), null, null);
						mDeletedSms = true;
						mDeleteCount++;
						if (Constants.DEBUG)
							Log.v(TAG, "Successfully deleted " + rows + " sms from the outbox");
					}
				} while (c.moveToNext());
			}

			c.close();
		} catch (Exception e) {
			if (Constants.DEBUG)
				Log.e(TAG, "Could not delete SMS from inbox: " + e.getMessage());
		}

	}

	// JUnit Testing
	public ArrayList<SMS> getPendingSMS() {
		return mPendingSms;
	}

	private void stopService() {
		if (mMessageCount >= mDeleteCount)
			deleteSmsFromOutbox(true);

		stopSelf();
	}

	@Override
	public void onDestroy() {
		if (Constants.DEBUG)
			Log.v(TAG, "Ending the SendSmsService");
		unregisterReceiver(mSmsSentReceiver);
		mSmsSentReceiver = null;
		mContentResolver.unregisterContentObserver(mSentObserver);
		mSentObserver = null;
		super.onDestroy();
	}

	// JUnit Testing, had to make this class public
	public class SMS {
		private int smsBroadcast;
		private int smsId;
		private String smsNumber;
		private String smsMessage;

		public SMS(int id, int broadcast, String phoneNumber, String message) {
			smsBroadcast = broadcast;
			smsNumber = phoneNumber;
			smsMessage = "!Reply!: " + message;
			smsId = id;
		}

		public int getBroadcast() {
			return smsBroadcast;
		}

		public String getNumber() {
			return smsNumber;
		}

		public String getMessage() {
			return smsMessage;
		}

		public int getId() {
			return smsId;
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