/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alphabetbloc.chvsettings.data;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.alphabetbloc.chvsettings.receivers.DeviceAdmin;

/**
 * Monitors and manages changes in the DeviceAdminPolicy
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 */
public class Policy {

	public static final int REQUEST_ADD_DEVICE_ADMIN = 1;
	public static final String SHARED_PREF = "SHARED_PREF";
	public static final String KEY_PASSWORD_LENGTH = "PW_LENGTH";
	public static final String KEY_PASSWORD_QUALITY = "PW_QUALITY";
	public static final String KEY_MAX_FAILED_PW = "PW_MAX_FAILED";
	public static final String KEY_MAX_TIME_TO_LOCK = "PW_MAX_TIME_LOCK";
	public static final String PROVIDER_ID = "PROVIDER_ID";

	// Password quality choices must match arrays.xml
	public final static int[] PASSWORD_QUALITY_VALUES = new int[] { DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC, DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC,
			DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC };
	private static final String TAG = "Policy";

	private Context mContext;
	private DevicePolicyManager mDPM;
	private ComponentName mPolicyAdmin;
	private SharedPreferences mPrefs;
	private int mPasswordQuality;
	private int mPasswordLength;
	private int mMaxPwdToWipe;
	private long mMaxTimeToLock;

	public Policy(Context context) {
		mContext = context;
		mPrefs = mContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);

		mPasswordQuality = mPrefs.getInt(KEY_PASSWORD_QUALITY, 3);
		mPasswordLength = mPrefs.getInt(KEY_PASSWORD_LENGTH, 5);
		mMaxPwdToWipe = mPrefs.getInt(KEY_MAX_FAILED_PW, 50);
		mMaxTimeToLock = mPrefs.getLong(KEY_MAX_TIME_TO_LOCK, 600);

		mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		mPolicyAdmin = new ComponentName(context, DeviceAdmin.class);

		Log.e(TAG, "Policy passwordQuality is" + getPasswordQuality());
	}

	public void initializeDefaultPolicy() {
		setPasswordQuality(mPasswordQuality);
		setPasswordLength(mPasswordLength);
		setMaxFailedPw(mMaxPwdToWipe);
		setMaxTimeToLock(mMaxTimeToLock);
		Log.e(TAG, "Initializing passwordQuality is" + getPasswordQuality());
	}

	// 4 Policy Setters for Android 2.3
	/**
	 * Set the password quality
	 */
	public void setPasswordQuality(int quality) {
		mPrefs.edit().putInt(KEY_PASSWORD_QUALITY, quality).commit();
		mPasswordQuality = mPrefs.getInt(KEY_PASSWORD_QUALITY, 3);
		mDPM.setPasswordQuality(mPolicyAdmin, PASSWORD_QUALITY_VALUES[quality]);
	}

	/**
	 * Set the minimum password length
	 */
	public void setPasswordLength(int length) {
		mPrefs.edit().putInt(KEY_PASSWORD_LENGTH, length).commit();
		mPasswordLength = mPrefs.getInt(KEY_PASSWORD_LENGTH, 5);
		mDPM.setPasswordMinimumLength(mPolicyAdmin, length);
	}

	/**
	 * Set the maximum failed password attempts prior to wiping the phone
	 */
	public void setMaxFailedPw(int attempts) {
		mPrefs.edit().putInt(KEY_MAX_FAILED_PW, attempts).commit();
		mMaxPwdToWipe = mPrefs.getInt(KEY_MAX_FAILED_PW, 50);
		mDPM.setMaximumFailedPasswordsForWipe(mPolicyAdmin, attempts);
	}

	/**
	 * Set the maximum time screen can be unlocked.
	 * 
	 */
	public void setMaxTimeToLock(long time) {
		mPrefs.edit().putLong(KEY_MAX_TIME_TO_LOCK, time).commit();
		mMaxTimeToLock = mPrefs.getLong(KEY_MAX_TIME_TO_LOCK, 600);
		mDPM.setMaximumTimeToLock(mPolicyAdmin, time);
		Log.e(TAG, "max time to screen lock set to:" + time);
	}

	/**
	 * Get new random password that fulfills all policy requirements.
	 * 
	 * @return
	 */
	public boolean createNewSecretPwd() {
		String rAlphaNum = (new StringGenerator(mPasswordLength)).getRandomAlphaNumericString();
		return mDPM.resetPassword(rAlphaNum, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
	}

	/**
	 * Resets the device unlock password and forces re-entry
	 * 
	 * @return True if password reset
	 */
	public boolean resetPassword(String pwd) {
		return mDPM.resetPassword(pwd, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
	}

	// 4 Policy Getters for Android 2.3
	/**
	 * Get password quality type
	 * 
	 * @return
	 */
	public int getPasswordQuality() {
		return mPasswordQuality;
	}

	/**
	 * Get minimum password length.
	 * 
	 * @return
	 */
	public int getPasswordLength() {
		return mPasswordLength;
	}

	/**
	 * Get Maximum passwords allowed to be entered before device is wiped.
	 * 
	 * @return
	 */
	public int getMaxFailedPwd() {
		return mMaxPwdToWipe;
	}

	/**
	 * Get maximum time screen is allowed to be unlocked
	 * 
	 * @return
	 */
	public long getMaxTimeToLock() {
		return mMaxTimeToLock;
	}

	/**
	 * Getter for the policy administrator ComponentName object.
	 * 
	 * @return
	 */
	public ComponentName getPolicyAdmin() {
		return mPolicyAdmin;
	}

	/**
	 * Indicates whether the device administrator is currently active.
	 * 
	 * @return
	 */
	public boolean isAdminActive() {
		if (mDPM.isAdminActive(mPolicyAdmin))
			Log.e(TAG, "admin is active");
		else
			Log.e(TAG, "admin is NOT active");

		return mDPM.isAdminActive(mPolicyAdmin);
	}

	/**
	 * Indicates whether the active password is sufficient
	 * 
	 * @return
	 */
	public boolean isActivePasswordSufficient() {
		if (mDPM.isActivePasswordSufficient())
			Log.e(TAG, "password is sufficient");

		return mDPM.isActivePasswordSufficient();

	}

	/**
	 * Indicates whether the device administrator is currently active and the
	 * active password is sufficient
	 * 
	 * @return
	 */
	public boolean isDeviceSecured() {
		return isAdminActive() && isActivePasswordSufficient();
	}

	/**
	 * Removes the device administrator
	 * 
	 */
	public void removeActiveAdmin() {
		Log.e(TAG, "remove active admin");
		mDPM.removeActiveAdmin(mPolicyAdmin);
	}

}
