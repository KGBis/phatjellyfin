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

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum HttpStatus {

	// @formatter:off

    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),

    // 3xx Redirection
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    NOT_MODIFIED(304, "Not Modified"),

    // 4xx Client Errors
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONFLICT(409, "Conflict"),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout");
    // @formatter:on

	private static final Map<Integer, HttpStatus> BY_CODE = new HashMap<>();

	static {
		for (HttpStatus status : values()) {
			BY_CODE.put(status.code, status);
		}
	}

	private final int code;

	private final String description;

	HttpStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static String getDescription(int code) {
		HttpStatus status = BY_CODE.get(code);
		return (status != null) ? status.getDescription() : "Unknown Status Code";
	}

    // --- Some additional helper methods ---

    public boolean is4xxClientError() {
		return code >= 400 && code < 500;
	}

	public boolean is5xxServerError() {
		return code >= 500 && code < 600;
	}

	public boolean isError() {
		return code >= 400;
	}

}
