package com.alphabetbloc.accessadmin.services;

import java.util.ArrayList;
import java.util.Collections;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.activities.MessageHoldActivity;
import com.alphabetbloc.accessadmin.activities.SetAppPreferences;
import com.alphabetbloc.accessadmin.activities.SetUserPassword;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.data.StringGenerator;
import com.alphabetbloc.accessadmin.receivers.DeviceAdmin;
import com.alphabetbloc.accessadmin.receivers.LockPhoneReceiver;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.commonsware.cwac.wakeful.WakelockWorkListener;
import com.commonsware.cwac.wakeful.WakelockWorkReceiver;

/**
 * Service used to conduct all device admin work. The service holds a wakelock
 * while doing device admin work by extending @CommonsWare's
 * WakefulIntentService. The service takes care of scheduling an alarm to
 * continue the device admin work even if there is a reboot or if the process is
 * killed. The alarm will continue to wake the device at the time specified in
 * the WakelockListener, and try to complete the work through a wakelock on
 * boot. The service will then cancel its own alarms on successful completion of
 * its intent.
 * 
 * <p>
 * DEVICE_ADMIN_WORK type extras: <br>
 * 1. SEND_SMS: sends SMS with a repeat alarm until sent 2. SEND_GPS: Sending an
 * SMS with GPS coordinates<br>
 * 3. VERIFY_SIM: Sending SMS with new serial and line when SIM is changed<br>
 * 4. LOCK_SCREEN: Locks the Screen<br>
 * 5. WIPE_ODK_DATA: Wiping the patient sensitive data<br>
 * 6. WIPE_DATA: Wiping the entire device to factory reset (will allow user to
 * setup new device)<br>
 * 7. RESET_TO_DEFAULT_PWD: Resetting password to a default, depends on what the
 * password quality is<br>
 * 8. LOCK_RANDOM_PWD: Resetting password to a random string (so as to
 * permanently lock device until reset password to default)<br>
 * 9. HOLD_DEVICE: Starting MessageHoldActivity to send message to user before
 * e.g. locking the device.<br>
 * 10. CANCEL_ALARMS: Reset all alarms<br>
 * <p>
 * To use this service and ensure a wakelock, do not call directly, but call
 * through WakefulIntentService. First, create an intent for
 * DeviceAdminService.class and add intent extras to resolve the action for
 * DeviceAdminService. Then pass the intent through the sendWakefulWork method:
 * <br>
 * <br>
 * <b>Example:</b>
 * <p>
 * Intent i = new Intent(mContext, DeviceAdminService.class);<br>
 * i.putExtra(Constants.DEVICE_ADMIN_WORK, deviceAdminAction); <br>
 * WakefulIntentService.sendWakefulWork(mContext, i);<br>
 * </p>
 * 
 * @author Louis.Fazen@gmail.com
 * 
 */

public class DeviceAdminService extends WakefulIntentService {

	DevicePolicyManager mDPM;
	ComponentName mDeviceAdmin;
	SharedPreferences mPrefs;
	Policy mPolicy;

	private Context mContext;
	private int mAirplaneCount = 0;

	private static final String TAG = DeviceAdminService.class.getSimpleName();
	private static final int SIM_VERIFIED = 1;
	private static final int SIM_MISSING = 2;
	private static final int SIM_CHANGED = 3;

	private boolean mResetAlarm;

	public DeviceAdminService() {
		super("AppService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {

		if (Constants.DEBUG)
			Log.d(TAG, "DeviceAdminService is Called");
		mContext = getApplicationContext();
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(DeviceAdminService.this, DeviceAdmin.class);
		mPolicy = new Policy(mContext);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		resolveIntent(intent);
	}

	private void resolveIntent(Intent intent) {
		String smsLine;
		String smsMessage;

		int standingIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
		int smsIntent = intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0);
		if (Constants.DEBUG)
			Log.v(TAG, "DeviceAdminService Called with \n\tStandingIntent=" + standingIntent + " \n\tNewSMSIntent=" + smsIntent);

		// May be new Intent or called from BOOT... so resolve intent:
		if (smsIntent != 0) {
			// we have a new intent from SMS
			smsLine = intent.getStringExtra(Constants.SMS_LINE);
			smsMessage = intent.getStringExtra(Constants.SMS_MESSAGE);
			if (smsLine == null)
				smsLine = Constants.DEFAULT_SMS_REPLY_LINE;
			if (smsMessage == null)
				smsMessage = "";

			// Reset alarm only for higher order intents
			if (smsIntent >= standingIntent)
				resetAlarm(smsIntent, smsLine, smsMessage);
			else
				mResetAlarm = false;

		} else {
			// Service called by alarm or boot, so recreate intent from saved
			// (after boot or kill, intent extras would be lost)
			// do not reset alarms
			smsIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
			if (Constants.DEBUG)
				Log.d(TAG, "DeviceAdminService is Called with smsIntentSaved=" + smsIntent);
			smsLine = mPrefs.getString(Constants.SAVED_SMS_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
			smsMessage = mPrefs.getString(Constants.SAVED_SMS_MESSAGE, "");

			// ALWAYS Reset the Alarm if CancelAdminAlarms is called from Alarm
			// or Boot because there should be no other alarms saved
			mResetAlarm = true;
		}

		switch (smsIntent) {
		case Constants.LOCK_SCREEN:
			lockDevice();
			break;
		case Constants.HOLD_DEVICE:
			holdAdminMessage(smsMessage);
			break;
		case Constants.LOCK_RANDOM_PWD:
			lockPromptPassword();
			break;
		case Constants.HOLD_DEVICE_LOCKED:
			holdDeviceLocked();
			break;
		case Constants.STOP_HOLD_DEVICE:
			stopHoldDevice();
			break;
		case Constants.EDIT_ACCESS_MRS_PREF:
			editAccessMrsPreference(smsMessage);
			break;
		case Constants.RESET_PWD_TO_SMS_PWD:
			resetPromptPassword(smsMessage);
			break;
		case Constants.RESET_TO_DEFAULT_PWD:
			resetPromptPassword();
			break;
		case Constants.SEND_SMS:
			sendRepeatingSMS(smsIntent, smsLine, smsMessage);
			break;
		case Constants.SEND_GPS:
			sendGPSCoordinates();
			break;
		case Constants.RESET_ADMIN_ID:
			resetSmsAdminId();
			break;
		case Constants.SEND_ADMIN_ID:
			smsAdminId();
			break;
		case Constants.VERIFY_SIM:
			verifySIMCode();
			break;
		case Constants.WIPE_ODK_DATA:
			wipeOdkData();
			break;
		case Constants.WIPE_DATA:
			wipeDevice();
			break;
		case Constants.FACTORY_RESET:
			factoryReset();
			break;
		case Constants.CANCEL_ALARMS:
			cancelAdminAlarms(smsMessage);
			break;
		default:
			cancelAdminAlarms(null);
			if (Constants.DEBUG)
				Log.e(TAG, "No Intent Received or Saved.");
			break;
		}
	}

	private void editAccessMrsPreference(String smsMessage) {

		int equals = smsMessage.indexOf("=");
		String preferenceKey = smsMessage.substring(0, equals);
		String preferenceValue = smsMessage.substring(equals + 1);

		Intent i = new Intent(SetAppPreferences.ACCESS_MRS_SET_PREFERENCE);
		i.putExtra(SetAppPreferences.PREFERENCE_KEY, preferenceKey);
		i.putExtra(SetAppPreferences.PREFERENCE_VALUE, preferenceValue);
		sendBroadcast(i);

		cancelAdminAlarms("Requested change to AccessMRS preference \'" + preferenceKey + "\'.");
	}

	/**
	 * Holds the device in an activity the user can not leave, and posts a wait
	 * message to the user in a dialog box.
	 * 
	 */
	private void holdAdminMessage(String message) {
		holdDevice(Constants.ADMIN_MESSAGE, message, null, null);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (MessageHoldActivity.sMessageHoldActive)
			cancelAdminAlarms("Now holding device with admin message.");
		else
			sendSingleSMS("Unable to hold device. Alarm is still active.");
	}

	/**
	 * WARNING! PERMANENT! DOES NOT CANCEL ALARMS! THIS CAN ONLY BE RESET BY
	 * ADMIN.
	 * 
	 * Convenience Method for holdDevice(int, message, message, message) to
	 * holds the device with the device locked message.
	 */
	private void holdDeviceLocked() {
		
		// Dont repeat the intent if already active
		if (MessageHoldActivity.sMessageHoldActive && MessageHoldActivity.sHoldType == Constants.DEVICE_LOCKED)
			return;

		if (MessageHoldActivity.sMessageHoldActive && MessageHoldActivity.sPermanentHold) {
			if (Constants.DEBUG)
				Log.e(TAG, "Updating ongoing activity with HoldType=" + MessageHoldActivity.sHoldType);
			
			// Update current activity if already locked in another activity type
			MessageHoldActivity.sHoldType = Constants.DEVICE_LOCKED;
			MessageHoldActivity.sMessage = getString(R.string.locked_message);
			MessageHoldActivity.sSubMessage = getString(R.string.return_thin_message);
			MessageHoldActivity.sAdditionalInfo = null;

		} else {

			// Start new activity
			Intent i = new Intent(mContext, MessageHoldActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(Constants.HOLD_TYPE, Constants.DEVICE_LOCKED);
			i.putExtra(MessageHoldActivity.MESSAGE, getString(R.string.locked_message));
			i.putExtra(MessageHoldActivity.SUBMESSAGE, getString(R.string.return_thin_message));
			mContext.startActivity(i);
		
		}

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (MessageHoldActivity.sMessageHoldActive)
			sendSingleSMS("Currently holding device. Alarm is still active.");
		else
			sendSingleSMS("Unable to hold device. Alarm is still active.");
	}

	/**
	 * WARNING! PERMANENT! DOES NOT CANCEL ALARMS! THIS CAN ONLY BE RESET BY
	 * ADMIN.
	 * 
	 * Holds the device in an activity the user can not leave, and posts one or
	 * more messages for the user. The view (holdType) may either be an admin
	 * message, or alarm view.
	 * 
	 * 
	 * 
	 */
	private void holdDevice(int holdType, String message, String subMessage, String additionalInfo) {
		if (Constants.DEBUG)
			Log.e(TAG, "Hold Device is being called with MessageHoldType=" + holdType);

		Intent i = new Intent(mContext, MessageHoldActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.putExtra(Constants.HOLD_TYPE, holdType);
		if (message != null)
			i.putExtra(MessageHoldActivity.MESSAGE, message);
		if (subMessage != null)
			i.putExtra(MessageHoldActivity.SUBMESSAGE, subMessage);
		if (additionalInfo != null)
			i.putExtra(MessageHoldActivity.ADDITIONAL_INFO, additionalInfo);
		mContext.startActivity(i);

	}

	private void stopHoldDevice() {
		if (Constants.DEBUG)
			Log.e(TAG, "StopHoldDevice is being called.");
		Intent i = new Intent(mContext, MessageHoldActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(MessageHoldActivity.STOP_HOLD, true);
		mContext.startActivity(i);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (!MessageHoldActivity.sMessageHoldActive)
			cancelAdminAlarms("Stopped device hold.");
		else
			sendSingleSMS("Unable to stop device hold. Alarm is still active.");
	}

	/**
	 * Locks the device but does not change the password. Sends an confirmation
	 * SMS to the reporting line.
	 * 
	 */
	public void lockDevice() {
		if (Constants.DEBUG)
			Log.d(TAG, "locking the device");
		mDPM.lockNow();

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 2);
		if (isDeviceLocked())
			cancelAdminAlarms("Locked device.");
		else
			sendSingleSMS("Unable to lock device. Alarm is still active.");
	}

	/**
	 * Check whether device is locked (either screen asleep or on lock screen).
	 * 
	 * @return true if device is locked
	 */
	public boolean isDeviceLocked() {
		KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mPolicy.isActivePasswordSufficient();
		if (myKM.inKeyguardRestrictedInputMode() && mPolicy.isActivePasswordSufficient()) {
			if (Constants.DEBUG)
				Log.d(TAG, "screen is locked");
			return true;
		} else if (!pm.isScreenOn() && mPolicy.isActivePasswordSufficient()) {
			if (Constants.DEBUG)
				Log.d(TAG, "screen is off and password protected.");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * WARNING! THIS IS PERMANENT. DOES NOT CANCEL ALARMS BECAUSE IT WIPES
	 * ENTIRE DEVICE.
	 * 
	 * Factory reset. This method is not called directly, but only after patient
	 * data has been wiped.
	 */
	private void factoryReset() {
		sendSingleSMS("Performing a Factory Reset");
		// wait a minute for SMS to send, then perform factory reset
		android.os.SystemClock.sleep(1000 * 60);
		mDPM.wipeData(0);
	}

	/**
	 * WARNING! THIS IS PERMANENT. DOES NOT CANCEL ALARMS BECAUSE IT WIPES
	 * ENTIRE DEVICE.
	 * 
	 * This method wipes all client data from AccessMRS. After confirmation, it
	 * requests factory reset.
	 */
	public void wipeDevice() {
		// NB. we don't monitor this, we simply clear Client data. On completion
		// client data will be wiped (managed by AccessMRS). On receipt of
		// a broadcast to wipe data and if preference is set, then this process
		// will resume and the factoryReset() method will be called.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putBoolean(Constants.PERFORM_FACTORY_RESET, true).commit();
		wipeOdkData();
	}

	/**
	 * WARNING! THIS IS PERMANENT. DOES NOT CANCEL ALARMS BECAUSE WE WAIT TO
	 * CONFIRM THE BROADCAST BACK FROM ACCESSMRS BEFORE CANCELLING THE ALARM.
	 * 
	 * Wipes ODK Data. The alarm is only cancelled after a broadcast has been
	 * received from AccessMRS confirming all patient data was wiped. Otherwise,
	 * it will continue to send the broadcast request to wipe all client data.
	 */
	public void wipeOdkData() {
		// we don't check this one, because we dont manage the process
		// instead we wait for a broadcast to tell us to send a message
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putBoolean(Constants.PERFORM_FACTORY_RESET, false).commit();

		if (Constants.DEBUG)
			Log.e(TAG, "wiping client data from device");
		sendSingleSMS("Wiping Client Data");

		Intent i = new Intent(Constants.WIPE_DATA_SERVICE);
		i.putExtra(Constants.WIPE_DATA_FROM_ADMIN_SMS_REQUEST, true);
		sendBroadcast(i);
	}

	/**
	 * Resets the AdminID code for sending SMS, and sends an SMS with the new
	 * Admin ID to the reporting line.
	 */
	public void resetSmsAdminId() {

		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String oldAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");
		String rAlphaNum = (new StringGenerator(15)).getRandomAlphaNumericString();
		prefs.edit().putString(Constants.UNIQUE_DEVICE_ID, rAlphaNum).commit();
		String newAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");

		if (!newAdminId.equals(oldAdminId))
			smsAdminId(); // This will in turn call CancelAdminAlarms
		else
			sendSingleSMS("Unable to reset Admin ID. Alarm is still active.");
	}

	private void smsAdminId() {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String adminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");
		sendSingleSMS("New AdminId =" + adminId);
		cancelAdminAlarms("Admin code has now been changed.");
	}

	// ////////////// PASSWORD RESET //////////////
	public void resetPromptPassword() {
		// TODO Feature: allow admin to edit this default value in a Preference
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String defaultPwd = prefs.getString(Constants.DEFAULT_PASSWORD, "12345");
		boolean success = resetPassword(defaultPwd);
		showPasswordResetScreen();

		if (success)
			cancelAdminAlarms("Reset password to default.");
		else
			sendSingleSMS("Failed to reset password to default. Alarm is still active.");
	}

	public void resetPromptPassword(String tempPassword) {
		boolean success = resetPassword(tempPassword);
		showPasswordResetScreen();

		if (success)
			cancelAdminAlarms("Reset password to SMS request.");
		else
			sendSingleSMS("Failed to reset password to SMS request. Alarm is still active.");
	}

	/**
	 * Resets the password to a default string that follows the device admin
	 * policy, and sends an SMS confirmation to the reporting line.
	 */
	private boolean resetPassword(String tempPassword) {
		boolean reset = false;

		int pwdLength = mPolicy.getPasswordLength();
		int pwdQuality = mPolicy.getPasswordQuality();

		// Set to Temporary Password
		mPolicy.setPasswordLength(5);
		mPolicy.setPasswordQuality(2);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (mDPM.resetPassword(tempPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY))
			reset = true;
		mDPM.lockNow();

		// Reset Policy to force pwd change (IF stronger than tempPassword)
		mPolicy.setPasswordLength(pwdLength);
		mPolicy.setPasswordQuality(pwdQuality);

		if (reset) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			settings.edit().putBoolean(Constants.SIM_ERROR_PHONE_LOCKED, false).commit();
		}
		return reset;
	}

	public void lockPromptPassword() {
		boolean success = lockRandomPassword(true);
		showPasswordResetScreen();

		if (success)
			cancelAdminAlarms("Reset password to new random string.");
		else
			sendSingleSMS("Unable to reset to new random password. Alarm is still active.");

	}

	/**
	 * Locks the device with a random secure string that only the device knows.
	 * The only way to unlock device is through sending an sms to reset the
	 * password to default. Also sends an SMS to the reporting line.
	 */
	private boolean lockRandomPassword(boolean sendPassword) {
		boolean randomLock = false;
		Policy policy = new Policy(mContext);
		String pwd = policy.createNewSecretPwd();
		if (policy.resetPassword(pwd)) {
			randomLock = true;

			if (sendPassword)
				sendSingleSMS("Device successfully locked with new password=" + pwd);
		}

		mDPM.lockNow();

		if (randomLock) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			settings.edit().putBoolean(Constants.SIM_ERROR_PHONE_LOCKED, true).commit();
		}
		return randomLock;
	}

	private void showPasswordResetScreen() {
		Intent i = new Intent(mContext, SetUserPassword.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(SetUserPassword.SUGGEST_RESET_PASSWORD, true);
		mContext.startActivity(i);
	}

	// ////////////// CANCEL ALARMS //////////////
	private void resetAlarm(int smsIntent, String smsMessage) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String smsLine = prefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
		resetAlarm(smsIntent, smsLine, smsMessage);
	}

	private void resetAlarm(int smsIntent, String smsLine, String smsMessage) {

		mResetAlarm = true;

		// kill any old alarms so only 1 active device admin process
		// (all alarms should have same simple pi)
		cancelAlarms(mContext);

		// schedule new alarm to continue after kill or reboot
		mPrefs.edit().putInt(Constants.SAVED_DEVICE_ADMIN_WORK, smsIntent).commit();
		mPrefs.edit().putString(Constants.SAVED_SMS_LINE, smsLine).commit();
		mPrefs.edit().putString(Constants.SAVED_SMS_MESSAGE, smsMessage).commit();
		scheduleAlarms(new WakelockWorkListener(), mContext, true);
	}

	/**
	 * Cancels all Device Admin Alarms, regardless of type. Sends an SMS to the
	 * reporting line when complete.
	 */
	public void cancelAdminAlarms(String message) {
		if (!mResetAlarm)
			return;

		// Cancel everything
		cancelAlarms(mContext);
		mPrefs.edit().putInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0).commit();
		mPrefs.edit().remove(Constants.SAVED_SMS_LINE).commit();
		mPrefs.edit().remove(Constants.SAVED_SMS_MESSAGE).commit();

		// confirm that this worked before asking about alarms
		android.os.SystemClock.sleep(1000 * 2);
		if (!isAdminAlarmActive()) {
			if (message != null)
				sendSingleSMS(message + " All alarms now cancelled.");
		} else {
			// Should never happen. Set an alarm to cancel alarms!
			message = "Alarm:" + message;
			resetAlarm(Constants.CANCEL_ALARMS, message);
			if (Constants.DEBUG)
				Log.d(TAG, "Something went wrong... alarms are not canceling");
		}
	}

	/**
	 * Indicates whether there is an existing device admin alarm. There is only
	 * one alarm active at any given time.
	 * 
	 * @return true if alarm is active
	 */
	public boolean isAdminAlarmActive() {
		Intent i = new Intent(mContext, WakelockWorkReceiver.class);
		return (PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_NO_CREATE) != null);
	}

	// ////////////// SEND GPS //////////////
	/**
	 * Send an SMS with the current location (by either GPS or network) to the
	 * default reporting line. SMS message body is of the form: <br>
	 * "Time=#################### Lat=################### Lon=################# Alt=########### Acc=###"
	 * "
	 * 
	 */
	public void sendGPSCoordinates() {
		if (Constants.DEBUG)
			Log.d(TAG, "sending GPS");
		// taken from RMaps
		final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Location loc1 = lm.getLastKnownLocation("gps");
		final Location loc2 = lm.getLastKnownLocation("network");

		boolean boolGpsEnabled = lm.isProviderEnabled("gps");
		boolean boolNetworkEnabled = lm.isProviderEnabled("network");
		String str = "";
		Location loc = null;

		if (loc1 == null && loc2 != null)
			loc = loc2;
		else if (loc1 != null && loc2 == null)
			loc = loc1;
		else if (loc1 == null && loc2 == null)
			loc = null;
		else
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;

		if (boolGpsEnabled) {
		} else if (boolNetworkEnabled)
			str = getString(R.string.message_gpsdisabled);
		else if (loc == null)
			str = getString(R.string.message_locationunavailable);
		else
			str = getString(R.string.message_lastknownlocation);
		if (str.length() > 0)
			Log.d(TAG, str);

		StringBuilder sb = new StringBuilder();

		if (loc != null) {
			sb.append("Time=");
			sb.append(String.valueOf(loc.getTime()));
			sb.append(" Lat=");
			sb.append(String.valueOf(loc.getLatitude()));
			sb.append(" Lon=");
			sb.append(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude()) {
				sb.append(" Alt=");
				sb.append(String.valueOf(loc.getAltitude()));
			}
			if (loc.hasAccuracy()) {
				sb.append(" Acc=");
				sb.append(String.valueOf(loc.getAccuracy()));
			}
		} else {
			sb.append("No location available");
		}

		sendRepeatingSMS(Constants.SEND_GPS, sb.toString()); // This will in
																// turn call
																// CancelAdminAlarms

	}

	// ////////////// SEND SIM CODE //////////////
	/**
	 * Verify that the SIM Code has in fact changed by toggling Airplane Mode on
	 * if necessary. If changed, log change and send the SIM. If not change,
	 * then cancelAlarms.
	 * 
	 */
	public void verifySIMCode() {
		// TODO Feature: make this into a real preference...
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean useSimLock = settings.getBoolean(Constants.USE_SIM_LOCK, true);
		if (!useSimLock) {
			cancelAdminAlarms(null);
			return;
		}

		int simStatus = checkSimStatus();
		boolean wipeData = logSimChange(simStatus);
		int count = 0;
		switch (simStatus) {
		case SIM_VERIFIED:
			if (Constants.DEBUG)
				Log.v(TAG, "SIM Verified after Airplane Mode Was Turned Off");
			cancelAdminAlarms(null);
			return;
		case SIM_CHANGED:
			count = settings.getInt(Constants.SIM_CHANGE_COUNT, 0);
			if (count < 2)
				holdDevice(Constants.SIM_ERROR, getString(R.string.sim_message_replace_sim), getString(R.string.sim_submessage_lock_phone), getString(R.string.return_info_message));
			else
				holdDevice(Constants.SIM_ERROR, getString(R.string.sim_message_replace_sim), getString(R.string.sim_submessage_attempts_change, count), getString(R.string.return_info_message));
			break;
		case SIM_MISSING:
		default:
			count = settings.getInt(Constants.SIM_MISSING_COUNT, 0);
			if (count < 2)
				holdDevice(Constants.SIM_ERROR, getString(R.string.sim_message_replace_sim), getString(R.string.sim_submessage_lock_phone), getString(R.string.return_info_message));
			else
				holdDevice(Constants.SIM_ERROR, getString(R.string.sim_message_replace_sim), getString(R.string.sim_submessage_attempts_missing, count), getString(R.string.return_info_message));
			break;
		}

		// SIM is missing or changed!
		// 1. Send Repeating SMS with SIM Code
		resetAlarm(Constants.SEND_SMS, makeSIMCodeSms());

		// 2. Set Alarm to lock the phone and potentially wipe data
		setLockPhoneAlarm(wipeData);
	}

	private void setLockPhoneAlarm(boolean wipeData) {
		Intent i = new Intent(mContext, LockPhoneReceiver.class);
		i.putExtra(Constants.SIM_ERROR_WIPE_DATA, wipeData);
		PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, 0);

		AlarmManager aM = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		aM.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3, pi);
	}

	private int checkSimStatus() {
		int simStatus = 0;

		// Toggle Airplane Mode if necessary
		boolean enabled = Settings.System.getInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (enabled)
			turnAirplaneModeOff();

		// Get Current SIM
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String registeredSimSerial = settings.getString(Constants.SIM_SERIAL, null);
		String registeredSimLine = settings.getString(Constants.SIM_LINE, null);
		TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String currentSimSerial = tm.getSimSerialNumber();
		String currentSimLine = tm.getLine1Number();
		if (currentSimLine == null || currentSimSerial == null || currentSimSerial.equals("")) {
			if (Constants.DEBUG)
				Log.w(TAG, "SIM has been taken out of phone or is not registering with device \n\t CURRENT SIM LINE: " + currentSimLine + " \n\t CURRENT SIM SERIAL: " + currentSimSerial);
			simStatus = SIM_MISSING;

		} else if (!currentSimLine.equals(registeredSimLine) || !currentSimSerial.equals(registeredSimSerial)) {
			if (Constants.DEBUG)
				Log.w(TAG, "SIM has been changed from the initial registered SIM \n\t CURRENT SIM LINE: " + currentSimLine + " DOES NOT MATCH.  \n\t CURRENT SIM SERIAL: " + currentSimSerial + " DOES NOT MATCH.");
			simStatus = SIM_CHANGED;

		} else if (currentSimLine.equals(registeredSimLine) && currentSimSerial.equals(registeredSimSerial)) {
			simStatus = SIM_VERIFIED;
		}

		return simStatus;
	}

	private boolean turnAirplaneModeOff() {

		boolean enabled = Settings.System.getInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;

		if (enabled && mAirplaneCount < 4) {
			// Try to turn airplane mode off
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
			Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			i.putExtra("state", 0);
			mContext.sendBroadcast(i);
			if (Constants.DEBUG)
				Log.v(TAG, "turning off airplane mode");
			android.os.SystemClock.sleep(1000 * 5);

			mAirplaneCount++;
			turnAirplaneModeOff();
		} else {
			// Airplane Mode is off, or not able to turn off
			mAirplaneCount = 0;
		}

		return enabled;
	}

	private boolean logSimChange(int simStatus) {
		boolean wipeData = false;

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String simCountPeriodPref = "";
		String simCountPref = "";
		String simThresholdPref = "";

		switch (simStatus) {
		case SIM_VERIFIED:
			return wipeData;
		case SIM_CHANGED:
			simThresholdPref = Constants.SIM_CHANGE_THRESHOLD;
			simCountPeriodPref = Constants.SIM_CHANGE_RESET_PERIOD;
			simCountPref = Constants.SIM_CHANGE_COUNT;
			break;
		case SIM_MISSING:
		default:
			simThresholdPref = Constants.SIM_MISSING_THRESHOLD;
			simCountPeriodPref = Constants.SIM_MISSING_RESET_PERIOD;
			simCountPref = Constants.SIM_MISSING_COUNT;
			break;
		}

		// Get threshold for locking phone
		int simLockThreshold = settings.getInt(simThresholdPref, 7);

		// Record all most recent SIM Changes in the Count Period set in Prefs
		int simCountPeriod = settings.getInt(simCountPeriodPref, 7);
		Long now = System.currentTimeMillis();
		int days = 1000 * 60 * 60 * 24;

		// Remove any previous lock that does not fall in the count period
		ArrayList<Long> recentSimChange = new ArrayList<Long>();
		for (int i = 0; i < simLockThreshold; i++) {
			long previousLockTime = settings.getLong(simCountPref + "_" + i, 0);
			if (previousLockTime > 0) {
				long deltaSimChange = now - previousLockTime;
				if (deltaSimChange < (simCountPeriod * days))
					recentSimChange.add(previousLockTime);
			}
		}

		// If already at lock threshold, replace oldest SIM change with current
		// one
		if (recentSimChange.size() == simLockThreshold)
			recentSimChange.remove(Collections.min(recentSimChange));
		recentSimChange.add(now);

		// Save the new list of SIM changes to preferences
		SharedPreferences.Editor editor = settings.edit();
		for (int i = 0; i < recentSimChange.size(); i++)
			editor.putLong(simCountPref + "_" + i, recentSimChange.get(i));

		// Save the count of recent locks for quick reference
		editor.putInt(simCountPref, recentSimChange.size());
		editor.commit();

		if (recentSimChange.size() >= simLockThreshold)
			wipeData = true;

		if (Constants.DEBUG)
			Log.e(TAG, "Logging that the SIM has been changed! \n\t Current SIM Change Count=" + recentSimChange.size() + "\n\t Requires WIPE DATA=" + wipeData);

		return wipeData;
	}

	/**
	 * Send an SMS with the current SIM code to the default reporting line. SMS
	 * message body is of the form: <br>
	 * "IMEI=#################### New SIM=########## Serial=##############"
	 * 
	 */
	public String makeSIMCodeSms() {
		TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		StringBuilder sb = new StringBuilder();

		String imei = tm.getDeviceId();
		String line = tm.getLine1Number();
		String serial = tm.getSimSerialNumber();

		if (imei != null) {
			sb.append("IMEI=");
			sb.append(imei);
		}

		if (line != null) {
			sb.append(" New SIM=");
			sb.append(line);
		}

		if (serial != null) {
			sb.append(" Serial=");
			sb.append(serial);
		}

		if (line == null && serial == null) {
			sb.append("Could not obtain SIM information");
		}
		return sb.toString();
	}

	// ////////////// SEND SMS //////////////
	/**
	 * Send an SMS associated with a wakelock alarm. Will monitor and cancel the
	 * alarm once the message has been sent.
	 * 
	 * @param smstype
	 *            The intent int extra that specifies the type of SMS to be sent
	 *            (GPS coordinates, SIM code, general)
	 * @param smsline
	 *            The phone number to send the SMS
	 * @param newmessage
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendRepeatingSMS(int smstype, String line, String message) {
		String lastSentAdminMessage = mPrefs.getString(String.valueOf(smstype), "");
		String outgoingSms = SendSMSService.SMS_REPLY_PREFIX + message;
		if (outgoingSms.equals(lastSentAdminMessage)) {
			if (Constants.DEBUG)
				Log.d(TAG, "Message has already been sent. Alarm Cancelled and Prefs erased.");

			cancelAdminAlarms("All SMS now confirmed as successfully sent.");
			mPrefs.edit().putString(String.valueOf(smstype), "").commit();
		} else {
			if (Constants.DEBUG)
				Log.d(TAG, "Attempting to send a repeating SMS with \n\t ID=" + smstype + "\n\t MESSAGE=" + message);
			ComponentName comp = new ComponentName(mContext.getPackageName(), SendSMSService.class.getName());
			Intent i = new Intent();
			i.setComponent(comp);
			i.putExtra(Constants.SMS_SENT_CONFIRMATION, true);
			i.putExtra(Constants.DEVICE_ADMIN_WORK, smstype);
			i.putExtra(Constants.SMS_LINE, line);
			i.putExtra(Constants.SMS_MESSAGE, message);
			mContext.startService(i);
		}
	}

	public void sendRepeatingSMS(int smstype, String message) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
		sendRepeatingSMS(smstype, line, message);
	}

	/**
	 * Send an single SMS to the reporting line from SharedPreferences.
	 * 
	 * @param message
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendSingleSMS(String message) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, Constants.DEFAULT_SMS_REPLY_LINE);
		sendSingleSMS(line, message);
	}

	/**
	 * Send an single SMS with desired message and body.
	 * 
	 * @param line
	 *            The phone number to send the SMS
	 * @param message
	 *            The body of the SMS (should not be longer than 160
	 *            characters).
	 */
	public void sendSingleSMS(String line, String message) {
		ComponentName comp = new ComponentName(mContext.getPackageName(), SendSMSService.class.getName());
		Intent i = new Intent();
		i.setComponent(comp);
		i.putExtra(Constants.SMS_LINE, line);
		i.putExtra(Constants.SMS_MESSAGE, message);
		mContext.startService(i);

	}

}
