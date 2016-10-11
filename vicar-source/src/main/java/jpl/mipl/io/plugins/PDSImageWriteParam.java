package jpl.mipl.io.plugins;

import jpl.mipl.io.vicar.*;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

/**
 * PDSImageWriteParam
 * @author Steve Levoe
 *
 . This class will be used to pass in some needed values useful to a PDSImageWriter
 * currently all the default items in the ImageWriteParam are IGNORED.
 * The current items are to support transcoding.
 * Someday we'll add them too.
 */
public class PDSImageWriteParam extends ImageWriteParam {

	String outputFileName; // used by the writer to put the filename into the label
	
	boolean transcodeIIOmetadata = false; // turns on attempt to tramscode if true
	String xslFileName = null; // transcoder xsl script
	String vicarLabelString = null ; // a String containing a complete vicar label read from the input file
	// useful for creating an emebeded vicar label
	VicarLabel vicarLabel = null; 
	boolean embedVicarLabel = false; // controls if an embeded vicar label is added to the output file
	
	boolean debug = false;
	boolean outputXML = false;
	
	boolean dirty = false; // flag to indicate if the image's data has changed
	// later we may add a way to search for a default script in the jar file
	boolean addMerItems = false; // one set of items
	boolean addStatistics = false;  // a different set of items
    boolean calculateStatistics = false; // calculate the values, otherwise placeholders are added
    
    boolean addLinePrefix = true; // if prefix metadata is present write it to the output file
    // there will be a VicarBinaryLinePrefix object in the PDSMetadata Object
    
	boolean addBinaryHeader = true; // if binary header metadata is present write it to the output file
		// there will be a VicarBinaryHeader object in the PDSMetadata Object
	
	// PDS detached label support
	boolean detachedLabel = false;
	boolean detachedLabelOnly = false;
	boolean dataFileIsVicarImage = false;
	
	// these would be used when vicar and label only is true
	int recordLength = 0;
	int vicarLabelRecordCt = 0;
	int vicarImageFileRecords = 0;
	
	// byte count of the label = recordLength * vicarLabelRecordCt
	// inputFilename is used in the PDS label as the file to point to
	// ^IMAGE = ("inputFilename", vicarLabelRecordCt)
	// the writer must make the PDS label use recordLength as the label record length
	// outputFilename is the name of the file that the PDS label is written to
	
	String dataFilename = null;
	String detachedLabelFilename = null;
	boolean usePIRL = false;
	String inputFileName; // used by the writer to put the filename into the label
	String pds_ptr = null; // used by writer to add and/or override PDS label ^POINTER items
	
//	 use to determine the format of the input data file
    int  vicarPixelSize = 0;
    String vicarFormat = null;
    String vicarIntFmt = null;
    String vicarRealFmt = null;

    /**
     * VicarBinaryLinePrefix vicarBinaryLinePrefix = null;
     *
     * we could also add a VicarBinaryLinePrefix Object 
     * this would allow someone to add prefix data to a PDS image where the prefix could be generated 
     * any where. OR they can add it to the PDSMetadata Object which is how it currently works
     * **/
    
    
    /* these values must be calculated somewhere and passed to the writer via params
    
    int first_line = 1;
    int first_line_sample = 1;
    String sample_bit_mask = "2#0000111111111111#furble";
    
    int invalid_constant = 0; 
    int missing_constant = 0;
    
    String mean = "UNK";
    String median = "UNK";
    String maximum = "UNK";
    String minimum = "UNK";
    String standard_deviation = "UNKNOWN";
    String checksum = "UNK";
    
    
	
	/**
	 * Constructor for PDSImageWriteParam.
	 */
	public PDSImageWriteParam() {
		super();
	}

	/**
	 * Constructor for PDSImageWriteParam.
	 * @param locale
	 */
	public PDSImageWriteParam(Locale locale) {
		super(locale);
	}
	
	public PDSImageWriteParam( String _outputFileName, // used by the writer to put the filename into the label
				boolean _transcodeIIOmetadata, // turns on attempt to transcode if true
				String _xslFileName,// transcoder xsl script
				String _vicarLabelString, // a String containing a complete vicar label read from the input file
																	// useful for creating an emebeded vicar label
				VicarLabel _vicarLabel ) {
					
		super();
		outputFileName = _outputFileName;
		transcodeIIOmetadata = _transcodeIIOmetadata; 
		xslFileName =  _xslFileName;
		vicarLabelString = _vicarLabelString;
		vicarLabel = _vicarLabel;
	}
	
	
	// getters and setters
	public void setAddMerItems(boolean f) {
    	addMerItems = f;
    }
    
      public boolean getAddMerItems() {
    	return addMerItems ;
    }
    
	public void setAddLinePrefix(boolean f) {
			addLinePrefix = f;
		}
    
	  public boolean getAddLinePrefix() {
			return addLinePrefix ;
		}
    
	public void setAddBinaryHeader(boolean f) {
				addBinaryHeader = f;
			}
    
		public boolean getAddBinaryHeader() {
				return addBinaryHeader ;
			}
			
    public void setAddStatistics(boolean f) {
    	addStatistics = f;
    }
    
      public boolean getAddStatistics() {
    	return addStatistics ;
    }
    
    public void setCalculateStatistics(boolean f) {
    	calculateStatistics = f;
    }
    
      public boolean getCalculateStatistics() {
    	return calculateStatistics ;
    }
    
    
	public String getOutputFileName() {
		return outputFileName;
	}
	
	  public void setOutputFileName(String _outputFileName) {
		outputFileName = _outputFileName;
	}
	
    //	 String inputFileName; // used by the writer to put the filename into the label
	public String getInputFileName() {
		return inputFileName;
	}
	
	  public void setInputFileName(String _inputFileName) {
		inputFileName = _inputFileName;
	}
//	 PDS detatched label support
	// boolean detachedLabel = false;
	public void setDetachedLabel(boolean f) {
		detachedLabel = f;
	}
	
	public boolean getDetachedLabel() {
		return detachedLabel ;
	}
	
	// boolean detatchedLabelOnly = false;
	public void setDetachedLabelOnly(boolean f) {
		detachedLabelOnly = f;
	}
	
	public boolean getDetachedLabelOnly() {
		return detachedLabelOnly ;
	}
	
	// vicarImageFileRecords
	public void setVicarImageFileRecords(int i) {
		vicarImageFileRecords = i;
	}
	
	public int getVicarImageFileRecords() {
		return vicarImageFileRecords;
	}
	
	public void setRecordLength(int rec) {
		recordLength = rec;
	}
	
	public int getRecordLength() {
		return recordLength ;
	}
	
	
	public void setVicarLabelRecordCt(int ct) {
		vicarLabelRecordCt = ct;
	}
	
	public int getVicarLabelRecordCt() {
		return vicarLabelRecordCt ;
	}
	// these would be used when vicar and label only is true
	// int recordLength = 0;
	// int vicarLabelRecordCt = 0;
	
	public void setUsePIRL(boolean f) {
		usePIRL = f;
	}
	
	public boolean getUsePIRL() {
		return usePIRL ;
	}
	
	public String getPds_ptr () {
		return pds_ptr;
	}
	public void setPds_ptr (String _pds_ptr) {
		 pds_ptr = _pds_ptr;
	}
	
	public boolean getTranscodeIIOmetadata () {
		return transcodeIIOmetadata;
	}
	public void setTranscodeIIOmetadata (boolean _transcodeIIOmetadata) {
		 transcodeIIOmetadata = _transcodeIIOmetadata;
	}
	
	public String getXslFileName () {
		return xslFileName;
	}
	public void setXslFileName (String _xslFileName) {
		 xslFileName = _xslFileName;
	}
	
	public String getVicarLabelString() {
		return vicarLabelString;
	}
	public void setVicarLabelString(String _vicarLabelString) {
		vicarLabelString =  _vicarLabelString;
	}
	
	// useful for creating an emebeded vicar label
	public VicarLabel getVicarLabel() {
		return vicarLabel;
	}
	public void setVicarLabel(VicarLabel _vicarLabel) {
		vicarLabel = _vicarLabel;
	}
	
	public void setDebug(boolean _debug) {
		debug = _debug;
	}
	public boolean getDebug() {
		return debug ;
	}

	public void setDirty(boolean d) {
		dirty = d;
	}
	
	public boolean getDirty() {
		return dirty ;
	}
	
		
	// boolean dataFileIsVicarImage = false;
	public void setDataFileIsVicarImage(boolean z) {
		dataFileIsVicarImage = z;
	}
	public boolean getDataFileIsVicarImage() {
		return dataFileIsVicarImage;
	}
	

	public void setDataFilename(String name) {
		dataFilename = name;
	}
	
	public String getDataFilename() {
		return dataFilename;
	}
	
	/**
	 * set the value for the vicar image label size in bytes
	 * Will be set by VicarInputFile when this metadata object is created
	 * @param size
	 */
	public void setVicarPixelSize(int size) {
		vicarPixelSize = size;
	}
	/**
	 * get the value for the vicar image label size in bytes
	 * may be useful for creating PDS detached labels
	 * @return
	 */
	public int getVicarPixelSize() {
		return vicarPixelSize ;
	}
	
	
	/**
	 * set the value for the vicar image label size in bytes
	 * Will be set by VicarInputFile when this metadata object is created
	 * @param size
	 */
	public void setVicarFormat(String format) {
		vicarFormat = format;
	}
	/**
	 * get the value for the vicar image label size in bytes
	 * may be useful for creating PDS detached labels
	 * @return
	 */
	public String getVicarFormat() {
		return vicarFormat ;
	}
	
	/**
	 * set the value for the vicar image label size in bytes
	 * Will be set by VicarInputFile when this metadata object is created
	 * @param size
	 */
	public void setVicarIntFmt(String format) {
		vicarIntFmt = format;
	}
	/**
	 * get the value for the vicar image label size in bytes
	 * may be useful for creating PDS detached labels
	 * @return
	 */
	public String getVicarIntFmt() {
		return vicarIntFmt ;
	}
	
	/**
	 * set the value for the vicar image label size in bytes
	 * Will be set by VicarInputFile when this metadata object is created
	 * @param size
	 */
	public void setVicarRealFmt(String format) {
		vicarRealFmt = format;
	}
	/**
	 * get the value for the vicar image label size in bytes
	 * may be useful for creating PDS detached labels
	 * @return
	 */
	public String getVicarRealFmt() {
		return vicarRealFmt ;
	}
	
	/**
	 * get a String for the PDS SAMPLE_TYPE based on the values set in
	 * int  vicarPixelSize = 0;
    String vicarFormat = null;
    String vicarIntFmt = null;
    String vicarRealFmt = null;
	 * @param _embed
	 */
	public String getPDS_SAMPLE_TYPE() {
	// vicarPixelSize = 0;
    String SAMPLE_TYPE = null;
    
		if (vicarFormat == null) {
			return SAMPLE_TYPE;
		} else if (vicarFormat.equalsIgnoreCase("BYTE")) {
			
		} else if (vicarFormat.equalsIgnoreCase("HALF")) {
			
		} else if (vicarFormat.equalsIgnoreCase("FULL")) {
			
		} else if (vicarFormat.equalsIgnoreCase("REAL")) {
			
		} else if (vicarFormat.equalsIgnoreCase("DOUB")) {
		}
			
		
		
	return SAMPLE_TYPE;
    // String vicarIntFmt = null;
    // String vicarRealFmt = null;
	}
	
	/**
	 * get a String for the PDS SAMPLE_TYPE based on the values set in
	 * int  vicarPixelSize = 0;
    String vicarFormat = null;
    String vicarIntFmt = null;
    String vicarRealFmt = null;
	 * @param _embed
	 */
	public int getPDS_SAMPLE_BITS() {
	// vicarPixelSize = 0;
    int SAMPLE_BITS = 0;
    
		if (vicarFormat == null) {
			return SAMPLE_BITS;
		} else if (vicarFormat.equalsIgnoreCase("BYTE")) {
			
		} else if (vicarFormat.equalsIgnoreCase("HALF")) {
			
		} else if (vicarFormat.equalsIgnoreCase("FULL")) {
			
		} else if (vicarFormat.equalsIgnoreCase("REAL")) {
			
		} else if (vicarFormat.equalsIgnoreCase("DOUB")) {
		}
			
		
		
	return SAMPLE_BITS;
    // String vicarIntFmt = null;
    // String vicarRealFmt = null;
	}
	// ---------------------------------
	
		
	// the VicarLabel must also exist ?? We could still make a basici label from RenderedImage
	 public void setEmbedVicarLabel(boolean _embed) {
		embedVicarLabel = _embed;
	}
	public boolean getEmbedVicarLabel() {
		return embedVicarLabel ;
	}
	
	public void setOutputXML(boolean xml) {
		outputXML =  xml;
	}
	public boolean getOutputXML() {
		return outputXML ;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("PDSImageWriteParam\n");
		

	sb.append("outputFileName = "+outputFileName+"\n");
	sb.append("transcodeIIOmetadata ="+ transcodeIIOmetadata+"\n");
	sb.append("xslFileName = "+xslFileName+"\n");
	// String vicarLabelString = null ; // a String containing a complete vicar label read from the input file
	// useful for creating an emebeded vicar label
	// VicarLabel vicarLabel = null; 
	sb.append("embedVicarLabel = "+embedVicarLabel+"\n");
	
	sb.append("debug = "+debug+"\n");
	sb.append("outputXML = "+outputXML+"\n");
	
	sb.append("dirty = "+dirty+"\n");
	sb.append("addMerItems = "+addMerItems+"\n");
	sb.append("addStatistics = "+addStatistics+"\n");
    sb.append("calculateStatistics = "+calculateStatistics+"\n");
	sb.append("addLinePrefix = "+addLinePrefix+"\n");
	sb.append("addBinaryHeader = "+addBinaryHeader+"\n");
	
//	 PDS detatched label support
	boolean detatchedLabel = false;
	boolean detatchedLabelOnly = false;
	boolean dataFileIsVicarImage = false;
	String dataFilename = null;
	String detatchedLabelFilename = null;

	return sb.toString();
	}
}
