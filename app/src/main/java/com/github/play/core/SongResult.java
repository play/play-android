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

import static java.lang.String.CASE_INSENSITIVE_ORDER;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Result class for a task that results in an {@link IOException} or an array of
 * {@link Song} objects
 */
public class SongResult {

	/**
	 * Songs
	 */
	public final Song[] songs;

	/**
	 * Failure exception
	 */
	public final IOException exception;

	/**
	 * Albums
	 */
	public final Song[] albums;

	/**
	 * Create result with songs
	 *
	 * @param songs
	 */
	public SongResult(final Song[] songs) {
		this.songs = songs;
		exception = null;

		final Map<String, Song> albums = new TreeMap<String, Song>(
				CASE_INSENSITIVE_ORDER);
		for (Song song : songs)
			albums.put(song.getAlbumId(), song);
		this.albums = albums.values().toArray(new Song[albums.size()]);
	}

	/**
	 * Create result with exception
	 *
	 * @param error
	 */
	public SongResult(final IOException error) {
		songs = null;
		albums = null;
		exception = error;
	}
}
