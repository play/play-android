/*
 * Copyright 2012 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.play.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.actionbarsherlock.R.id;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.core.PlayPreferences;

/**
 * Activity to configure the settings for a Play server
 */
public class SettingsActivity extends SherlockActivity {

	private PlayPreferences settings;

	private EditText loginText;

	private EditText urlText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.settings);

		settings = new PlayPreferences(this);

		loginText = (EditText) findViewById(id.et_login);
		String login = settings.getLogin();
		if (login != null)
			loginText.setText(login);

		urlText = (EditText) findViewById(id.et_url);
		String url = settings.getUrl();
		if (url != null)
			urlText.setText(url);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.settings, optionsMenu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case id.m_save:
			String login = loginText.getText().toString().trim();
			String url = urlText.getText().toString().trim();
			if (!TextUtils.isEmpty(login) && !TextUtils.isEmpty(url)) {
				settings.setLogin(login).setUrl(url);
				setResult(RESULT_OK);
				finish();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
