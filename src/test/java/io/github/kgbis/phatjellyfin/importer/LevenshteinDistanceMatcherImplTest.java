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

import io.github.kgbis.phatjellyfin.client.model.Artist;
import io.github.kgbis.phatjellyfin.client.model.Item;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevenshteinDistanceMatcherImplTest {

	Artist jellyfinArtist;

	String jellyfinAlbum;

	@Mock
	private ArtistHelper artistHelper;

	@InjectMocks
	private LevenshteinDistanceMatcherImpl underTest;

	@BeforeEach
	void setUp() {
		jellyfinArtist = Artist.builder().name("artist name").id("c9eebdae964c08cc873c946a8202893f").build();
		jellyfinAlbum = "album name";
	}

	@Test
	void whenTrackNumberDoesNotMatch_thenThereIsNoMatch() {
		Item item = new Item("name", "serverId", "id", "audio", false, 1, null, jellyfinAlbum, jellyfinArtist.getName(),
				List.of(jellyfinArtist), List.of(jellyfinArtist), 2026, "any/path");

		Map<JellyfinMetadata, String> metadata = Map.of(JellyfinMetadata.ALBUM, "Whatever Album",
				JellyfinMetadata.ALBUM_ARTIST, "An Artist", JellyfinMetadata.ARTIST, "An Artist",
				JellyfinMetadata.TITLE, "Track title");
		ScannedFile scannedFile = new ScannedFile("any/path", 10, 1, 2026, metadata);

		boolean match = underTest.fallbackMatch(item, scannedFile);
		assertFalse(match);
	}

	@Test
	void whenAlbumAndArtistMatch_thenThereIsMatch() {
		Item item = new Item("Jellyfin track name", "serverId", "id", "audio", false, 10, null, jellyfinAlbum,
				jellyfinArtist.getName(), List.of(jellyfinArtist), List.of(jellyfinArtist), 2026, "any/path");

		Map<JellyfinMetadata, String> metadata = Map.of(JellyfinMetadata.ALBUM, jellyfinAlbum,
				JellyfinMetadata.ALBUM_ARTIST, jellyfinArtist.getName(), JellyfinMetadata.ARTIST,
				jellyfinArtist.getName(), JellyfinMetadata.TITLE, "Track title");
		ScannedFile scannedFile = new ScannedFile("any/path", 10, 1, 2026, metadata);

		when(artistHelper.toArtists(jellyfinArtist.getName())).thenReturn(List.of(jellyfinArtist));
		boolean match = underTest.fallbackMatch(item, scannedFile);
		assertTrue(match);
	}

	@Nested
	class SameArtists {

		@Test
		void when_AlbumIsSimilar_then_MatchIsTrue() {
			// @formatter:off
			Map<String, String> albums = Map.of(
					"Leno", "Leño",
					"B-Sides N' EPs", "B-Sides N' EP's",
					"Album with Spaces", "Album with   Spaces",
					"Live the Budokan", "Live at the Budokan",
					"Popular Democratico Cientifico", "Popular, Democrático y Científico",
					"Paseando Sobre el Agua - Single", "Paseando Sobre el Agua (Single)"
			);
			// @formatter:on
			for (Map.Entry<String, String> entry : albums.entrySet()) {
				Item item = new Item("Jellyfin track name", "serverId", "id", "audio", false, 10, null, entry.getKey(),
						jellyfinArtist.getName(), List.of(jellyfinArtist), List.of(jellyfinArtist), 2026,
						"any/path/in/jellyfin/server/10 - Jellyfin track name.mp3");

				// @formatter:off
				Map<JellyfinMetadata, String> metadata = Map.of(
						JellyfinMetadata.ALBUM, entry.getValue(),
						JellyfinMetadata.ALBUM_ARTIST, jellyfinArtist.getName(),
						JellyfinMetadata.ARTIST, jellyfinArtist.getName(),
						JellyfinMetadata.TITLE, "Track title");
				// @formatter:on

				ScannedFile scannedFile = new ScannedFile("any/path/in/music/library/10 - Track title.mp3", 10, 1, 2026,
						metadata);
				when(artistHelper.toArtists(jellyfinArtist.getName())).thenReturn(List.of(jellyfinArtist));
				boolean match = underTest.fallbackMatch(item, scannedFile);
				assertTrue(match, "Album mismatch: " + entry.getKey() + " vs " + entry.getValue());
			}
		}

		@Test
		void when_AlbumIsDifferent_then_MatchIsFalse() {
			// @formatter:off
			Map<String, String> albums = Map.of(
					"Heaven and Hell", "Between Heaven and Hell",
					"Live at Last", "Live Evil",
					"Live At The Regal", "Live In Cook County Jail",
					"It's Alive", "It's Alive Tribute",
					"The End", "Endgame"
			);
			// @formatter:on
			for (Map.Entry<String, String> entry : albums.entrySet()) {
				Item item = new Item("Jellyfin track name", "serverId", "id", "audio", false, 10, null, entry.getKey(),
						jellyfinArtist.getName(), List.of(jellyfinArtist), List.of(jellyfinArtist), 2026,
						"any/path/in/jellyfin/server/10 - Jellyfin track name.mp3");

				// @formatter:off
				Map<JellyfinMetadata, String> metadata = Map.of(
						JellyfinMetadata.ALBUM, entry.getValue(),
						JellyfinMetadata.ALBUM_ARTIST, jellyfinArtist.getName(),
						JellyfinMetadata.ARTIST, jellyfinArtist.getName(),
						JellyfinMetadata.TITLE, "Track title");
				// @formatter:on

				ScannedFile scannedFile = new ScannedFile("any/path/in/music/library/10 - Track title.mp3", 10, 1, 2026,
						metadata);
				boolean match = underTest.fallbackMatch(item, scannedFile);
				assertFalse(match, "Album mismatch: " + entry.getKey() + " vs " + entry.getValue());
			}
		}

	}

	@Nested
	class SameAlbum {

		@Test
		void when_NoTrackNumber_and_AnySimilarArtist_then_MatchIsTrue() {
			String twinArtists = "Artist James\0Reverend Brown";
			// @formatter:off
			Map<String, String> artistNames = Map.of( // Jellyfin <-> Scanned
					"Big Bad Voodo Daddy", "Big Bad Voodoo Daddy",
					"Artist Name", twinArtists,
					"Bald Religion", "Bad Religion"
			);
			// @formatter:on

			Item item;
			Map<JellyfinMetadata, String> metadata;
			ScannedFile scannedFile;
			boolean match;
			for (Map.Entry<String, String> entry : artistNames.entrySet()) {
				Artist albumArtist = Artist.builder().name(entry.getKey()).build();
				Artist dummyArtist = Artist.builder().name("Dummy Track Artist").build();
				item = new Item("Jellyfin track name", "serverId", "id", "audio", false, null, null, jellyfinAlbum,
						entry.getKey(), List.of(albumArtist), List.of(albumArtist, dummyArtist), 2026,
						"any/path/in/jellyfin/Jellyfin track name.mp3");

				// @formatter:off
				metadata = Map.of(JellyfinMetadata.ALBUM, jellyfinAlbum,
						JellyfinMetadata.ALBUM_ARTIST, entry.getValue(),
						JellyfinMetadata.ARTIST, entry.getValue(),
						JellyfinMetadata.TITLE, "Track title");
				// @formatter:on

				scannedFile = new ScannedFile("any/path/in user library/25 - Track title.mp3", 25, 1, 2026, metadata);

				when(artistHelper.toArtists(entry.getValue())).thenAnswer(onMock -> {
					if (onMock.getArgument(0, String.class).equals(twinArtists))
						return List.of(Artist.builder().name("Artist James").build(),
								Artist.builder().name("Reverend Brown").build());
					else
						return List.of(Artist.builder().name(entry.getValue()).build());
				});
				match = underTest.fallbackMatch(item, scannedFile);

				assertTrue(match, () -> "Album Artist mismatch: " + entry.getKey() + " vs " + entry.getValue());
			}
		}

		@Test
		void when_NoTrackNumber_and_DifferentArtist_then_MatchIsFalse() {
			String twinArtists = "Carla Thomas\0Rufus Thomas";

			// @formatter:off
			Map<String, String> artistNames = Map.of( // Jellyfin <-> Scanned
					twinArtists, "Booker T. & the MG's",
					"Óscar Avendaño", "Óscar Avendaño y los Profesionales",
					"The Doors", "The Threes"
			);
			// @formatter:on

			Item item;
			Map<JellyfinMetadata, String> metadata;
			ScannedFile scannedFile;
			boolean match;
			for (Map.Entry<String, String> entry : artistNames.entrySet()) {
				Artist albumArtist = Artist.builder().name(entry.getKey()).build();
				Artist dummyArtist = Artist.builder().name("Dummy Track Artist").build();
				item = new Item("Jellyfin track name", "serverId", "id", "audio", false, null, null, jellyfinAlbum,
						entry.getKey(), List.of(albumArtist), List.of(albumArtist, dummyArtist), 2026,
						"any/path/in/jellyfin/Jellyfin track name.mp3");

				// @formatter:off
				metadata = Map.of(JellyfinMetadata.ALBUM, jellyfinAlbum,
						JellyfinMetadata.ALBUM_ARTIST, entry.getValue(),
						JellyfinMetadata.ARTIST, entry.getValue(),
						JellyfinMetadata.TITLE, "Track title");
				// @formatter:on

				scannedFile = new ScannedFile("any/path/in user library/25 - Track title.mp3", 25, 1, 2026, metadata);

				when(artistHelper.toArtists(entry.getValue())).thenAnswer(onMock -> {
					if (onMock.getArgument(0, String.class).equals(twinArtists))
						return List.of(Artist.builder().name("Carla Thomas").build(),
								Artist.builder().name("Rufus Thomas").build());
					else
						return List.of(Artist.builder().name(entry.getValue()).build());
				});
				match = underTest.fallbackMatch(item, scannedFile);

				assertFalse(match, () -> "Album Artist mismatch: " + entry.getKey() + " vs " + entry.getValue());
			}
		}

		@Test
		void when_SameTrackNumber_and_SimilarArtist_then_MatchIsTrue() {
			// @formatter:off
			Map<String, String> artistNames = Map.of( // Jellyfin <-> Scanned
					"Jimi Hendrix Experience", "The Jimi Hendrix Experience",
					"Rolling Stones", "The Rolling Stones",
					"Doors", "The Doors"
			);
			// @formatter:on

			Item item;
			Map<JellyfinMetadata, String> metadata;
			ScannedFile scannedFile;
			boolean match;
			for (Map.Entry<String, String> entry : artistNames.entrySet()) {
				Artist albumArtist = Artist.builder().name(entry.getKey()).build();
				Artist dummyArtist = Artist.builder().name("Dummy Track Artist").build();
				item = new Item("Jellyfin track name", "serverId", "id", "audio", false, 25, null, jellyfinAlbum,
						entry.getKey(), List.of(albumArtist), List.of(albumArtist, dummyArtist), 2026,
						"any/path/in/jellyfin/25 - Jellyfin track name.mp3");

				// @formatter:off
				metadata = Map.of(JellyfinMetadata.ALBUM, jellyfinAlbum,
						JellyfinMetadata.ALBUM_ARTIST, entry.getValue(),
						JellyfinMetadata.ARTIST, entry.getValue(),
						JellyfinMetadata.TITLE, "Track title");
				// @formatter:on

				scannedFile = new ScannedFile("any/path/in user library/25 - Track title.mp3", 25, 1, 2026, metadata);

				when(artistHelper.toArtists(entry.getValue())).thenReturn(List.of(Artist.builder().name(entry.getValue()).build()));
				match = underTest.fallbackMatch(item, scannedFile);

				assertTrue(match, () -> "Album Artist mismatch: " + entry.getKey() + " vs " + entry.getValue());
			}
		}

		@Test
		void when_SameTrackNumber_and_DifferentArtist_then_MatchIsFalse() {
			jellyfinArtist = Artist.builder().name("Óscar Avendaño").build();
			String artistName = "Óscar Avendaño y Reposado";

			Item item = new Item("Jellyfin track name", "serverId", "id", "audio", false, 10, null, jellyfinAlbum,
					jellyfinArtist.getName(), List.of(jellyfinArtist), List.of(jellyfinArtist), 2026, "any/path");

			Map<JellyfinMetadata, String> metadata = Map.of(JellyfinMetadata.ALBUM, jellyfinAlbum,
					JellyfinMetadata.ALBUM_ARTIST, artistName, JellyfinMetadata.ARTIST, artistName,
					JellyfinMetadata.TITLE, "Track title");
			ScannedFile scannedFile = new ScannedFile("any/path", 10, 1, 2026, metadata);

			when(artistHelper.toArtists(artistName)).thenReturn(List.of(Artist.builder().name(artistName).build()));
			boolean match = underTest.fallbackMatch(item, scannedFile);
			assertFalse(match, "Artist " + jellyfinArtist.getName()  + " vs " + artistName);
		}

	}

}