/*
 * Copyright (C) 2012 Louis Fazen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.alphabetbloc.accessadmin.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListActivity;
import com.alphabetbloc.accessadmin.R;

public class AdminSettingsActivity extends SherlockListActivity {
    List<Map<String, Object>> mList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mList = new ArrayList<Map<String, Object>>();
        
        addListItem(getString(R.string.list_admin_password), SetAdminPassword.class.getName());
        addListItem(getString(R.string.list_admin_policy), SetDevicePolicy.class.getName());
        addListItem(getString(R.string.list_app_settings), SetAppPreferences.class.getName());
        addListItem(getString(R.string.list_sms_settings), ViewSmsSettings.class.getName());
        
        setListAdapter(new SimpleAdapter(this, mList, android.R.layout.simple_list_item_1, new String[] { "title" }, new int[] { android.R.id.text1 }));
        getListView().setTextFilterEnabled(true);
    }
    
    
    protected void addListItem(String title, String className) {
    	Intent i = new Intent();
    	i.setClassName(this, className);
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.put("title", title);
        temp.put("intent", i);
        mList.add(temp);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, Object> map = (Map<String, Object>)l.getItemAtPosition(position);
        Intent intent = (Intent) map.get("intent");
        startActivity(intent);
    }
}
