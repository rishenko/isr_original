package com.astrodoorways.converter.converters;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.db.filesystem.FileInfo;
import com.astrodoorways.converter.db.imagery.Metadata;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(BlockJUnit4ClassRunner.class)
public class VicarImageConverterTest {
    private static final String CASSINI_IMG_NEEDS_CALIB = "src/test/resources/test-dirs/read/data/cassini/needsCalib/N1711575444_1.IMG";
    private static final String CASSINI_IMG_NEEDS_CALIB_WRITE = "src/test/resources/test-dirs/write/data/cassini/needsCalib";
    private static final String CASSINI_IMG_OUTPUT_FILE = "src/test/resources/test-dirs/write/data/cassini/needsCalib/2012/2/27/CASSINI-HUYGENS_ENCELADUS_CL1-CL2_20120327084559975w1024-h1024.TIFF";

    @BeforeClass
    public static void setup() {
        ApplicationProperties.setProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR, "src/test/resources/calib");
    }

    /*@Test
    public void testCalibration() throws IOException, ParseException {
        final File file = new File(CASSINI_IMG_NEEDS_CALIB);
        Metadata metadata = buildMetadata(file);
        VicarImageConverter vicarConverter = new VicarImageConverter(metadata, null, CASSINI_IMG_NEEDS_CALIB_WRITE, "TIFF");
        // BufferedImage outputImage = vicarConverter.readImage(file);
        // vicarConverter.buildMetadataMap(vicarConverter.getIIOMetaData());
        // outputImage = vicarConverter.buildOutputImage(outputImage);
        // assertTrue(vicarConverter.calibrateCassiniImages(outputImage) != null);
    }

    @Test
    public void testOutputPathGeneration() throws IOException, ParseException {
        final File file = new File(CASSINI_IMG_NEEDS_CALIB);
        Metadata metadata = buildMetadata(file);
        VicarImageConverter vicarConverter = new VicarImageConverter(metadata, null, CASSINI_IMG_NEEDS_CALIB_WRITE, "TIFF");
        // BufferedImage outputImage = vicarConverter.readImage(file);
        // vicarConverter.buildMetadataMap(vicarConverter.getIIOMetaData());
        // outputImage = vicarConverter.buildOutputImage(outputImage);
        // String outputFilePath = vicarConverter.generateOutputFilePath(outputImage.getWidth(), outputImage.getHeight());
        assertEquals(CASSINI_IMG_OUTPUT_FILE, outputFilePath);
    }

    @Test
    public void testOutputFileGeneration() throws IOException, ParseException {
        Files.deleteIfExists(Paths.get(CASSINI_IMG_OUTPUT_FILE));

        final File file = new File(CASSINI_IMG_NEEDS_CALIB);
        Metadata metadata = buildMetadata(file);
        VicarImageConverter vicarConverter = new VicarImageConverter(metadata, null, CASSINI_IMG_NEEDS_CALIB_WRITE, "TIFF");
        vicarConverter.convert();
        String outputFile = vicarConverter.getOutputtedFilePaths().get(0);
        assertEquals(outputFile, CASSINI_IMG_OUTPUT_FILE);
        File writtenFile = new File(outputFile);
        assertTrue(writtenFile.exists());
    }*/

    // TODO: Need to add test coverage for sequenced files.

    private static Metadata buildMetadata(File file) throws IOException {
        String fileName = file.getName();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(fileName);
        int positionLastPeriod = fileName.lastIndexOf(".");
        String extension = fileName.substring(positionLastPeriod + 1, fileName.length());
        fileInfo.setExtension(extension);
        fileInfo.setDirectory(file.getParentFile().getCanonicalPath());

        Metadata metadata = new Metadata();
        metadata.setFileInfo(fileInfo);
        metadata.setMission("CASSINI-HUYGENS");

        return metadata;
    }
}
