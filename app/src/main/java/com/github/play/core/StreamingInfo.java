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
 * Information about the streaming location of the Play server and also the
 * pusher application key
 */
public class StreamingInfo implements Serializable {

	private static final long serialVersionUID = -6632557432169389967L;

	/**
	 * URL to stream music from
	 */
	public final String streamUrl;

	/**
	 * Pusher application key
	 */
	public final String pusherKey;

	/**
	 * Create streaming info with given values
	 *
	 * @param streamUrl
	 * @param pusherKey
	 */
	public StreamingInfo(final String streamUrl, final String pusherKey) {
		this.streamUrl = streamUrl;
		this.pusherKey = pusherKey;
	}
}
