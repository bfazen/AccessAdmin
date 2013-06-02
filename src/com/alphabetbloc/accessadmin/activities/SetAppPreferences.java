package com.alphabetbloc.accessadmin.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.alphabetbloc.accessadmin.R;

/**
 * @author Louis Fazen (louis.fazen@gmail.com)
 */
public class SetAppPreferences extends SherlockActivity {
	/** Called when the activity is first created. */

	// Packages
	public static final String ACCESS_MRS_PACKAGE = "com.alphabetbloc.accessmrs";
	private static final String ACCESS_FORMS_PACKAGE = "com.alphabetbloc.accessforms";
	private static final String USHAHIDI_PACKAGE = "com.ushahidi.android.app";
	private static final String ADW_PACKAGE = "com.android.launcher";
	private static final String SETTINGS_PACKAGE = "com.android.settings";
	
	// Intents
	public static final String ACCESS_MRS_SET_PREFERENCE = "com.alphabetbloc.accessmrs.SET_PREFERENCE";
	private static final String ACCESS_FORMS_SET_PREFERENCE = "com.alphabetbloc.accessforms.SET_PREFERENCE";
	private static final String USHAHIDI_SET_PREFERENCE = "com.alphabetbloc.accessushahidi.SET_PREFERENCE";
	private static final String ADW_SET_PREFERENCE = "com.alphabetbloc.accessadw.SET_PREFERENCE";

	// Generic Key/Value Intents
	public static final String PREFERENCE_KEY = "com.alphabetbloc.accessadmin.PREFERENCE_KEY";
	public static final String PREFERENCE_VALUE = "com.alphabetbloc.accessadmin.PREFERENCE_VALUE";

	private CheckBox accessFormsMenuToggle;
	private CheckBox accessMrsLogToggle;
	private CheckBox ushahidiToggle;
	private CheckBox accessMrsToggle;
	private CheckBox adwToggle;
	private SharedPreferences mSettings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editprefs);
		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		mSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		showAndroidSettings();
		if (isPackageInstalled(ADW_PACKAGE))
			showAdwSettings();
		if (isPackageInstalled(ACCESS_MRS_PACKAGE))
			showAccessMrsSettings();
		if (isPackageInstalled(ACCESS_FORMS_PACKAGE))
			showAccessFormsSettings();
		if (isPackageInstalled(USHAHIDI_PACKAGE))
			showUshahidiSettings();
	}

	private boolean isPackageInstalled(String packageName) {
		try {
			getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	private void showAndroidSettings(){
		((TableLayout) findViewById(R.id.settings_section)).setVisibility(View.VISIBLE);
		Button settingsButton = (Button) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ADW_PACKAGE, "com.android.settings.Settings"));
				startActivity(i);
			}
		});

	}
	
	private void showAdwSettings() {
		((TableLayout) findViewById(R.id.adw_section)).setVisibility(View.VISIBLE);
		Button adwButton = (Button) findViewById(R.id.adwButton);
		adwButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ADW_PACKAGE, "com.android.launcher.MyLauncherSettings"));
				startActivity(i);
			}
		});

		adwToggle = (CheckBox) findViewById(R.id.adw_checkbox);
		if (mSettings.getBoolean(getString(R.string.adw_menu_enabled), false))
			adwToggle.setChecked(true);
		adwToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Intent i = new Intent(ADW_SET_PREFERENCE);
				// i.putExtra(PREFERENCE_KEY,
				// getString(R.string.show_menu_preference));
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setAction("com.android.launcher.ViewMenuPreference.ACTION");
				i.setComponent(new ComponentName("com.android.launcher", "com.android.launcher.ViewMenuPreference"));
				if (adwToggle.isChecked()) {
					// i.putExtra(PREFERENCE_VALUE, String.valueOf(true));
					i.putExtra("ShowMenu", true);
					mSettings.edit().putBoolean(getString(R.string.adw_menu_enabled), true).commit();
				} else {
					// i.putExtra(PREFERENCE_VALUE, String.valueOf(false));
					i.putExtra("ShowMenu", false);
					mSettings.edit().putBoolean(getString(R.string.adw_menu_enabled), false).commit();
				}
				startActivity(i);

			}
		});
	}

	private void showAccessMrsSettings() {
		((TableLayout) findViewById(R.id.access_mrs_section)).setVisibility(View.VISIBLE);
		Button accessMrsButton = (Button) findViewById(R.id.access_mrs_button);
		accessMrsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ACCESS_MRS_PACKAGE, ACCESS_MRS_PACKAGE + ".ui.admin.PreferencesActivity"));
				startActivity(i);
			}
		});

		// View Menu Toggle
		accessMrsToggle = (CheckBox) findViewById(R.id.access_mrs_checkbox);
		if (mSettings.getBoolean(getString(R.string.access_mrs_menu_enabled), false))
			accessMrsToggle.setChecked(true);
		accessMrsToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(ACCESS_MRS_SET_PREFERENCE);
				i.putExtra(PREFERENCE_KEY, getString(R.string.show_menu_preference));
				if (accessMrsToggle.isChecked()) {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(true));
					mSettings.edit().putBoolean(getString(R.string.access_mrs_menu_enabled), true).commit();
				} else {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(false));
					mSettings.edit().putBoolean(getString(R.string.access_mrs_menu_enabled), false).commit();
				}
				sendBroadcast(i);
			}
		});

		// Log Toggle
		accessMrsLogToggle = (CheckBox) findViewById(R.id.access_mrs_log_checkbox);
		if (mSettings.getBoolean(getString(R.string.access_mrs_log_enabled), false))
			accessMrsLogToggle.setChecked(true);
		accessMrsLogToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(ACCESS_MRS_SET_PREFERENCE);
				i.putExtra(PREFERENCE_KEY, getString(R.string.enable_activity_log_preference));
				if (accessMrsLogToggle.isChecked()) {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(true));
					mSettings.edit().putBoolean(getString(R.string.access_mrs_log_enabled), true).commit();
				} else {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(false));
					mSettings.edit().putBoolean(getString(R.string.access_mrs_log_enabled), false).commit();
				}
				sendBroadcast(i);

			}
		});

	}

	private void showAccessFormsSettings() {
		((TableLayout) findViewById(R.id.access_forms_section)).setVisibility(View.VISIBLE);
		Button accessFormsButton = (Button) findViewById(R.id.access_forms_button);
		accessFormsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(ACCESS_FORMS_PACKAGE, "org.odk.collect.android.preferences.PreferencesActivity"));
				startActivity(i);
			}
		});

		// View Menu Preference
		accessFormsMenuToggle = (CheckBox) findViewById(R.id.access_forms_checkbox);
		if (mSettings.getBoolean(getString(R.string.access_forms_menu_enabled), false))
			accessFormsMenuToggle.setChecked(true);
		accessFormsMenuToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(ACCESS_FORMS_SET_PREFERENCE);
				i.putExtra(PREFERENCE_KEY, getString(R.string.show_menu_preference));
				if (accessFormsMenuToggle.isChecked()) {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(true));
					mSettings.edit().putBoolean(getString(R.string.access_forms_menu_enabled), true).commit();

				} else {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(false));
					mSettings.edit().putBoolean(getString(R.string.access_forms_menu_enabled), false).commit();
				}
				sendBroadcast(i);

			}
		});

	}

	private void showUshahidiSettings() {
		((TableLayout) findViewById(R.id.ushahidi_section)).setVisibility(View.VISIBLE);
		Button ushahidiButton = (Button) findViewById(R.id.ushahidiButton);
		ushahidiButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setComponent(new ComponentName(USHAHIDI_PACKAGE, "com.ushahidi.android.app.Settings"));
				startActivity(i);
			}
		});

		// check boxes / toggle buttons
		ushahidiToggle = (CheckBox) findViewById(R.id.ushahidi_checkbox);
		if (mSettings.getBoolean(getString(R.string.ushahidi_menu_enabled), false))
			ushahidiToggle.setChecked(true);
		ushahidiToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent i = new Intent(USHAHIDI_SET_PREFERENCE);
				i.putExtra(PREFERENCE_KEY, getString(R.string.show_menu_preference));
				if (ushahidiToggle.isChecked()) {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(true));
					mSettings.edit().putBoolean(getString(R.string.ushahidi_menu_enabled), true).commit();
				} else {
					i.putExtra(PREFERENCE_VALUE, String.valueOf(false));
					mSettings.edit().putBoolean(getString(R.string.ushahidi_menu_enabled), false).commit();
				}
				sendBroadcast(i);

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