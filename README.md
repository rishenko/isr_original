# In Saturn's Rings Image Processing Application
Original software written for the film In Saturn's Rings. Downloader acquires the files through web scraping
PDS Imaging sites, and the Controller converts the images from NASA Vicar/PDS/FITs formats to 
32-bit floating point TIFF.

This is a proof of concept application from late 2012 that quickly turned into production use for the film. It
is currently undergoing a massive overhaul to transition from a proof of concept to a true production
application with sane means of maintenance, readability, and testability.

## Software Stack
* JDK 21
* Spring Boot 3.5 (command-line app; no web server)
* H2 database
* NASA's VICAR image library (vendored in `vicar-source`), JAI 1.1.3 from the OSGeo repository
* The JDK's built-in TIFF writer (`javax.imageio.plugins.tiff`), which also writes the EXIF tags

## Environment Setup

* Clone the repo and use JDK 21 (`export JAVA_HOME=$(/usr/libexec/java_home -v 21)` on macOS).
* Fetch the Cassini calibration files the tests use (about 33 MB, not the whole 2.7 GB archive):
  `scripts/fetch-calibration.sh` (defaults to `../isr-calib/coiss_0011_v3`).
* Build and run all tests: `mvn verify -Disr.calib.dir=../isr-calib/coiss_0011_v3`
  * Without `-Disr.calib.dir` (or a `converter/src/test/resources/calib` symlink), calibrated test cases are skipped.

## Tests

The converter tests in `converter/src/test/java/.../golden` are golden-master tests. Their expected values
(`converter/src/test/resources/golden`) were recorded by running the original 2018 code (commit `fafa508`) over
the sample images in `converter/src/test/resources/test-dirs`. They check, per sample image: the raw pixels read,
the metadata values, the output file name and folder, each Cassini calibrator on its own, the converted pixels
(bit for bit), the EXIF tags written, and the whole pipeline's output files and database rows.

The goldens record the old behaviour **including its bugs** (for example 12-hour timestamps in file names and
zero-based month folders). When fixing such a bug, update the affected golden lines in the same commit so the
change in behaviour is visible in review. The unit tests in `.../unit` pin these bugs explicitly as well.

Tests must run in UTC; the surefire configuration sets `-Duser.timezone=UTC`.

# Running the Converter
* Build: `mvn -pl converter -am package -DskipTests`. The runnable jar is `converter/target/converter-<VERSION>.jar`.
* Copy the `converter-<VERSION>.jar` and `RunConverter.properties` files to the same path as converter-<VERSION>.jar
* Modify `RunConverter.properties` to reflect your environment. This includes setting:
  * `image.directory.read` - path containing image files to be processed
  * `image.directory.write` - path to folder for finished, processed image files
  * `cassini.calibration.dir` - path to the directory containing the Cassini calibration files
    (the folder that contains `calib/`, e.g. the one `scripts/fetch-calibration.sh` fills)
* Run `java -jar converter-<VERSION>.jar` from the command line.
