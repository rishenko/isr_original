# In Saturn's Rings Image Processing Application

> **No longer maintained.** This repository is kept public for reference, and it will get no further updates.
> To convert NASA planetary images to 32-bit floating point TIFF today, use [GDAL](https://gdal.org) instead (see below).

Original software written for the film In Saturn's Rings. Downloader acquires the files through web scraping
PDS Imaging sites, and the Controller converts the images from NASA Vicar/PDS/FITs formats to 
32-bit floating point TIFF.

This is a proof of concept application from late 2012 that quickly turned into production use for the film.

## Use GDAL instead

The PDS archives now publish calibrated images, and GDAL reads their formats directly (VICAR, PDS3, PDS4 and FITS)
and writes 32-bit float TIFF. Tested with GDAL 3.13 on this project's sample files, these commands match the
pixel values (minimum, maximum and sum) that this project's converter produced:

```sh
# Cassini ISS calibrated image (VICAR, already 32-bit float I/F)
gdal_translate -ot Float32 N1701606691_1_CALIB.IMG N1701606691_1_CALIB.tif

# Voyager ISS calibrated or GEOMED image (PDS3, 16-bit integers).
# Multiply by REFLECTANCE_SCALING_FACTOR from the .LBL file (3.345E-04 here) to get I/F.
gdal_translate -ot Float32 -scale 0 1 0 0.0003345 C3250015_GEOMED.LBL C3250015_GEOMED.tif

# New Horizons LORRI calibrated image (open the PDS3 label, not the .fit file)
gdal_translate -ot Float32 lor_0034853589_0x630_sci.lbl lor_0034853589_0x630_sci.tif
```

Where to get calibrated data:
- Cassini and Voyager: [OPUS](https://opus.pds-rings.seti.org) at the PDS Ring-Moon Systems Node
- New Horizons: the [PDS Small Bodies Node](https://pds-smallbodies.astro.umd.edu), also mirrored at the
  [Ring-Moon Systems Node](https://pds-rings.seti.org/newhorizons/)

Differences from this application: GDAL keeps PDS label fields as TIFF metadata rather than EXIF, and it doesn't
rename files or sort them into dated folders.

The documentation below describes the original 2018 code.

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