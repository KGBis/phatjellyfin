/*
 * PhatJellyfin
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.kgbis.phatjellyfin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;

public class JsonBodyHandler {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private JsonBodyHandler() {
		/* This utility class should not be instantiated */
	}

	public static <T> HttpResponse.BodyHandler<T> ofJson(Class<T> targetType) {
		return _ -> {
			// Convert the network byte stream into an input stream
			BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();

			// Map inputstream to the java class type
			return BodySubscribers.mapping(upstream, inputStream -> {
				try (inputStream) {
					return objectMapper.readValue(inputStream, targetType);
				}
				catch (Exception e) {
					throw new RuntimeException("Error parsing JSON", e);
				}
			});
		};
	}

	public static <T> HttpResponse.BodyHandler<T> ofJson(TypeReference<T> targetType) {
		return _ -> {
			BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();
			return BodySubscribers.mapping(upstream, inputStream -> {
				try (inputStream) {
					return objectMapper.readValue(inputStream, targetType);
				}
				catch (Exception e) {
					throw new RuntimeException("Error parsing JSON", e);
				}
			});
		};
	}

}
