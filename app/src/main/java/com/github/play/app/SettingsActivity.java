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

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.graphics.Paint.UNDERLINE_TEXT_FLAG;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.R.id;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.EditTextUtils;
import com.github.kevinsawicki.wishlist.EditTextUtils.BooleanRunnable;
import com.github.kevinsawicki.wishlist.Toaster;
import com.github.kevinsawicki.wishlist.ViewFinder;
import com.github.play.R.layout;
import com.github.play.R.menu;
import com.github.play.R.string;
import com.github.play.core.PlayPreferences;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Activity to configure the settings for a Play server
 */
public class SettingsActivity extends SherlockActivity {

	private static final String PREFIX_HTTP = "http://";

	private static final String PREFIX_HTTPS = "https://";

	private PlayPreferences settings;

	private EditText tokenText;

	private EditText urlText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.settings);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		settings = new PlayPreferences(this);

		ViewFinder finder = new ViewFinder(this);
		tokenText = finder.find(id.et_token);
		String token = settings.getToken();
		if (token != null)
			tokenText.setText(token);

		urlText = finder.find(id.et_url);
		String url = settings.getUrl();
		if (url != null)
			urlText.setText(url);

		TextView tokenLink = finder.find(id.tv_token_link);
		tokenLink
				.setPaintFlags(UNDERLINE_TEXT_FLAG | tokenLink.getPaintFlags());
		tokenLink.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String url = urlText.getText().toString();
				boolean valid = !TextUtils.isEmpty(url);
				if (valid) {
					if (!url.startsWith(PREFIX_HTTP)
							& !url.startsWith(PREFIX_HTTPS))
						url = PREFIX_HTTPS + url;
					if (!url.endsWith("/"))
						url += "/token";
					else
						url += "token";
					try {
						new URI(url);
					} catch (URISyntaxException e) {
						valid = false;
					}
				}

				if (valid)
					startActivity(new Intent(ACTION_VIEW, Uri.parse(url)));
				else
					Toaster.showLong(SettingsActivity.this,
							string.enter_play_server_url);
			}
		});

		EditTextUtils.onDone(tokenText, new BooleanRunnable() {

			public boolean run() {
				saveSettings();
				return true;
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu optionsMenu) {
		getSupportMenuInflater().inflate(menu.settings, optionsMenu);
		return true;
	}

	private void saveSettings() {
		String token = tokenText.getText().toString().trim();
		if (TextUtils.isEmpty(token))
			return;
		String url = urlText.getText().toString().trim();
		if (TextUtils.isEmpty(url))
			return;

		if (!url.startsWith(PREFIX_HTTP) & !url.startsWith(PREFIX_HTTPS))
			url = PREFIX_HTTPS + url;

		try {
			new URI(url);
		} catch (URISyntaxException e) {
			Toaster.showLong(this, string.enter_play_server_url);
			return;
		}

		boolean changed = !token.equals(settings.getToken())
				|| !url.equals(settings.getUrl());
		if (changed) {
			settings.setToken(token).setUrl(url);
			setResult(RESULT_OK);
		} else
			setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, PlayActivity.class);
			intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		case id.m_save:
			saveSettings();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
