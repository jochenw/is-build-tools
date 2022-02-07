package com.github.jochenw.isbuildtools.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Properties;

import com.github.jochenw.afw.core.cli.Cli;
import com.github.jochenw.afw.core.components.Application;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog.Level;
import com.github.jochenw.afw.core.log.simple.SimpleLogFactory;
import com.github.jochenw.afw.core.props.DefaultPropertyFactory;
import com.github.jochenw.afw.core.props.IPropertyFactory;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.isbuildtools.actions.Action;

public class Main {
	public static enum ActionId {
		simpleBuild, abeBuild,
	}
	public static class Options {
		private Path wmHomeDir;
		private Path projectDir;
		private Path targetDir;
		private ActionId action;
		private Level logLevel;
		private Path logFile;
		private Path propertyFile;
	}

	protected Module newModule(Options pOptions) {
		return (b) -> {
			Action.MODULE.configure(b);
			b.bind(Path.class, "wm.home.dir").toInstance(pOptions.wmHomeDir);
			b.bind(Path.class, "project.dir").toInstance(pOptions.projectDir);
			b.bind(Path.class, "build.dir").toInstance(pOptions.targetDir);
		};
	}

	protected IPropertyFactory newPropertyFactory(Options pOptions) {
		final Properties properties;
		if (pOptions.propertyFile == null) {
			properties = new Properties();
		} else {
			properties = Streams.load(pOptions.propertyFile);
		}
		properties.put("wm.home.dir", pOptions.wmHomeDir);
		properties.put("project.dir", pOptions.projectDir);
		properties.put("build.dir", pOptions.targetDir);
		return new DefaultPropertyFactory(properties);
	}

	protected IComponentFactory newComponentFactory(Options pOptions) {
		final Application app = new Application(newModule(pOptions),
				                                () -> SimpleLogFactory.of(pOptions.logFile, pOptions.logLevel),
	                                            () -> newPropertyFactory(pOptions));
		return app.getComponentFactory();
	}

	protected Action getAction(Options pOptions) {
		final IComponentFactory cf = newComponentFactory(pOptions);
		return cf.requireInstance(Action.class, pOptions.action.name());
	}

	protected void run(Options pOptions) throws Exception {
		final Action action = getAction(pOptions);
		action.run();
	}

	public static void main(String[] pArgs) throws Exception {
		final Options opts = new Options();
		final Cli<Options> cli = Cli.of(opts)
				.pathOption("wmHomeDir").dirRequired().required()
				     .handler((c,p) -> opts.wmHomeDir = p).end()
				.pathOption("projectDir").dirRequired().required()
				     .handler((c,p) -> opts.projectDir = p).end()
				.enumOption(ActionId.class, "action").required()
				     .handler((c,a) -> opts.action = a).end()
		        .pathOption("logFile").handler((c,p) -> opts.logFile = p).end()
		        .pathOption("buildDir").defaultValue("target").handler((c,p) -> opts.targetDir = p).end()
		        .pathOption("propertyFile").handler((c,p) -> opts.propertyFile = p).end()
		        .pathOption("targetDir").defaultValue("target").handler((c,p) -> opts.targetDir = p).end()
		        .enumOption(Level.class, "logLevel").defaultValue(Level.INFO.name())
		        	.handler((c,l) -> opts.logLevel = l).end()
		        .errorHandler((msg) -> {
		        	final PrintStream ps = System.err;
		        	if (msg != null) {
		        		ps.println(msg);
		        		ps.println();
		        	}
		        	ps.println("Usage: java " + Main.class.getName() + " <OPTIONS>");
		        	ps.println();
		        	ps.println("Required options are:");
		        	ps.println("  -wmHomeDir <D>  Sets te path of the webMethods home directory.");
		        	ps.println("  -projectDir <D> Sets the path of the project directory.");
		        	ps.println("  -action <A> Sets the action to execute, either of");
		        	ps.println("              " + Strings.join("|", ActionId.class));
		        	ps.println();
		        	ps.println("Other options are:");
		        	ps.println("  -targetDir <D> Sets the path of the target directory. Defaults");
		        	ps.println("                 to ${projectDir}/target.");
		        	ps.println("  -logFile <F>   Sets the path of the log file.");
		        	ps.println("                 By default, System.out is used instead of a log file.");
		        	ps.println("  -logLevel <L>  Sets the log level, either of ");
		        	ps.println("                 " + Strings.join("|", Level.class));
		        	ps.println("                 Defaults to INFO.");
		        	ps.println("                 By default, System.out is used instead of a log file.");
		        	ps.println("  -propertyFile <F> Sets the path of a property file.");
		        	ps.println("                 By default, only the builtin properties are used.");
		        	System.exit(1);
		        	return null;
		        });
		cli.parse(pArgs);
		new Main().run(opts);
	}
}
