package com.github.jochenw.isbuildtools.actions;

import java.nio.file.Path;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.Scopes;
import com.github.jochenw.isbuildtools.compile.MarkdownConverter;
import com.github.jochenw.isbuildtools.compile.PackageCompiler;
import com.github.jochenw.isbuildtools.compile.PackageFileCreator;

public abstract class Action {
	private final IComponentFactory componentFactory;
	private final BuildUtils buildUtils;
	private final Path wmHomeDir;
	private final Path projectDir;
	private final Path targetDir;

	protected Action(IComponentFactory pComponentFactory) {
		componentFactory = pComponentFactory;
		wmHomeDir = componentFactory.requireInstance(Path.class, "wm.home.dir");
		projectDir = componentFactory.requireInstance(Path.class, "project.dir");
		targetDir = componentFactory.requireInstance(Path.class, "build.dir");
		buildUtils = componentFactory.requireInstance(BuildUtils.class);
	}

	public IComponentFactory getComponentFactory() { return componentFactory; }
	public Path getWmHomeDir() { return wmHomeDir; }
	public Path getProjectDir() { return projectDir; }
	public Path getTargetDir() { return targetDir; }
	public BuildUtils getBuildUtils() { return buildUtils; }

	public static Module MODULE = (b) -> {
		b.bind(Action.class, com.github.jochenw.isbuildtools.cli.Main.ActionId.simpleBuild.name())
		 .toClass(SimpleBuildAction.class).in(Scopes.SINGLETON);
		b.bind(Action.class, com.github.jochenw.isbuildtools.cli.Main.ActionId.abeBuild.name())
		 .toClass(AbeBuildAction.class).in(Scopes.SINGLETON);
		b.bind(BuildUtils.class).in(Scopes.SINGLETON);
		b.bind(PackageCompiler.class).in(Scopes.SINGLETON);
		b.bind(MarkdownConverter.class).in(Scopes.SINGLETON);
		b.bind(PackageFileCreator.class).in(Scopes.SINGLETON);
	};

	public abstract void run() throws Exception;
}
