package com.github.jochenw.isbuildtools.compile;

import java.util.function.Consumer;

import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;


public abstract class AbstractContentHandler implements ContentHandler {
	public static class TerminationRequest extends RuntimeException {
		private static final long serialVersionUID = -246073957943419109L;

		public TerminationRequest() {}
	}
	
	private int level;
	private Locator locator;
	private final StringBuilder sb = new StringBuilder();
	private boolean collectingText;
	private int collectLevel;
	private Consumer<String> collector;

	protected void terminate() {
		throw new TerminationRequest();
	}
	protected int getLevel() { return level; }
	protected void startCollecting(int pLevel, Consumer<String> pCollector) {
		if (collectingText) {
			throw new IllegalStateException("Already collecting");
		}
		collectingText = true;
		sb.setLength(0);
		collectLevel = pLevel;
		collector = pCollector;
	}
	protected int incLevel() {
		return level++;
	}
	protected int decLevel() {
		return --level;
	}
	protected void startCollecting(Consumer<String> pCollector) {
		startCollecting(level, pCollector);
	}
	protected void assertNotCollecting(String pEvent) throws SAXException {
		if (collectingText) {
			throw error("Unexpected event, while collecting text: " + pEvent + ", level=" + level + ", waiting for level=" + collectLevel);
		}
	}
	
	@Override
	public void setDocumentLocator(Locator pLocator) {
		locator = pLocator;
	}

	protected Locator getDocumentLocator() { return locator; }
	protected Locator getLocator() {
		if (locator == null) {
			return null;
		} else {
			return new LocatorImpl(locator);
		}
	}
	protected SAXParseException error(String pMessage) {
		final LocatorImpl li = new LocatorImpl(getDocumentLocator());
		return new SAXParseException(pMessage, li);
	}

	@Override
	public void startDocument() throws SAXException {
		level = 0;
	}

	@Override
	public void endDocument() throws SAXException {
		if (level != 0) {
			throw error("Expected level=0, got " + level);
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// Ignore this
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// Ignore this
	}

	@Override
	public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
		assertNotCollecting("START_ELEMENT");
		++level;
	}

	protected void stopCollecting(boolean pInvokeCollector) {
		if (pInvokeCollector) {
			collector.accept(sb.toString());
		}
		collectingText = false;
	}
	
	@Override
	public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
		--level;
		if (collectingText) {
			if (level == collectLevel) {
				stopCollecting(true);
			} else {
				assertNotCollecting("END_ELEMENT");
			}
		}
	}

	@Override
	public void characters(char[] pChars, int pStart, int pLength) throws SAXException {
		if (collectingText) {
			sb.append(pChars, pStart, pLength);
		}
	}

	@Override
	public void ignorableWhitespace(char[] pChars, int pStart, int pLength) throws SAXException {
		if (collectingText) {
			sb.append(pChars, pStart, pLength);
		}
	}

	@Override
	public void processingInstruction(String pTarget, String pData) throws SAXException {
		throw error("Unexpected PI: " + pTarget + ", " + pData);
	}

	@Override
	public void skippedEntity(String pName) throws SAXException {
		throw error("Unexpected skipped entity: " + pName);
	}

	protected boolean isElement(String pExpectedName, String pUri, String pLocalName) {
		if (pUri != null &&  pUri.length() > 0) {
			return false;
		}
		return pExpectedName.equals(pLocalName);
	}

	protected boolean isElement(String pExpectedUri, String pExpectedName, String pUri, String pLocalName, Attributes pAttrs, String... pAttributes) {
		if (!isElement(pExpectedUri, pExpectedName, pUri, pLocalName)) {
			return false;
		}
		for (int i = 0;  i < pAttributes.length;  i += 2) {
			final String attrName = pAttributes[i];
			final String attrValue = pAttributes[i+1];
			final String value = pAttrs.getValue(XMLConstants.NULL_NS_URI, attrName);
			if (!attrValue.equals(value)) {
				return false;
			}
		}
		return true;
	}

	protected boolean isElement(String pExpectedName, String pUri, String pLocalName, Attributes pAttrs, String... pAttributes) {
		if (!isElement(pExpectedName, pUri, pLocalName)) {
			return false;
		}
		for (int i = 0;  i < pAttributes.length;  i += 2) {
			final String attrName = pAttributes[i];
			final String attrValue = pAttributes[i+1];
			final String value = pAttrs.getValue(XMLConstants.NULL_NS_URI, attrName);
			if (!attrValue.equals(value)) {
				return false;
			}
		}
		return true;
	}

	protected boolean isElement(String pExpectedUri, String pExpectedName, String pUri, String pLocalName) {
		if (pExpectedUri == null  ||  pExpectedUri.length() == 0) {
			return isElement(pExpectedName, pUri, pLocalName);
		} else {
			return pExpectedUri.equals(pUri)  &&  pExpectedName.equals(pLocalName);
		}
	}

	protected void assertElement(String pExpectedName, String pUri, String pLocalName) throws SAXException {
		if (!isElement(pExpectedName, pUri, pLocalName)) {
			throw error("Expected element " + pExpectedName + ", got " + asQName(pUri, pLocalName));
		}
	}

	protected void assertElement(String pExpectedUri, String pExpectedName, String pUri, String pLocalName) throws SAXException {
		if (!isElement(pExpectedUri, pExpectedName, pUri, pLocalName)) {
			throw error("Expected element " + asQName(pExpectedUri, pExpectedName) + ", got " + asQName(pUri, pLocalName));
		}
	}
	
	protected String asQName(String pUri, String pLocalName) {
		if (pUri == null  ||  pUri.length() == 0) {
			return "{}" + pLocalName;
		} else {
			return "{" + pUri + "}" + pLocalName;
		}
	}

	public void finished() {
		// Nothing to do
	}
}
