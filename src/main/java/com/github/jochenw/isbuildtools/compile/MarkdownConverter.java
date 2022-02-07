package com.github.jochenw.isbuildtools.compile;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.util.MutableInteger;

public class MarkdownConverter {
	private final ILog log;

	public @Inject MarkdownConverter(IComponentFactory pFactory) {
		log = pFactory.requireInstance(ILogFactory.class).getLog(MarkdownConverter.class);
	}

	public boolean hasMarkdownFiles(Path pPackageDir) {
		try {
			findMarkdownFiles(pPackageDir, (s) -> {
				throw new IllegalStateException(s);
			});
		} catch (IllegalStateException e) {
			return true;
		}
		return false;
	}

	public void findMarkdownFiles(Path pPackageDir, Consumer<String> pConsumer) {
		final FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
				if (pFile.getFileName().toString().endsWith(".md")) {
					final Path relativePath = pPackageDir.relativize(pFile);
					final String relativePathStr = relativePath.toString().replace('\\', '/');
					pConsumer.accept(relativePathStr);
				}
				return FileVisitResult.CONTINUE;
			}
		};
		final Path pubDir = pPackageDir.resolve("pub");
		if (Files.isDirectory(pubDir)) {
			try {
				Files.walkFileTree(pubDir, fv);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public List<String> getMarkdownFiles(Path pPackageDir) {
		final List<String> list = new ArrayList<>();
		findMarkdownFiles(pPackageDir, list::add);
		return list;
	}

	public void convertToHTML(Path pPackageDir) {
		log.entering("convertHTML", pPackageDir.toString());
		final MutableInteger counter = new MutableInteger();
		findMarkdownFiles(pPackageDir, (s) -> {
			counter.inc();
			final Path markdownFile = pPackageDir.resolve(s);
			final Path htmlFile = pPackageDir.resolve(s.replace(".md", ".html"));
			final Parser parser = Parser.builder().build();
			final Node node;
			log.trace("convertHTML", "Reading Markdown file", markdownFile);
			try (Reader reader = Files.newBufferedReader(markdownFile, StandardCharsets.UTF_8)) {
				node = parser.parseReader(reader);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			final HtmlRenderer renderer = HtmlRenderer.builder().build();
			log.trace("convertHTML", "Writing HTML file", htmlFile);
			try (Writer writer = Files.newBufferedWriter(htmlFile, StandardCharsets.UTF_8)) {
				renderer.render(node, writer);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		log.exiting("convertHTML", counter.intValue());
	}
}
