package com.alphabetbloc.chvsettings.services;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.activities.MessageHoldActivity;
import com.alphabetbloc.chvsettings.activities.SetUserPassword;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;
import com.alphabetbloc.chvsettings.data.Policy;
import com.alphabetbloc.chvsettings.data.StringGenerator;
import com.alphabetbloc.chvsettings.receivers.DeviceAdmin;
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
 * 3. SEND_SIM: Sending SMS with new serial and line when SIM is changed<br>
 * 4. LOCK_SCREEN: Locks the Screen<br>
 * 5. WIPE_ODK_DATA: Wiping the patient sensitive data<br>
 * 6. WIPE_DATA: Wiping the entire device to factory reset (will allow user to
 * setup new device)<br>
 * 7. RESET_TO_DEFAULT_PWD: Resetting password to a default, depends on what the
 * password quality is<br>
 * 8. LOCK_SECRET_PWD: Resetting password to a random string (so as to
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
	private static final String TAG = "DeviceAdminService";
	private static final String PERFORM_FACTORY_RESET = "perform_factory_reset";
	private boolean mResetAlarm;

	public DeviceAdminService() {
		super("AppService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {

		mContext = getApplicationContext();
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(DeviceAdminService.this, DeviceAdmin.class);
		mPolicy = new Policy(mContext);

		// May be new Intent or called from BOOT... so resolve intent:
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String smsLine;
		String smsMessage;
		int smsIntent;

		if (intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0) != 0) {
			// we have a new intent from SMS
			smsIntent = intent.getIntExtra(Constants.DEVICE_ADMIN_WORK, 0);
			Log.d(TAG, "DeviceAdminService is Called with smsIntentExtra=" + smsIntent);
			smsLine = intent.getStringExtra(Constants.SMS_LINE);
			smsMessage = intent.getStringExtra(Constants.SMS_MESSAGE);
			if (smsLine == null)
				smsLine = "";
			if (smsMessage == null)
				smsMessage = "";

			// if new intent is a priority intent, set up alarms in case process
			// is killed...
			int standingIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
			if (smsIntent >= standingIntent) {
				// kill any old alarms so only 1 active device admin process
				// (all alarms should have same simple pi)
				cancelAlarms(mContext);
				
				// schedule new alarm to continue after kill or reboot
				mPrefs.edit().putInt(Constants.SAVED_DEVICE_ADMIN_WORK, smsIntent).commit();
				mPrefs.edit().putString(Constants.SAVED_SMS_LINE, smsLine).commit();
				mPrefs.edit().putString(Constants.SAVED_SMS_MESSAGE, smsMessage).commit();
				scheduleAlarms(new WakelockWorkListener(), mContext, true);
				mResetAlarm = true;
			} else {
				//Don't delete existing alarms of higher order intents
				mResetAlarm = false;
			}

		} else {
			// Service called by alarm or boot, so recreate intent from saved
			// (after boot or kill, intent extras would be lost)
			// do not reset alarms
			smsIntent = mPrefs.getInt(Constants.SAVED_DEVICE_ADMIN_WORK, 0);
			Log.d(TAG, "DeviceAdminService is Called with smsIntentSaved=" + smsIntent);
			smsLine = mPrefs.getString(Constants.SAVED_SMS_LINE, "");
			smsMessage = mPrefs.getString(Constants.SAVED_SMS_MESSAGE, "");
		}

		switch (smsIntent) {
		case Constants.SEND_SMS:
			sendRepeatingSMS(smsIntent, smsLine, smsMessage);
			break;
		case Constants.SEND_GPS:
			sendGPSCoordinates();
			break;
		case Constants.SEND_SIM:
			sendSIMCode();
			lockSecretPassword(false);
			break;
		case Constants.LOCK_SCREEN:
			lockDevice();
			break;
		case Constants.WIPE_DATA:
			wipeDevice();
			break;
		case Constants.WIPE_ODK_DATA:
			wipeOdkData();
			break;
		case Constants.RESET_TO_DEFAULT_PWD:
			resetPassword();
			break;
		case Constants.RESET_PWD_TO_SMS_PWD:
			resetPassword(smsMessage);
			break;
		case Constants.RESET_ADMIN_ID:
			resetSmsAdminId();
			break;
		case Constants.LOCK_SECRET_PWD:
			lockSecretPassword(true);
			break;
		case Constants.HOLD_DEVICE:
			holdDevice(smsMessage);
			break;
		case Constants.STOP_HOLD_DEVICE:
			stopHoldDevice();
			break;
		case Constants.FACTORY_RESET:
			factoryReset();
			break;
		case Constants.CANCEL_ALARMS:
			cancelAdminAlarms();
			break;
		default:
			break;
		}
	}

	/**
	 * Holds the device in an activity the user can not leave, and posts a wait
	 * message to the user in a dialog box.
	 * 
	 */
	// TODO! check this
	private void holdDevice(String toast) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putBoolean(Constants.SHOW_MENU, false).commit();

		Intent i = new Intent(mContext, MessageHoldActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(Constants.TOAST_MESSAGE, toast);
		mContext.startActivity(i);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (MessageHoldActivity.sMessageHoldActive)
			cancelAdminAlarms();
	}

	private void stopHoldDevice() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putBoolean(Constants.SHOW_MENU, true).commit();

		Intent i = new Intent(mContext, MessageHoldActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(MessageHoldActivity.STOP_HOLD, true);
		mContext.startActivity(i);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (!MessageHoldActivity.sMessageHoldActive)
			cancelAdminAlarms();
	}

	/**
	 * Locks the device, sends an confirmation SMS to the reporting line.
	 * 
	 */
	public void lockDevice() {
		Log.d(TAG, "locking the device");
		mDPM.lockNow();

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (isDeviceLocked()) {
			cancelAdminAlarms();
			sendSingleSMS("Device locked");
		}
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
			Log.d(TAG, "screen is locked");
			return true;
		} else if (!pm.isScreenOn() && mPolicy.isActivePasswordSufficient()) {
			Log.d(TAG, "screen is off and password protected.");
			return true;
		} else {
			return false;
		}
	}

	public void factoryReset() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean resetDevice = prefs.getBoolean(PERFORM_FACTORY_RESET, false);
		if (resetDevice) {
			sendSingleSMS("Performing a Factory Reset");

			// wait a minute for SMS to send, then perform factory reset
			android.os.SystemClock.sleep(1000 * 60);
			mDPM.wipeData(0);
		}
	}

	public void wipeDevice() {
		// NB. we don't monitor this, we simply clear Client data. On completion
		// client data will be wiped (managed by AccessMRS). On receipt of
		// a broadcast to wipe data and if preference is set, then this process
		// will resume and the factoryReset() method will be called.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = prefs.edit();
		editor.putBoolean(PERFORM_FACTORY_RESET, true);
		editor.commit();
		wipeOdkData();
	}

	public void wipeOdkData() {
		// we don't check this one, because we dont manage the process
		// instead we wait for a broadcast to tell us to send a message
		Log.e(TAG, "wiping client data from device");
		sendSingleSMS("Wiping Client Data");

		Intent i = new Intent(Constants.WIPE_DATA_SERVICE);
		i.putExtra(Constants.WIPE_DATA_FROM_ADMIN_SMS_REQUEST, true);
		sendBroadcast(i);

		cancelAdminAlarms();
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

		if (!newAdminId.equals(oldAdminId)) {
			cancelAdminAlarms();
			sendRepeatingSMS(Constants.RESET_ADMIN_ID, newAdminId);
		} else {
			sendRepeatingSMS(Constants.RESET_ADMIN_ID, "Unable to reset Admin ID");
		}
	}

	public void resetPassword() {
		// TODO!: allow admin to edit this default value
		// final SharedPreferences prefs = new EncryptedPreferences(this,
		// this.getSharedPreferences(Constants.ENCRYPTED_PREFS,
		// Context.MODE_PRIVATE));
		// String defaultPwd = prefs.getString(Constants.DEFAULT_PASSWORD,
		// "12345");
		String defaultPwd = "12345";
		resetPassword(defaultPwd);
	}

	/**
	 * Resets the password to a default string that follows the device admin
	 * policy, and sends an SMS confirmation to the reporting line.
	 */
	public void resetPassword(String tempPassword) {
		int pwdLength = mPolicy.getPasswordLength();
		int pwdQuality = mPolicy.getPasswordQuality();

		// Set to Temporary Password
		mPolicy.setPasswordLength(5);
		mPolicy.setPasswordQuality(2);

		// confirm that this worked before canceling the alarm
		android.os.SystemClock.sleep(1000 * 5);
		if (mDPM.resetPassword(tempPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)) {
			cancelAdminAlarms();
			sendSingleSMS("Device successfully locked with default password");
		} else {
			sendSingleSMS("Unable to lock device and reset to default password");
		}
		mDPM.lockNow();

		// Reset Policy to force pwd change (IF stronger than tempPassword)
		mPolicy.setPasswordLength(pwdLength);
		mPolicy.setPasswordQuality(pwdQuality);

		Intent i = new Intent(mContext, SetUserPassword.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(SetUserPassword.SUGGEST_RESET_PASSWORD, true);
		mContext.startActivity(i);
	}

	/**
	 * Locks the device with a random secure string that only the device knows.
	 * The only way to unlock device is through sending an sms to reset the
	 * password to default. Also sends an SMS to the reporting line.
	 */
	public void lockSecretPassword(boolean sendPassword) {
		Policy policy = new Policy(mContext);
		String pwd = policy.createNewSecretPwd();
		if (policy.resetPassword(pwd)) {
			if (sendPassword)
				sendSingleSMS("Device successfully locked with new password=" + pwd);
			else
				sendSingleSMS("Device successfully locked with new random password.");

			cancelAdminAlarms();
		} else {
			sendSingleSMS("Unable to lock device and reset to new random password");
		}
		mDPM.lockNow();

		Intent i = new Intent(mContext, SetUserPassword.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(SetUserPassword.SUGGEST_RESET_PASSWORD, true);
		mContext.startActivity(i);
	}

	/**
	 * Cancels all Device Admin Alarms, regardless of type. Sends an SMS to the
	 * reporting line when complete.
	 */
	public void cancelAdminAlarms() {
		if (mResetAlarm)
			cancelAlarms(mContext);

		// confirm that this worked before asking about alarms
		android.os.SystemClock.sleep(1000 * 10);
		if (!isAdminAlarmActive())
			sendSingleSMS("All device admin alarms have been cancelled.");
		else
			Log.d(TAG, "Something went wrong... alarms are not canceling");
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

	/**
	 * Send an SMS with the current location (by either GPS or network) to the
	 * default reporting line. SMS message body is of the form: <br>
	 * "Time=#################### Lat=################### Lon=################# Alt=########### Acc=###"
	 * "
	 * 
	 */
	public void sendGPSCoordinates() {
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

		sendRepeatingSMS(Constants.SEND_GPS, sb.toString());

	}

	/**
	 * Send an SMS with the current SIM code to the default reporting line. SMS
	 * message body is of the form: <br>
	 * "IMEI=#################### New SIM=########## Serial=##############"
	 * 
	 */
	public void sendSIMCode() {
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

		sendRepeatingSMS(Constants.SEND_SIM, sb.toString());

	}

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
		if (message.equals(lastSentAdminMessage)) {
			cancelAdminAlarms();
			Log.d(TAG, "Message has already been sent. Alarm Cancelled.");
		} else {
			ComponentName comp = new ComponentName(mContext.getPackageName(), SendSMSService.class.getName());
			Intent i = new Intent();
			i.setComponent(comp);
			i.putExtra(Constants.SMS_LINE, line);
			i.putExtra(Constants.SMS_MESSAGE, message);
			mContext.startService(i);
		}
	}

	public void sendRepeatingSMS(int smstype, String message) {
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
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
		String line = prefs.getString(Constants.SMS_REPLY_LINE, "");
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
