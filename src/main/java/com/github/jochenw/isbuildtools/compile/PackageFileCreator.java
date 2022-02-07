package com.github.jochenw.isbuildtools.compile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.props.IPropertyFactory;
import com.github.jochenw.afw.core.util.Streams;

public class PackageFileCreator {
	private final ILog log;
	private final IPropertyFactory propertyFactory;

	public @Inject PackageFileCreator(IComponentFactory pFactory) {
		log = pFactory.requireInstance(ILogFactory.class).getLog(PackageFileCreator.class);
		propertyFactory = pFactory.requireInstance(IPropertyFactory.class);
	}

	public void createArchive(Path pPackageDir, Path pDistDir) {
		final String packageName = pPackageDir.getFileName().toString();
		final String projectVersion = propertyFactory.getPropertyValue("project.version");
		final String archiveNameProperty = propertyFactory.getPropertyValue("project.archive.name");
		final String archiveName;
		if (archiveNameProperty == null  ||  archiveNameProperty.length() == 0) {
			if (projectVersion == null  ||  projectVersion.length() == 0) {
				archiveName = packageName + ".zip";
				log.trace("createArchive", "Properties project.version, and project.archive.name, are empty, so archiveName is simple", archiveName);
			} else {
				archiveName = packageName + "-" + projectVersion + ".zip";
				log.trace("createArchive", "Property project.version is present, but project.archive.name is not, so archiveName is versioned", archiveName);
			}
		} else {
			archiveName = archiveNameProperty;
			log.trace("createArchive", "Property project.archive.name is present, so archiveName is fixed", archiveName);
		}
		createArchive(pPackageDir, pDistDir, archiveName);
	}

	protected void createArchive(Path pPackageDir, Path pDistDir, final String archiveName) {
		final Path archivePath = pDistDir.resolve(archiveName);
		log.info("createArchive", archivePath.toString());
		final Path dir = archivePath.getParent();
		if (dir != null) {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		final List<String> files = new ArrayList<String>();
		try {
			final FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
					final Path relativePath = pPackageDir.relativize(pFile);
					final String relativePathStr = relativePath.toString().replace('\\', '/');
					if (!relativePathStr.endsWith(".bak")) {
						files.add(relativePathStr);
					}
					return FileVisitResult.CONTINUE;
				}
			};
			Files.walkFileTree(pPackageDir, fv);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		files.sort((s1,s2) -> s1.compareToIgnoreCase(s2));
		try (OutputStream os = Files.newOutputStream(archivePath);
			 BufferedOutputStream bos = new BufferedOutputStream(os);
			 ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
			for (String s : files) {
				final Path p = pPackageDir.resolve(s);
				log.trace("createArchive", "Adding file", p);
				final ZipEntry ze = new ZipEntry(s);
				ze.setLastModifiedTime(Files.getLastModifiedTime(p));
				ze.setMethod(ZipEntry.DEFLATED);
				zos.putNextEntry(ze);
				try (InputStream in = Files.newInputStream(p)) {
					Streams.copy(in, zos);
				}
				zos.closeEntry();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	
}
