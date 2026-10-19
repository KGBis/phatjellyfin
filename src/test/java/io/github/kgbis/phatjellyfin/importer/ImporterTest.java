package io.github.kgbis.phatjellyfin.importer;

import io.github.kgbis.phatjellyfin.config.ConfigManager;
import io.github.kgbis.phatjellyfin.output.Console;
import io.github.kgbis.phatjellyfin.output.MessageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ImporterTest {

	@Mock
	private ConfigManager configManager;

	@Mock
	private MessageManager messageManager;

	@Mock
	private Console console;

	@Mock
	private MusicScanner musicScanner;

	@Mock
	private JellyfinMatcher jellyfinMatcher;

	@Mock
	private JellyfinWriter jellyfinWriter;

	@InjectMocks
	private Importer underTest;

	@TempDir
	private Path tempDir;

	@Test
	void when_PathNotFound_then_operationResultIsFailure() {
		List<Path> paths = List.of(tempDir.resolve("abracadabra"));
		OperationResult<Void> operationResult = underTest.rootFoldersExistAndReadable(paths);
		assertFalse(operationResult.isSuccess());
		assertTrue(operationResult.getErrorCause().startsWith("Path does not exist"));
	}

	@Test
	void when_PathIsNotDirectory_then_operationResultIsFailure() throws IOException {
		Path path = tempDir.resolve("abracadabra");
		Files.createFile(path);
		List<Path> paths = List.of(path);
		OperationResult<Void> operationResult = underTest.rootFoldersExistAndReadable(paths);
		assertFalse(operationResult.isSuccess());
		assertTrue(operationResult.getErrorCause().startsWith("Path is not a directory"));
	}

	// Don't be a dork: don't run this test as root.
	@Test
	@DisabledOnOs(OS.WINDOWS)
	void when_PathIsNotReadable_then_operationResultIsFailure() throws IOException {
		Path path = tempDir.resolve("abracadabra");
		Files.createDirectory(path);
		Files.setPosixFilePermissions(path, Set.of());

		List<Path> paths = List.of(path);
		OperationResult<Void> operationResult = underTest.rootFoldersExistAndReadable(paths);
		assertFalse(operationResult.isSuccess());
		assertTrue(operationResult.getErrorCause().startsWith("Path is not readable"));
	}

	@Test
	void when_PathIsOk_then_operationResultIsSuccess() throws IOException {
		Path path = tempDir.resolve("abracadabra");
		Files.createDirectory(path);
		List<Path> paths = List.of(path);
		OperationResult<Void> operationResult = underTest.rootFoldersExistAndReadable(paths);
		assertTrue(operationResult.isSuccess());
		assertNull(operationResult.getErrorCause());
	}

}