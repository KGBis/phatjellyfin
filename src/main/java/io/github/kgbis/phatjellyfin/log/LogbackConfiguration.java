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
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.kgbis.phatjellyfin.Application.APP_NAME;
import static io.github.kgbis.phatjellyfin.Application.GZ_FILE_NAME;
import static io.github.kgbis.phatjellyfin.Application.LOG_FILE_NAME;
import static io.github.kgbis.phatjellyfin.Application.getVersion;

@Singleton
@Slf4j
public class LogbackConfiguration {

	private static final String LOGGING_PATTERN = "%date{ISO8601} %-26(%-5p [%t]) %logger{1} - %m%n";

	private final ConfigManager configManager;

	@Inject
	public LogbackConfiguration(ConfigManager configManager) {
		this.configManager = configManager;
	}

	public void configure(Level rootLevel, boolean logToConsole) {
		Path logDir = configManager.getLogFile();
		createDir(logDir);

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
		rollingPolicy.setMaxHistory(7);
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
		// Ideally only ERROR messages as it throws a lot of WARN messages if the MP3 has
		// incorrect data or non-standard tags
		Logger jAudioTagger = context.getLogger("org.jaudiotagger");
		if (rootLevel.equals(Level.DEBUG) || rootLevel.equals(Level.TRACE)) {
			jAudioTagger.setLevel(Level.ERROR);
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

	private ConsoleAppender<ILoggingEvent> consoleAppender(LoggerContext context) {
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

	private void createDir(Path path) {
		try {
			Files.createDirectories(path);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot create directory " + path, e);
		}
	}

}
