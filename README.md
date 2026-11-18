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

The goldens started as a record of the old behaviour, bugs included. Bugs fixed since then (12-hour timestamps,
zero-based month folders, crashes on missing calibration values, and so on) changed their golden lines in the same
commit, so each change in behaviour is visible in review. Re-record with `-Dgolden.generate=true` and review the
diff line by line before committing it.

Tests must run in UTC; the surefire configuration sets `-Duser.timezone=UTC`. The converter itself writes all
times in UTC whatever the machine's time zone.

# Running the Converter
* Build: `mvn -pl converter -am package -DskipTests`. The runnable jar is `converter/target/converter-<VERSION>.jar`.
* Copy the `converter-<VERSION>.jar` and `RunConverter.properties` files to the same path as converter-<VERSION>.jar
* Modify `RunConverter.properties` to reflect your environment. This includes setting:
  * `image.directory.read` - path containing image files to be processed
  * `image.directory.write` - path to folder for finished, processed image files
  * `cassini.calibration.dir` - path to the directory containing the Cassini calibration files
    (the folder that contains `calib/`, e.g. the one `scripts/fetch-calibration.sh` fills)
* Run `java -jar converter-<VERSION>.jar` from the command line.
* Optional properties:
  * `image.name.sequence` - name output files as a numbered sequence, `TARGET_FILTER_<name>_0000000001`,
    numbered 1, 2, 3... in order of image time within each target and filter
  * `funpack.path` - path to cfitsio's `funpack`, used to unpack `.fz` and tile-compressed `.fits` files
    (default `funpack` on the PATH; if it is missing, `.fits` files are read as they are)
  * `preprocessed.file.dir` - keep the unpacked FITS files here instead of a temporary folder
  * `job.process` - resume an interrupted job by its id (logged at the start of each run). Files already
    converted are skipped and a half-written output is replaced.

Output files go to `<write dir>/YYYY/MM/DD/` by image time (UTC), or `<write dir>/unknown/` under the input file's
name when the mission isn't recognised.

Jobs are recorded in an H2 database at `~/.isr/converter` (set `isr.db.dir` in the properties file, or `-Disr.db.dir=<folder>`, to move it), which is what
makes resuming possible. It can be deleted between jobs.
