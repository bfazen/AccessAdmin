/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.alphabetbloc.accessadmin.activities;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.receivers.DeviceAdmin;
import com.alphabetbloc.accessadmin.services.DeviceAdminService;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class SetDevicePolicy extends SherlockActivity implements ActionBar.OnNavigationListener {
	private static final int SET_ADMIN = 1;
//	private static final String TAG = "SetDevicePolicy";
	private Context mContext;
	private Policy mPolicy;
	private CheckBox mAdminCheckBox;
	private Button mWipeDataButton;
	private Button mWipeOdkDataButton;

	private Spinner mPasswordQuality;
	final static int mPasswordQualityValues[] = Policy.PASSWORD_QUALITY_VALUES;
	private static final int MINIMUM_PWD_LENGTH = 5;
	private static final int MINIMUM_LOCK_TIME = 10;
	private static final int MINIMUM_PWD_TO_WIPE = 1;

	private SeekBar mPasswordLength;
	private SeekBar mMaxPwdToWipe;
	private SeekBar mMaxTimeToLock;

	private TextView mPasswordLengthText;
	private TextView mMaxPwdToWipeText;
	private TextView mMaxTimeToLockText;
	private int mMaxPwds;
	private Resources mRes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_policy);
		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		mContext = this;
		mRes = mContext.getResources();
		mPolicy = new Policy(this);

		// Admin Buttons
		mAdminCheckBox = (CheckBox) findViewById(R.id.enable_admin_checkbox);
		mAdminCheckBox.setOnCheckedChangeListener(mAdminCheckBoxListener);
		mWipeDataButton = (Button) findViewById(R.id.wipe_data_button);
		mWipeDataButton.setOnClickListener(mWipeDataListener);
		mWipeOdkDataButton = (Button) findViewById(R.id.wipe_odk_button);
		mWipeOdkDataButton.setOnClickListener(mWipeOdkDataListener);

		// Password Quality
		mPasswordQuality = (Spinner) findViewById(R.id.pwd_quality);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.password_types, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPasswordQuality.setAdapter(adapter);
		mPasswordQuality.setSelection(mPolicy.getPasswordQuality());
		mPasswordQuality.setOnItemSelectedListener(mPasswordQualityListener);

		// PasswordLength
		mPasswordLength = (SeekBar) findViewById(R.id.seek_pwd_length);
		mPasswordLength.setMax(25);
		mPasswordLength.setProgress(mPolicy.getPasswordLength() - MINIMUM_PWD_LENGTH);
		mPasswordLength.setOnSeekBarChangeListener(mPasswordLengthListener);
		mPasswordLengthText = (TextView) findViewById(R.id.text_pwd_length);
		String pwdlength = String.valueOf(mPolicy.getPasswordLength());
		mPasswordLengthText.setText(pwdlength + " characters");

		// Passwords Before Wipe
		mMaxPwdToWipe = (SeekBar) findViewById(R.id.seek_pwd_to_wipe);
		mMaxPwdToWipe.setMax(100 - MINIMUM_PWD_TO_WIPE);
		mMaxPwdToWipe.setProgress(mPolicy.getMaxFailedPwd() - MINIMUM_PWD_TO_WIPE);
		mMaxPwdToWipe.setOnSeekBarChangeListener(mMaxPwdToWipeListener);
		mMaxPwdToWipeText = (TextView) findViewById(R.id.text_pwd_to_wipe);
		String pwds = String.valueOf(mPolicy.getMaxFailedPwd());
		mMaxPwdToWipeText.setText("Reset device after " + pwds + " wrong passwords");
		mMaxPwds = mPolicy.getMaxFailedPwd();

		// Time before screen lock
		mMaxTimeToLock = (SeekBar) findViewById(R.id.seek_time_to_lock);
		mMaxTimeToLock.setMax(1800 - MINIMUM_LOCK_TIME);
		mMaxTimeToLock.setProgress((int) ((mPolicy.getMaxTimeToLock() - MINIMUM_LOCK_TIME) / 1000));
		mMaxTimeToLock.setOnSeekBarChangeListener(mMaxTimeToLockListener);
		mMaxTimeToLockText = (TextView) findViewById(R.id.text_time_to_lock);
		String time;
		int sec = (int) (mPolicy.getMaxTimeToLock() / 1000);
		int min = sec / 60;
		if (min > 0 && (sec % 60) != 0)
			time = String.valueOf(min) + " min, " + String.valueOf((sec % 60)) + " sec";
		else if (min > 0 && (sec % 60) == 0)
			time = String.valueOf(min) + " min ";
		else
			time = String.valueOf(sec) + " sec";
		mMaxTimeToLockText.setText("Lock before " + time);

	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshView();
	}

	private void refreshView() {
		if (mPolicy.isAdminActive())
			mAdminCheckBox.setChecked(true);
		else
			mAdminCheckBox.setChecked(false);

		enableButtons(mPolicy.isAdminActive());

	}

	private void enableButtons(boolean adminActive) {
		if (adminActive) {
			mWipeOdkDataButton.setEnabled(true);
			mWipeDataButton.setEnabled(true);
			mPasswordQuality.setEnabled(true);
			mPasswordLength.setEnabled(true);
			mMaxPwdToWipe.setEnabled(true);
			mMaxTimeToLock.setEnabled(true);
		} else {
			mWipeOdkDataButton.setEnabled(false);
			mWipeDataButton.setEnabled(false);
			mPasswordQuality.setEnabled(false);
			mPasswordLength.setEnabled(false);
			mMaxPwdToWipe.setEnabled(false);
			mMaxTimeToLock.setEnabled(false);
		}
	}

	private OnCheckedChangeListener mAdminCheckBoxListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			// if there is a discrepancy, then change states, otherwise do
			// nothing...
			if (!isChecked && mPolicy.isAdminActive()) {
				mPolicy.removeActiveAdmin();
				enableButtons(false);
			} else if (isChecked && !mPolicy.isAdminActive()) {
				activateDeviceAdmin();
				enableButtons(true);
			}
		}
	};

	private void activateDeviceAdmin() {
		Intent initAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		ComponentName deviceAdmin = new ComponentName(SetDevicePolicy.this, DeviceAdmin.class);
		initAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
		initAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please set up a device administrator to ensure the security of this device.");
		startActivityForResult(initAdmin, SET_ADMIN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// do nothing right now...
	}

	private OnClickListener mWipeDataListener = new OnClickListener() {
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);
			builder.setTitle(R.string.alert_title_first_warning);
			builder.setMessage(R.string.alert_wipe_body_first_warning);
			builder.setIcon(R.drawable.priority);
			builder.setPositiveButton(R.string.alert_ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);
					builder.setTitle(R.string.alert_title_second_warning);
					builder.setMessage(R.string.alert_wipe_body_second_warning);
					builder.setIcon(R.drawable.priority);
					builder.setPositiveButton(R.string.alert_ok_button, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mPolicy.isAdminActive()) {
								Intent i = new Intent(mContext, DeviceAdminService.class);
								i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.WIPE_DATA);
								WakefulIntentService.sendWakefulWork(mContext, i);
								mWipeDataButton.setEnabled(false);
							}
						}
					});
					builder.setNegativeButton(R.string.alert_second_cancel_button, null);
					builder.show();
				}
			});
			builder.setNegativeButton(R.string.alert_first_cancel_button, null);
			builder.show();
		}
	};

	private OnClickListener mWipeOdkDataListener = new OnClickListener() {
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);
			builder.setTitle(R.string.alert_title_first_warning);
			builder.setMessage(R.string.alert_odk_body_first_warning);
			builder.setIcon(R.drawable.priority);
			builder.setPositiveButton(R.string.alert_ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);
					builder.setTitle(R.string.alert_title_second_warning);
					builder.setMessage(R.string.alert_odk_body_second_warning);
					builder.setIcon(R.drawable.priority);
					builder.setPositiveButton(R.string.alert_ok_button, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mPolicy.isAdminActive()) {
								Intent i = new Intent(mContext, DeviceAdminService.class);
								i.putExtra(Constants.DEVICE_ADMIN_WORK, Constants.WIPE_ODK_DATA);
								WakefulIntentService.sendWakefulWork(mContext, i);
								mWipeOdkDataButton.setEnabled(false);
							}
						}
					});
					builder.setNegativeButton(R.string.alert_second_cancel_button, null);
					builder.show();
				}
			});
			builder.setNegativeButton(R.string.alert_first_cancel_button, null);
			builder.show();
		}
	};

	private OnItemSelectedListener mPasswordQualityListener = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			mPolicy.setPasswordQuality(position);

		}

		public void onNothingSelected(AdapterView<?> parent) {
			mPolicy.setPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
		}
	};

	private OnSeekBarChangeListener mMaxPwdToWipeListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (mMaxPwds < 50) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);

				builder.setTitle(R.string.alert_title_first_warning);
				builder.setMessage(String.format(mRes.getString(R.string.max_pwd_setting_first_warning), mMaxPwds));
				builder.setIcon(R.drawable.priority);
				builder.setPositiveButton(String.format(mRes.getString(R.string.max_pwd_setting_ok_button), mMaxPwds), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						if (mMaxPwds < 10) {
							AlertDialog.Builder builder = new AlertDialog.Builder(SetDevicePolicy.this);
							builder.setTitle(R.string.alert_title_second_warning);
							builder.setMessage(String.format(mRes.getString(R.string.max_pwd_setting_second_warning), mMaxPwds));
							builder.setIcon(R.drawable.priority);
							builder.setPositiveButton(String.format(mRes.getString(R.string.max_pwd_setting_ok_button), mMaxPwds), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									mPolicy.setMaxFailedPw(mMaxPwds);
								}
							});
							builder.setNegativeButton(R.string.max_pwd_setting_cancel_button, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									mMaxPwdToWipe.setProgress(mPolicy.getMaxFailedPwd() - MINIMUM_PWD_TO_WIPE);
								}
							});
							builder.show();
						} else {
							mPolicy.setMaxFailedPw(mMaxPwds);
						}
					}

				});

				builder.setNegativeButton(R.string.max_pwd_setting_cancel_button, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mMaxPwdToWipe.setProgress(mPolicy.getMaxFailedPwd() - MINIMUM_PWD_TO_WIPE);
					}
				});
				builder.show();

			} else {
				mPolicy.setMaxFailedPw(mMaxPwds);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Auto-generated method stub

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			progress = progress + MINIMUM_PWD_TO_WIPE;
			mMaxPwds = progress;
			mMaxPwdToWipeText.setText("Reset device after " + progress + " wrong passwords");
		}
	};

	private OnSeekBarChangeListener mMaxTimeToLockListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// Auto-generated method stub

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Auto-generated method stub

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int sec, boolean fromUser) {
			String time;
			sec = sec + MINIMUM_LOCK_TIME;
			int min = sec / 60;
			if (min > 0 && (sec % 60) != 0)
				time = String.valueOf(min) + " min, " + String.valueOf((sec % 60)) + " sec";
			else if (min > 0 && (sec % 60) == 0)
				time = String.valueOf(min) + " min";
			else
				time = String.valueOf(sec) + " sec";
			mMaxTimeToLockText.setText("Lock before " + time);

			long timeMs = 1000L * sec;
			mPolicy.setMaxTimeToLock(timeMs);

		}
	};

	private OnSeekBarChangeListener mPasswordLengthListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// Auto-generated method stub

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// Auto-generated method stub

		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			progress = progress + MINIMUM_PWD_LENGTH;
			mPasswordLengthText.setText(progress + " characters");
			mPolicy.setPasswordLength(progress);

		}
	};

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		SubMenu sub = menu.addSubMenu("Menu");
		sub.setIcon(R.drawable.submenu);
		sub.add(0, R.string.list_short_password, 0, getString(R.string.list_short_password));
		sub.add(0, R.string.list_short_policy, 0, getString(R.string.list_short_policy));
		sub.add(0, R.string.list_short_apps, 0, getString(R.string.list_short_apps));
		sub.add(0, R.string.list_short_sms, 0, getString(R.string.list_short_sms));
		sub.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent i = new Intent(this, AdminSettingsActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return false;
		case R.string.list_short_policy:
			startActivity(new Intent(this, SetDevicePolicy.class));
			finish();
			return true;
		case R.string.list_short_apps:
			startActivity(new Intent(this, SetAppPreferences.class));
			finish();
			return true;
		case R.string.list_short_password:
			startActivity(new Intent(this, SetAdminPassword.class));
			finish();
			return true;
		case R.string.list_short_sms:
			startActivity(new Intent(this, ViewSmsSettings.class));
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
