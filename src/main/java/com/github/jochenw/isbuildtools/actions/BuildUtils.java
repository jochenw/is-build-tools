package com.github.jochenw.isbuildtools.actions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.util.Exceptions;

public class BuildUtils {
	private final ILog log;

	public @Inject BuildUtils(IComponentFactory pComponentFactory) {
		log = pComponentFactory.requireInstance(ILogFactory.class).getLog(BuildUtils.class);
	}

	public void copyDirectory(Path pSource, Path pTarget, String... pExcludes) {
		log.trace("copyDirectory", "-> ", pSource, pTarget, pExcludes);
		final Predicate<String> predicate = (s) -> {
			if (pExcludes != null) {
				for (String exc : pExcludes) {
					if (exc.equals(s)) {
						return false;
					}
				}
			}
			return true;
		};
		copyDirectory(pSource, pTarget, predicate);
		log.trace("copyDirectory", " <-");
	}

	public void copyDirectory(Path pSource, Path pTarget, Predicate<String> pExcludeFilter) {
		final FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path pDir, BasicFileAttributes pAttrs) throws IOException {
				final Path relativePath = pSource.relativize(pDir);
				final String relativePathStr = relativePath.toString().replace('\\', '/');
				if (!pExcludeFilter.test(relativePathStr)) {
					log.tracef("copyDirectory", "Skipping subdirectory %s", pDir);
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
				final Path relativePath = pSource.relativize(pFile);
				final Path targetFile = pTarget.resolve(relativePath);
				log.tracef("copyDirectory", "Copying file %s to %s", pFile, targetFile);
				final Path targetDir = targetFile.getParent();
				if (targetDir != null) {
					Files.createDirectories(targetDir);
				}
				Files.copy(pFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		};
		try {
			Files.walkFileTree(pSource, fv);
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}
}
