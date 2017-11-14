# In Saturn's Rings Image Processing Application
Original software written for the film In Saturn's Rings. Downloader acquires the files through web scraping
PDS Imaging sites, and the Controller converts the images from NASA Vicar/PDS/FITs formats to 
32-bit floating point TIFF.

This is a proof of concept application that quickly turned into production usage for the film. It
is currently undergoing a massive overhaul to transition from a proof of concept app to a production
app with sane choices tending toward maintenance, readability, and testability.

## Software Stack
* JDK 8+
* Spring Boot 2
* Spring 5
* H2 database
* exiftool and J-Exiftool wrapper

## Environment Setup
* Clone repo
* Run `mvn clean install -DskipTests` from the top level to install dependencies.

# Running the Converter
* Within the converter module, run `mvn clean package -DskipTests`
* Copy the `converter-<VERSION>.jar` and `RunConverter.properties` files to the same path as converter-<VERSION>.jar
* Modify `RunConverter.properties` to reflect your environment. This includes setting:
  * `image.directory.read` - path containing image files to be processed
  * `image.directory.write` - path to folder for finished, processed image files
  * `cassini.calibration.dir` - path to the directory containing the cassini calibration files (specifically you need
coiss_0011v3).
* Run `java -jar converter-<VERSION>.jar` from the command line.

