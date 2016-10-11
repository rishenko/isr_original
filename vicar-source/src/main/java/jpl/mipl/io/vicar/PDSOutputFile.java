/*
* PDSOutputFile.java
*
*/

package jpl.mipl.io.vicar;

import jpl.mipl.io.streams.*;
import java.io.*;
import java.awt.image.*;
import javax.media.jai.*;

import org.w3c.dom.Document;

import jpl.mipl.io.plugins.*;

/**
 * This class manages a single PDS output image file.
 * <p>
 * All accesses to the PDS file are thread-safe, assuming that nobody else
 * tries to access the underlying stream directly.  Thus, multiple threads
 * can issue simultaneous <code>writeRecord()</code> or <code>writeTile()</code>
 * requests, although each request is handled one at a time via synchronization
 * on the <code>VicarOutputFile</code> object.  However, if you have a
 * sequential-only stream, all accesses must still be strictly sequential...
 * meaning that the use of multiple threads with sequential streams will not
 * work (the request order would be non-deterministic).  For random-hard
 * streams, threads will work but could cause performance hits depending on
 * the ordering of the requests.  Random-easy streams should be fine.
 * <p>
 * Due to a scarcity of useful Output streams and wrappers in the Java I/O
 * system, there are currently no "random-hard" streams, and the only
 * random-easy stream is
 * <code>jpl.mipl.io.streams.RandomAccessFileOutputStream</code> (which is
 * automatically wrapped around the <code>RandomAccessFile</code>-related
 * <code>open()</code> methods.  Anything else is sequential only.
 * Hopefully the Image I/O package will provide useful random-access streams,
 * so the code is written with random-easy and random-hard still in mind.
 * <p>
 * The ability to create PDS detached labels is now a part of this class.
 * 2 situations can be handled.
 * 1) The input file is a vicar image. Only a PDS label will be created. 
 * The input file will not be modified. 
 * detachedLabelOnly must be set to true.
 * inputFilename, recordLength and vicarLabelRecordCt must all be set.
 * These values will be used to allow the Detached label to point to the 
 * input image file. In the PDS label the ^IMAGE = ("inputFilename", vicarLabelRecordCt)
 * These values must be set properly in the detached label 
 * RECORD_BYTES, FILE_RECORDS, LABEL_RECORDS
 * <p>
 * @see VicarOutputImage
 * @see VicarOutput
 */

public class PDSOutputFile extends VicarOutputFile
{
    /** Both _output_stream and _output_stream_random point to the same
     *  stream if _output_stream is an instance of RandomAccessFileOutputStream.
     */
   //  protected RandomAccessFileOutputStream _output_stream_random; // special case not in VivarOutputFile
    
    /** these are alll declared in VicarOutputFile.java
     * we don't want to redeclare them, theybare listsed here as documentation
    protected OutputStream _output_stream;
    
    protected DataOutput _output_stream_wrap;
    

    protected VicarLabel _label;
    protected SystemLabel _system;
    
    
    protected int _lblsize_front;
    protected long _image_size_bytes;
    protected long _current_file_pos;	// Current position for seq files
    protected boolean _expandable;	// true iff file length not specified
    protected int _expandable_size;	// size N2 or N3 should be

    protected boolean _file_open;

    // Random access mode flags 
    protected boolean _random_allowed;
    protected boolean _random_easy;

    protected VicarDataFormat _data_format;
    protected VicarDataFormat _binary_data_format;

    protected VicarInput _primary_input;
    protected VicarLabel _primary_label;
    protected SystemLabel _primary_system;

    // Internal data buffers

    protected int _int_buffer[];
    protected int _int_bufsize;
    protected float _float_buffer[];
    protected int _float_bufsize;
    protected double _double_buffer[];
    protected int _double_bufsize;
***/
    
////////////////////////////////////////////////////////////////////////
// PDS specific
    Document pdsDOM = null;
    String outputFilename = null;
    String inputFilename = null;
    // PDSMetadata pdsMetadata;
    protected VicarLabel _embeddedVicarLabel = null;
    protected SystemLabel _embeddedSystemLabel = null;
    boolean _embedVicarLabel = false;
    boolean detachedLabelOnly = false;
    boolean detachedLabel = false;
    boolean usePIRL = false;
    String pds_ptr = null;
    int recordLength = 0;
    int vicarLabelRecordCt = 0;
    int vicarImageFileRecords = 0;
    
    boolean _debug = false;
  //  boolean _debug = true;
 

/***********************************************************************
 * Dummy constructor (for now). !!!! Need to add good ones that call open().
 */
    public PDSOutputFile()
    {
    	super();
    	
	
	// PDS
	Document pdsDOM = null;
	outputFilename = null;
	inputFilename = null;
    // PDSMetadata pdsMetadata;
	if (_debug) System.out.println("Constructor PDSOutputFile");
    }

/**
 * if the value is set it will used in the PDS label in the ^IMAGE line
 */
public void setOutputFilename(String s) {
    outputFilename = s;
}

/**
 * if the value is set it will used in the PDS label in the ^IMAGE line
 */
public void setInputFilename(String s) {
    inputFilename = s;
}

/**
 * if the value is set it will used in the PDS label to set the values of
 * any POINTER items such as ^IMAGE
 * May only be allowed if detachedLabelOnly is true
 */
public void setPds_ptr(String s) {
    pds_ptr = s;
}



/** 
 * used if a detached PDS label is being written.
 * used to set the record length of the PDS label
 * @param l
 */
public void setRecordLength(int l) {
	recordLength = l;
}

/** 
 * used if a detached PDS label is being written.
 * used to set the FILE_RECORDS value of the PDS label
 * @param r
 */
public void setVicarImageFileRecords(int r) {
	vicarImageFileRecords = r;
}

/**
 * used if a detached PDS label is being written.
 * used to set the start record of the data portion of the 
 * data file. This indicates the number of records in the embedded vicar
 * label of the image data file.
 * @param ct
 */
public void setVicarLabelRecordCt(int ct) {
	vicarLabelRecordCt = ct;
}
/*
 * if the value is set only a detached PDS label will be written
 */
public void setDetachedLabelOnly(boolean f) {
    detachedLabelOnly = f;
}

/**
* if the value is set a detached PDS label will be written
*/
public void setDetachedLabel(boolean f) {
   detachedLabel = f;
}
/**
 * sets the PDS metadata Document which is used to write the
 * PDS label
 */
 public void setPdsDOM(Document doc) {
        pdsDOM = doc;
        // check for proper type ??
}

/* 
 * flag used to control inclusion of an emebedded Vicar Label in the file
 */
public void setEmbedVicarLabel(boolean b) {
	_embedVicarLabel = b;
}

/* 
 * convenience method to test the _embedVicarLabel flag
 */
public boolean doEmbedVicarLabel() {
	return _embedVicarLabel ;
}

/* 
 *  sets the VicarLabel object used to create the embedded Vicar Label.
 * This object should be derived from a VicarImageReader. 
 * One could also be synthesised by a program.
 */
 public void setEmbeddedVicarLabel(VicarLabel label) {
   	_embeddedVicarLabel = label;
 }
  
  /*
   * sets the Vicar SystemLabel object used to create the Vicar Label. <br>
   * If the image data has been modified in some way a new SystemLabel must be generated.
   * The SystemLabel from a VicarImageReader will reflect the SystemLabel 
   * values for the input image.<br>
   * The new SystemLabel should reflect the SystemLabel information about the output image. 
   * Otherwise the image will not be read in correctly by image readers which use the 
   * information in the embedded Vicar Label.
   * */
 public void setEmbeddedSystemLabel(SystemLabel label) {
   	_embeddedSystemLabel = label;
 }
    
 /*
 * turns debug printing on or off
 * */
public void setDebug(boolean d) {
	_debug = d;
}
 


/***********************************************************************
 * Does the actual work of opening the file.  Writes the SystemLabel, and
 * any Vicar label currently present.
 * @throws IOException
 * @throws AlreadyOpenException if the file's been opened already
 **********************
    protected void openInternal() throws IOException, AlreadyOpenException
    {
        
    if (_debug) System.out.println("PDSOutputFile.openInternal() (0) ");
	if (_file_open)
	    throw new AlreadyOpenException("file has been opened already");

    

	_label = getVicarLabel();
	
	_system = getSystemLabel();
	 

	// Set up defaults for missing label items.
	// Note that the host format defaults for input files are VAX, because
	// that was the only kind of file in existence before the host type
	// labels were added.  Output files default to Java.

	if (!_system.isHostValid()) {
	    _system.setHost("JAVA");
	}
	if (!_system.isIntFmtValid()) {
	    _system.setIntFmt("HIGH");
	}
	if (!_system.isRealFmtValid()) {
	    _system.setRealFmt("IEEE");
	}
 
       if (_debug) System.out.println("PDSOutputFile.openInternal() (2) ");
	// Check to make sure the image sizes are specified.  Check for the
	// case of a grow-able file and set the flag appropriately.

	if ((_system.getN1() <= 0) || (_system.getN2() < 0) ||
		(_system.getN3() < 0)) {
	    throw new IllegalArgumentException("Image sizes must be > 0");
	}
	_expandable = false;
	_expandable_size = 0;
	if (_system.getN2() == 0) {		// expandable line
	    if (_system.getN3() != 1)
		throw new IllegalArgumentException(
		     "If N2 is 0 to indicate an expandable file, N3 must be 1");
	    _expandable = true;
	}
	if (_system.getN3() == 0)
	    _expandable = true;

	if (_expandable && !_random_allowed)
	    throw new IllegalArgumentException(
				"Expandable files must be random-access");

	// Install the system label into the VICAR one

	try {
	    _system.writeLabel(_label.getSystem());
	} catch (Exception e) {
	    throw new IOException("Error in System bean processing: " + e);
	}

	// Now write the label to the file.  The length is unlimited since
	// we have not written any data.

	_file_open = true;

	seekToLocation(0);	// just to be sure - affects only random streams
	 if (_debug) System.out.println("PDSOutputFile _output_stream "+ _output_stream);
	********************************/
	 
	 
	protected void writeOutLabels() throws IOException
     {
	 // create the PDS label String from the PDS metadata Document
	 if (_debug) System.out.println("PDSOutputFile.writeOutLabels() (0) ");
	 
	DOMtoPDSlabel dom2PDS = new DOMtoPDSlabel(pdsDOM );
	dom2PDS.setDebug(_debug);
	dom2PDS.setOutputFilename(outputFilename);
	dom2PDS.setPds_ptr(pds_ptr);
	
	dom2PDS.setVicarLabelString(null); // if we have an embedded VicarLabel it will be set later
	 
	 // Get the host and data formats and set up the VicarDataFormat object.
	/**
	_data_format = new VicarDataFormat(_system.getHost(),
			_system.getIntFmt(), _system.getRealFmt());
			
   if (_debug) System.out.println("_data_format "+_data_format );
	
	// this is the stream we will use to write the file out		 
	_output_stream_wrap = _data_format.getDataOutputWrapper(_output_stream);
	***/

   // get a String for the emebeddded Vicar Label 
     
    if ( _embedVicarLabel && _embeddedVicarLabel != null && _embeddedSystemLabel != null) {
    	// if (_debug) 
    	if (_debug) System.out.println("writing embeded Vicar Label");
    	
    	// use this output stream to capture the VicarLabel into a String
       ByteArrayOutputStream ba_output_stream = new ByteArrayOutputStream();
     
     // Install the system label into the VICAR one
      if (_debug)  {
     	System.out.println(" _embeddedSystemLabel "+ _embeddedSystemLabel);
     	System.out.println(" _embeddedVicarLabel "+ _embeddedVicarLabel);
      }
      
	try {
	    _embeddedSystemLabel.writeLabel(_embeddedVicarLabel.getSystem());
	} catch (Exception e) {
	    throw new IOException("Error in embedded System bean processing: " + e);
	}
     
	VicarLabel.ItemPos ePos = _embeddedVicarLabel.writeLabelChunk(ba_output_stream,
								 0, _embeddedSystemLabel.getRecsize(), null);
	if (!ePos.isComplete) {
	    throw new IOException("Error writing initial embedded VICAR label");
	}
	

	//  be sure to work with the vicar label passed in from  the reader/writeParam
	 // not the one used to control writing of the PDS image data
	 

	int _embeddedVicar_lblsize = ePos.lblsize;

	_image_size_bytes = (_system.getNLB() +
					(_system.getN2() * _system.getN3())) * _system.getRecsize();

	 if (_debug) System.out.println("_embeddedVicar_lblsize_front = " + _embeddedVicar_lblsize );
	 
	int recordSize = _system.getRecsize();
	int emVicLabRecords =_embeddedVicar_lblsize /  recordSize ;
	if (_debug) System.out.println("_embedded VicarLabel is "+emVicLabRecords);
	// assume the Vicar Label String is correctly padded and is an integer number 
	// of Records
 
 	_embeddedVicar_lblsize = 0;
	// _current_file_pos = _embeddedVicar_lblsize_front;
	
	String vicarLabelString =  ba_output_stream.toString() ; // or toByteArray();
	 if (_debug)  {
	 	System.out.println("vicarLabelString.length() = "+vicarLabelString.length() );
		System.out.println("vicarLabelString = "+vicarLabelString);
	 }
	 /*** 
	  * write the Vicar Label String to a file.
	  * this was a test used to check if the Vicar Label String looked correct *
	// byte[] vicarLabelByteArray =  ba_output_stream.toByteArray();
	String labelFile = "vicarLabel.txt" ;
	
	FileWriter fileWriter = new FileWriter(labelFile);
	fileWriter.write(vicarLabelString);
	fileWriter.close();
	****************************************************************/
	
	/* give the Vicar Label String to the PDS label writer. The writer will see if
	 * the String is null or not, If the String is present the PDS label will include
	 * pointers to the label and take its size in records into account. The final PDS label 
	 * will have this string emebedded in it.
	 */
	dom2PDS.setVicarLabelString( vicarLabelString ) ; 
    }
    else {
    	if (_debug) System.out.println(" NO embeded Vicar Label");
    	dom2PDS.setVicarLabelString( null ) ;
    }
	 
        // Dom2PDS.display();
    if (detachedLabelOnly == true ) {
    	// set things up so we only create a detached label
    	// make the detached label point to the vicar image file (inputFilename)
    	dom2PDS.setOutputFilename(outputFilename);
    	dom2PDS.setInputFilename(inputFilename);
    	dom2PDS.setDetachedLabelOnly(true);
    	
    	dom2PDS.setPds_ptr(pds_ptr);
    	// int vicarLabelRecordCt, recordLength
    	if (_debug) {
    		System.out.println(" detachedLabelOnly *****************");
    		System.out.println(" vicarLabelRecordCt "+vicarLabelRecordCt);
    		System.out.println(" recordLength "+recordLength);
    		System.out.println(" vicarImageFileRecords "+vicarImageFileRecords);
    	}
    	dom2PDS.setInputVicarLabelRecordCt(vicarLabelRecordCt);
    	dom2PDS.setInputVicarRecordLength(recordLength);
    	dom2PDS.setInputVicarFileRecords(vicarImageFileRecords);
    }
         if (_debug) System.out.println(" calling DOMtoPDSlabel");
        
        // this gives us the completed PDS label, ready to write to the file 
        String s = dom2PDS.toString();
        
         if (_debug) System.out.println(" *** AFTER  DOMtoPDSlabel");
        
        //    FileOutputStream fos = new FileOutputStream(outFile);
        //  fos.write(s.getBytes() ); // write wants a byte[]
	  //   _output_stream_wrap.write( s.getBytes() ); // old way
	  // new way to agree with changes Bob made
	 byte b[] = s.getBytes();
	 // _output_stream_wrap.write( b ); // old way
	 stream_write(b, 0, b.length);
	 
	// the emebeded Vicar Label is included in the label size and label string
	_lblsize_front = dom2PDS.getLabelSize();
	
	int _pdsLabelSizeBytes = _lblsize_front ;
	// was // int _image_size_bytes = dom2PDS.getImageSize();
	_image_size_bytes = dom2PDS.getImageSize(); // this sets the global, Do I want that to happen?
	_current_file_pos = _lblsize_front;

	
	
    }



/***********************************************************************
 * Sets the VICAR label for this image.  The provided <code>VicarLabel</code>
 * object contains the property and history labels that are to be written
 * to the file.  Note that the system label portion of <code>VicarLabel</code>
 * is ignored; in fact, the system label portion (only!) might be modified
 * by this routine to reflect the current system label (no guarantees one
 * way or the other).
 * <p>
 * Unlike <code>setSystemLabel()</code>, this routine can be called at any
 * time after the system label is set, assuming the file is random-access.
 * You can call this routine multiple times in order to update the label, as
 * well.  For a sequential-only file, however, this routine must only be
 * called before the file is opened.
 * <p>
 * The VICAR label is generally obtained first via a call to
 * <code>getVicarLabel</code> on this (or another) image.  It may then
 * be modified before submitting it back via this routine.
 * <p>
 * Before the label is set, a call to <code>getVicarLabel</code> will
 * return the existing label if the file already exists.  If not, the
 * primary input's label is returned.  If that hasn't been set, an empty
 * VICAR label is returned.  Once the label is set via this routine,
 * future get calls will return the current label.
 * <p>
 * It is up to the application to add a VICAR history task if desired.
 * Use <code>VicarLabel.createHistoryTask()</code>.
 * @see VicarIOBase#getVicarLabel
 * @see VicarLabel#createHistoryTask
 * @throws IOException
 */
    public synchronized void setVicarLabel(VicarLabel vl) throws IOException
    {
	if (_file_open && !_random_allowed) {
	    throw new NonSequentialAccessException(
	       "Can't write VICAR labels to a sequential file after it's open");
	}

	_label = (VicarLabel)vl.clone();


	if (_file_open) {		// write the new label out
	    writeLabelInternal();
	    // we should overdie sritwLabelInternal too!!!
	}

    }

/***********************************************************************
 * Internal routine to write the label to the file.  Writes both the initial
 * and EOL labels.  Sequential checks must already have been performed (this
 * can only be called for random files).  This routine does merge in the
 * system label first.
 * <p>
 * Labels are NOT written if the file is expandable.  close() is responsible
 * for calling this routine (with _expandable false) in order to flush the
 * final labels.
 */
    protected void writeLabelInternal() throws IOException
    {
    	boolean notNow = true;
    	
    if (notNow) return ;
    	
	if (_expandable)		// do nothing
	    return;

	seekToLocation(0);

	// Install the current system label into the VICAR one

	try {
	    _system.writeLabel(_label.getSystem());
	} catch (Exception e) {
	    throw new IOException("Error in System bean processing: " + e);
	}

	// Write the label

	VicarLabel.ItemPos pos = _label.writeLabelChunk(_output_stream,
			_lblsize_front, _system.getRecsize(), null);
	_lblsize_front = pos.lblsize;
	if (!pos.isComplete) {
	    seekToLocation(_lblsize_front + _image_size_bytes);

	    pos = _label.writeLabelChunk(_output_stream,
			0, _system.getRecsize(), pos);
	    if (!pos.isComplete)
		throw new IOException("Error writing VICAR EOL label");
	}
    }


/////////////////////////////////////////////////////////////////////



   
}

