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
package com.github.play.core;

import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Stored settings for the configured Play server
 */
public class PlayPreferences {

	private static final String URL = "url";

	private static final String TOKEN = "token";

	private SharedPreferences preferences;

	/**
	 * Create settings for context
	 *
	 * @param context
	 */
	public PlayPreferences(final Context context) {
		preferences = context.getSharedPreferences("play-settings",
				MODE_PRIVATE);
	}

	/**
	 * Set preference key to given value
	 *
	 * @param key
	 * @param value
	 * @return this settings instance
	 */
	protected PlayPreferences set(final String key, String value) {
		if (value.length() == 0)
			value = null;
		preferences.edit().putString(key, value).commit();
		return this;
	}

	/**
	 * Does the given URL contain syntax errors?
	 *
	 * @param url
	 * @return true if no syntax errors, false otherwise
	 */
	protected boolean isValidUrl(final String url) {
		if (TextUtils.isEmpty(url))
			return false;
		try {
			new URI(url);
			return true;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * Get Play server URL
	 *
	 * @return URL or null if not configured
	 */
	public String getUrl() {
		String url = preferences.getString(URL, null);
		return isValidUrl(url) ? url : null;
	}

	/**
	 * Set Play server URL
	 *
	 * @param url
	 * @return this settings instance
	 */
	public PlayPreferences setUrl(final String url) {
		return isValidUrl(url) ? set(URL, url) : this;
	}

	/**
	 * Get configured token for API calls
	 *
	 * @return login or null if not configured
	 */
	public String getToken() {
		return preferences.getString(TOKEN, null);
	}

	/**
	 * Set token to be used for API calls
	 *
	 * @param token
	 * @return this settings instance
	 */
	public PlayPreferences setToken(final String token) {
		return set(TOKEN, token);
	}
}
