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

import java.io.Serializable;

/**
 * Song model
 */
public class Song implements Serializable {

	private static final long serialVersionUID = -4757872544894909909L;

	/**
	 * Id
	 */
	public final String id;

	/**
	 * Name
	 */
	public final String name;

	/**
	 * Artist
	 */
	public final String artist;

	/**
	 * Album
	 */
	public final String album;

	/**
	 * Starred status
	 */
	public final boolean starred;

	/**
	 * Create song
	 *
	 * @param id
	 * @param name
	 * @param artist
	 * @param album
	 * @param starred
	 */
	public Song(final String id, final String name, final String artist,
			final String album, final boolean starred) {
		this.id = id;
		this.name = name;
		this.artist = artist;
		this.album = album;
		this.starred = starred;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : null;
	}

	/**
	 * Get unique album id
	 *
	 * @return id
	 */
	public String getAlbumId() {
		return artist + '#' + album;
	}

	@Override
	public boolean equals(Object o) {
		if (id == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof Song))
			return false;

		return id.equals(((Song) o).id);
	}

	@Override
	public String toString() {
		return name + " by " + artist + " from " + album;
	}
}
