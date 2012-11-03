package com.alphabetbloc.chvsettings.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.alphabetbloc.chvsettings.R;

/**
 * @author Louis Fazen (louis.fazen@gmail.com)
 */
public class SetAppPreferences extends SherlockActivity{
	/** Called when the activity is first created. */
	private static final String SHOW_SETTINGS_MENU = "show_settings_menu";
	private static final String ENABLE_ACTIVITY_LOG = "enable_activity_log";
	private static final String ACCESS_MRS_PACKAGE = "com.alphabetbloc.accessmrs";
	private Button collectButton;
	private Button accessMrsButton;
	private Button adwButton;
	private Button ushahidiButton;
	private SharedPreferences.Editor editor;
	private CheckBox collectMenuToggle;
	private CheckBox accessMrsLogToggle;
	private CheckBox ushahidiToggle;
	private CheckBox accessMrsToggle;
	private CheckBox adwToggle;

	//TODO!: check if the packages exist before showing the buttons... if don't exist, set to GONE...
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.editprefs);
		SharedPreferences settings =  PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		editor = settings.edit();

		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		adwButton = (Button) findViewById(R.id.adwButton);
		adwButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				 Intent i = new Intent(Intent.ACTION_VIEW);
				 i.setComponent(new
				 ComponentName("com.android.launcher","com.android.launcher.MyLauncherSettings"));
				 startActivity(i);
			}
		});

		collectButton = (Button) findViewById(R.id.collectButton);
		collectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName("org.odk.collect.android", "org.odk.collect.android.preferences.PreferencesActivity"));
				startActivity(i);
			}
		});

		accessMrsButton = (Button) findViewById(R.id.access_mrs_button);
		accessMrsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ACCESS_MRS_PACKAGE, ACCESS_MRS_PACKAGE + ".ui.admin.PreferencesActivity"));
				startActivity(i);
			}
		});

		ushahidiButton = (Button) findViewById(R.id.ushahidiButton);
		ushahidiButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName("com.ushahidi.android.app", "com.ushahidi.android.app.Settings"));
				startActivity(i);
			}
		});

		// check boxes / toggle buttons
		ushahidiToggle = (CheckBox) findViewById(R.id.ushahidi_checkbox);

		if (settings.getBoolean("UshahidiMenuEnabled", false)) {
			ushahidiToggle.setChecked(true);
		}

		ushahidiToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName("com.ushahidi.android.app", "com.ushahidi.android.app.ViewMenuPreference"));
				if (ushahidiToggle.isChecked()) {
					ushahidiToggle.setChecked(true);
					i.putExtra("ShowMenu", true);

					editor.putBoolean("UshahidiMenuEnabled", true);

				} else {
					ushahidiToggle.setChecked(false);
					i.putExtra("ShowMenu", false);
					editor.putBoolean("UshahidiMenuEnabled", false);

				}
				editor.commit();
				startActivity(i);

			}
		});

		accessMrsToggle = (CheckBox) findViewById(R.id.access_mrs_checkbox);
		if (settings.getBoolean("AccessMRSMenuEnabled", false)) {
			accessMrsToggle.setChecked(true);
		}

		accessMrsToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ACCESS_MRS_PACKAGE, ACCESS_MRS_PACKAGE + ".ui.admin.DeviceAdminPreference"));
				if (accessMrsToggle.isChecked()) {
					accessMrsToggle.setChecked(true);
					i.putExtra(SHOW_SETTINGS_MENU, true);

					editor.putBoolean("AccessMRSMenuEnabled", true);

				} else {
					accessMrsToggle.setChecked(false);
					i.putExtra(SHOW_SETTINGS_MENU, false);
					editor.putBoolean("AccessMRSMenuEnabled", false);

				}
				editor.commit();
				startActivity(i);

			}
		});

		collectMenuToggle = (CheckBox) findViewById(R.id.collect_checkbox);
		if (settings.getBoolean("CollectMenuEnabled", false)) {
			collectMenuToggle.setChecked(true);
		}

		collectMenuToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName("org.odk.collect.android", "org.odk.collect.android.activities.DeviceAdminPreference"));
				if (collectMenuToggle.isChecked()) {
					collectMenuToggle.setChecked(true);
					i.putExtra(SHOW_SETTINGS_MENU, true);

					editor.putBoolean("CollectMenuEnabled", true);

				} else {
					collectMenuToggle.setChecked(false);
					i.putExtra(SHOW_SETTINGS_MENU, false);
					editor.putBoolean("CollectMenuEnabled", false);

				}
				editor.commit();
				startActivity(i);

			}
		});

		accessMrsLogToggle = (CheckBox) findViewById(R.id.access_mrs_log_checkbox);
		if (settings.getBoolean("AccessMRSLogEnabled", false)) {
			accessMrsLogToggle.setChecked(true);
		}

		accessMrsLogToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ACCESS_MRS_PACKAGE, ACCESS_MRS_PACKAGE + ".ui.admin.DeviceAdminPreference"));
				if (accessMrsLogToggle.isChecked()) {
					accessMrsLogToggle.setChecked(true);
					i.putExtra(ENABLE_ACTIVITY_LOG, true);
					editor.putBoolean("AccessMRSLogEnabled", true);

				} else {
					accessMrsLogToggle.setChecked(false);
					i.putExtra(ENABLE_ACTIVITY_LOG, false);
					editor.putBoolean("AccessMRSLogEnabled", false);
				}
				editor.commit();
				startActivity(i);

			}
		});

		adwToggle = (CheckBox) findViewById(R.id.adw_checkbox);
		if (settings.getBoolean("AdwMenuEnabled", false)) {
			adwToggle.setChecked(true);
		}

		adwToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName("com.android.launcher", "com.android.launcher.ViewMenuPreference"));
				if (adwToggle.isChecked()) {
					adwToggle.setChecked(true);
					i.putExtra("ShowMenu", true);
					i.setAction("com.android.launcher.ViewMenuPreference.ACTION");
					editor.putBoolean("AdwMenuEnabled", true);

				} else {
					adwToggle.setChecked(false);
					i.putExtra("ShowMenu", false);
					editor.putBoolean("AdwMenuEnabled", false);

				}
				editor.commit();
				startActivity(i);

			}
		});

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