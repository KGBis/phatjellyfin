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
package io.github.kgbis.phatjellyfin.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kgbis.phatjellyfin.client.JellyfinClient;
import io.github.kgbis.phatjellyfin.client.model.Artist;
import io.github.kgbis.phatjellyfin.client.model.Items;
import io.github.kgbis.phatjellyfin.config.Config;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JellyfinMatcherTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Mock
	private ConfigManager configManager;

	@Mock
	private Console console;

	@Mock
	private MessageManager messageManager;

	@Mock
	private JellyfinClient jellyfinClient;

	@Mock
	private Matcher matcher;

	@Mock
	private ArtistHelper artistHelper;

	@InjectMocks
	private JellyfinMatcher underTest;

	@Test
	void when_albumFound_and_allTracksMatch() throws IOException, InterruptedException {
		String name = "Más Madera";
		Items album = albumItemFromFile(name);
		Items tracks = tracksItemFromFile(name);

		when(configManager.current()).thenReturn(Config.builder().libraryPaths(List.of("/media/music")).build());
		when(jellyfinClient.getItems(anyMap())).thenReturn(album).thenReturn(tracks);

		Map<MusicKey, AlbumToUpdate> toUpdateMap = underTest.matchAgainstJellyfin(getMasMaderaPreparedlibrary());

		assertEquals(1, toUpdateMap.size());
		assertEquals(2, toUpdateMap.get(toUpdateMap.keySet().iterator().next()).getTracks().size());
	}

	@Test
	void when_albumFound_and_noTracksMatch() throws IOException, InterruptedException {
		String name = "Californication";
		Items album = albumItemFromFile(name);
		Items tracks = tracksItemFromFile(name);
		when(configManager.current()).thenReturn(Config.builder().libraryPaths(List.of("/media/music")).build());
		when(jellyfinClient.getItems(anyMap())).thenReturn(album).thenReturn(tracks);

		Map<MusicKey, AlbumToUpdate> toUpdateMap = underTest.matchAgainstJellyfin(getCalifornicationPreparedlibrary());

		assertEquals(0, toUpdateMap.size());
	}

	@Test
	void when_albumFound_and_someTracksMatch() throws IOException, InterruptedException {
		String name = "Temptation";
		Items album = albumItemFromFile(name);
		Items tracks = tracksItemFromFile(name);
		when(configManager.current()).thenReturn(Config.builder().libraryPaths(List.of("/media/music")).build());
		when(jellyfinClient.getItems(anyMap())).thenReturn(album).thenReturn(tracks);

		Map<MusicKey, AlbumToUpdate> toUpdateMap = underTest
			.matchAgainstJellyfin(getCalifornicationMixedPreparedlibrary());

		assertEquals(1, toUpdateMap.size());

		AlbumToUpdate albumToUpdate = toUpdateMap.get(toUpdateMap.keySet().iterator().next());
		assertEquals(1, albumToUpdate.getTracks().size());
		assertEquals(1, albumToUpdate.getUnmatchedTracks().size());
	}

	@Test
	void when_albumNotFound() throws IOException, InterruptedException {
		Items album = albumItemFromFile("Loco por incordiar"); // Empty list
		String name = "Rosendo";
		Items artist = artistItemFromFile(name);
		Items albumByArtist = albumItemFromFile("Rosendo");

		when(configManager.current()).thenReturn(Config.builder().libraryPaths(List.of("/media/music")).build());
		when(jellyfinClient.getItems(anyMap())).thenReturn(album).thenReturn(albumByArtist);
		when(artistHelper.findArtist(name)).thenReturn(Optional.of(artist.items().getFirst()));

		Map<MusicKey, AlbumToUpdate> toUpdateMap = underTest.matchAgainstJellyfin(getLocoPreparedlibrary());

		assertEquals(0, toUpdateMap.size());
	}

	private Items albumItemFromFile(String name) throws IOException {
		return loadItems(name + "_album.json");
	}

	private Items tracksItemFromFile(String name) throws IOException {
		return loadItems(name + "_tracks.json");
	}

	private Items artistItemFromFile(String name) throws IOException {
		return loadItems(name + "_artist.json");
	}

	private Items loadItems(String filename) throws IOException {
		return mapper.readValue(getClass().getResourceAsStream("/" + filename), Items.class);
	}

	private Map<MusicKey, List<ScannedFile>> getMasMaderaPreparedlibrary() {
		String album = "Más Madera";
		String artist = "Leño";
		MusicKey key = new MusicKey(List.of(Artist.builder().name(artist).build()), album);
		List<ScannedFile> value = List.of(
				ScannedFile.builder()
					.path(Matcher.normalize("/Leño/1980 - Más Madera/02 - Donde esta la salvacion.mp3"))
					.track(2)
					.year(1980)
					.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, artist,
							JellyfinMetadata.ARTIST, artist, JellyfinMetadata.TITLE, "¿Dónde está la salvación?"))
					.build(),
				ScannedFile.builder()
					.path(Matcher.normalize("/Leño/1980 - Más Madera/06 - Si senor, si senor.mp3"))
					.track(6)
					.year(1980)
					.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, artist,
							JellyfinMetadata.ARTIST, artist, JellyfinMetadata.TITLE, "Sí señor, sí señor"))
					.build());

		return Map.of(key, value);
	}

	private Map<MusicKey, List<ScannedFile>> getLocoPreparedlibrary() {
		String album = "Loco Por Incordiar";
		String artist = "Rosendo";
		MusicKey key = new MusicKey(List.of(Artist.builder().name(artist).build()), album);
		List<ScannedFile> value = List.of(
				ScannedFile.builder()
					.path(Matcher.normalize("/Rosendo/1985 - Loco Por Incordiar/01 - Agradecido.mp3"))
					.track(1)
					.year(1985)
					.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, artist,
							JellyfinMetadata.ARTIST, artist, JellyfinMetadata.TITLE, "Agradecido"))
					.build(),
				ScannedFile.builder()
					.path(Matcher.normalize("/Rosendo/1985 - Loco Por Incordiar/02 - Corazon.mp3"))
					.track(2)
					.year(1985)
					.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, artist,
							JellyfinMetadata.ARTIST, artist, JellyfinMetadata.TITLE, "Corazón"))
					.build());

		return Map.of(key, value);
	}

	private Map<MusicKey, List<ScannedFile>> getCalifornicationPreparedlibrary() {
		String album = "Californication";
		String albumArtist = "Various Artists";
		MusicKey key = new MusicKey(List.of(Artist.builder().name(albumArtist).build()), album);
		List<ScannedFile> value = List.of(ScannedFile.builder()
			.path(Matcher.normalize(
					"/Soundtracks/2008 - Temptation (Music From The Showtime Series Californication)/01 - You can't always get what you want.mp3"))
			.track(2)
			.year(2008)
			.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, albumArtist,
					JellyfinMetadata.ARTIST, "The Rolling Stones", JellyfinMetadata.TITLE,
					"You can't always get what you want"))
			.build(),
				ScannedFile.builder()
					.path(Matcher.normalize(
							"/Soundtracks/2008 - Temptation (Music From The Showtime Series Californication)/04 - L.A. Woman.mp3"))
					.track(4)
					.year(2008)
					.metadata(Map.of(JellyfinMetadata.ALBUM, album, JellyfinMetadata.ALBUM_ARTIST, albumArtist,
							JellyfinMetadata.ARTIST, "The Doors\0Paul Oakenfold", JellyfinMetadata.TITLE, "L.A. Woman"))
					.build());

		return Map.of(key, value);
	}

	private Map<MusicKey, List<ScannedFile>> getCalifornicationMixedPreparedlibrary() {
		String album = "Temptation (Music from the Showtime Series Californication)";
		String albumArtist = "Various Artists";
		MusicKey key = new MusicKey(List.of(Artist.builder().name(albumArtist).build()), album);
		// @formatter:off
		List<ScannedFile> value = List.of(
				ScannedFile.builder()
						.path(Matcher.normalize(
								"/Soundtracks/2008 - Temptation (Music From The Showtime Series Californication)/01 - Paint it black.mp3")
						).track(1).year(2008)
						.metadata(Map.of(
								JellyfinMetadata.ALBUM, album,
								JellyfinMetadata.ALBUM_ARTIST, albumArtist,
								JellyfinMetadata.ARTIST, "The Rolling Stones",
								JellyfinMetadata.TITLE, "You can't always get what you want")
						).build(),
				ScannedFile.builder()
					.path(Matcher.normalize(
							"/Soundtracks/2008 - Temptation (Music From The Showtime Series Californication)/04 - L.A. Woman.mp3"))
					.track(4).year(2008).metadata(Map.of(
							JellyfinMetadata.ALBUM, album,
								JellyfinMetadata.ALBUM_ARTIST, albumArtist,
								JellyfinMetadata.ARTIST, "The Doors\0Paul Oakenfold",
								JellyfinMetadata.TITLE, "L.A. Woman")
						).build());

		return Map.of(key, value);
	}

}