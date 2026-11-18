package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.metadata.handler.GenericMetadataHandler;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;

/**
 * SimpleDateFormat is not thread-safe. Metadata is read on several threads, each with its own
 * handlers, so a handler's formatter must not be shared between instances. A static one in
 * GenericMetadataHandler corrupted parses ("For input string: .2322E32322E3") on CI.
 */
public class FormatterSharingTest {

	@Test
	public void genericHandlersDoNotShareAFormatter() {
		assertNotSame(new GenericMetadataHandler().getFormatter(), new GenericMetadataHandler().getFormatter());
	}
}
