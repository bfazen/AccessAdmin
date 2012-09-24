/**
 * 
 */
package com.alphabetbloc.chvsettings.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;
import com.alphabetbloc.chvsettings.data.Policy;
import com.alphabetbloc.chvsettings.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Receives and parses SMS messages, sends the intent on to DeviceAdminService,
 * and blocks the SMS from being placed into the inbox.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class SmsReceiver extends BroadcastReceiver {

	private static final String TAG = "SmsReceiver";

	private static String Imei;
	private static String lockDevice;
	private static String sendGPS;
	private static String wipeData;
	private static String wipeSdOdk;
	private static String resetPwdToDefault;
	private static String lockSecretPwd;
	private static String resetAdminId;
	private static String holdScreen;
	private static String cancelAlarm;
	private static String mToast = null;
	private Context mContext;
	private SmsMessage[] mSms;
	private int mExtra;

	public SmsReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		Log.e("SmsReceiver", "Sms Receiver is receiving!");
		Policy policy = new Policy(context);
		if (intent.getAction().equals(Constants.SMS_RECEIVED) && policy.isAdminActive()) {
			Bundle bundle = intent.getExtras();

			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				mSms = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					mSms[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}

				if (mSms.length > -1) {
					readSMS();
				}

			}
		} else if (intent.getAction().equals(Constants.WIPE_DATA_COMPLETE)) {
			Intent i = new Intent(mContext, DeviceAdminService.class);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.FACTORY_RESET);
			WakefulIntentService.sendWakefulWork(mContext, i);
		}
	}

	// TODO! does this receiver need to be a wakelock receover, or do all
	// receivers have a wakelock for the duration of their onReceive?
	private void readSMS() {
		if (Imei == null)
			createSmsStrings();
		if (matchingSmsString()) {
			abortBroadcast();
			Intent i = new Intent(mContext, DeviceAdminService.class);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, mExtra);
			if (mToast != null)
				i.putExtra(Constants.SMS_MESSAGE, mToast);
			WakefulIntentService.sendWakefulWork(mContext, i);
		}

	}

	private void createSmsStrings() {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String smsAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, null);

		// REQUIRE smsAdminId:
		lockDevice = smsAdminId + Constants.SMS_CODE_LOCK;
		sendGPS = smsAdminId + Constants.SMS_CODE_GPS;
		wipeData = smsAdminId + Constants.SMS_CODE_WIPE_DATA;
		wipeSdOdk = smsAdminId + Constants.SMS_CODE_WIPE_ODK;
		holdScreen = smsAdminId + Constants.SMS_CODE_HOLD;
		cancelAlarm = smsAdminId + Constants.SMS_CODE_CANCEL_ALARM;
		resetPwdToDefault = smsAdminId + Constants.SMS_CODE_RESET_PWD_DEFAULT;

		// DO NOT REQUIRE smsAdminId:
		lockSecretPwd = Constants.SMS_CODE_RESET_PWD_SECRET;
		resetAdminId = Constants.SMS_CODE_RESET_ADMIN_ID;

	}

	private boolean matchingSmsString() {

		if (mSms[0].getMessageBody().equals(lockDevice)) {
			mExtra = Constants.LOCK_SCREEN;
			return true;
		} else if (mSms[0].getMessageBody().equals(sendGPS)) {
			mExtra = Constants.SEND_GPS;
			return true;
		} else if (mSms[0].getMessageBody().equals(wipeData)) {
			mExtra = Constants.WIPE_DATA;
			return true;
		} else if (mSms[0].getMessageBody().equals(wipeSdOdk)) {
			mExtra = Constants.WIPE_ODK_DATA;
			return true;
		} else if (mSms[0].getMessageBody().equals(lockSecretPwd)) {
			mExtra = Constants.LOCK_SECRET_PWD;
			return true;
		} else if (mSms[0].getMessageBody().equals(resetAdminId)) {
			mExtra = Constants.RESET_ADMIN_ID;
			return true;
		} else if (mSms[0].getMessageBody().equals(resetPwdToDefault)) {
			mExtra = Constants.RESET_TO_DEFAULT_PWD;
			return true;
		} else if (mSms[0].getMessageBody().equals(cancelAlarm)) {
			mExtra = Constants.CANCEL_ALARMS;
			return true;
		} else if (mSms[0].getMessageBody().contains(holdScreen)) {
			mExtra = Constants.HOLD_DEVICE;
			int toast = mSms[0].getMessageBody().indexOf(":");
			mToast = mSms[0].getMessageBody().substring(toast + 1, mSms.length);
			return true;
		} else {
			return false;
		}
	}
}
