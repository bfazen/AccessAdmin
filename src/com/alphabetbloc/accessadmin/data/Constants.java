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

package com.alphabetbloc.accessadmin.data;

import android.app.AlarmManager;


public class Constants {
	//DEBUGGING
	public static boolean DEBUG = true;
	
	//RECEIVERS
	public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	public static final String AIRPLANE_MODE = "android.intent.action.AIRPLANE_MODE";
	public static final String WIPE_DATA_COMPLETE = "com.alphabetbloc.android.settings.WIPE_DATA_SERVICE_COMPLETE";
	public static final String WIPE_DATA_SERVICE = "com.alphabetbloc.accessmrs.WIPE_DATA_SERVICE";
	public static final String WIPE_DATA_FROM_ADMIN_SMS_REQUEST = "com.alphabetbloc.accessmrs.WIPE_DATA_FROM_ADMIN_SMS_REQUEST";
	
	//INTENTS
	public static final String DEVICE_ADMIN_WORK = "device_admin_work";
	public static final String SAVED_DEVICE_ADMIN_WORK = "saved_device_admin_work";

	//SMS PREFERENCES
	public static final String SMS_LINE = "sms_line";
	public static final String SMS_MESSAGE = "sms_message";
	public static final String SAVED_SMS_LINE = "saved_sms_line";
	public static final String SAVED_SMS_MESSAGE = "saved_sms_message";
	public static final String SMS_SENT = "sms_sent"; 
	public static final String LAST_SENT_GPS_MESSAGE = "last_sent_gps_message";
	public static final String LAST_SENT_SIM_MESSAGE = "last_sent_sim_message";
	public static final String PERFORM_FACTORY_RESET = "perform_factory_reset";
	public static final String SMS_SENT_CONFIRMATION = "sms_sent_confirmation";
	
	//SMS CODES
	public static final String SMS_CODE_ADMIN_PREFIX = "AdminCode=";
	public static final String SMS_CODE_LOCK = "-lock";
	public static final String SMS_CODE_GPS = "-gps";
	public static final String SMS_CODE_WIPE_DATA = "-reset-device";
	public static final String SMS_CODE_WIPE_ODK = "-wipe-data";
	public static final String SMS_CODE_HOLD = "-hold:"; 
	public static final String SMS_CODE_STOP_HOLD = "-stop-hold"; 
	public static final String SMS_CODE_CANCEL_ALARM = "-cancel";
	public static final String SMS_CODE_RESET_PWD_SECRET = "create.new.password.and.sms.to.admin";
	public static final String SMS_CODE_RESET_PWD_DEFAULT = "-reset-pwd";
	public static final String SMS_CODE_RESET_ADMIN_ID = "create.new.admin.id.and.sms.to.admin";
	public static final String SMS_CODE_RESET_PWD_TO_SMS_PWD = "-reset-pwd:";
	public static final String SMS_CODE_EDIT_ACCESS_MRS_PREF = "-accessmrs-pref:";
	public static final String SMS_CODE_VERIFY_SIM = "-check-sim";
	public static final String SMS_CODE_HOLD_LOCKED = "-hold-lock";
	public static final String SMS_CODE_SEND_SMS = "-sms";

	//DEVICE ADMIN ACTIONS: IN ORDER OF PRIORITY
	public static final int LOCK_SCREEN = 100;
	public static final int HOLD_DEVICE = 200;
	public static final int LOCK_RANDOM_PWD = 250;
	public static final int HOLD_DEVICE_LOCKED = 300;
	public static final int STOP_HOLD_DEVICE = 400;
	public static final int EDIT_ACCESS_MRS_PREF = 500;
	public static final int RESET_PWD_TO_SMS_PWD = 700;
	public static final int RESET_TO_DEFAULT_PWD = 800;
	public static final int SEND_SMS = 900;
	public static final int SEND_GPS = 1000;
	public static final int RESET_ADMIN_ID = 1100;
	public static final int SEND_ADMIN_ID = 1150;
	public static final int VERIFY_SIM = 1200;
	public static final int WIPE_ODK_DATA = 1300;
	public static final int WIPE_DATA = 1400;
	public static final int FACTORY_RESET = 1500;
	public static final int CANCEL_ALARMS = 1600;
	
	//ALARM PREFERENCES AND INTERVALS
	public static final String ALARM_LOCK_SCREEN = "ALARM_LOCK_SCREEN";
	public static final String ALARM_HOLD_DEVICE = "ALARM_HOLD_DEVICE";
	public static final String ALARM_RESET_TO_DEFAULT_PWD = "ALARM_RESET_TO_DEFAULT_PWD";
	public static final String ALARM_SEND_SMS = "ALARM_SEND_SMS";
	public static final String ALARM_SEND_GPS = "ALARM_SEND_GPS";
	public static final String ALARM_LOCK_RANDOM_PWD = "ALARM_LOCK_RANDOM_PWD";
	public static final String ALARM_RESET_ADMIN_ID = "ALARM_RESET_ADMIN_ID";
	public static final String ALARM_VERIFY_SIM = "ALARM_VERIFY_SIM";
	public static final String ALARM_WIPE_ODK_DATA = "ALARM_WIPE_ODK_DATA";
	public static final String ALARM_WIPE_DATA = "ALARM_WIPE_DATA";
	public static final String ALARM_CANCEL_ALARMS = "ALARM_CANCEL_ALARMS";
	public static final String ALARM_FACTORY_RESET = "ALARM_FACTORY_RESET";
	public static final String ALARM_HOLD_DEVICE_LOCKED = "ALARM_HOLD_DEVICE_LOCKED";
	public static final String ALARM_RESET_PWD_TO_SMS_PWD = "ALARM_RESET_PWD_TO_SMS_PWD";
	public static final String ALARM_STOP_HOLD_DEVICE = "ALARM_STOP_HOLD_DEVICE";
	public static final String ALARM_SEND_ADMIN_ID = "ALARM_SEND_ADMIN_ID";
	
	public static final Long ALARM_INTERVAL_ALWAYS = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 30; //30 seconds
	public static final Long ALARM_INTERVAL_SHORT = AlarmManager.INTERVAL_FIFTEEN_MINUTES / 15; //1 min
	public static final Long ALARM_INTERVAL_MEDIUM = AlarmManager.INTERVAL_FIFTEEN_MINUTES; // 15 minutes
	public static final Long ALARM_INTERVAL_LONG = AlarmManager.INTERVAL_HOUR * 2; // 2 hours
	
	//ENCRYPTED PREFERENCES:
	public static final String UNIQUE_DEVICE_ID = "unique_device_id";
	public static final String SMS_REPLY_LINE = "sms_reply_line";
	public static final String DEFAULT_SMS_REPLY_LINE = "0715050383"; 
	public static final String DEFAULT_PASSWORD = "default_password"; 
	public static final String SECRET_PASSWORD = "secret_password";
	public static final String ENCRYPTED_PREFS = "encrypted_prefs";
	public static final String ADMIN_PASSWORD = "admin_password";
	public static final String DEFAULT_ADMIN_PASSWORD = "";
	
	public static final int RESULT_ENABLE = 1;
	
	//INSTALLATION AND ON BOOT
	public static final String NEW_INSTALL = "new_install";
	
	//STATE
	public static final String SIM_ERROR_PHONE_LOCKED = "sim_error_phone_locked";
	
	// SIM LOCK
	public static final String USE_SIM_LOCK = "use_sim_lock";
	public static final String SIM_SERIAL = "sim_serial";
	public static final String SIM_LINE = "sim_line";
	
	public static final String LAST_SIM_CHANGE = "last_sim_change";
	public static final String SIM_CHANGE_COUNT = "sim_change_count";
	public static final String SIM_CHANGE_THRESHOLD = "sim_change_threshold";
	public static final String SIM_CHANGE_RESET_PERIOD = "sim_change_reset_period";
	
	public static final String LAST_SIM_MISSING = "last_sim_missing";
	public static final String SIM_MISSING_COUNT = "sim_missing_count";
	public static final String SIM_MISSING_THRESHOLD = "sim_missing_threshold";
	public static final String SIM_MISSING_RESET_PERIOD = "sim_missing_reset_period";
	
	public static final String SIM_ERROR_WIPE_DATA = "sim_error_wipe_data";
	
	//DEVICE HOLD TYPES
	public static final String HOLD_TYPE = "hold_type";
	public static final int ADMIN_MESSAGE = 0;
	public static final int SIM_ERROR = 1;
	public static final int DEVICE_LOCKED = 2;
	
	//ADMIN LOGINS
	public static final String LAST_ADMIN_PWD_ATTEMPT = "last_admin_pwd_attempt";
	public static final String ADMIN_PWD_COUNT = "last_pwd_count";

	public static final String SHOW_MENU = "show_admin_menu";



	
}

