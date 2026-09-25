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
package io.github.kgbis.phatjellyfin;

import ch.qos.logback.classic.Level;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.kgbis.phatjellyfin.arguments.CliArguments;
import io.github.kgbis.phatjellyfin.arguments.CliParser;
import io.github.kgbis.phatjellyfin.client.JellyfinClient;
import io.github.kgbis.phatjellyfin.client.model.Library;
import io.github.kgbis.phatjellyfin.client.model.SystemInfo;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.ConfigStorage;
import io.github.kgbis.phatjellyfin.importer.Importer;
import io.github.kgbis.phatjellyfin.ioc.PhatJellyfinModule;
import io.github.kgbis.phatjellyfin.log.LogbackConfiguration;
import io.github.kgbis.phatjellyfin.config.ConfigException;
import io.github.kgbis.phatjellyfin.importer.OperationResult;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.util.List;

import static io.github.kgbis.phatjellyfin.output.MessageManager.LOGO;
import static io.github.kgbis.phatjellyfin.output.MessageManager.SERVER_INFO_MESSAGE;

@Singleton
@Slf4j
public class PhatJellyfin {

	private final Console console;

	private final MessageManager messageManager;

	private final CliParser cliParser;

	private final ConfigManager configManager;

	private final Importer importer;

	private final JellyfinClient jellyfinClient;

	private StopWatch stopWatch;

	@Inject
	public PhatJellyfin(Console console, MessageManager messageManager, CliParser cliParser,
			ConfigManager configManager, Importer importer, JellyfinClient jellyfinClient) {
		this.console = console;
		this.messageManager = messageManager;
		this.cliParser = cliParser;
		this.configManager = configManager;
		this.importer = importer;
		this.jellyfinClient = jellyfinClient;
	}

	public void initialize(String[] args) {
		// start stopwatch
		stopWatch = StopWatch.createStarted();

		// parse command line arguments
		CliArguments cliArguments = cliParser.parseCommandLine(args, jc -> {
			jc.usage();
			System.exit(0);
		});

		// Configure log output to file and console (optional)
		LogbackConfiguration.configure(Level.DEBUG, cliArguments.isLogToConsole());

		// Store parsed arguments
		configManager.storeArguments(cliArguments);
	}

	private void start() {
		// print logo on console
		console.println(LOGO);

		try {
			checkRootFolder();
			showConfig();
			checkJellyfinServer();
			importer.run();
		}
		catch (IOException | InterruptedException e) { // NOSONAR
			log.error(e.getMessage());
			abort("Unable to read configuration file: " + ConfigStorage.getConfigFile());
		}
		catch (ConfigException e) {
			log.warn(e.getMessage());
			showConfigAndExit();
		}

		console.println(stopWatch);
	}

	private void checkJellyfinServer() {
		try {
			SystemInfo info = jellyfinClient.getServerBasicInfo();
			console.println(SERVER_INFO_MESSAGE.formatted(info.serverName(), info.version(), info.localAddress()));

			List<Library> libraries = jellyfinClient.getLibraries();
			configManager.addServerLibraries(libraries);
		}
		catch (IOException | InterruptedException e) { // NOSONAR
			abort("Unable to reach Jellyfin server: " + e.getMessage());
		}

	}

	/**
	 * Checks if folder path exists and we can read from it
	 */
	private void checkRootFolder() throws IOException {
		OperationResult<Void> result = importer.rootFolderExistsAndReadable(configManager.current().getRootFolder());
		if (!result.isSuccess()) {
			abort(result.getErrorCause());
		}
	}

	/**
	 * Utility method to show error message to console and exit
	 * @param message Message to show
	 */
	private void abort(String message) {
		console.printErrorAndStopWatch(message, stopWatch);
		System.exit(-1);
	}

	private void showConfig() {
		console.print(messageManager.buildConfigMessage());
	}

	private void showConfigAndExit() {
		console.printAndStopWatch(messageManager.buildNoConfigMessage(), stopWatch);
		System.exit(0);
	}

	static void main(String[] args) {
		// System property for Guice
		System.setProperty("guice_bytecode_gen_option", "DISABLED");

		Injector injector = Guice.createInjector(new PhatJellyfinModule());
		PhatJellyfin phatJellyfin = injector.getInstance(PhatJellyfin.class);

		phatJellyfin.initialize(args);
		phatJellyfin.start();
	}

}
