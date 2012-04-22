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
 * Wrapper class to encapsulate all data send via a Play push update
 */
public class StatusUpdate implements Serializable {

	private static final long serialVersionUID = -877849357884315386L;

	/**
	 * Currently playing song
	 */
	public final Song playing;

	/**
	 * Songs in the queue
	 */
	public final Song[] queued;

	/**
	 * Create status update
	 *
	 * @param playing
	 * @param queued
	 */
	public StatusUpdate(final Song playing, final Song[] queued) {
		this.playing = playing;
		this.queued = queued;
	}
}
