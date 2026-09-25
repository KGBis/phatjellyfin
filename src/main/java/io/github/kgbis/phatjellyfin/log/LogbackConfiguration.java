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
package io.github.kgbis.phatjellyfin.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.kgbis.phatjellyfin.Application.APP_NAME;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class LogbackConfiguration {

	/* Log paths */
	private static final String WIN_LOG_FOLDER = "LOCALAPPDATA";

	private static final String LINUX_LOG_FOLDER = ".cache";

	private static final String MACOS_LIB_FOLDER = "Library";

	private static final String MACOS_LOG_FOLDER = "Logs";

	private static final String LOG_FILE_NAME = APP_NAME + ".log";

	private static final String GZ_FILE_NAME = LOG_FILE_NAME + "-%d{yyyy-MM-dd}.gz";

	private static final String LOGGING_PATTERN = "%date{ISO8601} %-26(%-5p [%t]) %logger{1} - %m%n";

	private static final String UNSUPPORTED = "Unsupported OS: %s";

	private static final String USER_HOME = System.getProperty("user.home");

	private static String version;

	public static void configure(Level rootLevel, boolean logToConsole) {
		Path logDir = getOSLogDirectory();

		// Logger context
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		// Rolling file appender
		String filename = Paths.get(logDir.toString(), LOG_FILE_NAME).toString();
		String gz = Path.of(logDir.toString(), GZ_FILE_NAME).toString();

		RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
		fileAppender.setContext(context);
		fileAppender.setFile(filename);

		TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		rollingPolicy.setContext(context);
		rollingPolicy.setParent(fileAppender);
		rollingPolicy.setFileNamePattern(gz);
		rollingPolicy.setMaxHistory(30);
		rollingPolicy.start();

		fileAppender.setRollingPolicy(rollingPolicy);

		PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
		fileEncoder.setContext(context);
		fileEncoder.setPattern(LOGGING_PATTERN);
		fileEncoder.setCharset(StandardCharsets.UTF_8);
		fileEncoder.start();

		fileAppender.setEncoder(fileEncoder);
		fileAppender.start();

		// jAudioTagger (too much logs when in DEBUG)
		Logger jAudioTagger = context.getLogger("org.jaudiotagger");
		if (rootLevel.equals(Level.DEBUG) || rootLevel.equals(Level.TRACE)) {
			jAudioTagger.setLevel(Level.INFO);
		}

		// Root logger
		Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (logToConsole)
			root.addAppender(consoleAppender(context));
		root.addAppender(fileAppender);
		root.setLevel(Level.INFO);
		log.info("Starting {} version {}", APP_NAME, getVersion());
		log.info("Logging initialized. Root level: {}", rootLevel);
		root.setLevel(rootLevel);
	}

	private static ConsoleAppender<ILoggingEvent> consoleAppender(LoggerContext context) {
		// Console appender
		ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
		console.setContext(context);
		PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
		consoleEncoder.setContext(context);
		consoleEncoder.setPattern(LOGGING_PATTERN);
		consoleEncoder.setCharset(StandardCharsets.UTF_8);
		consoleEncoder.start();
		console.setEncoder(consoleEncoder);
		console.start();

		return console;
	}

	private static String getVersion() {
		if (version == null) {
			try (InputStream in = LogbackConfiguration.class.getClassLoader().getResourceAsStream("version.txt")) {
				if (in == null) {
					version = "unknown";
				}
				else {
					version = new String(in.readAllBytes()).trim();
				}
			}
			catch (IOException _) {
				version = "unknown";
			}
		}

		return version;
	}

	private static Path getOSLogDirectory() {
		Path logDir;
		if (SystemUtils.IS_OS_WINDOWS) {
			String logFolder = System.getenv(WIN_LOG_FOLDER);
			logDir = Path.of(logFolder != null ? logFolder : USER_HOME, APP_NAME, "logs");
		}
		else if (SystemUtils.IS_OS_MAC) {
			logDir = Path.of(USER_HOME, MACOS_LIB_FOLDER, MACOS_LOG_FOLDER, APP_NAME);
		}
		else if (SystemUtils.IS_OS_LINUX) {
			logDir = Path.of(USER_HOME, LINUX_LOG_FOLDER, APP_NAME, "logs");
		}
		else {
			throw new UnsupportedOperationException(String.format(UNSUPPORTED, SystemProperties.getOsName()));
		}

		createDir(logDir);
		return logDir;
	}

	private static void createDir(Path path) {
		try {
			Files.createDirectories(path);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot create directory " + path, e);
		}
	}

}
