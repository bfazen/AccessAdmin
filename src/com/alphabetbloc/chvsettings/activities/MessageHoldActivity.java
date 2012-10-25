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

import android.os.Bundle;
import android.widget.TextView;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;

public class MessageHoldActivity extends DeviceHoldActivity {

	public static final String STOP_HOLD = "stop_hold";
	private String mToast = "";
	public static boolean sMessageHoldActive = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean stopHold = getIntent().getBooleanExtra(STOP_HOLD, false);
		if(stopHold)
			finish();
		setContentView(R.layout.message_hold);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sMessageHoldActive = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		sMessageHoldActive = true;
		refreshView();
	}
	
	private void refreshView(){
		mToast = getIntent().getStringExtra(Constants.TOAST_MESSAGE);
		TextView tv = (TextView) findViewById(R.id.message);
		tv.setText(mToast);
	}
}
