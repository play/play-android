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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;
import static com.github.kevinsawicki.http.HttpRequest.CHARSET_UTF8;
import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

/**
 * Service class to make requests to the Play API
 */
public class PlayService {

	private static final String USER_AGENT = "PlayAndroid/2.0";

	private static final Song[] EMPTY_SONGS = new Song[0];

	private static class SongWrapper {

		private Song[] songs;
	}

	static {
		// Disable http.keepAlive on Froyo and below
		if (SDK_INT <= FROYO)
			HttpRequest.keepAlive(false);
	}

	private static String encode(final String raw) {
		try {
			String encoded = URLEncoder.encode(raw, CHARSET_UTF8);
			return encoded.replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			return raw;
		}
	}

	private final Gson gson = new GsonBuilder().setFieldNamingPolicy(
			LOWER_CASE_WITH_UNDERSCORES).create();

	private final String baseUrl;

	private final String token;

	/**
	 * Create play service using base URL
	 *
	 * @param baseUrl
	 * @param token
	 */
	public PlayService(final String baseUrl, final String token) {
		if (baseUrl.endsWith("/"))
			this.baseUrl = baseUrl;
		else
			this.baseUrl = baseUrl + '/';
		this.token = token;
	}

	/**
	 * Create object of class type from content of request
	 *
	 * @param request
	 * @param target
	 * @return object of target class type
	 * @throws IOException
	 */
	protected <V> V fromJson(final HttpRequest request, final Type target)
			throws IOException {
		final Reader reader = request.bufferedReader();
		try {
			return gson.fromJson(reader, target);
		} catch (JsonParseException e) {
			IOException ioException = new IOException("Parsing JSON failed");
			ioException.initCause(e);
			throw ioException;
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
				// Ignored
			}
		}
	}

	/**
	 * Get songs from request response
	 *
	 * @param request
	 * @return non-null but possibly empty array of songs
	 * @throws IOException
	 */
	protected Song[] getSongs(final HttpRequest request) throws IOException {
		final SongWrapper wrapper = fromJson(request, SongWrapper.class);
		if (wrapper != null && wrapper.songs != null)
			return wrapper.songs;
		else
			return EMPTY_SONGS;
	}

	/**
	 * Verify request response code is a 200 OK and throw an exception when it
	 * is not
	 *
	 * @param request
	 * @return request
	 * @throws IOException
	 */
	protected HttpRequest ok(HttpRequest request) throws IOException {
		if (!request.ok())
			throw new IOException("Unexpected response code of "
					+ request.code());
		return request;
	}

	/**
	 * Create a GET request for the given URL
	 *
	 * @param url
	 * @return request
	 */
	protected HttpRequest get(final String url) {
		return HttpRequest.get(baseUrl + url).authorization(token)
				.userAgent(USER_AGENT);
	}

	/**
	 * Create a POST request for the given URL
	 *
	 * @param url
	 * @return request
	 */
	protected HttpRequest post(final String url) {
		String encoded = HttpRequest.encode(baseUrl + url);
		return HttpRequest.post(encoded).authorization(token)
				.userAgent(USER_AGENT);
	}

	/**
	 * Create a DELETE request for the given URL
	 *
	 * @param url
	 * @return request
	 */
	protected HttpRequest delete(final String url) {
		String encoded = HttpRequest.encode(baseUrl + url);
		return HttpRequest.delete(encoded).authorization(token)
				.userAgent(USER_AGENT);
	}

	/**
	 * Get currently playing song
	 *
	 * @return song
	 * @throws IOException
	 */
	public Song getNowPlaying() throws IOException {
		try {
			return fromJson(ok(get("now_playing")), Song.class);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get songs in the queue
	 *
	 * @return non-null but possibly empty array of songs
	 * @throws IOException
	 */
	public Song[] getQueue() throws IOException {
		try {
			return getSongs(ok(get("queue")));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get {@link StreamingInfo} for Play service
	 *
	 * @return URL to music stream
	 * @throws IOException
	 */
	public StreamingInfo getStreamingInfo() throws IOException {
		try {
			return fromJson(ok(get("streaming_info")), StreamingInfo.class);
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Download art image for {@link Song} to given file
	 *
	 * @param song
	 * @param file
	 * @return true if succeeded, false if failed
	 */
	public boolean getArt(Song song, File file) {
		try {
			HttpRequest request = get("images/art/" + song.id + ".png");
			if (request.ok() && request.contentLength() > 0) {
				request.receive(file);
				return true;
			} else
				return false;
		} catch (HttpRequestException e) {
			return false;
		}
	}

	/**
	 * Star song
	 *
	 * @param song
	 * @throws IOException
	 */
	public void star(Song song) throws IOException {
		try {
			ok(post("star?id=" + song.id));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Unstar song
	 *
	 * @param song
	 * @throws IOException
	 */
	public void unstar(Song song) throws IOException {
		try {
			ok(delete("star?id=" + song.id));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Remove the given song from the queue
	 *
	 * @param song
	 * @throws IOException
	 */
	public void dequeue(Song song) throws IOException {
		try {
			ok(delete("queue?id=" + song.id));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Add the given song to the queue
	 *
	 * @param song
	 * @throws IOException
	 */
	public void queue(Song song) throws IOException {
		try {
			ok(post("queue?id=" + song.id));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Add starred songs to the queue
	 *
	 * @return non-null but possibly empty array of queued songs
	 * @throws IOException
	 */
	public Song[] queueStars() throws IOException {
		try {
			return getSongs(ok(post("queue/stars")));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Requests some songs that match the freeform subject to be played
	 *
	 * @param subject
	 * @return non-null but possibly empty array of queued songs
	 * @throws IOException
	 */
	public Song[] queueSubject(String subject) throws IOException {
		try {
			return getSongs(ok(post("freeform?subject=" + subject)));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Search for songs matching query
	 *
	 * @param query
	 * @return non-null but possibly empty array of queued songs
	 * @throws IOException
	 */
	public Song[] search(final String query) throws IOException {
		try {
			return getSongs(ok(get("search?q=" + encode(query))));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get all songs on album by artist
	 *
	 * @param artist
	 * @param album
	 * @return non-null but possibly empty array of songs
	 * @throws IOException
	 */
	public Song[] getSongs(final String artist, final String album)
			throws IOException {
		try {
			return getSongs(ok(get("artist/" + encode(artist) + "/album/"
					+ encode(album))));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}

	/**
	 * Get all songs by artist
	 *
	 * @param artist
	 * @return non-null but possibly empty array of songs
	 * @throws IOException
	 */
	public Song[] getSongs(final String artist) throws IOException {
		try {
			return getSongs(ok(get("artist/" + encode(artist))));
		} catch (HttpRequestException e) {
			throw e.getCause();
		}
	}
}
