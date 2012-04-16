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

/**
 * Stored settings for the configured Play server
 */
public class PlayPreferences {

	private static final String URL = "url";

	private static final String LOGIN = "login";

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
	 * Get Play server URL
	 * 
	 * @return URL or null if not configured
	 */
	public String getUrl() {
		return preferences.getString(URL, null);
	}

	/**
	 * Set Play server URL
	 * 
	 * @param url
	 * @return this settings instance
	 */
	public PlayPreferences setUrl(final String url) {
		preferences.edit().putString(URL, url.length() > 0 ? url : null)
				.commit();
		return this;
	}

	/**
	 * Get configured login name
	 * 
	 * @return login or null if not configured
	 */
	public String getLogin() {
		return preferences.getString(LOGIN, null);
	}

	/**
	 * Set login name
	 * 
	 * @param login
	 * @return this settings instance
	 */
	public PlayPreferences setLogin(final String login) {
		preferences.edit().putString(LOGIN, login.length() > 0 ? login : null)
				.commit();
		return this;
	}
}
