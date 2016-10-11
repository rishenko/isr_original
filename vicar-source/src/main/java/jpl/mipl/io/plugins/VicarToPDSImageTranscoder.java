/*
 * @(#)VicarToPDSImageTranscoder.java	1.0 00/08/30
 *
  * Steve Levoe JPL/NASA
 */


package jpl.mipl.io.plugins;

import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageReader;
import javax.imageio.ImageTranscoder;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.*;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import org.w3c.dom.*; // DOM, Document, Node

// import VicarIO stuff
// VicarInputFile  SystemLabel
import jpl.mipl.io.streams.*;
import jpl.mipl.io.vicar.*;
// import jpl.mipl.io.plugins.*;
import jpl.mipl.io.util.*;

import jpl.mipl.io.*;

import java.net.*;

/**
 * @version 1.0
 * Controls conversion of metadata in a Document from a Vicar Image to a Document in a format
 * understood by the PDSImageWriter. An XSL script is used to transform the Document.
 * Parameters are passed in using an ImageWriteParam. Currently the transcoder doesn't do anything with
 * the generic parameters passed by ImageWriteParam. The subclass PDSWriteParam does pass parameters which control
 * the operation of the transcoder. A String refering to the xsl script may be passed in and it will be used for the transform,
 * If no xsl script file name is passed  xsl script is used a default xsl script will be used from the jar file.
 */
public class VicarToPDSImageTranscoder implements ImageTranscoder {

	String xslFromJar = "jpl/mipl/io/xsl/VicarToPDSmer1.xsl"; // name of the default xsl script stored in the same jar as this class
	
	boolean debug = false; // controls debug printing 
	
    // Constructor
    public VicarToPDSImageTranscoder() {
        
    }
    
    /**
    *
    * Everything is held in the image Metadata, stream Metadata is null
    *
    **/
    public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
        return null;
    }
    
    
    /*
     *  Convert Image Metadata from a Vicar image to the Inmage Metadata format expected
     * by a the PDSImageWriter. 
     * @param IIOMetadata - The VicarMetadata from a reader <br>
     * @param ImageTypeSpecifier - can be NULL, not currently used
     * @param ImageWriteParam - if it is an instanceof a PDSImageWriteParam then a few fields from it will be used to control
     * transcoding.<br>
     * PDSImageWriteParam.getXslFileName() provides the XSL file used to perform the transform of the metadata. <br>
     * PDSImageWriteParam.getDebug() supplies a boolean flag to control debug printing. <br>
     * PDSImageWriteParam.getOutputXML() supplies a boolean flag to control output of intermediate XML 
     * text file for debug purposes. <br>
     * PDSImageWriteParam.getOutputFileName() supplies the name of the PDS output file. This file name is used as 
     * the base name for the XML text files output by this methid.<br>
     * PDSImageWriteParam.getDirty() supplies a flag indicating if the image's data has been modified
     */
    public IIOMetadata convertImageMetadata( IIOMetadata inData,
        ImageTypeSpecifier imageType, ImageWriteParam imageWriteParam) {
            
        /** look at ImageType ??? do I care???
        // imageType.getColorModel()
        
        // imageType.getSampleModel()
        // use this to find out about the image.
        // fill in the image specific items
        **/
        
        boolean doTranscode = false;
        boolean dirty = false ; 
        // indicates the image's data has been modified
        String xslFile = null;
        String outputFilename = null; 

        boolean outputXML = false;
        PDSImageWriteParam pdsWriteParam = null;
        PDSMetadata pm = null;
        InputStream xslInputStream = null;
        boolean addLinePrefix = false;
        
        if ( imageWriteParam instanceof PDSImageWriteParam ) {
        	if (debug) 
        		System.out.println("VicarToPDSImageTranscoder.convertImageMetadata() "+imageWriteParam);
        	
        	pdsWriteParam = (PDSImageWriteParam) imageWriteParam ;
        	doTranscode = pdsWriteParam.getTranscodeIIOmetadata() ;  
        	outputFilename = pdsWriteParam.getOutputFileName();
        	xslFile = pdsWriteParam.getXslFileName();
        	debug = pdsWriteParam.getDebug();
        	outputXML = pdsWriteParam.getOutputXML();
        	dirty = pdsWriteParam.getDirty(); 	
        	addLinePrefix = pdsWriteParam.getAddLinePrefix();
        }
        
        if (debug) System.out.println("xslFile ="+xslFile);
        if (xslFile == null || xslFile.equals("-") )  {
        	// get one from the jar file
        	 xslInputStream = InputStreamFromJar(xslFromJar);
        	 
        }
        
         if (debug)  {
         	System.out.println("VicarToPDSImageTranscoder.convertImageMetadata()");
         	System.out.println("outputFilename="+outputFilename);
         	System.out.println("xslFile="+xslFile);
         	System.out.println("xslFromJar="+xslFromJar );
			System.out.println("xslInputStream "+xslInputStream);
         	System.out.println("debug="+debug);
         	System.out.println("dirty="+dirty);
         	System.out.println("outputXML="+outputXML);
			System.out.println("addLinePrefix="+addLinePrefix);
         }
        
     
     if (inData instanceof VicarMetadata) {
     		// should be, but check just in case
            if (debug) System.out.println("VicarToPDSImageTranscoder.convertImageMetadata()");
             
            VicarMetadata vm = (VicarMetadata) inData;
            VicarLabel vicarLabel = null;
            
            
            
            	String xPath = "";
            	DOMutils domUtil = new DOMutils();
            	String xmlFile1 = outputFilename+".tr.1.xml";
            	String xmlFile2 = outputFilename+".tr.2.xml";
            	String xmlFile3 = outputFilename+".tr.3.xml";
            	// use the VicarMetadata and transform it to pdsMetadata using the xsl file
            	// String nativeIm = vm.nativeImageMetadataFormatName;
				String nativeIm = vm.getNativeMetadataFormatName();
            	Node doc1 =  vm.getAsTree(nativeIm) ;  // getNativeTree();
            	Document doc2 = null;
            	if (doc1 == null) {
            		System.out.println("vicarMetadata document returned null, metadata transform failed");
            		return null;
            	}
            	else {
            		if (debug) System.out.println("doc1 "+doc1);
            		if (outputXML) domUtil.serializeNode(doc1,xmlFile1,"xml");
            		if (debug) System.out.println("--- transform vicar metadata to PDS with "+xslFile);
            		if (xslFile != null && xslFile.equals("-") == false) {
        				doc2 = domUtil.transformDocument(doc1, xslFile);
        				if (debug) System.out.println("--- transform succeeded using "+xslFile);
            		}
            		else if (xslInputStream != null) {
            			if (debug) {
            				System.out.println("--- transform ====== xslInputStream =========");
            			}
            			doc2 = domUtil.transformDocument(doc1, xslInputStream);
        				if (debug) System.out.println("--- transform succeeded using default transform "+xslFromJar);
            		}
            	}
        		if (doc2 != null) {
        			if (outputXML) domUtil.serializeDocument(doc2, xmlFile2, "xml");
        			if (debug) System.out.println("--- serialize to "+xmlFile2);
        			// dirty = true; // just for test
        			if (dirty) {
        				// remove OBJECT IMAGE from doc2
        				xPath = "//OBJECT[@name='IMAGE']"; // "//OBJECT[@key='IMAGE']""
        				if (debug) System.out.println("Image is dirty, remove "+xPath);  
        				boolean success = domUtil.deleteNode(doc2, xPath); 
        				// if (debug) 
        				xPath = "//OBJECT[@name='IMAGE_DATA']"; // "//OBJECT[@key='IMAGE_DATA']""
        				if (debug) System.out.println("Image is dirty, remove "+xPath);  
        				success = domUtil.deleteNode(doc2, xPath);   				       				
        			}
        			
        			/*
        			 * remove Object IMAGE_HEADER
        			 * This OBJECT will be created in the output for the specific image
        			 */
        			xPath = "//OBJECT[@name='IMAGE_HEADER']";       			
        			boolean success = domUtil.deleteNode(doc2, xPath); 
        			if (debug) 
        				System.out.println("remove "+xPath+ "  "+success);  
        			
        			xPath = "//object[@name='IMAGE_HEADER']";       			
        			success = domUtil.deleteNode(doc2, xPath); 
        			if (debug) 
        				System.out.println("remove "+xPath+ "  "+success);  
        			
        			xPath = "//GROUP[@name='IMAGE_HEADER']";      			 
        			success = domUtil.deleteNode(doc2, xPath); 
        			if (debug) 
        				System.out.println("remove "+xPath+ "  "+success);  
        			
        			xPath = "//group[@name='IMAGE_HEADER']";      			 
        			success = domUtil.deleteNode(doc2, xPath); 
        			if (debug) 
        				System.out.println("remove "+xPath+ "  "+success); 	
        			
        			
        			pm = new PDSMetadata(doc2); // even if it is NULL still use it ???
        			
					if (debug) System.out.println("transform succeeded");
					
					VicarBinaryLinePrefix vicarBinaryLinePrefix = vm.getVicarBinaryLinePrefix();
					if (debug) {
						 System.out.println("Transcoder: VicarBinaryLinePrefix "+vicarBinaryLinePrefix );
						 System.out.println("   addLinePrefix "+addLinePrefix );
					}
					
							if (addLinePrefix == true && vicarBinaryLinePrefix != null)	{
								if (debug) System.out.println("VicarBinaryLinePrefix transfered ");
								pm.setVicarBinaryLinePrefix(vicarBinaryLinePrefix);
							}
        			
        		}
        		else {
        			 // even if it is NULL still use it ???
        			System.out.println("transform failed, no Document created");
        		}
        	} 
        	else {
        		System.out.println("Vicar to PDS metadata transform failed. incompatable input");
        	}
        	
		
        	
        return pm; // the transformed PDS metadata
        
    }
    
    /*
     * returns the name of the default xsl file stored in the jar
     * */
    public String getXslFromJar() {
    	return xslFromJar;
    }
  
  /* 
   * sets debug printing on or off
   */ 
   public void setDebug(boolean d) {
   	debug = d;
   }
   
   public boolean getDebug() {
   	return debug;
   }
   
   /* Returns an InputStream if the file name in the input arg is found.in a jar on the classpath.<br>
    * Used by the transcoder to get the default xsl script stored in the jar.<br>
    * The InputStream allows the file to be used like any other opened file.
    * */
    public InputStream InputStreamFromJar(String arg) {
    	if (debug) System.out.println(" InputStreamFromJar: "+arg);
    	// java.lang.ClassLoader
    	// ClassLoader cl = new ClassLoader();
    	// ClassLoader cl = this.getClass().getSystemClassLoader();
    	
    	InputStream is = null;
    	URL url;
    	boolean go = true;
    	
    	url = ClassLoader.getSystemResource(arg);
    	try {
    	is = url.openStream();
    	}
    	catch( IOException ioe) {
    		System.out.println(" InputStreamFromJar: IOException "+ioe);
    	}
    	
    	// if (go) 
    	return is;
    	
    	
    	
	} 
}