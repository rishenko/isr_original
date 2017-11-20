# In Saturn's Rings Image Processing Application
Original software written for the film In Saturn's Rings. Downloader acquires the files through web scraping
PDS Imaging sites, and the Controller converts the images from NASA Vicar/PDS/FITs formats to 
32-bit floating point TIFF.

This is a proof of concept application from late 2012 that quickly turned into production use for the film. It
is currently undergoing a massive overhaul to transition from a proof of concept to a true production
application with sane means of maintenance, readability, and testability.

## Software Stack
* JDK 8+
* Spring Boot 2
* Spring 5
* H2 database
* NASA's VICAR image library
* mjeanroy's exiftool library

## Environment Setup

* Clone repo
* Run `mvn clean install -DskipTests` from the top level directory of the project to install dependencies.

In order for the tests to pass, you will need to download Cassini's calibration data - the
[coiss_0011_v3](https://pds-imaging.jpl.nasa.gov/data/cassini/cassini_orbiter/coiss_0011_v3.tar.gz) archive. Then
 perform the following steps:
* Expand the archive anywhere on your file system.
* Create a symlink to the expanded folder - `converter/src/test/resources/calib`
* Run the tests in Converter to verify they all pass.

# Running the Converter
* From the command line and within the converter module, run `mvn clean package -DskipTests`.
* Copy the `converter-<VERSION>.jar` and `RunConverter.properties` files to the same path as converter-<VERSION>.jar
* Modify `RunConverter.properties` to reflect your environment. This includes setting:
  * `image.directory.read` - path containing image files to be processed
  * `image.directory.write` - path to folder for finished, processed image files
  * `cassini.calibration.dir` - path to the directory containing the cassini calibration files (specifically you need
).
* Run `java -jar converter-<VERSION>.jar` from the command line.