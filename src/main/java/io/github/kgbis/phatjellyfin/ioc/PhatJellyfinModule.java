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
package io.github.kgbis.phatjellyfin.ioc;

import com.google.inject.AbstractModule;
import io.github.kgbis.phatjellyfin.config.ConfigStorage;
import io.github.kgbis.phatjellyfin.config.ConfigStorageImpl;
import io.github.kgbis.phatjellyfin.importer.Matcher;
import io.github.kgbis.phatjellyfin.importer.LevenshteinDistanceMatcherImpl;
import jakarta.inject.Singleton;

public class PhatJellyfinModule extends AbstractModule {

    /**
     * {@inheritDoc}
     * <p>
     * Only Factories, eager singletons and default interface implementations are
     * configured here. The rest of {@link Singleton} annotated classes are not set here
     * for clarity. Guice scans all them all automatically.
     */
    @Override
    protected void configure() {
        bind(ConfigStorage.class).to(ConfigStorageImpl.class).in(Singleton.class);
        bind(Matcher.class).to(LevenshteinDistanceMatcherImpl.class).in(Singleton.class);
    }
}
