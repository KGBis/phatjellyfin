package io.github.kgbis.phatjellyfin.client;

import io.github.kgbis.phatjellyfin.config.Config;
import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.config.JellyfinServerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JellyfinClientTest {

	@Mock
	private ConfigManager configManager;

	@InjectMocks
	private JellyfinClient underTest;

	@Test
	void when_UrlIsMissing_then_throwsIllegalArgumentException() throws IOException {
		Config config = Config.builder()
			.rootFolders(Set.of(Path.of("anypath")))
			.recursive(true)
			.apply(true)
			.serverData(new JellyfinServerData("", "anyAPIkey"))
			.defaults(false)
			.build();
		when(configManager.current()).thenReturn(config);
		assertThrows(IllegalArgumentException.class, () -> underTest.getServerBasicInfo());
	}

	@Test
	void when_APIKeyIsMissing_then_throwsIllegalArgumentException() throws IOException {
		Config config = Config.builder()
			.rootFolders(Set.of(Path.of("anypath")))
			.recursive(true)
			.apply(true)
			.serverData(new JellyfinServerData("http://localhost:8096", null))
			.defaults(false)
			.build();
		when(configManager.current()).thenReturn(config);
		assertThrows(IllegalArgumentException.class, () -> underTest.getServerBasicInfo());
	}

}