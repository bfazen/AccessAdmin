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

package com.alphabetbloc.chvsettings.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.alphabetbloc.chvsettings.data.Policy;

/**
 * Android DeviceAdminReceiver class.  When enabled, it lets you control
 * some of its policy and reports when there is interesting activity.
 */
public class DeviceAdmin extends DeviceAdminReceiver {

    static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Policy.SHARED_PREF, 0);
    }

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "Device Administrator Enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Are you sure you want to disable the Administrator on this device?";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "Device Administrator Disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "Your password has been successfully changed.");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "Your password has not been changed.");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        showToast(context, "Your password is successful.");
    } 

    
}
