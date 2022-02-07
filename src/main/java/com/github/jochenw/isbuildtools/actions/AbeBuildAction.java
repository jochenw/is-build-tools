package com.github.jochenw.isbuildtools.actions;

import java.util.List;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;

public class AbeBuildAction extends AbstractAction {
	private final ILog log;

	public @Inject AbeBuildAction(IComponentFactory pComponentFactory) {
		super(pComponentFactory);
		log = getLogger();
	}

	@Override
	public void run() throws Exception {
		log.entering("run");
		final List<String> packageDirs = findPackageDirectories(null, null);
		log.debug("run", "List of package directories: {}",
				  packageDirs);
		log.exiting("run");
	}
}
