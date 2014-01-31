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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Receives and parses SMS messages, sends the intent on to DeviceAdminService,
 * and blocks the SMS from being placed into the inbox.
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */

public class SmsReceiver extends BroadcastReceiver {

	 private static final String TAG = SmsReceiver.class.getSimpleName();

	private static String Imei;
	private static String lockDevice;
	private static String sendGPS;
	private static String wipeData;
	private static String wipeSdOdk;
	private static String resetPwdToDefault;
	private static String resetPwdToSmsPwd;
	private static String lockRandomPwd;
	private static String resetAdminId;
	private static String holdScreen;
	private static String stopHoldScreen;
	private static String cancelAlarm;
	private static String editAccessMrsPreference;
	private static String sendSms;
	private static String holdDeviceLocked;
	private static String verifySim;
	private static String mSmsMessage = null;
	private Context mContext;
	private int mExtra;

	public SmsReceiver() {
		// Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Constants.DEBUG)
			Log.v("SmsReceiver", "Received a new SMS");
		
		mContext = context;
		Policy policy = new Policy(context);
		if (intent.getAction().equals(Constants.SMS_RECEIVED) && policy.isAdminActive()) {
			Bundle bundle = intent.getExtras();

			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] smsMessage = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					smsMessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}

				if (smsMessage.length > -1) {
					String message = smsMessage[0].getMessageBody();
					if(Constants.DEBUG)
						Log.v("SmsReceiver", "Received a new SMS with \n\tMESSAGE=\'" + message + "\'");
					String prefix = Constants.SMS_CODE_ADMIN_PREFIX;
					if (message.length() > prefix.length()) {
						String substring = message.substring(0, prefix.length());
						if (substring.equalsIgnoreCase(Constants.SMS_CODE_ADMIN_PREFIX))
							readSMS(message);
					}
				}
			}

		} 
	}

	private void readSMS(String sms) {
		if (Imei == null)
			createSmsStrings();
		if (matchingSmsString(sms)) {
			try {
				abortBroadcast(); 
			} catch (Exception e) {
				Log.e(TAG, "Unordered broadcast"); 
			}
			
			Intent i = new Intent(mContext, DeviceAdminService.class);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, mExtra);
			if (mSmsMessage != null)
				i.putExtra(Constants.SMS_MESSAGE, mSmsMessage);
			WakefulIntentService.sendWakefulWork(mContext, i);
		}

	}

	private void createSmsStrings() {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String smsAdminCode = Constants.SMS_CODE_ADMIN_PREFIX + prefs.getString(Constants.UNIQUE_DEVICE_ID, null);
		if (Constants.DEBUG)
			Log.e("CODE", "smsCode=" + smsAdminCode);
		// REQUIRE smsAdminCode:
		lockDevice = smsAdminCode + Constants.SMS_CODE_LOCK;
		sendGPS = smsAdminCode + Constants.SMS_CODE_GPS;
		wipeData = smsAdminCode + Constants.SMS_CODE_WIPE_DATA;
		wipeSdOdk = smsAdminCode + Constants.SMS_CODE_WIPE_ODK;
		holdScreen = smsAdminCode + Constants.SMS_CODE_HOLD;
		stopHoldScreen = smsAdminCode + Constants.SMS_CODE_STOP_HOLD;
		cancelAlarm = smsAdminCode + Constants.SMS_CODE_CANCEL_ALARM;
		resetPwdToDefault = smsAdminCode + Constants.SMS_CODE_RESET_PWD_DEFAULT;
		resetPwdToSmsPwd = smsAdminCode + Constants.SMS_CODE_RESET_PWD_TO_SMS_PWD;
		editAccessMrsPreference = smsAdminCode + Constants.SMS_CODE_EDIT_ACCESS_MRS_PREF;
		sendSms = smsAdminCode + Constants.SMS_CODE_SEND_SMS;
		holdDeviceLocked = smsAdminCode + Constants.SMS_CODE_HOLD_LOCKED;
		verifySim = smsAdminCode + Constants.SMS_CODE_VERIFY_SIM;
		
		// DO NOT REQUIRE smsAdminCode:
		lockRandomPwd = Constants.SMS_CODE_ADMIN_PREFIX + Constants.SMS_CODE_RESET_PWD_SECRET;
		resetAdminId = Constants.SMS_CODE_ADMIN_PREFIX + Constants.SMS_CODE_RESET_ADMIN_ID;

	}
	


	private boolean matchingSmsString(String sms) {

		if (sms.equals(lockDevice)) {
			mExtra = Constants.LOCK_SCREEN;
			return true;
		} else if (sms.equals(sendGPS)) {
			mExtra = Constants.SEND_GPS;
			return true;
		} else if (sms.equals(wipeData)) {
			mExtra = Constants.WIPE_DATA;
			return true;
		} else if (sms.equals(wipeSdOdk)) {
			mExtra = Constants.WIPE_ODK_DATA;
			return true;
		} else if (sms.equals(lockRandomPwd)) {
			mExtra = Constants.LOCK_RANDOM_PWD;
			return true;
		} else if (sms.equals(resetAdminId)) {
			mExtra = Constants.RESET_ADMIN_ID;
			return true;
		} else if (sms.equals(cancelAlarm)) {
			mExtra = Constants.CANCEL_ALARMS;
			return true;
		} else if (sms.equals(stopHoldScreen)) {
			mExtra = Constants.STOP_HOLD_DEVICE;
			return true;
		} else if (sms.equals(resetPwdToDefault)) {
			mExtra = Constants.RESET_TO_DEFAULT_PWD;
			return true;
		} else if (sms.equals(sendSms)) {
			mExtra = Constants.SEND_SMS;
			mSmsMessage = "AccessAdmin is active on this device. Version = " + mContext.getString(R.string.app_version);
			return true;
		} else if (sms.equals(holdDeviceLocked)) {
			mExtra = Constants.HOLD_DEVICE_LOCKED;
			return true;
		} else if (sms.equals(verifySim)) {
			mExtra = Constants.VERIFY_SIM;
			return true;
		} else if (sms.contains(editAccessMrsPreference)) {
			mExtra = Constants.EDIT_ACCESS_MRS_PREF;
			int message = sms.indexOf(":");
			mSmsMessage = sms.substring(message + 1);
			return true;
		} else if (sms.contains(resetPwdToSmsPwd)) {
			mExtra = Constants.RESET_PWD_TO_SMS_PWD;
			int message = sms.indexOf(":");
			mSmsMessage = sms.substring(message + 1);
			return true;
		} else if (sms.contains(holdScreen)) {
			mExtra = Constants.HOLD_DEVICE;
			int message = sms.indexOf(":");
			mSmsMessage = sms.substring(message + 1);
			return true;
		} else {
			return false;
		}
	}
}
