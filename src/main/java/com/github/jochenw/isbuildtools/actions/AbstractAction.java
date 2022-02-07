package com.github.jochenw.isbuildtools.actions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Predicates;

public abstract class AbstractAction extends Action {
	private final ILog log;

	protected @Inject AbstractAction(IComponentFactory pComponentFactory) {
		super(pComponentFactory);
		log = pComponentFactory.requireInstance(ILogFactory.class).getLog(AbstractAction.class);
	}

	protected List<String> findPackageDirectories(Predicate<String> pFilter,
			                                      BiConsumer<String,Path> pSkippedPackagesListener) {
		final Predicate<String> filter = Objects.notNull(pFilter, Predicates.alwaysTrue());
		final Path projectDir = getProjectDir();
		final List<String> packageDirs = new ArrayList<>();
		try {
			Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path pDir, BasicFileAttributes pAttrs) throws IOException {
					final Path manifestFile = pDir.resolve("manifest.v3");
					if (Files.isRegularFile(manifestFile)) {
						final Path relativePath = projectDir.relativize(pDir);
						final String relativePathStr = relativePath.toString().replace('\\', '/');
						if (filter.test(relativePathStr)) {
							packageDirs.add(relativePathStr);
						} else {
							if (pSkippedPackagesListener != null) {
								pSkippedPackagesListener.accept(pDir.getFileName().toString(), pDir);
							}
							log.debug("findPackageDirectories", "Ignoring package, as instructed by filter: {}", relativePathStr);
						}
						return FileVisitResult.SKIP_SUBTREE;
					} else {
						return FileVisitResult.CONTINUE;
					}
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return packageDirs;
	}

	protected ILog getLogger() {
		return getComponentFactory().requireInstance(ILogFactory.class).getLog(getClass());
	}
}
