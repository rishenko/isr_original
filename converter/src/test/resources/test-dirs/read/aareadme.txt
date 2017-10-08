PDS_VERSION_ID         = PDS3
RECORD_TYPE            = STREAM
OBJECT                 = TEXT
  PUBLICATION_DATE     = 2007-03-12
  INTERCHANGE_FORMAT   = ASCII
  NOTE                 = "Readme file for Cassini ISS EDR and
                          Calibration Files Archive Volume DVDs"
END_OBJECT             = TEXT
END





CASSINI IMAGING SCIENCE SUBSYSTEM EDR AND CALIBRATION FILES
ARCHIVE VOLUME DVD

  Volume ID:    COISS_2023
  Range (SCLK): 1526038904 - 1530373711
  Range (UTC):  2006-131T11:11:10.746 - 2006-181T15:17:30.106





AAREADME CONTENTS
-----------------
1. Introduction
2. DVD Format
3. Data Calibration
4. DVD Contents
5. File Formats
6. Contact Information





------------------------------------------------------------------------------
1.  Introduction

The Cassini Imaging Science Subsystem (ISS) archive is comprised of a 
two-volume scheme -- DATA and CALIBRATION volumes. 

The 'DATA' volumes are considered to be mostly static.  These volumes contain 
the raw (uncalibrated) experiment data record image files (EDRs), attached 
and detached label files, helpful and required Planetary Data System (PDS) 
files and useful documentation related to the image datasets.  Static refers 
to the fact that, once produced and validated, the contents of these volumes 
are less likely to be updated or modified.  Exceptions are those cases where 
new information is made available or where images are reprocessed, due to 
previous errors, and are made available on later volumes.  No calibration 
files are found on the DATA volumes, except in-flight calibration images 
found as sequenced in spacecraft clock (SCLK) order.

The 'CALIBRATION' volumes are considered to be dynamic.  These volumes 
contain all calibration-related files (except in-flight calibration images). 
Because the state of the calibration effort will improve as our understanding 
of the camera and mission parameters improves, these volumes will be updated 
periodically and released with the latest available calibration-related data 
and information. Included on the calibration volumes are the calibration data 
files, a set of sample calibrated images generated using the ISS team's 
calibration processing software, the calibration image files taken during 
pre-flight ISS ground calibration, calibration algorithms and documentation, 
along with the calibration processing software and related files. The ISS team
is not systematically performing calibration processing on the ISS EDRs. 
However, this second volume includes all the necessary tools and 
documentation to support future users in performing their own calibration 
processing of the ISS EDR image datasets.



Labeling and Identification:

The ISS archive collection is further divided into three datasets:

1) The raw EDR images, in spacecraft clock (SCLK) order, from launch up to the
start of the Saturn approach science phase. In addition to the EDR images, 
also included are the support images, in-flight calibration images and 
images used for navigation purposes.  They are identified with DATA_SET_NAME
and DATA_SET_ID:

        CASSINI ORBITER EARTH/VENUS/JUPITER ISSNA/ISSWA 2 EDR V1.0
        CO-E/V/J-ISSNA/ISSWA-2-EDR-V1.0

2) The raw EDR images (in SCLK order), support images, in-flight cliabraiton 
images and navigation images from the start of Saturn approach science through
the end of mission.  They are identified with DATA_SET_NAME and DATA_SET_ID:

        CASSINI ORBITER SATURN ISSNA/ISSWA 2 EDR VERSION 1.0
        CO-S-ISSNA/ISSWA-2-EDR-V1.0

3) The calibration files, including calibration data, ground calibration image
files, documentation, calibration software and sample calibrated images. They 
are identified with DATA_SET_NAME and DATA_SET_ID:

        CASSINI ORBITER CALIBRATION ISSNA/ISSWA 2 EDR VERSION 1.0
        CO-CAL-ISSNA/ISSWA-2-EDR-V1.0

Additional volume naming conventions and labeling schemes (defined for
each volume in the voldesc.cat file in the root directory) are:
  
  STANDARD_DATA_PRODUCT_ID:
    ISS_E/V/JEDR
    ISS_SEDR  
    ISS_CAL

  VOLUME_SERIES_NAME:
    MISSION TO SATURN

  VOLUME_ID:
    COISS_xxxx   
      (where first x = 1 for Jupiter, 2 for Saturn, 0 for calibration)
      (where next xxx = sequential numbering of volumes starting with 001)
        
  VOLUME_NAME:
    CASSINI ISS EARTH/VENUS/JUPITER EDR SCLK xxxxxxxxxx to SCLK yyyyyyyyyy
    CASSINI ISS SATURN EDR SCLK xxxxxxxxxx to yyyyyyyyyy
    CASSINI ISS CALIBRATION FILES
      (where xxxxxxxxxx = 10-digit start sclk of volume)
      (where yyyyyyyyyy = 10-digit stop sclk of volume)

  VOLUME_SET_ID:
    USA_NASA_PDS_COISS_xxxx 
      (where first x = 1 for Jupiter, 2 for Saturn, 0 for calibration)
      (where next xxx = sequential numbering of volumes)

  VOLUME_SET_NAME:
    CASSINI ISS EXPERIMENT DATA RECORDS AND CALIBRATION FILES

  VOLUME_VERSION_ID:
    VERSION x
      (where x represents the volume version numbered sequentially 
       starting with the original volume as 1)

  VOLUMES:
    XXX 
      (where xxx represents the total number of volumes in the Cassini ISS 
       Experiment Data Records and Calibration Files volume set)



GETTING STARTED

To begin using the ISS archive collection one should become familiar with 
the contents of the root and document directories on both the DATA and 
CALIBRATION volumes.  These directories include files that provide 
important detailed descriptive information on the ISS instrument, the 
archive files and formats and using the ISS team's calibration software, 
along with ISS team science objectives and rationale for image targeting. 

The Space Science Review publication released by the Imaging Team also 
provides important detail not found elsewhere in the archive and is a must 
read before embarking on analyzing the Cassini imaging data. You will find 
the most detailed information available on the instrument and the science 
objectives in this publication. At the time of this writing, the SSR paper 
has not been approved for inclusion in this archive collection.  It is hoped 
that in time, approval will be granted and the SSR paper will become a file 
within the document directory.

Certain files are provided for use as human-readable files and some for 
use by home institution computer systems.  For instance, the comprehensive 
index.tab file can be used to populate one's own image catalog for user 
search and query capabilities (eg., querying for specific target names or 
target list, lat/lon ranges, cameras (narrow or wide), filters or selecting 
based on phase, incidence and emission angles).  More information on the 
index.tab file can be found in the indxinfo.txt file in the index directory.

The data directory contains the EDR image data files and accompanying detached
PDS label files.  These files are organized on the archive volumes in
subdirectories in Spacecraft Clock (SCLK) order.  Read the datainfo.txt to 
learn more about the contents of the data directory.

The EDR image data files are in a format called VICAR (Video Image 
Communication and Retrieval).  VICAR is an entire system of software, 
formats, and procedures for image storage and processing and was developed 
and is maintained by JPL's MIPS. A full explanation of VICAR, its standards, 
its software and reference information can be found at the website:
http://www-mipl.jpl.nasa.gov/vicar/. Information on tools for visualizing 
VICAR images can also be found there. One such tool is the NASAview 
software, which is provided by PDS and can be downloaded from the official 
PDS site: http://pds.jpl.nasa.gov.

For a brief tutorial on Cassini image calibration, see the 
theoretical_basis.ps document, as well as section 5 of the CISSCAL manual, 
both of which can be found in the document subdirectory on the Calibration 
archive volume.

Finally, the PDS Discipline Nodes are chartered to assist users with using 
the datasets they curate. Contact them for assistance if you find you have 
questions on getting started with using the Cassini image archive. 





------------------------------------------------------------------------------
2.  DVD Format   

This DVD has been formatted so that a variety of computer systems
may access the data.  Specifically, it is formatted according to the 
ISO-9660 Level 2 Interchange Standard. For further information, refer 
to the ISO-9660 Standard Document: RF#ISO 9660-1988, April 15, 1988.

NOTE: The volumes have both ISO and UDF file systems.  With the 
exception of the EDR product files, all file names on the volumes should 
be lower-case.  Filename case may not be preserved if your computer 
system reads the ISO filesystem instead of the UDF filesystem; though 
most computers should default to read the UDF file system. 





------------------------------------------------------------------------------
3.  Data Calibration

The first ten volumes of the calibration data set contain the pre-
flight ground calibration images. Volume eleven contains the collection 
of calibration data files, calibration software processing files, sample 
calibrated images and related documentation.  

The ground calibration image files recorded by the ISS cameras 
prior to launch have been archived on the first ten calibration
archive volumes.  These were originally produced on a collection
of CD-ROMs by the Instrument Operations team. They were intended
to be PDS-compliant when produced.  The Imaging Node later
converted the CD-ROMs to DVDs for inclusion in this ISS archive
collection.

The Cassini ISS archive volumes contain an extras directory that is
included only on the eleventh Calibration volume. This directory contains
the source code for the Cassini Imaging Science Subsystem Calibration 
(CISSCAL) software. This software, developed by the Cassini Imaging 
team, allows the user to radiometrically and geometrically process the 
EDR-level images into higher level calibrated images. 

CISSCAL was developed using the Interactive Data Language (IDL). IDL 
Version 5.5 or later is required to compile and run the code. Note that,
in the case that your computer system reads the ISO filesystem (instead
of the UDF filesystem) of the calibration DVD volume, filenames may 
display as uppercase instead of the default lowercase. This will make 
the CISSCAL software unusable, as IDL requires lowercase filenames. To 
get around this issue, the entire contents of the CISSCAL subdirectory
have also been provided as a g-zipped TAR archive. 

The CISSCAL manual, cisscal_v3_manual.tex, is located in the document
directory on the eleventh Calibration volume.

The calibration (calib) directory on the eleventh Calibration archive 
volume contains the calibration data files (sometimes called 
"calibration support files") necessary for processing the raw EDR 
images into higher-level products. The calibration data files range in 
format from text files (filter transmission functions, QE functions, 
etc.) to VICAR image files (blemish pixel maps, bright-dark pixel pair 
maps, flatfields, etc.), to Tagged Image File Format (TIFF) images and 
assorted binary-format data files. 

The calib directory is formatted in such a way as to be compatible with 
CISSCAL; users who wish to use CISSCAL will generally want to copy the 
entire calib directory intact to a location on their local filesystem where 
they have write priveledges. As with the CISSCAL subdirectory, all 
contents of the calib subdirectory will additionally be provided as a 
g-zipped TAR archive to avoid filename case problems that may arise when 
reading the DVD on some computer systems.

Both the calibration software and calibration data files will be updated 
throughout the mission; this may include newly generated data files.

The voluminous ISS Calibration Report can be found on all of the calibration
volumes in the /document/report/ sub-directory as a hypertext (HTML) file;
with thanks to the PDS Imaging Node for producing them.





------------------------------------------------------------------------------
4.  DVD Contents

Files found on the ISS archive DVDs are organized into a series of 
subdirectories below the top-level directory.  The structure and file 
contents differ between the DATA and CALIBRATION volumes with images and 
image dataset-related files being found on the DATA volumes and 
calibration-related files being found on the CALIBRATION volumes. 

Each volume within a dataset has a VOLUME_ID that is unique across the PDS
archive.  The VOLUME_ID is identified as follows: "<mission and instrument 
identifier>_<dataset number><3 digit volume number>"  i.e. COISS_1001

The following two tables show the directory structures for the DATA and 
CALIBRATION volumes.  Directory names are enclosed in square brackets ([]).  


----------------
| DATA volumes |
----------------

FILE                   CONTENTS
---------------------  ------------------------------------------------------
[ROOT] (Top level directory)
|- aareadme.txt        The file you are now reading.
|- errata.txt          Known anomolies and errors pertaining to this DVD.
|- voldesc.cat         Contents description of this DVD volume.
|
|- [catalog]           Directory with PDS catalog information about
|   |                  the Cassini ISS EDR and calibration data sets.
|   |- catinfo.txt     Description of files in the catalog directory.
|   |- jupiterds.cat   Jupiter dataset description (Jupiter data set only).
|   |- saturnds.cat    Saturn dataset description (Saturn data set only).
|   |- issna_inst.cat  Cassini ISS narrow angle camera instrument description.
|   |- isswa_inst.cat  Cassini ISS wide angle camera instrument description.
|   |- insthost.cat    Cassini Orbiter description catalog object.
|   |- mission.cat     Cassini-Huygens mission description catalog object.
|   |- person.cat      Cassini Imaging personnel objects.
|   |- projref.cat     Cassini project reference objects.
|   |- issref.cat      Cassini Imaging reference objects.
|
|- [data]              Contains the EDR image data files and labels.
|   |- datainfo.txt    Description of files in the data directory.
|
|- [document]          Directory containing document files.
|   |- docinfo.txt     Description of files in document directory.
|   |- archsis.txt     CO ISS Archive Volume Software Interface Specification.
|   |- archsis.lbl     PDS detached label for the Archive Volume SIS.
|   |- archsis.pdf     PDF version of Archive SIS.
|   |- edrsis.txt      CO ISS Vicar Image Data File & PDS Label SIS (EDR SIS).
|   |- edrsis.lbl      PDS detached label for the EDR SIS.
|   |- edrsis.pdf      PDF version of EDR SIS.
|   |- [report]        MIPS product and quality reports.
|
|- [index]             Directory containing index files.
|   |- indxinfo.txt    Description of files in the index directory.
|   |- index.tab       Volume index for APXS derived data (DDRs).
|   |- index.lbl       PDS label for index.tab.
|
|- [label]             Directory containing labels and include files.
|   |- labinfo.txt     Description of label directory files.
|   |- tlmtab.fmt      Binary Telemetry Header format.
|   |- prefix16.fmt    Binary Line Prefix format (ground cal images).
|   |- prefix.fmt      Binary Line Prefix format (cruise).
|   |- prefix2.fmt     Binary Line Prefix format (tour).
|   |- prefix3.fmt     Binary Line Prefix format (tour update).
|   |- vicar2.txt      ASCII VICAR Label format.




-----------------------
| CALIBRATION volumes |
-----------------------

FILE                   CONTENTS
---------------------  -------------------------------------------------------
[ROOT] (Top level directory)
|- aareadme.txt        The file you are now reading.
|- errata.txt          Known anomolies and errors pertaining to this DVD.
|- voldesc.cat         Contents description of this DVD volume.
|
|- [calib]             Directory containing Cassini ISS calibration files.
|   |- calinfo.txt     Description of files in the calib directory and
|   |                  information about calibration process.
|   |- calib.tar.gz    G-zipped TAR archive containing entire contents of all 
|   |                  subdirectories within calib directory
|   |- [antibloom]     Antiblooming mask files.
|   |- [bitweight]     Bitweight correction files.
|   |- [correction]    Correction factor files.
|   |- [darkcurrent]   Dark current parameter files.
|   |- [distortion]    Distortion parameter files.
|   |- [dustring]      Dustring correction files.
|   |- [efficiency]    Camera transmission calculation files.
|   |- [lut]           Inverse look-up table file.
|   |- [offset]        Camera shutter offset information files.
|   |- [slope]         Ground calibration slope files.
|
|- [catalog]           Directory with PDS catalog information about
|   |                  the Cassini ISS EDR and calibration data sets.
|   |- catinfo.txt     Description of files in the catalog directory.
|   |- calds.cat       Calibration dataset description.
|   |- issna_inst.cat  Cassini ISS narrow angle camera instrument description.
|   |- isswa_inst.cat  Cassini ISS wide angle camera instrument description.
|   |- insthost.cat    Cassini Orbiter description catalog object.
|   |- mission.cat     Cassini-Huygens mission description catalog object.
|   |- person.cat      Cassini Imaging personnel objects.
|   |- projref.cat     Cassini project reference objects.
|   |- issref.cat      Cassini Imaging reference objects.
|
|- [data]              Contains sample calibrated EDR image files and labels.
|   |- datainfo.txt    Description of files in the data directory.
|
|- [document]          Directory containing document files.
|   |- docinfo.txt     Description of files in document directory.
|   |- archsis.txt     CO ISS Archive Volume Software Interface Specification.
|   |- archsis.lbl     PDS detached label for the Archive Volume SIS.
|   |- archsis.pdf     PDF version of Archive SIS.
|   |- edrsis.txt      CO ISS Vicar Image Data File & PDS Label SIS (EDR SIS).
|   |- edrsis.lbl      PDS detached label for the EDR SIS.
|   |- edrsis.pdf      PDF version of EDR SIS.
|   |- cisscal_v3_manual.tex  ISS Calibration Software User Guide in LaTex
|   |- cisscal_v3_manual.pdf  (PDF version)
|   |- cisscal_v3_manual.lbl  (detached label)
|   |- in_flight_cal.tex      ISS Calibration In-Flight Guide in LaTex.
|   |- in_flight_cal.pdf      (PDF version)
|   |- in_flight_cal.lbl      (detached label)
|   |- theoretical_basis.tex  ISS Calibration Theoretical Basis in LaTex
|   |- theoretical_basis.pdf  (PDF version)
|   |- theoretical_basis.lbl  (detached label)
|   |- [report]        Ground calibration reports.
|
|- [index]             Directory containing index files.
|   |- indxinfo.txt    Description of files in the index directory.
|   |- index.tab       Volume index for APXS derived data (DDRs).
|   |- index.lbl       PDS label for index.tab.
|
|- [label]             Directory containing labels and include files.
|   |- labinfo.txt     Description of label directory files.
|   |- tlmtab.fmt      Binary Telemetry Header format.
|   |- prefix16.fmt    Binary Line Prefix format (ground cal images).
|   |- prefix.fmt      Binary Line Prefix format (cruise).
|   |- prefix2.fmt     Binary Line Prefix format (tour).
|   |- prefix3.fmt     Binary Line Prefix format (tour update).
|   |- vicar2.txt      ASCII VICAR Label format.
|
|- [extras]            Directory of CISSCAL calibration software files.
|   |- extrinfo.txt    Description of extras directory files.
|   |- cisscal.tar.gz  G-zipped TAR archive containing entire contents of
|   |                  cisscal directory
|   |- [cisscal]       Directory containing CISSCAL IDL source code.


For a further discussion on the file naming conventions and 
labeling refer to the DOCUMENT directory and the Archive Volume 
Software Interface Specification.





------------------------------------------------------------------------------
5.  File Formats

This section describes the file formats used for the types of files 
found on the Cassini ISS EDR and Calibration Files Archive Volumes.


Document File Formats:

Cassini ISS document files are found in one or more of the following 
formats: .txt - ASCII text files, .htm - Hypertext Markup Language, 
.tex - LaTeX TeX macro package, and/or .pdf - Portable Document Format. 

ASCII - A flat, human-readable ASCII text version of each document must 
be included on the volume.  Also, adhering to PDS recommendations, 
plain text files have line lengths restricted to 78 characters or 
fewer in order to accommodate printing and display on standard devices. 
Each line is terminated by the two-character carriage-return/linefeed 
sequence, <CR><LF> (ASCII decimal character codes 13 and 10, respectively), 
for a maximum total line length of 80 characters.  Document .txt files 
are flat ASCII text files which may have embedded PDS labels (see PDS 
Label Formats below).

PDF - Document .txt files may be accompanied by corresponding .pdf 
document files in cases where documents contain formatting and figures 
could not easily be rendered as ASCII text.  Portable Document Format 
(PDF) is a proprietary format of Adobe Systems Incorporated that is 
frequently used for distributing documents. Adobe offers a free 
downloadable reader, Acrobat Reader, from their website at 
http://www.adobe.com.  Be sure to download the latest version of 
Acrobat Reader to view the PDF files on the this volume.

LaTeX - LaTeX is a TeX macro package that provides a document 
processing system. LaTeX allows markup to describe the structure of a 
document, so that the user need not think about presentation.  LaTeX is 
a high-quality typesetting system, with features designed for the 
production of technical and scientific documentation.  LaTeX files have 
relatively little markup embedded in the text and are generally 
considered human-readable and may, therefore, be used to satisfy the 
ASCII text version requirement.  One exception may be tables within the 
document which will not appear properly typeset unless the LaTeX file 
is first compiled and converted to a different format. LaTeX is free, 
and is currently developed and maintained by LaTeX3 Project. 
Information about the system and various conversion software can be 
found at their current website: http://www.latex-project.org.

HTML - The hypertext markup language (HTML) file contains ASCII text 
plus HTML markup commands that enable it to be viewed in a Web browser. 
The hypertext file may be accompanied by ancillary files such as images 
and style sheets that are incorporated into the document by the Web 
browser.



Tabular File Formats:

Tabular files (.tab suffix) exist in the index directory. Tabular 
files are ASCII files formatted for direct reading into many 
database management systems on various computers. All fields 
are separated by commas, and character fields are enclosed in 
double quotation marks ("). (Character fields are padded with 
spaces to keep quotation marks in the same columns of successive 
records.) Character fields are left justified, and numeric fields 
are right justified. The "start byte" and "bytes" values listed 
in the labels do not include the commas between fields or the 
quotation marks surrounding character fields. The records are 
of fixed length, and the last two bytes of each record contain 
the ASCII carriage-return/line feed character sequence, <CR><LF>. 
This allows a table to be treated as a fixed length record file 
on computers that support this file type and as a text file with 
embedded line delimiters on those that don't.   All tabular 
files are described by detached PDS labels. The PDS label file 
has the same name as the data file it describes, with the 
extension .lbl; for example, the file index.tab is accompanied 
by the detached label file index.lbl in the same directory.



PDS Label Formats:

PDS labels are object-oriented. The object to which the label 
refers (e.g., IMAGE, TABLE, etc.) is denoted by a statement of 
the form:

    ^object = location

in which the carat character ('^', also called a pointer in this
context) indicates that the object starts at the given location.  
For an object in the same file as the label, the location is an 
integer representing the starting record number of the object (the 
first record in the file is record 1).  For an object located 
outside the label file, the location denotes the name of the file 
containing the object, along with the starting record or byte 
number.  For example:

    ^IMAGE = ("N1294562651_1.IMG",3)

indicates that the IMAGE object begins at record 3 of the file
N1294562651_1.IMG, in the same directory as the detached label file. 
 
Below is a list of the possible formats that use the ^object 
keyword.

    ^object = n
    ^object = n <BYTES>
    ^object = "filename.ext"
    ^object = ("filename.ext",n)
    ^object = ("filename.ext",n <BYTES>)

where:

    n         starting record or byte number of the object,
              counting from beginning of the file (record 1,
              byte 1); default is record number.
    <BYTES>   indicates that number given is in units of bytes.
    filename  upper-case file name.
    ext       upper-case file extension.

A combination of detached and attached PDS labels are found throughout
the archive volumes.

All image data files in the archive have PDS labels, detached in a 
separate file. For examples of PDS labels for each type of data 
product, see the Data Product SIS, edrsis.txt, in the document directory
[Applicable Document #3].



Software File Formats:

The Cassini ISS calibration software (CISSCAL) has been developed 
by the Cassini Imaging team using Interactive Data Language (IDL) 
software. No compiled executables are supplied.  Source code is 
written in IDL such that Version 5.5 or later is required to run 
CISSCAL. The source code is machine independent and will thus run 
on any machine for which an appropriate version of IDL is available. 

To avoid any filename case issues which may arise when reading the DVD
volumes, the entire contents of the CISSCAL software directory and calib
directory have also been provided in separate g-zipped TAR archives. 
These archives can be decompressed using the standard 'tar' and 'gunzip' 
commands included with most LINUX and UNIX distributions, or with a 
program like WinZip for users running Windows.



Catalog File Formats:

Catalog files (suffix .cat) exist in the catalog directory. They 
are text files formatted in an object-oriented structure consisting 
of sets of 'keyword=value' declarations. Each line is terminated by 
the two-character carriage-return/linefeed sequence, <CR><LF> (ASCII 
decimal character codes 13 and 10, respectively). PDS recommends 
catalog files have line length restricted to 72 characters or fewer, 
including the <CR><LF>, to accommodate PDS data ingestion 
requirements set forth by their internal catalogs and databases.



Image Data File Formats:

The image processing software used to create the EDR image data files is
called VICAR (Video Image Communication And Retrieval).  VICAR is an
entire system of software, formats, and procedures for image storage
and processing and was developed and is maintained by JPL's MIPS. A full 
explanation of VICAR, its standards, its software and reference
information can be found at website: http://www-mipl.jpl.nasa.gov/vicar/. 

Each image data file has a filename ending with the '.IMG' suffix, and 
contains several fixed-length data records. These are: the ASCII VICAR 
Label (or simply "image header"), the Binary Label Header (or "Binary
Telemetry Header"), and the Image Line Records, which are comprised of the
Binary Line Prefix plus the actual pixel data. All of these are briefly 
described in the paragraphs below. For more complete information about the 
format and content of the image data products, see the Cassini ISS Software 
Interface Specification (SIS) documents found in the document directory of 
these volumes. These image files are reconstructed from the best available 
telemetry data and line-filled where necessary to produce the most 
complete image records possible.

The ASCII VICAR Label is included to facilitate image processing and 
allow easy validation of products using existing VICAR software. 
These labels consist of a set of ASCII "keyword=value" pairs 
describing the important characteristics of the image. The VICAR Label is 
designed to be human-readable because it often is used to annotate 
products derived from the image, such as prints or plots. In addition, 
it is maintained through the various processed versions of the 
image to allow traceability. Also, the Label items may be extracted by 
software modules in order to guide automated processing procedures.
 
The VICAR Label contains required System items (such as image size 
information), History items (recording processing history for 
the file), and optional Property items (such as items describing 
gain states, etc.).

The Binary Label Header (also known as the Binary Telemetry Header) 
contains machine-readable information about the image as a whole and 
is populated directly from the telemetry available for the product. 
Many of these items are in the VICAR Label as well, but non-VICAR sites 
may ignore the VICAR Label and use the Binary Telemetry Header to 
construct their own human-readable label. This record contains 60 bytes 
of information and is padded with zeros to the image record length.  
Items in this header are copied directly from the Extended ISS 
Science header returned in telemetry.

There is one Line Record for each image line, comprised of a 24-byte
Binary Prefix followed by the 8- or 16-bit pixel data for that line. The
Prefix contains information about the image line derived from telemetry. 
This information may vary from line to line, so is not appropriate to 
include in the Binary Telemetry Header. Note that for Lossy compressed
images, the data are not associated with lines, so there is no way to 
associate a given record with a line number. In this case, the Binary Line
Prefix contains information extracted from the lossy records received.




------------------------------------------------------------------------------
6.  Contact Information

For questions concerning the Cassini ISS images on this volume,
contact:

    PDS Imaging Node
    Susan K. LaVoie
    M/S 168-514
    Jet Propulsion Laboratory
    4800 Oak Grove Drive
    Pasadena, CA  91109-8099
    (818) 354-5677

    WWW Site:  http://pds-imaging.jpl.nasa.gov/
    E-mail:    feedback@pds-imaging.jpl.nasa.gov

For questions related to the Cassini Imaging Experiment
and related data analysis contact:

    Dr. Carolyn Porco
    Space Sciences Institute
    4750 Walnut Street
    Suite 205
    Boulder, CO 80301

    WWW Site: http://www.ciclops.org
    E-mail:   cpcomments@ciclops.org


For general information related to the PDS, contact:

    Planetary Data System, PDS Operator
    M/S 202-101
    Jet Propulsion Laboratory
    4800 Oak Grove Drive
    Pasadena, CA  91109-8099
    (818) 354-4321

    WWW Site:  http://pds.jpl.nasa.gov/
    E-mail:    pds_operator@jpl.nasa.gov





------------------------------------------------------------------------------
7.  Cognizant Persons & Acknowledgments

A great many people over the last 13.5+ years have contributed 
to the success of the Cassini Imaging Science experiment. It would 
be an impossible task to name every one. 

Special thanks goes to the engineers at JPL with whom the Imaging 
Team had the pleasure of working in designing and developing the 
cameras in the pre-launch years: notable among them are Tom Livermore, 
William Harris, Cindy Kahn, Len Snyder, and Lloyd Adams. 

We also thank the scientists, engineers and others across the 
imaging team and at JPL who have either made significant 
contributions in the pre-launch years to the calibration of the 
instrument or are currently responsible for various aspects of 
the operations, in-flight calibration and archiving of the ISS data: 
John Barbara, Michael Belanger, Emma Birath, Rachel Carson,
Sebastien Charnoz, Chris Clark, Diane Conner, Tilmann Denk, 
Preston Dyches, Mike Evans, Joe Ferrier, Heidi Finn, Kevin Grazier, 
Paul Helfenstein, Pauline Helfenstein, Bob Jacobson, Ben Knowles, 
Dyer Lytle, Nicole Martin, Dave O'Brien, Leslie Pieri, Jon Proton, 
Josh Riley, Diane Sherman, Joseph Spitale, Elizabeth Turtle, 
Ashwin Vasavada, Daren Wilson, Charlie Avis, Amy Culver, John Diehl, 
James Gerhard, Tina Pavlicek, Candy Hansen, Brad Wallis, and others. 

Special thanks also goes to Rafael Alanis and John Diehl of the 
PDS Imaging Node for their continual support in defining the archive 
volumes, Ron Joyner and Steve Adams for keeping the PDS Central Node
perspective, and to Diane Conner, Cassini Data Engineer, for her 
exceptional attention to detail and keeping the ISS archive team 
on track with Project requirements.  

Usability of these volumes was ensured through the formal review 
process established by PDS and Project personnel. Sample volumes & 
directory files, along with the final peer review volumes were 
reviewed prior to final production start. We appreciate the support 
provided by the review panelists consisting of PDS Discipline Node 
and Central Node scientists and engineers and the external imaging 
scientists, especially Eric Eliason for his early-on review of the 
archive design and Michael Malin's invaluable archiving advice. We 
appreciate their role in making sure the ISS archive meets PDS 
standards and provides adequate detail for future scientific and 
engineering use and research.

This volume collection set was designed and produced at the Cassini 
Imaging Central Laboratory for Operations (CICLOPS) located at Space 
Sciences Institute in Boulder, Colorado. 
