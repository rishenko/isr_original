/*
*
* @(#)ImageToPDS_DOM.java	1.0 00/12/15
 *
 * Steve Levoe
 * Jet Propulsion Laboratory
 * Multimission Image Processing Laboratory
 * 04-19-2001 ImageIO EA2 version
*
***************************************/
// package jpl.mipl.io.plugins.vicar;
// this file and DOMutils want to end up in the above package

package jpl.mipl.io.plugins;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.*;

import java.awt.image.*;

import javax.xml.parsers.*;

import java.io.IOException;
import java.util.*;

import javax.imageio.metadata.*;

// import VicarIO stuff
// VicarInputFile  SystemLabel
import jpl.mipl.io.streams.*;
import jpl.mipl.io.vicar.*;
import jpl.mipl.io.util.*;

/**
 * This class builds a DOM Document from a rendered image
 * <BR>
 * The System label type information is extracted from a RenderedImage.
 * This is the information any formats label needs to specify how
 * the data in the image is laid out in the file.
 * Each format will overide this class to produce a DOM with the keywords
 * specific to that format. This class will emit a generic DOM which is the
 * main class which should be overridden.
 * <br>
 * Add a way to put filename into this DOM ??
 *
 * user calls getDocument() to get the Document object build here
 */
public class ImageToPDS_DOM extends ImageToDOM {
    
    String myFormatName = "pds"; 
    String myLabelName = "PDS_LABEL";
    String myNativeMetadataFormatName = "jpl.mipl.io.plugins.vicar.pdsimage_1.0";
    
    // pds specific variables
    String band_storage_type = "BAND_SEQUENTIAL" ; // "SAMPLE_INTERLEAVED" BAND_SEQUENTIAL   LINE_INTERLEAVED
    // String band_storage_type = "SAMPLE_INTERLEAVED" ; 
    // "BIP" = "SAMPLE_INTERLEAVED"
    // "BSQ" = "BAND_SEQUENTIAL"   
    // "BIL" = "LINE_INTERLEAVED"
    // check the sampleModel to decide this (ignore for now)
    // get the class of the sample model to determine organization  
    // if (sm instanceof ComponentSampleModel) org = "BSQ";
    // only for color images
    String band_sequence = "(RED, GREEN, BLUE)";
    String sample_type = "UNSIGNED_INTEGER";
    
    // file_records isn't known until the file is written
    // that value is calculated when the label is actually written out
    int file_records = 0;
    
    int nbb = 0; // this is the number of binary prefix bytes per line
       
    PDSimageStatistics pdsImageStatistics = null;
    
    // String keyString = "name"; // or "key"""
    String keyString = "key" ;
    // String versions, calculate must set them ??? - it will use doubles internally ??
    
    
    /**
    public ImageToPDS_DOM (BufferedImage bi_) {
        // RenderedImage is an implementation of BufferedImage
        
        
        // super(bi_, myFormatName, myNativeMetadataFormatName);
        super(bi_);
        formatName = myFormatName;
        nativeMetadataFormatName = myNativeMetadataFormatName;
        labelName = myLabelName;
        
        if (debug)     
          System.out.println("ImageToDOM(BufferedImage "+formatName+" constructor");
        // getValues();
        // buildDom();
    }
    **/
	public ImageToPDS_DOM (RenderedImage ri_) {
		
		super(ri_);
        
		nbb = 0;
		formatName = myFormatName;
		nativeMetadataFormatName = myNativeMetadataFormatName;
		labelName = myLabelName;
        
		if (debug)     
			System.out.println("ImageToDOM(RenderedImage "+formatName+" constructor");
	}
	
    public ImageToPDS_DOM (RenderedImage ri_, int nbb_) {
        // RenderedImage is an implementation of BufferedImage
        
        
        // super(bi_, myFormatName, myNativeMetadataFormatName);
        super(ri_);
        
        nbb = nbb_;
        formatName = myFormatName;
        nativeMetadataFormatName = myNativeMetadataFormatName;
        labelName = myLabelName;
        
        if (debug)     
          System.out.println("ImageToDOM(RenderedImage "+formatName+" constructor");
        // getValues();
        // buildDom();
    }
    
    // ----------------------------------------------------------
    
    /** 
    * This is an overdide to calculate the number of bytes in a single
    * record based on information specific to the PDS format.<br>
    * This method is called from getValues()
    **/
    public int calcRecord_bytes() {
    
    
    // file_records depends on this value, however it isn't
    // calculated until the file is actually written out
    if (band_storage_type.equals("BAND_SEQUENTIAL") )  {
        record_bytes = width * bytes_per_sample + nbb;
        // file_records = (lines * bands) + label_records;
    }
    else if (band_storage_type.equals("LINE_INTERLEAVED") )  {
        record_bytes = width * bytes_per_sample + nbb;
        // file_records = (lines * bands) + label_records;
    }
    else if (band_storage_type.equals("SAMPLE_INTERLEAVED") )  {
        record_bytes = width * bytes_per_sample * bands; // + nbb ????
        // file_records = lines  + label_records;
    }
    
    if (debug) {
    	System.out.println("ImageToPDS_DOM.calcRecord_bytes "
    	     +band_storage_type+ "  "+record_bytes+"  "+bands+"  "+width); 
		System.out.println(" nbb="+nbb);
    }
    
    return record_bytes;
    
    // "SAMPLE_INTERLEAVED" BAND_SEQUENTIAL   LINE_INTERLEAVED
    // record_bytes = width * bytes_per_sample; 
    // FILE_RECORDS = (lines * bands) + label_records
    // if SAMPLE_INTERLEAVED
    // record_bytes = width * bytes_per_sample * bands;
    // FILE_RECORDS = lines  + label_records
    }
    
    
    public void setPDSimageStatistics(PDSimageStatistics imageStatistics) {
    	pdsImageStatistics = imageStatistics;
    }
    
    public PDSimageStatistics setPDSimageStatistics() {
    	return pdsImageStatistics ;
    }
    
    
    
    
    
    /**
    * This is the class each format MUST overide to construct a Document
    * with Elements specific to that format
    **/
    public void buildDom () 
    {
    if (debug) System.out.println("--------------- buildDom -------- ");
        
    // String formatStr = "NONE";
    
    // each format will have a test like this to get a format descriptor for their format
    // decide when  "MSB_UNSIGNED_INTEGER" "UNSIGNED_INTEGER" is appropriate
    // decide when  "MSB_INTEGER" is appropriate 
    // is MSB always correct since we are writing from java ??
    if (dataType == DataBuffer.TYPE_BYTE) formatStr = "UNSIGNED_INTEGER";
    if (dataType == DataBuffer.TYPE_SHORT) formatStr = "MSB_INTEGER";
    if (dataType == DataBuffer.TYPE_USHORT) formatStr = "MSB_UNSIGNED_INTEGER"; // ??? IS THIS CORRECT "UNSIGNED_INTEGER" ???"
    if (dataType == DataBuffer.TYPE_INT) formatStr = "MSB_INTEGER";
    if (dataType == DataBuffer.TYPE_FLOAT) formatStr = "IEEE_REAL"; 
    if (dataType == DataBuffer.TYPE_DOUBLE) formatStr = "IEEE_REAL";
    // SAMPLE_BITS will be 32 or 64 to distinguish double or float
    
    sample_type = formatStr;
    // CHECK TO SEE IF SAMPLE_BITS IS CONSISTENT WITH THE DATA TYPE
    
    // String band_storage_type = "SAMPLE_INTERLEAVED"; // BAND_SEQUENTIAL   LINE_INTERLEAVED
    band_storage_type = "BAND_SEQUENTIAL" ; // "SAMPLE_INTERLEAVED" BAND_SEQUENTIAL   LINE_INTERLEAVED
    // "BIP" = "SAMPLE_INTERLEAVED"
    // "BSQ" = "BAND_SEQUENTIAL"   
    // "BIL" = "LINE_INTERLEAVED"
    // check the sampleModel to decide this (ignore for now)
    // get the class of the sample model to determine organization  
    // if (sm instanceof ComponentSampleModel) org = "BSQ";
    // only for color images
    band_sequence = "(RED, GREEN, BLUE)";
    
    if (debug) System.out.println("--------------- buildDom -------- 2");
    /**
    String formatStr
    int dataType
    int width
    int height
    int bands ;
    int[] sampleSize ; // in bits 
    int b0size ; // same as sampleSize[0]
    String sampleModelClassName = null;
    String colorModelClassName
    
    * <SYSTEM_LABEL NAME="pds">
    *   <dataType>dataType</dataType>
    *   <formatString> </formatString>
    *   <width> </width>
    *   <height>
    *   <bands>
    *   <sampleSize band="0">
    *   *** one for each band ???
    *   <sampleModelName>
    *   <colorModelName>
    * </SYSTEM_LABEL>
    **/
    
    /******* 
     * Now do the real construction of the PDS Document.
     * All of the values used to create the PDS specific Document are 
     * derived in the getValues()method of the superclass ImageToDOM
     * when the class is constructed.
     **/
    try {
          // DocumentBuilder builder = factory.newDocumentBuilder();
          // document = builder.newDocument();  // Create from whole cloth
          // look at DOMUtils. creat the Document in the same way
          // them we know it will work with the serializer, XPath, XSL tools
          // probably we should ALWAYS get5 new Documents from DOMUtils
          
          DOMutils domUtils = new DOMutils();
          _document = domUtils.getNewDocument();
          
          
          // this document already has _documentName in it as its root Element
          
          /***
          // for now till DOMutils is in the right place
          _document = new org.apache.xerces.dom.DocumentImpl();
          
          Class c = _document.getClass();
          _documentName = c.getName();
          
          Element documentNameNode = (Element) _document.createElement(_documentName); 
          _document.appendChild (documentNameNode);
          *************/
          // ----------------------------
          
          Element root = (Element) _document.createElement(nativeMetadataFormatName); 
          
          // documentNameNode.appendChild(root);
          _document.appendChild (root);
          if (debug) System.out.println("--------------- buildDom -------- 3");
          /**
          Element formatNode = _document.createElement(formatName);          
          root.appendChild (formatNode);
          **/
          // OR
          // <SYSTEM_LABEL name="generic">
          /***
          Element system = _document.createElement("SYSTEM_LABEL");
          system.setAttribute("name", formatName);
          root.appendChild (system);
          **/
          Element system = _document.createElement("PDS_LABEL");
          // system.setAttribute("format", formatName);
          // system.setAttribute("type", "SYSTEM");
          root.appendChild (system);
          
          // put everything inside system
          // this node can be extracted later and merged with a Document with the SAME
          // nativeMetadataFormatName
          Element item;
          String value;
          Text text; // this is Node's "value" Element
          // <PDS_VERSION_ID>PDS3</PDS_VERSION_ID> or <PDS3>PDS_VERSION_ID</PDS3>
          item = (Element) _document.createElement("PDS_VERSION_ID"); 
	      text = (Text) _document.createTextNode("PDS3");
	      item.appendChild(text);
          system.appendChild(item);
          
          
          // outputFilename
          if (filename != null) {
            item = (Element) _document.createElement("OUTPUT_FILENAME"); 
	        text = (Text) _document.createTextNode(filename);
	        item.appendChild(text);
            system.appendChild(item);
          }
          
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "RECORD_TYPE"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode("FIXED_LENGTH");
	      item.appendChild(text);
          system.appendChild(item);
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "RECORD_BYTES"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(""+record_bytes);
	      item.appendChild(text);
          system.appendChild(item);
       
          // this is a really a place holder, file_records will be calculated when the 
          // file is written out
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "FILE_RECORDS"); 
          item.setAttribute("quoted", "false");
	      text = (Text) _document.createTextNode("("+height+"*BANDS)+LABEL_RECORDS");
	      item.appendChild(text);
          system.appendChild(item);
          
          // this is also really a place holder, label_records will be calculated when the 
          // file is written out
          item = (Element) _document.createElement("IMAGE_START_RECORD"); 
          
          // item.setAttribute(keyString, "IMAGE"); 
          // item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode("LABEL_RECORDS+1");
	      item.appendChild(text);
          system.appendChild(item);
          
          // leave this comment out
          // item = (Element) _document.createElement("comment"); 
	      // text = (Text) _document.createTextNode("/* this is the IMAGE object description */");
	      // item.appendChild(text);
          // system.appendChild(item);
          
          String objectStr = "OBJECT"; // object"
          Element object = (Element) _document.createElement(objectStr); 
          // "^IMAGE"  ^ is an illegeal charater for an element name
          // may need to go to something else if IMAGE is used elsewhere
          // perhaps IMAGE_OBJECT
	      // object.setAttribute("name", "IMAGE_DATA"); 
	      object.setAttribute("name", "IMAGE");
          system.appendChild(object);
          
          // put all these items in the "^IMAGE" object
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "INTERCHANGE_FORMAT"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode("BINARY");
	      item.appendChild(text);
          object.appendChild(item);
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "LINES"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(""+height);
	      item.appendChild(text);
          object.appendChild(item);
          
          if (nbb != 0) {
          
		    item = (Element) _document.createElement("item"); 
		    item.setAttribute(keyString, "LINE_PREFIX_BYTES"); 
			item.setAttribute("quoted", "false"); 
			text = (Text) _document.createTextNode(""+nbb);
			item.appendChild(text);
			object.appendChild(item);
          }
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "LINE_SAMPLES"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(""+width);
	      item.appendChild(text);
          object.appendChild(item);
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "SAMPLE_TYPE"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(sample_type);
	      item.appendChild(text);
          object.appendChild(item);
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "SAMPLE_BITS"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(""+sampleSize[0]);
	      item.appendChild(text);
          object.appendChild(item);
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "BANDS"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(""+bands);
	      item.appendChild(text);
          object.appendChild(item);
          
          /** MER sis doen't include this item **           
          if (bands == 3) {
            item = (Element) _document.createElement("item"); 
            item.setAttribute(keyString, "STORAGE_SEQUENCE"); 
            item.setAttribute("quoted", "true"); 
	        text = (Text) _document.createTextNode(band_sequence);
	        item.appendChild(text);
            object.appendChild(item);
          }
          ************************************************/
          
          item = (Element) _document.createElement("item"); 
          item.setAttribute(keyString, "BAND_STORAGE_TYPE"); 
          item.setAttribute("quoted", "false"); 
	      text = (Text) _document.createTextNode(band_storage_type);
	      item.appendChild(text);
          object.appendChild(item);
          
          
          if (pdsImageStatistics != null) {         		
          		pdsImageStatistics.addItems(_document, object, keyString);
          }
          
          
          
          
          
          
        } catch (Exception e) {
            // Parser with specified options can't be built
            System.out.println("ImageToPDS_DOM.buildDOM() Exception "+ e );
            e.printStackTrace();

        }
        
        if (debug) {
        	System.out.println("--------------- buildDom -------- 5");
        	System.out.println("RenderedImageToDOM.buildDOM() ");
        }
        /***
        catch (ParserConfigurationException pce) {
            System.out.println("buildDocument ParserConfigurationException "+ pce );
        }
        catch (IOException ioe) {
            System.out.println("buildDocument IOException "+ ioe );
        }
        catch (SAXException saxe) {
            System.out.println("buildDocument SAXException "+ saxe );
        }
        ****/
    
    } // buildDom
  
  // from VicarLabel.java    
    public String toString()
    {
	    return "ImageToPDS_DOM.toString()";
    }
    


}