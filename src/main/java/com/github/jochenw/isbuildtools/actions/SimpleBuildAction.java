package com.github.jochenw.isbuildtools.actions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.isbuildtools.compile.MarkdownConverter;
import com.github.jochenw.isbuildtools.compile.PackageCompiler;
import com.github.jochenw.isbuildtools.compile.PackageFileCreator;
import com.github.jochenw.isbuildtools.compile.PackageCompiler.CompilerStatusException;


public class SimpleBuildAction extends AbstractAction {
	private final ILog log;
	private final PackageCompiler packageCompiler;
	private final MarkdownConverter markdownConverter;
	private final PackageFileCreator packageFileCreator;

	public @Inject SimpleBuildAction(IComponentFactory pComponentFactory) {
		super(pComponentFactory);
		log = getLogger();
		packageCompiler = pComponentFactory.requireInstance(PackageCompiler.class);
		markdownConverter = pComponentFactory.requireInstance(MarkdownConverter.class);
		packageFileCreator = pComponentFactory.requireInstance(PackageFileCreator.class);
	}

	@Override
	public void run() throws Exception {
		log.entering("run");
		final Map<String,Path> packagePathsByName = new HashMap<>();
		final List<String> packageDirs = findPackageDirectories(null, (n,p) -> packagePathsByName.put(n, p));
		
		log.debugf("run", "List of package directories: %s",
				  packageDirs);
		for (String packageDirStr : packageDirs) {
			log.debugf("run", "Building package %s", packageDirStr);
			build(packageDirStr, (n) -> {
				if (packagePathsByName.containsKey(n)) {
					return packagePathsByName.get(n);
				} else {
					for (String pStr : packageDirs) {
						final Path p = Paths.get(pStr);
						if (p.getFileName().toString().equals(n)) {
							return p;
						}
					}
				}
				return null;
			});
		}
		log.exiting("run");
	}

	protected void build(String pPackageDirStr, Function<String,Path> pPackageLocator) {
		final Path packageDir = getProjectDir().resolve(pPackageDirStr);
		log.entering("build", packageDir);
		// Copy the source directory to the target directory.
		final String packageName = packageDir.getFileName().toString();
		final Path targetOutputDir = getTargetDir().resolve("is-build-tools");
		final Path targetPackagesDir = targetOutputDir.resolve("packages");
		final Path packageBuildDir = targetPackagesDir.resolve(packageName);
		getBuildUtils().copyDirectory(packageDir, packageBuildDir, "code/classes");
		// Compile the Java sources into the classes directory.
		try {
			packageCompiler.compile(getWmHomeDir(), Paths.get(".").toAbsolutePath(), packageBuildDir, pPackageLocator);
		} catch (CompilerStatusException e) {
			log.error("build", e);
			throw Exceptions.show(e);
		}
		// Convert Markdown files to HTML
		markdownConverter.convertToHTML(packageBuildDir);
		// Build the Zip file
		final Path targetDistDir = targetOutputDir.resolve("dist");
		packageFileCreator.createArchive(packageBuildDir, targetDistDir);
		log.exiting("build");
	}
}
