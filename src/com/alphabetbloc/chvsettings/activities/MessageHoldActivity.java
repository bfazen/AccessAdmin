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

package com.alphabetbloc.chvsettings.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;

public class MessageHoldActivity extends DeviceHoldActivity {

	private static final int PROGRESS_DIALOG = 1;
	private String mToast = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		dismissDialog(PROGRESS_DIALOG);
		super.onPause();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		mToast = getIntent().getStringExtra(Constants.TOAST_MESSAGE);
		showDialog(PROGRESS_DIALOG);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			ProgressDialog progressDialog;
			progressDialog = new ProgressDialog(this);
			progressDialog.setIcon(android.R.drawable.ic_dialog_info);
			progressDialog.setMessage(mToast);
			progressDialog.setTitle(getString(R.string.please_wait));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			return progressDialog;
		}
		return null;
	}
}
