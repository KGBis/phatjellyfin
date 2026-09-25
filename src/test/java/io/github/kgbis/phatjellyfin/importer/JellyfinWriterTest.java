package io.github.kgbis.phatjellyfin.importer;

import io.github.kgbis.phatjellyfin.client.JellyfinClient;
import io.github.kgbis.phatjellyfin.client.model.Artist;
import io.github.kgbis.phatjellyfin.client.model.UpdateItem;
import io.github.kgbis.phatjellyfin.config.JellyfinMetadata;
import io.github.kgbis.phatjellyfin.output.Console;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JellyfinWriterTest {

	@Mock
	private JellyfinClient jellyfinClient;

	@Mock
	private Console console;

	@InjectMocks
	private JellyfinWriter underTest;

	@Nested
	class WhenWriting {

		// @Test
		void whenWritingAlbumsToUpdate() throws IOException, InterruptedException {
			Map<MusicKey, Pair<String, TrackMetadata>> albumsToUpdate = getAlbumsToUpdate();
			List<TrackMetadata> tracksToUpdate = getTracksToUpdate();

			OperationResult<Void> successResult = new OperationResult<>(true, null, null);
			OperationResult<Void> badRequestResult = new OperationResult<>(false, null, "Error processing request.");

			when(jellyfinClient.getCustomTagSeparatorsFromMusicLibrary()).thenReturn(";|/\\\0");
			doNothing().when(console).println(anyString());
			when(jellyfinClient.updateItem(anyString(), any(UpdateItem.class))).thenReturn(successResult)
				.thenReturn(successResult)
				.thenReturn(badRequestResult);

			// Map<OperationResult<Void>, List<UpdateItem>> result =
			// underTest.write(albumsToUpdate, tracksToUpdate);

			// assertEquals(2, result.get(successResult).size());
			// assertEquals(1, result.get(badRequestResult).size());
		}

		// @formatter:off
		private List<TrackMetadata> getTracksToUpdate() {
			return List.of(
					new TrackMetadata("track01Id", "/artist/album/01 - track one.mp3", 1,
							Map.of(JellyfinMetadata.TITLE, "Title track 1",
									JellyfinMetadata.ALBUM, "Album #1",
									JellyfinMetadata.ALBUM_ARTIST, "Album Artist")
					),
					new TrackMetadata("track02Id", "/artist/album/02 - track two.mp3", 1,
							Map.of(JellyfinMetadata.TITLE, "Title track 2",
									JellyfinMetadata.ALBUM, "Album #1",
									JellyfinMetadata.ALBUM_ARTIST, "Album Artist")
					)
			);
		}
		// @formatter:on

		private Map<MusicKey, Pair<String, TrackMetadata>> getAlbumsToUpdate() {
			String albumId = "12345";
			TrackMetadata albumMetadata = new TrackMetadata("12345", null, null,
					Map.of(JellyfinMetadata.ALBUM, "Album #1", JellyfinMetadata.ALBUM_ARTIST, "Album Artist"));
			MusicKey musicKey = new MusicKey(List.of(Artist.builder().name("Album Artist").build()), "Album #1");

			return Map.of(musicKey, Pair.of(albumId, albumMetadata));
		}

	}

}
