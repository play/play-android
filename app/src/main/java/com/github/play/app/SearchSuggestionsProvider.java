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

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

/**
 * Suggestion provider for previous Play searches
 */
public class SearchSuggestionsProvider extends SearchRecentSuggestionsProvider {

	/**
	 * Authority of search suggestion provider
	 */
	public static final String AUTHORITY = "com.github.play.search.suggest";

	/**
	 * Clear all recent suggestions in history
	 *
	 * @param context
	 */
	public static final void clear(final Context context) {
		new SearchRecentSuggestions(context, AUTHORITY, DATABASE_MODE_QUERIES)
				.clearHistory();
	}

	/**
	 * Add recent suggestion query to history
	 *
	 * @param context
	 * @param query
	 */
	public static final void add(final Context context, final String query) {
		new SearchRecentSuggestions(context, AUTHORITY, DATABASE_MODE_QUERIES)
				.saveRecentQuery(query, null);
	}

	/**
	 * Search suggestion provider for Play
	 */
	public SearchSuggestionsProvider() {
		setupSuggestions(AUTHORITY, DATABASE_MODE_QUERIES);
	}
}
