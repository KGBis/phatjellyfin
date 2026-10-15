package io.github.kgbis.phatjellyfin.importer;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UnmatchedAlbumToDiscard implements AlbumMatchingResult {

	private MusicKey musicKey;

	private List<ScannedFile> unmatchedTracks;

}
