package com.astrodoorways.converter.mission;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import javax.imageio.metadata.IIOMetadata;

/**
 * Everything specific to one mission (or instrument): recognising its files and reading their
 * labels (the handler), and any processing of the pixels, such as calibration.
 *
 * To add a mission, add a package under {@code mission} with a plugin and a handler, and list the
 * plugin in {@link Missions}.
 */
public interface MissionPlugin {

    /** Short name for logs. */
    String name();

    /**
     * A new label handler. Handlers keep state for the image being read, so every image needs its
     * own; the handler's canProcessMetadata decides whether this plugin applies to a file.
     */
    MetadataHandler newHandler();

    /**
     * Mission-specific processing of the pixels after they are read and widened to float, before
     * the image is written. The default does nothing.
     *
     * @param image       the image as read, with a float sample model
     * @param iioMetadata the image's metadata as read
     * @param labelValues the values the handler extracted (mission, target, time, ...)
     */
    default BufferedImage process(BufferedImage image, IIOMetadata iioMetadata, Map<String, String> labelValues)
            throws IOException {
        return image;
    }
}
