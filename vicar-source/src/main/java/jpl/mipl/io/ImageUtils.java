
/*
 *  ImageUtils.java
*
*  @version 1.1 10-16-06
*
* @author Steve Levoe NASA/JPL
* 
* ImageIO image read/write utilities
* <p>
* This class contains useful utilities to work with image files.
* In general to use these utilities on must first create an instance
* of the class woth a file name in the constructor. Then methods on the class may be called.
* The methods of this class make it possible to read and image file, format it 
* for display, display the image in a Frame and write the image to a file.
* If the class is created with a file name then methods can be called to retrieve,
* display it, and write the image to a new file.
* There are also methods to display metadata and create a java Hashtable
* from the image metadata.
* There are a number of methods which may be called on a RenderedImage to change the 
* format of the image data to allow it to be displayed properly. Most standard image 
* formats (jpg, png, gif) require 8 bit data. Use of 16 bit data relies on the ImageIO 
* plugin available. Some tif and png plugins can write 16bit data.
* * <p>
* 6-14-07 Steve Levoe
* setFileName() contained an error which caused the file path to be incorrectly set.
* Errors in reading images now throw IOExceptions. They used to cause and exit(0).
* The user must catch and do something with the exception. In many cases the exception
*  can be noted and the program can continue.
****/


package jpl.mipl.io;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

import jpl.mipl.io.plugins.IIOMetadataToDOM;
import jpl.mipl.io.plugins.VicarMetadata;
import jpl.mipl.io.plugins.VicarRenderedImage;
import jpl.mipl.io.util.DOMtoHashtable;
import jpl.mipl.io.util.DOMutils;
import jpl.mipl.io.vicar.VicarLabel;
import jpl.mipl.io.vicar.VicarLabelCategory;
import jpl.mipl.io.vicar.VicarLabelItem;
import jpl.mipl.io.vicar.VicarLabelSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
// import jpl.mipl.io.codec.*;
// import java.lang.Package ;


public class ImageUtils {
	
	RenderedImage renderedImage = null;
	BufferedImage bufferedImage = null;
	
	boolean debug = false;
	// boolean debug = true; // false;
	
	boolean getAsRenderedImage = false;
	boolean formatToByte = false;
	String inputFileName = "";
	String imageFormatName = "";
	
	IIOImage iioImage = null;
	
	
	
	
	// constructors
	public ImageUtils() {
	}
	
	public ImageUtils(String filename) {
		setFileName(filename);
	}
	
	public ImageUtils(File file) {
		setFileName(file);
		
		
	}
	
	/**
	 * If this value is set to TRUE before fullRead() is called the 
	 * images data will be converted to BYTE. This is a simple convenience to obtain 
	 * an easily displayable BYTE image that should have a decent stretch applied.<br>
	 * The extreme of the image will be taken and the image scaled correctly to fit 
	 * to byte. If this flag is false the data will be returned in its native data type.
	 * The user can apply any JAI Operators to it.
	 * @param b
	 */
	public void setFormatToByte( boolean b) {
		formatToByte = b;
	}
	
	/**
	 *  returns the state of the formatToBuyte flag
	 * 
	 */
	public boolean getFormatToByte( ) {
		return formatToByte;
	}
	
	/**
	 * returns the format name as returned by the ImageIO Reader used to read this image
	 * @return String
	 */
	public String getImageFormatName() {
		return imageFormatName;
	}
	
	
	
	/**
	 * Returns the file name that has been set
	 */
	public String getFileName() {
		return inputFileName;
	}
	
	/**
	 * Sets the name of the file to read.
	 * Does not cause the file to be read. 
	 * Call fullRead() to cause the file to be read.
	 * Not that the getAsRenderedImage flag affects how the image will be read.
	 * Then use other methods to obtain the image, metadata.
	 * @param filename
	 */
	public void setFileName(String filename) {
		setFileName(new File(filename));
		// now read in the image ?
	}
	
	/**
	 * Sets the name of the file to read.
	 * Does not cause the file to be read. 
	 * Call fullRead() to cause the file to be read.
	 * Note that the getAsRenderedImage flag affects how the image will be read.
	 * Then use other methods to obtain the image, metadata.
	 * @param file object
	 */
	public void setFileName(File f) {
		String name = f.getName();
		String path = f.getAbsolutePath();
		if (debug) System.out.println(" name="+name +"   path="+path);
		inputFileName = path;
		// now read in the image ?
	}
	
	/**
	 * Nothing implemented yet. Placeholder for a method to easily get values out of the
	 * images label if we have a Reader which has access to the Image label (Metadata)
	 * ISIS, VICAR and PDS should be doable.
	 * Always returns a null value.
	 * @param key
	 * @return String
	 */
	public String getParameter(String key) {
		String value = null;
		return value;
	}
	
	/**
	 * Nothing implemented yet. Placeholder for methods to easily get values out of the
	 * images label if we have a Reader which has access to the Image label (Metadata)
	 * ISIS, VICAR and PDS should be doable.
	 * Always returns a null value.
	 * @param propertyName find a property (may also work for "group" and "object" etc)
	 * @param key keyword to find a value for
	 * @return String
	 */
	public String getPropertyValue(String propertyName, String key) {
		String value = null;
		return value;
	}
	
	/**
	 * Get the renderedImage will only have data if getAsRenderedImage 
	 * is set to TRUE and fullRead has been called() first.
	 * A null return is valid. It means the file was not read.
	 * @return RenderedImage
	 */
	public RenderedImage getRenderedImage() {
		
			return renderedImage;
		
	}
	
	/**
	 * Get the bufferedImage will only have data if getAsRenderedImage 
	 * is set to FALSE and fullRead has been called() first. A BufferedImage
	 * is a displayable image. Most users will want this.
	 * A null return is valid. It means the file was not read.
	 * @return BufferedImage
	 */
	public BufferedImage getBufferedImage() {
		
		return bufferedImage;
	}
	
	/**
	 * Reformats the image to BYTE data and returns it.
	 * This method assumes the image was already read by calling fullRead().
	 * This method is useful be sure the data is displayable. To write a jpeg,
	 * gif, tif or png the data should be byte.
	 * @return RenderedImage
	 */
	public RenderedImage getFormatedRenderedImage() {
		// read it if necessary
			RenderedImage cim = null;
			int newDataType = DataBuffer.TYPE_BYTE;
			if (bufferedImage != null) {
				cim = processFormat(bufferedImage, newDataType);
				return cim;
			} 
			else if (renderedImage != null) {
				cim = processFormat(renderedImage, newDataType);
				return cim;
			}
			else 
				return cim;
		
	}
	/**
	 * Get the the image which was loaded by a call to fullRead().
	 * 
	 * @return RenderedImage since BufferedImage implements RenderedImage
	 * The returned image may be either. Null will be returned if the image
	 *  hasn't been read yet or an error was encountered.
	 */
	public RenderedImage getImage() {
		// see what we have and return it
		if (renderedImage != null) {
			return renderedImage;
		} 
		else if (bufferedImage != null) {
			return (RenderedImage) bufferedImage;
		}
		else {
			return null;
		}
		
	}
	
	/**
	 * Sets debug printing on or off
	 * @param b
	 */
	public void setDebug(boolean b) {
		debug = b;
	}
	
	/**
	 * Flag to request reader to return the image as a RenderedImage. 
	 * The default is to return a BufferedImage which is displayable.
	 * Multispectral images should be returned as RenderedImage.  
	 * Then the user will need to extract the bands they wish to display and create
	 * their own BufferedImage for display.
	 * @param b
	 */
	public void setGetAsRenderedImage(boolean b) {
		getAsRenderedImage = b;
	}
	
	/**
	 * Get the value of the flag
	 * @return boolean
	 */
	public boolean getGetAsRenderedImage() {
		return getAsRenderedImage ;
	}
	
	


	public void doHash() {
		
		Hashtable vproperties = new Hashtable();
		Hashtable vtasks = new Hashtable();
		
		Hashtable properties = new Hashtable();
		Hashtable tasks = new Hashtable();
		String format = "";
		String[] formats ;
		
		//  get the metadata
        // VicarLabel vl = (VicarLabel)load_img.getProperty("vicar_label");
        IIOMetadata im = null;
	
		if (iioImage == null) {
			System.out.println("ImageUtils.doHash() iioImage is null");
			return;
		}
			
		im = iioImage.getMetadata();
		
            
        if (im != null) {
        	formats = im.getMetadataFormatNames();    
        	format = im.getNativeMetadataFormatName() ;// nativeImageMetadataFormatName = "VICAR_LABEL"; 
        	
        	if (debug) {
        	  System.out.println("getNativeMetadataFormatName() "+format); 
        	  for (int i=0 ; i< formats.length ;i++) {
        		System.out.println(" formats["+i+"] " +formats[i]); 
        	  }
        	}
        
        	if (format.equalsIgnoreCase("VICAR_LABEL")) {
        		VicarMetadata vm = (VicarMetadata)im;
        		VicarLabel vl = vm.getVicarLabel();
        		VicarLabelSet vlSys = vl.getSystem();
        		VicarLabelCategory vlProps = vl.getProperty();
        		VicarLabelCategory vlTasks = vl.getHistory();
        		populateHash ( vlSys, vproperties );
        		for ( int i = 0; i < vlProps.getNumSets(); i++ )  {
            		populateHash ( vlProps.getSet ( i ), vproperties );
        		}
        		for ( int i = 0; i < vlTasks.getNumSets(); i++ )  {
            		populateHash ( vlTasks.getSet ( i ), vtasks );
        		}
        	}
        	
        	if (debug) {
        	  printHash(vproperties, "vproperties");
        	  printHash(vtasks, "vtasks");
        	
        	  System.out.println("printHash new version vproperties");
        	  printHash(vproperties);
        	  System.out.println("printHash new version vtasks");
        	  printHash(vtasks);
        	  System.out.println("*****************************");
        	}
        }
	}

	public void printHash(Hashtable h, String s) {
		System.out.println("printHash "+s);
		System.out.println(" hash: "+h);
	}
	
    protected void populateHash ( VicarLabelSet vls, Hashtable which )  {
        String currHash, currKey;
        StringBuffer currElement;
        currHash = vls.getName();
        if ( currHash == null && vls.getFlavor() == VicarLabelSet.LABEL_SYSTEM )  {
            currHash = "SYSTEM";
        }
        which.put ( currHash, new Hashtable() );
        for ( int j = 0; j < vls.size(); j++ )  {
            VicarLabelItem vli = vls.getItem ( j );
            currKey = vli.getKeyword();
            if ( currKey.equals ( "PROPERTY" ) || currKey.equals ("TASK" ) ) {
                continue;
            }
            if ( vli.getNumElements() > 1 )  {
                currElement = new StringBuffer ( "(" );
                for ( int c = 0; c < vli.getNumElements(); c++ )  {
                    currElement.append ( vli.getString(c));
                    if ( c != vli.getNumElements()-1 )  {currElement.append ( "," );}
                }
                currElement.append ( ")" );
            }  else  {
                currElement = new StringBuffer ( vli.getString());
            }
            ((Hashtable) (which).get ( currHash )).put ( currKey, currElement.toString() );
        }
    }



/*************************************************
* Does a complete ImageIO read, gets the RenderedImage. <br>
* and Metadata which is returned in an IIOImage Object.
* The name of the file to read was supplied to the constructor
* @throws IOException - if the file can't be read
**************************************************/	
public IIOImage fullRead() throws IOException {
	if (inputFileName == null || inputFileName =="") {
		System.out.println("No image filename supplied: ");
		// could htrow an Exception
		return null;
	}
	else {
		iioImage = fullRead (inputFileName);
		return iioImage;
	}
	
	
}

/*************************************************
* Does a complete ImageIO read, gets the RenderedImage. <br>
* and Metadata which is returned in an IIOImage Object.
* IOExceptions asre not caught, Instead they must be caught by the caller
* @param String - the name of the file to read
* @throws IOException - if the file can't be read
**************************************************/

public IIOImage fullRead(String fileName) throws IOException {
	
	
	inputFileName = fileName;
	
	// we will need a File, URL and stream version eventually
    
    ImageInputStream iis = null;
    IIOMetadata im = null;
    IIOMetadata sm = null;
    ImageReader reader = null;
    String readerClassName = "" ;
    String readerFormat = "";
    
    int numImages = 0;
    
    if (debug) System.out.println("ImageUtil open: " + fileName);
        try {
            iis = ImageIO.createImageInputStream(new File(fileName));
        

            if (iis == null) {
                System.out.println("Unable to get a stream!");
                // return null ;  // error return
                // throw an exception !!! instead
                throw new IOException("ImageUtils.fullRead() Unable to get a stream!");
            }

            Iterator iter = ImageIO.getImageReaders(iis);
            
            while (iter.hasNext()) {
                reader = (ImageReader)iter.next();
                if (debug) System.out.println("Using " +
                               reader.getClass().getName() +
                               " to read.");
                readerClassName = reader.getClass().getName() ;
                // get the format we are reading
                readerFormat = reader.getFormatName();
                break;
            }

            if (reader == null) {
                System.err.println("Unable to find a reader!");
                // System.exit(1);  // error return
                // return null;
                throw new IOException("ImageUtils.fullRead() Unable to find a reader!");
            }

        	imageFormatName = readerFormat;
            reader.setInput(iis, true);
            
        
            numImages = 1;
            // numImages = reader.getNumImages(true);
            if (debug) {
            	System.out.println("\nThe file contains " + numImages + " image"
                           + (numImages == 1 ? "" : "s") + ".");
            	System.out.println();
            }

            sm = reader.getStreamMetadata();
        
        /**/ } catch (IOException ioe) {
            System.out.println("I/O exception");
            // System.exit(1);  // error return
            throw new IOException("ImageUtils.fullRead() exception reading stream metadata!");
        } /**/
        
        if (sm == null) {
            if (debug) System.out.println("The file contains no stream metadata.");
        } else {
            if (debug) System.out.println("has Stream metadata");
            // String nativeFormatName = sm.getNativeMetadataFormatName();
            // displayMetadata(sm.getAsTree(nativeFormatName));
        }

		// flag to decide if we get as BufferedImage or RenderedImage
		
		/*
		 * * for now user must set this explictly
		 */
		 /*
		if (readerFormat.equalsIgnoreCase("vicar") || readerFormat.equalsIgnoreCase("pds") ||
			readerFormat.equalsIgnoreCase("isis")) { 
			getAsRenderedImage = true;
		}
		else {
			getAsRenderedImage = false;
		}
		*/
		
		
		if (getAsRenderedImage) {
			if (debug) System.out.println("get as RenderedImage *************** "+readerFormat );
			try {
				ImageReadParam param = reader.getDefaultReadParam();
				// add something to it
     			renderedImage = reader.readAsRenderedImage(0, param);
     			if (renderedImage instanceof VicarRenderedImage ) {
     				VicarRenderedImage vri = (VicarRenderedImage) renderedImage;
     				vri.setTileWidth(vri.getWidth());
     			}
            } catch (IOException ioe) {
                System.out.println("I/O exception !");
                // System.exit(1);  // error return
                throw new IOException("ImageUtils.fullRead() exception getting rendered image!");
            }
			// for now try to create the bufferedImage using the RenderedImage
			
			// bufferedImage = 
		}
		else {


		 try {
     			bufferedImage = reader.read(0);
            } catch (IOException ioe) {
                System.out.println("I/O exception !");
                // System.exit(1);  // error return
                throw new IOException("ImageUtils.fullRead() exception reading buffered Image!");
                
            }
		}

		// this forces the reader to read in and store the metadata
            try {
                im = reader.getImageMetadata(0);
            } catch (IOException ioe) {
                System.out.println("I/O exception obtaining Image Metadata!");
                // this exception isn't alasys a problem. it is not forwarded
                // the metatadta will just be null
                // System.exit(0);
            }
            
            if (debug) {
            	if (im == null) {
                	System.out.println("\nThe file has no Image metadata.");
            	} else {
                	System.out.println("\nThe file contains Image metadata.");
            	}
            }
            
            if (bufferedImage != null) {
            	if (debug)
            		printImageInfo(bufferedImage, "fullRead.bufferedImage");
            
            	// 2nd argument is a List of thumbnails     
  	 			iioImage  = new IIOImage(bufferedImage, null, im);
            }
            else if (renderedImage != null) {
            	if (debug)
            		printImageInfo(renderedImage, "fullRead.renderedImage");
            
            	// 2nd argument is a List of thumbnails     
  	 			iioImage  = new IIOImage(renderedImage, null, im);
            }
            try {
            	iis.close();
            }
            catch (IOException cioe ) {
            	System.err.println (" Error closing IIS");
            }
   			return iioImage;
/***
        for (int i = 0; i < numImages; i++) {
            System.out.println("\n---------- Image #" + i + " ----------");
            System.out.println();

            try {
                int width = reader.getWidth(i);
                System.out.println("width = " + width);

                int height = reader.getHeight(i);
                System.out.println("height = " + height);

                int numThumbnails = reader.getNumThumbnails(i);
                System.out.println("numThumbnails = " + numThumbnails);

                for (int j = 0; i < numThumbnails; j++) {
                    System.out.println("  width = " +
                                   reader.getThumbnailWidth(i, j) + 
                                   ", height = " +
                                   reader.getThumbnailHeight(i, j));
                }

                // File ff = new File(f);
                // BufferedImage bufferedImage = ImageIO.read(f);
                bufferedImage = reader.read(0);
            } catch (IOException ioe) {
                System.out.println("I/O exception !");
                System.exit(0);
            }
            
            
            if (bufferedImage == null) {
                System.out.println(inputFileName + " - couldn't read!");
                // return;
            }

           
            System.out.println("\n ImageToDOM");
            ImageToDOM i2dom = new ImageToDOM ( bufferedImage );
            Document d = i2dom.getDocument();
            displayMetadata((Node) d);
            makeFrameDomEcho4((Node) d, bufferedImage );
            
            System.out.println("<**************************************>");
            
            System.out.println("\n ImageToPDS_DOM");
            ImageToPDS_DOM i2PDSdom = new ImageToPDS_DOM ( bufferedImage );
            Document d1 = i2PDSdom.getDocument();
            displayMetadata((Node) d1);
            makeFrameDomEcho4((Node) d1, bufferedImage );
            
            System.out.println("<**************************************>");
           
            
            // this forces the reader to read in and store the metadata
            try {
                im = reader.getImageMetadata(i);
            } catch (IOException ioe) {
                System.out.println("I/O exception obtaining Image Metadata!");
                // System.exit(0);
            }
            
            if (im == null) {
                System.out.println("\nThe file has no Image metadata.");
            } else {
                System.out.println("\nThe file contains Image metadata.");
            }
          
            else {
                System.out.println("\nImage metadata:");
                String nativeFormatName = im.getNativeMetadataFormatName();
                
                Node imNode = im.getAsTree(nativeFormatName);
                // this could be loaded into the DomEcho4
                
                System.out.println("ImageDumper.displayMetadata() >>>>>>");
                displayMetadata(imNode);
                System.out.println("<<<<<< ImageDumper.displayMetadata()");
                // makeFrameDomEcho4(d);
                // makeFrameDomEcho4(imNode);
                IIOImage iioImage = new IIOImage(bufferedImage, null, im);
                System.out.println("<**************************************>");
                String fname = f.getName();
                System.out.println(" ");
                // pass image AND stream metadata to the DomEcho
                // we pass it the image so it has the image to wriiten out to
                // a file
                makeFrameDomEcho4(iioImage, sm, fname);
                // makeFrameDomEcho4(imNode, bufferedImage);
                
            }
            
        }
        
        // 2nd argument is a List of thumbnails     
   iioImage  = new IIOImage(bufferedImage, null, im);
   return iioImage;
        ***/
        
   
}


   // ------- operators -------------------
    
    /**
    * Converts an image which is not displayable to one that is.
    * from mica
    **/
    public RenderedImage conditionImageToByte(RenderedImage imageOp_base ) {

	
	ParameterBlock PB;
	RenderedImage temp=imageOp_base;
	 SampleModel sm = temp.getSampleModel();
	 int newDataType = DataBuffer.TYPE_BYTE;
	 
	 int numbands = sm.getNumBands();
	 int type = sm.getDataType();
	 if (debug) System.out.println("conditionImageToByte DataType = "+type);
	 if (type == DataBuffer.TYPE_BYTE) {
	 	if (debug) System.out.println("conditionImage  Image is BYTE, no conditioning needed");
	 
	 	return temp;
	 } else {
	 	if (debug) System.out.println("conditionImage  Performing image conversions...");
	 }

	

	// rescale the pixel values of the image based on the image extrema
	PB=new ParameterBlock();
	PB.addSource(temp).add(null).add(10).add(10);
	RenderedImage extrema=JAI.create("extrema",PB);

	// scale all pixels by:   v1= m * v0 +b    (assuming one band per pixel)
	double scale[][]=(double[][])extrema.getProperty("extrema");
	
	// double ceiling = getMaxForDataType(newDataType) ;
	
	
	
	// double ceiling=Short.MAX_VALUE*2;
	double ceiling = getMaxForDataType(newDataType) ;
	double max=1,min=ceiling;
	for(int i=0;i<scale[0].length;i++){
	    max=Math.max(max,scale[1][i]);
	    min=Math.min(min,scale[0][i]);
	}
	if (debug) System.out.println("conditonImageToByte extrema ceiling="+ceiling+"  min="+min+"  max="+max);
	// round max up to the nearest power of 2. 
	// max=Math.pow(2.0,Math.round(Math.log(max)/Math.log(2)));
	// min=0;
	
	
	// this will be for BYTE output
	double constant[] = new double[numbands];
	double offset[] = new double[numbands];
	
	for(int i=0;i<scale[0].length;i++){
		constant[i] = ceiling /(max-min);
		if (min >= 0.0) {
			offset[i] = min * constant[0] * -1.0; // offset is added only for unsigned ??
		}
		else {
			offset[i] = min * constant[0] ; // offset is added only for unsigned ??
		}
	}
		
		
	if (debug) {
		System.out.println("conditionImage  min="+min+"  max="+max);
		System.out.println("conditionImage  ceiling="+ceiling+"  constant="+constant[0]+"  offset="+offset[0]);
	}
	
	PB=new ParameterBlock();
	// PB.addSource(temp).add(new double[]{ceiling/(max-min)}).add(new double[]{ceiling*min/(min-max)});
	PB.addSource(temp).add(constant).add(offset);
	temp=JAI.create("rescale",PB);
	
	
	PB=new ParameterBlock();
	
	// PB.addSource(temp).add(java.awt.image.DataBuffer.TYPE_BYTE);
	PB.addSource(temp).add(newDataType);
	temp=JAI.create("Format",PB);

	imageOp_base=temp;
	if (debug) System.out.println("Conversion complete.");
	return  imageOp_base ;
    }
 
 
    /**
     * Returns the integer constant from DataBuffer for a String of the data type.
     * Useful for converting a command line argument into something useable by
     * a method. Strings which are recognized are: "byte", "short", "ushort",
     * "half", "int", "float", "real", "double".
     * "short" and "half" are equivalent.
     * @param dataType
     * @return -1 indicates there is no data type found for the string argument.
     */
	public int getDataTypeInt(String dataType) {
		
    
				if ( dataType.equalsIgnoreCase("byte") ) {
					return  DataBuffer.TYPE_BYTE;
					}	      
				else if ( dataType.equalsIgnoreCase("short") ) {
					return DataBuffer.TYPE_SHORT; // VICAR IS HALF
					}
				else if ( dataType.equalsIgnoreCase("ushort") ) {
							return DataBuffer.TYPE_USHORT; // VICAR IS HALF
							}							
				else if ( dataType.equalsIgnoreCase("half") ) {
							return DataBuffer.TYPE_SHORT; 
							}
				else if ( dataType.equalsIgnoreCase("int") ) {
							return DataBuffer.TYPE_INT; 
							}
				else if ( dataType.equalsIgnoreCase("float") ) {
							return DataBuffer.TYPE_FLOAT;
						}
				else if ( dataType.equalsIgnoreCase("real") ) {
							return DataBuffer.TYPE_FLOAT; 
						}	
				else if ( dataType.equalsIgnoreCase("double") ) {
							return DataBuffer.TYPE_DOUBLE; 
						}	
												 
				
	           
	    
				else  {
					System.out.println("ERROR: INput format type "+ dataType+" IS unknown");
	        	return -1;
					}
	
	}
 
	/**
	 * Formats a RenderedImage to a new format.
	 * if rescaleMin == 0.0 &&  rescaleMax == 0.0 then the extrema (min, max) of the image 
	 * will be found and used to determine how the scale the data to the new format.
	 * 
	 * @param image
	 * @param newDataType a String. 
	 * trings which are recognized are: "byte", "short", "ushort",
     * "half", "int", "float", "real", "double".
     * "short" and "half" are equivalent.
	 * @param rescaleMin minimum value used for calculating the rescale factor
	 * @param rescaleMax maximum value used for calculating the rescale factor
	 * @return the processed image
	 */
	public RenderedImage processFormat(RenderedImage image, String newDataType, 
			double rescaleMin, double rescaleMax) {
			int dataType = getDataTypeInt(newDataType);
			if (dataType != -1) {
				return processFormat(image, dataType, rescaleMin, rescaleMax);
			} else {
		
			return image;
			}
		
		
		}
	
	/**
	 * Formats a RenderedImage to a new format.
	 * Convenience for: <br>
	 * RenderedImage processFormat(RenderedImage image, String newDataType, 
			double rescaleMin, double rescaleMax) ;
	 * sets rescaleMin = 0.0 and  rescaleMax = 0.0 
	 * 
	 * @param image
	 * @param newDataType a String. 
	 * trings which are recognized are: "byte", "short", "ushort",
     * "half", "int", "float", "real", "double".
     * "short" and "half" are equivalent.
	 * @param rescaleMin minimum value used for calculating the rescale factor
	 * @param rescaleMax maximum value used for calculating the rescale factor
	 * @return the processed image
	 */
	public RenderedImage processFormat(RenderedImage image, String newDataType) {
		int dataType = getDataTypeInt(newDataType);
		if (dataType != -1) {
			return processFormat(image, dataType, 0.0, 0.0);
		} else {
		
		return image;
		}
		
		
	}
	
	/**
	 * Formats a RenderedImage to a new format.
	 * Convenience for: <br>
	 * RenderedImage processFormat(RenderedImage image, String newDataType, 
			double rescaleMin, double rescaleMax) ;
	 * sets rescaleMin = 0.0 and  rescaleMax = 0.0 
	 * 
	 * @param image
	 * @param newDataType integer value of new data type. Must be a valid DataBuffer
	 * constant.
	 * @param rescaleMin minimum value used for calculating the rescale factor
	 * @param rescaleMax maximum value used for calculating the rescale factor
	 * @return the processed image
	 */
	public RenderedImage processFormat(RenderedImage image, int newDataType) {
		return processFormat(image, newDataType, 0.0, 0.0);
	}
 /**
  * see:<br>
  * RenderedImage processFormat(RenderedImage image, String newDataType, 
			double rescaleMin, double rescaleMax) <br>
	
  * Similar to conditionImageToByte, takes the data type to convert to as 
  * an argument. Seems to work better. I
  * @param image 
  * @param newDataType integer value of new data type. Must be a valid DataBuffer
  * constant.
  * @return RenderedImage
  */
 public RenderedImage processFormat(RenderedImage image, int newDataType, 
 			double rescaleMin, double rescaleMax) {
        
        // DataBuffer.TYPE_BYTE
        RenderedImage sourceImage = image;
        
        ComponentSampleModel sampleModel = (ComponentSampleModel) image.getSampleModel();
        int oldDataType = sampleModel.getDataType();
        if (oldDataType == newDataType) return image;
        
        int numbands = sampleModel.getNumBands();
        
        
        // check if (oldDataType == newDataType) return image;
        if (debug) System.out.println("processFormat "+numbands+" bands   "+oldDataType+ " -> "+newDataType);
        
		   // make a new  SampleModel for the new image data type
		   // get all the stuff we need to know from the old sampleModel
		  int pStride =  sampleModel.getPixelStride();
		  int slStride = sampleModel.getScanlineStride();
		  int[] bandOffsets = sampleModel.getBandOffsets();
		  if (debug) System.out.println(" *** pStride="+pStride+"  slStride="+slStride+"  bandOffsets="+bandOffsets );
           //  int w = sampleModel.getWidth();
           // int h = sampleModel.getHeight();
         // ---------------------------------------------------------
         
		 double max = 0;
		 double min = 0;
		 double oldCeiling, newCeiling;
		 ParameterBlock PB;
		 RenderedImage temp = image;
		 
		 double scale[][];
         if (rescaleMin == 0.0 &&  rescaleMax == 0.0) {
//			rescale the pixel values of the image based on the image extrema
                 	
			PB=new ParameterBlock();
			PB.addSource(temp).add(null).add(10).add(10);
			RenderedImage extrema=JAI.create("extrema",PB);

			// scale all pixels by:   v1= m * v0 +b    (assuming one band per pixel)
			scale = (double[][])extrema.getProperty("extrema");
			// double ceiling=Byte.MAX_VALUE*2; // treat as unsigned
			oldCeiling = getMaxForDataType(oldDataType) ;
			newCeiling = getMaxForDataType(newDataType) ;
	
	
	
			// double ceiling=Short.MAX_VALUE*2;
			max=0;
			min=oldCeiling;
			for(int i=0;i<scale[0].length;i++){
				if (debug) System.out.println(i+") scale[0][i] "+scale[0][i]  +"   scale[1][i] "+scale[1][i]);
				if (scale[1][i] > 0) { max=Math.max(max,scale[1][i]); }
				else { max = scale[1][i]; }
	    			min=Math.min(min,scale[0][i]);
				}
			if (debug) System.out.println("processFormat extrema ceiling="+oldCeiling+"  min="+min+"  max="+max);
			// round max up to the nearest power of 2. 
			// max=Math.pow(2.0,Math.round(Math.log(max)/Math.log(2)));
			// min=0;
 		}
 		else {
 			// use what the user provided on the command line
 			min = rescaleMin;
 			max = rescaleMax;
			newCeiling = max;
 		}
	
	
	// this will be for BYTE output
	double constant[] = new double[]{1.0};
	double offset[] = new double[]{0.0};
	
	
	double delta = 0.0;
	if (min < 0 && max == 0) {
		delta = Math.abs(min);
	}
	else {
		delta = max-min;
	}
	
	constant[0] = newCeiling / delta;
	offset[0] = min * constant[0] * -1.0; // offset is added only for unsigned ??
	// offset[0] = min * -1.0; // offset is added only for unsigned ??
	
	if (debug) {
		System.out.println("processFormat constant="+constant[0]+"  offset="+offset[0]+"  delta="+delta);
	
	
		double min1 = (min * constant[0]) + offset[0];
		double max1 = (max * constant[0]) + offset[0];
		System.out.println("processFormat  min="+min+"  min1="+min1+"  max="+max+"  max1="+max1);
	}
	
	PB=new ParameterBlock();
	// PB.addSource(temp).add(new double[]{ceiling/(max-min)}).add(new double[]{ceiling*min/(min-max)});
	PB.addSource(temp).add(constant).add(offset);
	temp=JAI.create("rescale",PB);


	if (debug) {
		// do extrema again after the rescale
	
	
		PB=new ParameterBlock();
		PB.addSource(temp).add(null).add(10).add(10);
		RenderedImage extrema=JAI.create("extrema",PB);

		// scale all pixels by:   v1= m * v0 +b    (assuming one band per pixel)
		scale =(double[][])extrema.getProperty("extrema");
		// ceiling=Short.MAX_VALUE*2;
		max=1;
		min=newCeiling;
		for(int i=0;i<scale[0].length;i++){
			System.out.println(i+")new  scale[0][i] "+scale[0][i]  +"   scale[1][i] "+scale[1][i]);
	    	max=Math.max(max,scale[1][i]);
	    	min=Math.min(min,scale[0][i]);
		}
		System.out.println("processFormat new extrema  min="+min+"  max="+max);
	}
    
    image = temp;    
    
    
        
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(newDataType);
				
		// RenderedImage  formatedImage = JAI.create("format", pb, hints);
		RenderedImage  formatedImage = JAI.create("format", pb);
		return (formatedImage);    
		// 0 Flip_vertical
		// 1 Flip_horizontal
		// RenderedImage flippedImage = JAI.create("transpose", formatedImage, 0);
		// return (flippedImage);
    }
 
 /**
     * convenience for using format/rescale operators.
     * 
     * @param dataType
     * @return double. The naximum value for the dataType
     */
    public double getMaxForDataType(int dataType) {
    	
    	double max = 0.0;
    	if (dataType == DataBuffer.TYPE_BYTE) {
    		max = Byte.MAX_VALUE * 2;//used as unsigned
	        }
	    else if (dataType == DataBuffer.TYPE_SHORT) {
	    	max = Short.MAX_VALUE;
	        }	    
	    else if (dataType == DataBuffer.TYPE_USHORT) {	    	
	    	max = Short.MAX_VALUE * 2;
	        }
	    else if (dataType == DataBuffer.TYPE_INT) {
	    	max = Integer.MAX_VALUE; // or 0.0 ?? // assume unsigned ???
	        }	    
	    else if (dataType == DataBuffer.TYPE_FLOAT) {
	    	max = Float.MAX_VALUE; 
	        }
	    else if (dataType == DataBuffer.TYPE_DOUBLE) {
	    	max = Double.MAX_VALUE; 
	        }
	    
	    return max;
	 }   
    
  
  /**
  * prints useful debug information on a the image read in by fullRead()
  * 
  */
    public void printImageInfo() {
    	
		if (renderedImage != null) {
    		printImageInfo(renderedImage, " stored RenderedImage ");
		}
		else if (bufferedImage != null) {
    		printImageInfo(bufferedImage, " stored BufferedImage ");
		}
    }

 /**
  * prints useful debug information on a RenderedImage. The description is a String 
  * included in the print.
  * @param im the image to display info about
  * @param description a String to be included in the printout
  */
public void printImageInfo(RenderedImage im, String description) {
	
	if (im == null) return ;
	
	SampleModel sm = im.getSampleModel();
    ColorModel cm = im.getColorModel();
    int  width = im.getWidth();
    int height = im.getHeight();
   
    int dataType = sm.getDataType();
        
    
    System.out.println("RenderedImage "+description+"  -------------------");
    int bands = sm.getNumBands();
    int[] sampleSize = sm.getSampleSize(); // sampleSize[0] equals b0size
    int b0size = sm.getSampleSize(0);
    int elements = sm.getNumDataElements();
    
    	System.out.println("DataBuffer.TYPE_BYTE = "+DataBuffer.TYPE_BYTE);
    	System.out.println("DataBuffer.TYPE_SHORT = "+DataBuffer.TYPE_SHORT);
    	System.out.println("DataBuffer.TYPE_USHORT = "+DataBuffer.TYPE_USHORT);
    	System.out.println("DataBuffer.TYPE_INT = "+DataBuffer.TYPE_INT);
    	System.out.println("DataBuffer.TYPE_FLOAT = "+DataBuffer.TYPE_FLOAT);
    	System.out.println("DataBuffer.TYPE_DOUBLE = "+DataBuffer.TYPE_DOUBLE);
    	System.out.println("dataType "+dataType);
    	System.out.println("height="+height+"  width="+width+"  bands="+bands );
    	System.out.println("dataElements="+elements+"  b0size="+b0size   );
    	for (int i=0 ; i< sampleSize.length ; i++) {
        	System.out.println(" sampleSize["+i+"]="+sampleSize[i]);
    	}
    
}



	/**
	 * gets a String value for the input image data type.
	 * The value is derived from the dataType of the images SampleModel.<br>
	 * Used as an informational String.
	 * DataBuffer dataTypes are used	 
	 * 
	 * @param im	 * @return String	 
	 * */
	public String dataTypeString(RenderedImage im) {
			
		SampleModel sm = im.getSampleModel();
    
   		String s = "UNDEFINED";
   	
    	int dataType = sm.getDataType();
    
		if ( dataType == DataBuffer.TYPE_BYTE) {
	        s = "BYTE";
	        }	      
	    else if ( dataType == DataBuffer.TYPE_SHORT) {
	        s = "SHORT"; // VICAR IS HALF
	        } 
	    else if ( dataType == DataBuffer.TYPE_USHORT) {
	        s = "USHORT"; // VICAR IS HALF
	        } 
	    else if ( dataType == DataBuffer.TYPE_INT) {
	        s = "INT"; // FULL ???
	        // s = "FULL"; // VICAR USES FULL INSTEAD OF INT
	        } 
	    else if ( dataType == DataBuffer.TYPE_FLOAT) {
	        s = "FLOAT"; // FLOAT (VICAR USES REAL)
	        } 
	    else if ( dataType == DataBuffer.TYPE_DOUBLE) {
	        s = "DOUBLE";
	        }    
	           
	    
	    else  {
	        System.out.println("ERROR: INput format type "+ dataType+" IS unknown");
	        
	        }
	   return s;
	}



	/**
	 * display the IIOmetadata of the image which has been read
	 *
	 */
	void displayMetadata() {
	Document document = null;
	Node root = null;
	Node node = null;
	IIOMetadata im = null;
	
	if (iioImage != null) {
		
		im = iioImage.getMetadata();
		System.out.println(" ");
            
            if (im == null) {
                System.out.println("The image "+inputFileName+"  has no ImageMetadata.");
                // return null;
            } else {
                
                String nativeFormatName = im.getNativeMetadataFormatName();
                System.out.println("Image metadata: "+nativeFormatName);
                if (nativeFormatName.equalsIgnoreCase("VICAR_LABEL") || 
                	nativeFormatName.equalsIgnoreCase("PDS_LABEL") ||
                	nativeFormatName.equalsIgnoreCase("ISIS_LABEL") ||
                	nativeFormatName.equalsIgnoreCase("FITS_LABEL")
					) {
                	document =  (Document) im.getAsTree(nativeFormatName);
                	if (debug) System.out.println("VICAR, FITS, ISIS or PDS document "+ docInfo(document) );
                } else {
                	node =  im.getAsTree(nativeFormatName);
                	System.out.println("node "+ docInfo(node) );
                	// convert this to Document
                	IIOMetadataNode iomNode = (IIOMetadataNode) node;
                	IIOMetadataToDOM iomDOM = new IIOMetadataToDOM (iomNode) ;
                	document = iomDOM.getDocument();
                	if (debug) {
                		System.out.println("\n ##############################" );
                		System.out.println("document "+ docInfo(document) );
                	  }
                	}
                }              
            }
          
          /**
          if (document != null) {
          	
          	if (outputXML)  {
          		if (debug) System.out.println("write metadata to xml for the image: "+xmlFile1);
            	domUtils.serializeDocument(document, xmlFile1, "xml");
            	// a DOMtree viewer could also be called here
          	}
          } else {
          	if (debug) System.out.println("NO metadata to write to xml : "+xmlFile1);
          }  	
		**/
				
		root = (Node) document.getDocumentElement();
        if (root == null) {
            System.out.print("null metadata");
        }
        else {
            displayMetadata(root, 0);
        }
        System.out.println(" ============================ ");
	
    }

    /*
	 * returns a String with the class name of the suppllied Node. Since a Document descends from Node
	 * a Document may be supplied here. Useful in denugiing to see that a proper and compatable Document/Node 
	 * is being used
	 */
	public String docInfo(Node doc) {
	// public String docInfo(Document doc) {
		Class c = doc.getClass();
		String docName = c.getName();
		// System.out.println(
		return docName;
	}
    
	/**
	 * Displays the metadata of a supplied Node object which came from a metadata Documnet
	 * @param root
	 */
	public void displayMetadata(Node root) {
        if (root == null) {
            System.out.print("null metadata");
        }
        else {
            displayMetadata(root, 0);
        }
    }

	/**
	 * indents print of metadata to show level in the tree.
	 * @param level
	 */
    void indent(int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
    }

    /**
     * called to display specific node in a tree
     * A recursive private method
     */
    private void displayMetadata(Node node, int level) {
        // Print node name and attribute names and values
        indent(level);
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                                 "=\"" +
                                 attr.getNodeValue() +
                                 "\" ");
            }
        }

        // If the node is an IIOMetadataNode, print information
        // about the user object
        if (node instanceof IIOMetadataNode) {
            Object o = ((IIOMetadataNode)node).getUserObject();
            if (o != null) {
                System.out.print(" userObject=\"");
                System.out.print(o.getClass().getName());
                if (o instanceof byte[]) {
                    byte[] b = (byte[])o;
                    for (int i = 0; i < b.length; i++) {
                        System.out.print(" ");
                        System.out.print(b[i] & 0xff);
                    }
                } else {
                }
                System.out.print("\")");
            }
        }

        // Visit the children recursively
        Node child = node.getFirstChild();
        if (child != null) {
            System.out.println(">");
            while (child != null) {
                displayMetadata(child, level + 1);
                child = child.getNextSibling();
            }
            indent(level);
            System.out.println("</" + node.getNodeName() + ">");
        } else {
            System.out.println(">"+ node.getNodeValue()+"<"+node.getNodeName()+"/>");
        }
    }



	/**
	 * Creates a Hashtable from the image Matadata Document
	 * @param rootHash - this is ignored
	 * @return Hashtable
	 */
	public Hashtable getHashFromMetadata(Hashtable rootHash) {
		Document document = null;
		Hashtable hash = null;
		IIOMetadata im = null;
		if (iioImage != null) {
		
		im = iioImage.getMetadata();
		if (debug) System.out.println(" ");
            
            if (im == null) {
                System.out.println("The image "+inputFileName+"  has no ImageMetadata.");
                // return null;
            } else {
                
                String nativeFormatName = im.getNativeMetadataFormatName();
                if (debug) System.out.println("Image metadata: "+nativeFormatName);
                
                if (nativeFormatName.equalsIgnoreCase("VICAR_LABEL") || 
                	nativeFormatName.equalsIgnoreCase("PDS_LABEL") ||
                	nativeFormatName.equalsIgnoreCase("ISIS_LABEL") ||
					nativeFormatName.equalsIgnoreCase("FITS_LABEL")) {
                	document =  (Document) im.getAsTree(nativeFormatName);
                	if (debug) System.out.println("VICAR, FITS, ISIS or PDS document "+ docInfo(document) );
		
					DOMtoHashtable dh = new DOMtoHashtable(document);
					hash = dh.getHashtable();
                }
            }
		}
       return hash;
				
	}

/**
 * Prints the contents of a Hashtable. Assumes the Hashtable contains either
 *  All Keys are Strings, values may Strings or Hashtables. 
 * If a Hashtable is encountered it is recursively printed
 * @param h
 */
	public void printHash(Hashtable h) {
		System.out.println("ImageUtils.printHash() >>>>>>>>>>>>>" );
		printHash(h, 0) ;
		System.out.println("ImageUtils.printHash() <<<<<<<<<<<<<" );
	}

	/**
	 * Recursive private method that prints one level of a Hashtable
	 * @param h
	 * @param level
	 */
	private void printHash(Hashtable h, int level) {
		// recursively print embedded hashtables
		Enumeration keys = h.keys();
		String key, value;
		Object v;
		Enumeration subKeys;
		Hashtable subHash;
		while (keys.hasMoreElements()) {
			key = (String) keys.nextElement();
			v = h.get(key);
			indent(level);
			if (v instanceof String) {
				value = (String) v;
				System.out.println(key+" = "+value);
			}
			else if (v instanceof Hashtable) {
				System.out.println(key+" -------- ");
				printHash((Hashtable) v, level + 1);
			}
			else {
				System.out.println("--------");
			}
			
		}
		
		
		
	}

	/**
	 * 
	 * @param rootHash
	 */
	public void hashFromMetadata(Hashtable rootHash) {
	Document document = null;
	Node root = null;
	Node node = null;
	IIOMetadata im = null;
	
	if (iioImage != null) {
		
		im = iioImage.getMetadata();
		System.out.println(" ");
            
            if (im == null) {
                System.out.println("The image "+inputFileName+"  has no ImageMetadata.");
                // return null;
            } else {
                
                String nativeFormatName = im.getNativeMetadataFormatName();
                if (debug) System.out.println("Image metadata: "+nativeFormatName);
                if (nativeFormatName.equalsIgnoreCase("VICAR_LABEL") || 
                	nativeFormatName.equalsIgnoreCase("PDS_LABEL") ||
                	nativeFormatName.equalsIgnoreCase("ISIS_LABEL")) {
                	document =  (Document) im.getAsTree(nativeFormatName);
                	if (debug) System.out.println("VICAR, ISIS or PDS document "+ docInfo(document) );
                } else {
                	node =  im.getAsTree(nativeFormatName);
                	if (debug) System.out.println("node "+ docInfo(node) );
                	// convert this to Document
                	IIOMetadataNode iomNode = (IIOMetadataNode) node;
                	IIOMetadataToDOM iomDOM = new IIOMetadataToDOM (iomNode) ;
                	document = iomDOM.getDocument();
                	if (debug) System.out.println("document "+ docInfo(document) );
                }              
            }
          
          /**
          if (document != null) {
          	
          	if (outputXML)  {
          		if (debug) System.out.println("write metadata to xml for the image: "+xmlFile1);
            	domUtils.serializeDocument(document, xmlFile1, "xml");
            	// a DOMtree viewer could also be called here
          	}
          } else {
          	if (debug) System.out.println("NO metadata to write to xml : "+xmlFile1);
          }  	
		**/
				
		root = (Node) document.getDocumentElement();
        if (root == null) {
            System.out.print("null metadata");
        }
        else {
            displayMetadata(root, 0, rootHash);
        }
        System.out.println(" ============================ ");
	}
    }


private void displayMetadata(Node node, int level, Hashtable hash) {
        // Print node name and attribute names and values
        indent(level);
        Hashtable childHash = null;
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                                 "=\"" +
                                 attr.getNodeValue() +
                                 "\"");
            }
        }

        // If the node is an IIOMetadataNode, print information
        // about the user object
        if (node instanceof IIOMetadataNode) {
            Object o = ((IIOMetadataNode)node).getUserObject();
            if (o != null) {
                System.out.print(" userObject=\"");
                System.out.print(o.getClass().getName());
                if (o instanceof byte[]) {
                    byte[] b = (byte[])o;
                    for (int i = 0; i < b.length; i++) {
                        System.out.print(" ");
                        System.out.print(b[i] & 0xff);
                    }
                } else {
                }
                System.out.print("\")");
            }
        }

        // Visit the children recursively
        Node child = node.getFirstChild();
        if (child != null) {
            System.out.println(">");
            while (child != null) {
                displayMetadata(child, level + 1, childHash);
                child = child.getNextSibling();
            }
            indent(level);
            System.out.println("</" + node.getNodeName() + ">");
        } else {
            System.out.println(">"+ node.getNodeValue()+"<"+node.getNodeName()+"/>");
        }
    }

/**
 * test method that will print the metadata from a vicar image file
 *
 */
	public void vicarLabelTest() {
	System.out.println("vicarLabelTest");
	IIOMetadata im = null;
	String format = "";
		String[] formats ;
		DOMutils domUtils = new DOMutils();
	  	String xmlFile1 =  "vicarLabel1.xml";
	  	String xmlFile2 =  "vicarLabel2.xml";
		String xmlFile3 =  "vicarLabel3.xml";
		String xmlFile4 =  "vicarLabel4.xml";
		Document doc, doc1, doc11, doc2, doc3;
		Node node1, node2,node22, node3, node33;
		
		VicarLabel vl, vlFromXml ;
		
		if (iioImage == null) {
			System.out.println("iioImage is null");
			return;
		}
			
		im = iioImage.getMetadata();
		System.out.println(" ");
            
        if (im != null) {
        	formats = im.getMetadataFormatNames();    
        	format = im.getNativeMetadataFormatName() ;// nativeImageMetadataFormatName = "VICAR_LABEL"; 
        	
        	System.out.println("getNativeMetadataFormatName() "+format); 
        	for (int i=0 ; i< formats.length ;i++) {
        		System.out.println(" formats["+i+"] " +formats[i]); 
        	}
        
        	if (format.equalsIgnoreCase("VICAR_LABEL")) {
        		VicarMetadata vm = (VicarMetadata)im;
        		node1 = vm.getAsTree(format);
        		vl = vm.getVicarLabel();
        		VicarLabelSet vlSys = vl.getSystem();
        		VicarLabelCategory vlProps = vl.getProperty();
        		VicarLabelCategory vlTasks = vl.getHistory();
        		
        		System.out.println("write metadata to xml for the image: "+xmlFile1);
            	domUtils.serializeNode(node1, xmlFile1, "xml");
            	
            	doc1 = domUtils.getNewDocument();
            	
        		node2 = vl.toXML(doc1);
        		
        		node3 = node1.getFirstChild();
        		
        		node2 = vl.toXML(doc1);
        		doc1.appendChild(node2);
        		
        		if (doc1 != null) {
        			System.out.println("write metadata to xml for the image: "+xmlFile2);
            		domUtils.serializeDocument(doc1, xmlFile2, "xml");
        		}
        		
        		Vector errorList = new Vector();
        		vlFromXml = new VicarLabel();
        		
        		int nodeType = node1.getNodeType();
        		System.out.println("node1  "+node1.getNodeName()+" type "+nodeType+"  ELEMENT_NODE "+Node.ELEMENT_NODE);
        		

				nodeType = node3.getNodeType();
        		System.out.println("node3  "+node3.getNodeName()+" type "+nodeType+"  ELEMENT_NODE "+Node.ELEMENT_NODE);
        		// Element el = node1.)
        		vlFromXml.fromXML((Element) node3, (List) errorList);
        		System.out.println("vicarLabel vlFromXml ");
        		System.out.println(vlFromXml.toString());
        		
        		int size = errorList.size();
        		System.out.println("errorList "+size);
        		for (int i=0 ; i< size ; i++) {
        			String s = (String) errorList.get(i);
        			System.out.println(i+")  "+s);
        		}
        		System.out.println("errorList ************************");
        		
        		System.out.println("vicarLabel vlFromXml ");
        		System.out.println(vlFromXml.toString());
        		
        		// ----------------
        		
        		doc11 = domUtils.getNewDocument();
            	
        		// node22 = vl.toXML(doc11);
        		
        		// node33 = node1.getFirstChild();
        		
        		node22 = vlFromXml.toXML(doc11);
        		doc11.appendChild(node22);
        		
        		if (doc11 != null) {
        			System.out.println("write metadata to xml for the image: "+xmlFile3);
            		domUtils.serializeDocument(doc11, xmlFile3, "xml");
        		}
        	}
        }
	
	
	
	}

	/**
	 * Main used to run tests.
	 * There are 2 test which may be run. They are primarly examples of code to
	 * see how to use these utilities.
	 * 
	 * @param args
	 */
public static void main(String[] args)
    
  {    
  	
    String filename = "";
    String arg1 = "";
    boolean displayImage = true;
    // displayImage = false;
    if (args.length != 0) {
   		filename = args[0];
    }
    if (args.length > 1) {
    	arg1 = args[1];
    }
    ImageUtils imu = new ImageUtils();
    if (arg1.equalsIgnoreCase("jedi") ) {
    	imu.jediTest(filename, displayImage) ;
  	}
	if (arg1.equalsIgnoreCase("geotiff") ) {
		imu.geotiff(filename,displayImage);
	}
	
  }

/**
 * Test method to see how to use the methods in this class
 * @param inputFileName name of the image file to read
 * @param displayImage flag if image should be displayed
 */
public void jediTest(String inputFileName, boolean displayImage) {
	
	// read in the image
	
	RenderedImage renderedImage = null;
    BufferedImage bufferedImage = null;
    IIOImage iioImage = null;
    ImageInputStream iis = null;
    IIOMetadata im = null;
    IIOMetadata sm = null;
    int numImages = 0;
    
    // if (debug) 
    System.out.println("jediTest: " + inputFileName+"  *********************");
    
    ImageUtils imUtil = new ImageUtils(inputFileName) ;
    
    imUtil.setFormatToByte(true);
    
    
    try {
		imUtil.fullRead();
	} catch (IOException e) {
		
		e.printStackTrace();
	}
    
    System.out.println("********* Vicar Label XML test >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    
    imUtil.vicarLabelTest();
    
    System.out.println("********* Vicar Label XML test <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    
    if (displayImage) {
//    	imUtil.displayImage();
    	
    	// imUtil.setFormatToByte(true);
    	// RenderedImage ri = imUtil.getFormatedRenderedImage();
    	// imUtil.displayImage(ri);
    }
    
    // imUtil.printImageInfo();
	// // get the metadata
	// imUtil.displayMetadata();
	
	// convert metadata to a hash
	// Hashtable imHash = imUtil.getMetadataHash();
	
	/**
	imUtil.doHash();
	
	System.out.println("====================================================");
	Hashtable h = new Hashtable();
	Hashtable vicarHash;;
	h = imUtil.getHashFromMetadata(h) ; 
	System.out.println("print hash ====================================================");
	imUtil.printHash(h);
	System.out.println("           ====================================================");
	Object o = h.get("VICAR_LABEL");
	System.out.println("Object from get(\"VICAR_LABEL\" "+o);
	if (o instanceof Hashtable) {
		vicarHash = (Hashtable) o;
		o = vicarHash.get("SYSTEM");
		System.out.println("Object from get(\"SYSTEM\" "+o);
		if (o instanceof Hashtable) {
			Hashtable system = (Hashtable) o;
			String format = (String) system.get("FORMAT");
			System.out.println("Object from get(\"FORMAT\" "+format);
			String nl = (String) system.get("NL");
			System.out.println("Object from get(\"NL\" "+nl);
		}
	}
	**/
	
	// get the xsl file from the jar and print it out
	// String xslFilename = "jpl/mipl/io/xsl/VicarToPDSmer1.xsl";
	// URL xsl = ClassLoader.getSystemResource(xslFilename);
	
	// Stream = 
	
	// ask the hash some questions
	
	// lic void populateHash(IIOMetadata im, Hastable imHash) ;
	
	// print the document
	
	// print out the hash
	
	// example of acessing values using XPATH
	
	
	}

/**
 * Test method to see how to use the methods in this class
 * @param inputFileName name of the image file to read
 * @param displayImage flag if image should be displayed
 */
public void geotiff(String inputFileName, boolean displayImage) {
	
	// read in the image
	
	RenderedImage renderedImage = null;
    BufferedImage bufferedImage = null;
    IIOImage iioImage = null;
    ImageInputStream iis = null;
    IIOMetadata im = null;
    IIOMetadata sm = null;
    int numImages = 0;
    
    // if (debug) 
    debug = true;
    
    System.out.println("geotiff: " + inputFileName+"  *********************");
    
    ImageUtils imUtil = new ImageUtils(inputFileName) ;
    
    imUtil.setFormatToByte(true);
    
    try {
		imUtil.fullRead();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    System.out.println("********* geotiff Label XML test >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    System.out.println("********* displayImage="+displayImage+" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    
    if (displayImage) {
//    	imUtil.displayImage();
    	
    	// imUtil.setFormatToByte(true);
    	// RenderedImage ri = imUtil.getFormatedRenderedImage();
    	// imUtil.displayImage(ri);
    }
    
    imUtil.printImageInfo();
	
	// serialize metadata to a file
	imUtil.serializeMetadata();
	
	
	//	 // get the metadata
    System.out.println("********* geotiff displayMetadata >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	imUtil.displayMetadata();
}

/**
 * Writes the metadata to a text file as xml.
 * Replaces the input filenames extention with .xml
 *
 */
public void serializeMetadata() {
	System.out.println("serializeMatadata "+inputFileName +"  "+imageFormatName);
	IIOMetadata im = null;
	String format = "";
		String[] formats ;
		DOMutils domUtils = new DOMutils();
	  	String xmlFile =  inputFileName+".xml";
		Document doc;
		Node node;
				
		if (iioImage == null) {
			System.out.println("iioImage is null");
			return;
		}
			
		im = iioImage.getMetadata();
		System.out.println(" ");
            
        if (im != null) {
        	formats = im.getMetadataFormatNames();    
        	format = im.getNativeMetadataFormatName() ;// nativeImageMetadataFormatName = "VICAR_LABEL"; 
        	
        	if (debug) {
        	  System.out.println("getNativeMetadataFormatName() "+format); 
        	  for (int i=0 ; i< formats.length ;i++) {
        		System.out.println(" formats["+i+"] " +formats[i]); 
        	  }
        	}
        
        	String nativeFormatName = im.getNativeMetadataFormatName();
        	if (debug) System.out.println("Image metadata: "+nativeFormatName);
        	node =   im.getAsTree(nativeFormatName);
        	// if (debug) 
        	// 	System.out.println(format+" document "+ docInfo(document) );
        	
        	// Class c = doc.getClass();
    		// String docName = c.getName();
    		// System.out.println("docName "+docName);
    		
        	
        	String docName = docInfo(node);
        	if (debug) System.out.println("node "+ docName );
        	// convert this to Document
        	if (docName.equals("org.apache.xerces.dom.DocumentImp") ) {
        		if (node != null) {
            		System.out.println("write node metadata to xml for the image: "+xmlFile);
                	// domUtils.serializeDocument(doc, xmlFile, "xml");
            		domUtils.serializeNode(node, xmlFile, "xml");
            	}
        	} else {
        	  IIOMetadataNode iomNode = (IIOMetadataNode) node;
        	  IIOMetadataToDOM iomDOM = new IIOMetadataToDOM (iomNode) ;
        	  doc = iomDOM.getDocument();	
        	  if (doc != null) {
        		System.out.println("write doc metadata to xml for the image: "+xmlFile);
            	domUtils.serializeDocument(doc, xmlFile, "xml");
        		// domUtils.serializeNode(node, xmlFile, "xml");
        	  }
        	}
        		       		
        }
      }
	
/**
 * prints which ImageIO plugins are registered
 *
 */
	public void codecInfo() {
				// check to see what plugins are currently registered
			
				String[] readerFormatNames = null;
				String[] writerFormatNames = null;
                  
				readerFormatNames = ImageIO.getReaderFormatNames();
				System.out.println("registered reader plugin format names "+readerFormatNames.length);
				for (int i=0 ; i< readerFormatNames.length ; i++) {
					System.out.println((i+1) +") "+readerFormatNames[i] );
				}
        
				writerFormatNames = ImageIO.getWriterFormatNames();
				System.out.println("registered writer plugin format names "+writerFormatNames.length);
				for (int i=0 ; i< writerFormatNames.length ; i++) {
					System.out.println((i+1) +") "+writerFormatNames[i] );
				}
				System.out.println("----------------------");
        
		}

	

}
