/**
 * @(#)PDSLabelToDOM.java	1.0 
 *
 * Steve Levoe
 * Jet Propulsion Laboratory
 * Multimission Image Processing Laboratory
 * 2-2001 ImageIO EA2 version
 * 1-2003 JDK1,4 version
 * 4-2003 eliminate dependence on Jakarta oro package
 * use sun String methods instead
 *
 **/

package jpl.mipl.io.plugins;

import org.w3c.dom.Document;
// import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.*;

// import javax.xml.parsers.*;

import java.io.IOException;
// import java.util.*;

// import javax.imageio.metadata.*;
// import javax.imageio.*;
import javax.imageio.stream.*;

// import VicarIO stuff
// VicarInputFile  SystemLabel
// import jpl.mipl.io.streams.*;
// import jpl.mipl.io.vicar.*;
import jpl.mipl.io.util.*;

import java.io.*;
// import java.util.Vector;

import java.util.regex.*;

// import org.apache.oro.text.perl.*;


/**
 * Parses a ISIS image label and creates a Document Object with the values.
 * 
 * Using the JDK 1.4 String Matcher and Pattern methods instead.
 *
 @author Steve Levoe
 @version $Id: PDSLabelToDOM.java
 
 * currently does NOT handle HISTORY part of the label <br>
 * to add HISTORY we need to get the label size, read the whole thing 
 * into a buffer and process the buffer.
 */
public final class PDSLabelToDOM {

  /**
   * Reads an input stream with a ISIS label
   */
  
  
  BufferedReader _bufferedReader = null;
  ImageInputStream _imageInputStream = null;
  DataInput _dataInput = null;
  
  PrintWriter _output = null;
  
  Document _document = null;
  Node currentNode = null;
  String keyTag = "key"; // could also be "name"
  boolean useSubitem = true;
  // boolean useSubitem = false;
  
  String unitsTag = "units"; // could also be "name"
  boolean useUnitsAttribute = true;
  
  
  boolean debug = false;
  // boolean debug = true;
  
  /**
  * Constructors
  *
  * BufferedReader  and ImageInputStream  both have a readLine()
  * so either will work
  ***/
  public PDSLabelToDOM(BufferedReader input, PrintWriter output) {
    
    if (output == null) {
        _output = new PrintWriter(System.out) ;
    } else {
        _output = output ;
    }
    
    _bufferedReader = input;
    _document = buildDocument();
  }
   
  public PDSLabelToDOM(DataInput input, PrintWriter output) {
    
    if (output == null) {
        _output = new PrintWriter(System.out) ;
    } else {
        _output = output ;
    }
    
    _dataInput = input;
    _document = buildDocument();
  }
  
  public PDSLabelToDOM(ImageInputStream input, PrintWriter output) {
    
    if (output == null) {
        _output = new PrintWriter(System.out) ;
    } else {
        _output = output ;
    }
    _imageInputStream = input;
    _document = buildDocument();
  }
  
  /** 
  * isolates the type of stream being read from
  **/
  private String readLine() {
    String line = null;
    try {
        if (_bufferedReader != null) {
            line = _bufferedReader.readLine();
        } else if (_imageInputStream != null) {
            line = _imageInputStream.readLine();
        } else if (_dataInput != null) {
            line = _dataInput.readLine();
        }
    } catch(IOException e) {
      System.err.println("PDSLabelToDOM Error reading from input: readLine()");
      e.printStackTrace();
      // System.exit(1);
      return line;
    }  
   return line; 
  }
  
  public void setDebug(boolean d) {
  	debug = d;
  }
  
  public boolean getDebug() {
  	return debug ;
  }
  
  public Document getDocument() {
    return _document;
  }
  
  public void setUseSubitem(boolean f) {
  	useSubitem = f;
  }
  
  public boolean getUseSubitem(boolean f) {
  	return useSubitem ;
  }
  
  public void setUseUnitsAttribute(boolean f) {
  	useSubitem = f;
  }
  
  public boolean getUseUnitsAttribute(boolean f) {
  	return useUnitsAttribute ;
  }
  
  public int getLabelSize() {
  	int labelSize = 0;
  	String labelBuf;
  	return labelSize;	
  }
  /** 
  * Read the data in from the header and create the Document
  *
  **/
  public Document  buildDocument() {
    // Perl5Util perl = new Perl5Util();

    String key = "";
    String value = "";
    String v1 = "";
    String line;
    String units = "";
    int startPos=0;
    int endPos=0;
    int level = 0;
    
    Element element = null;
    Element item = null;
    int bytesRead = 0;
    int labelSize = -1; // don't check until labelSize != -1
    int recordBytes = -1;
    int qube = -1;
    
    
    /***
    _document = new org.apache.xerces.dom.DocumentImpl();
    **/
    DOMutils domUtils = new DOMutils();
          _document = domUtils.getNewDocument();
          // this document already has _documentName in it as its root Element
                
          Element root = (Element) _document.createElement(PDSMetadata.nativeImageMetadataFormatName); 
          
          _document.appendChild(root);
          currentNode = root; // always appendChild to the currentNode
          
     // create the patterns we want to look for
     Pattern nonEmptyLineP = Pattern.compile("\\S");
     // Pattern commentP = Pattern.compile("/\\*"); // "\Q/*\E"
     Pattern commentP = Pattern.compile("\\Q/*\\E");
     // Pattern sfduP = Pattern.compile("SFDU_LABEL",Pattern.CASE_INSENSITIVE);
     Pattern pdsVersionIdP = Pattern.compile("PDS_VERSION_ID",Pattern.CASE_INSENSITIVE);
     
     
     
     // create a bunch of Matchers 
     Matcher nonEmptyLineM, commentM, pdsVersionIdM;
     
     
     int lineCt = 0;
      while((line = readLine()) != null) {
      	// is /CR already stripped by readLine() ????
      	bytesRead += line.length();
      	units = "";
      	lineCt++;
      	if (debug) {
      		System.out.println(lineCt+") "+bytesRead+ " - "+labelSize+" "+line);
      		} 
      	// if (lineCt > 30) {
		// 	return _document;
      	// }
	    
	    // if (line.matches( "/\\S/")) {
	    // pattern = Pattern.compile(REGEX,Pattern.CASE_INSENSITIVE);
	    
	    // create the matcher for all possible cases (This is really stupid)
	    // then try a bunch until I find the correct case
	    
	    
	    nonEmptyLineM = nonEmptyLineP.matcher(line);
	    
	    if (nonEmptyLineM.find()) {
	    // if (line.matches( "\\S+")) {
	    // if (line.find( "\\S")) {
	        // System.out.println("*********** nonEmptyLine " +line);
	       
	        // sfduM = sfduP.matcher(line);
	        pdsVersionIdM = pdsVersionIdP.matcher(line);
        
	        int c1 = line.indexOf("/");
	        int c2 = line.indexOf("*");
	        // System.out.println("  ----- " +c1+" "+c2+" "+line);
	        
	        
	        // if (commentM.find()) {
	        if (c1 != -1 && c2 != -1 && c2 == (c1 + 1)) {
	            if (debug) _output.println("<comment>"+line+"</comment>" );
	            // System.out.println("<comment>"+line+"</comment>" );
	            element = (Element) _document.createElement("COMMENT");
	            Text text = (Text) _document.createTextNode(line);
	            element.appendChild(text);
	            currentNode.appendChild(element);
	        }
	        else if (pdsVersionIdM.find() ) {
	        	// System.out.println("find SFDU_LABEL " +line);
	        	String[] sv = line.split("=");
	           
	           
	           if (sv.length > 1) {
	               
	               key = sv[1];
	                
	               key = key.replaceFirst("^\\s*","");
	               key = key.replaceFirst("(\\s+)$",""); 
	               
	               value = sv[0];
	               value = value.replaceFirst("^\\s*","");
	               value = value.replaceFirst("(\\s+)$",""); 
	               
	               // System.out.println("<SFDU_LABEL>"+value+"</SFDU_LABEL>" );
	               
	               if (debug) {
	               	_output.println("<?xml version=\"1.0\"?>");
                   	_output.println("<!DOCTYPE PDS_LABEL SYSTEM \"pds_label.dtd\">");
                   	_output.println("<PDS_LABEL>");
	               	_output.println("<"+key+">"+value+"<"+key+">" );
	               } 
	               /* document is already create with ISIS_LABEL as the root ?? **        
	               // add nodes for all this stuff
	               element = (Element) _document.createElement("ISIS_LABEL");
	               currentNode.appendChild(element);
	               currentNode = (Node) element;
	               ****/
	                        
	               // add nodes for all this stuff
	               element = (Element) _document.createElement(key);
	               Text text = (Text) _document.createTextNode(value);
	               element.appendChild(text);
	               currentNode.appendChild(element);
	           }
	           else {
	           	System.out.println("PDSLabelToDOM PDS_VERSION_ID error line="+line);
	           }
	                        
	        }
	        // else if (line.matches( "=")) {
	        else if (line.indexOf("=") != -1) {
	            // output.println("=> "+line);
	            // split up the line
	            // Vector v = perl.split("/=/", line);
	           String[] sv = line.split("=");
	           String quoted = "false";
	            
	           // if (sv.length ==  1) {
	           	// System.out.println("#############################################");
	           if (debug) {
	           	System.out.print("sv.length = "+sv.length);
	           	System.out.println(" sv[0]="+sv[0]);
	           }
	           
	           if (sv.length > 1) {
	               // key = (String) v.elementAt(0);
	               key = sv[0];
	               // key = key.substitute("s/^\\s*//"); 
	               key = key.replaceFirst("^\\s*","");
	               key = key.replaceFirst("(\\s+)$",""); 
	               
	               v1 = sv[1];
	               String v1trim = v1.trim();
	               if (debug) { 
	               	System.out.println("          "+sv[1].length()+"  sv[1]="+sv[1]+"<v1>"+v1+"<>"+v1trim+"<");
	               }
	               int v1Len = v1.length();
	               int v1trimLen = v1trim.length();
	               if (debug) {
	               	System.out.println("         >"+v1.length()+"  "+v1trim.length()+"<");
	               }
	               
	               if (v1trim.length()  == 0) {
	               	v1 = readLine();
	               	if (debug) {
	               		System.out.println("$$$$$ new v1 "+v1);
	               	}
	               }
	               
	               // multivalued should be enclosed in brackets
	               // go till we find the end backet, then parse it up
	               // check for multivalue separated by a comma
	               if (v1.indexOf("(") != -1) {
	                    	// just commas with no quotes OR
	                    	// "," may also be " , " (spaces between comma and quotes)
	                    	// get all the values
	                   // remove the quotes from the quoted string
	                    startPos = -1;
	                    endPos = -1;
	                    quoted = "true";
	                    
	                    startPos = v1.indexOf("(");
	                    // endPos = v.lastIndexOf("\"");
	                    endPos = v1.indexOf(")", startPos +1);
	                    String s = "";
	                    String ss = "";
	                    StringBuffer sb=new StringBuffer();
	                    StringBuffer ssb=new StringBuffer();
	                    if (endPos != -1) { // closing bracket is in this string
	                       
	                       // s = v1.substring(startPos+1, endPos); 
	                       // value = s;
	                       value = v1; // remove extra spaces ???
	                    }
	                    else { // no closing bracket, go to the next line
	                        boolean keepReading = true;
	                        int qPos = -1;
	                        // s = v1.substring(startPos+1); // exclude "("
	                        s = v1.substring(startPos);  // include "("
	                        sb.append(s);
	                        do { 
	                        	s = readLine();
	                        	// remove spaces before the quotes
	                            s = s.replaceFirst("^\\s*","");
	                            
	                            qPos = s.indexOf(")");
	                            
	                            if ( qPos != -1) {
	                                ss = s.substring(0,qPos+1); // include bracket
	                                sb.append(ss);
	                                value = sb.toString();
	                                keepReading = false;
	                            }
	                            else {
	                            	sb.append(s);
	                            }
	                        } while (keepReading) ;
	                         
	                    }
	               /**
	               if (key.startsWith("ARTICULATION_DEV")) { 
	               
	               		System.out.println("!!!! key = "+key+" *********************");
	               		System.out.println("!!!! value="+value);
	               }
	               **/
	                
	               // now we have the complete value
	               // do subitems for each item ???   
	               // for now only if subitems are quoted ??? 
	               // check for quotes in the subitems
	               if(useUnitsAttribute) {
	                		// String units = "";
	                		// look for <units> pattern
	                		int u1 = value.indexOf("<");
	                		int u2 = value.indexOf(">");
	                		if (u1 != -1 && u2 != -1 && u1 < u2) {
	                			// get the units value, remove all instances from the value string
	                			try {
	                				units = value.substring(u1+1, u2);
	                				
	                				// remove all the instances from the value string
	                				if (debug) {
	                					System.out.println("UNITS units="+units);
	                					System.out.println("UNITS value="+value);
	                				}
	                				value = value.replaceAll("(\\s*)<\\w+>",""); 
	                				if (debug) System.out.println("UNITS value="+value+" ******");
	                			}
	                			catch (IndexOutOfBoundsException e) {
	                				units = "null";
	                				// print an error message ??
	                			}
	                		}
	               }
	               else {
	               	units = "";
	               }
	               
	               
	                		
	                		
	               
	               // if (value.indexOf("\"") != -1) {
	              if (value.indexOf("(") != -1) {
	               	if (useSubitem == false) {
	                	
	                	// remove any spaces before or after the backets
	                	value = value.replaceFirst("^\\s*\\(","(");
	               		value = value.replaceFirst("\\)(\\s*)$",")"); 
	               		
	                	// remove spaces AND brackets
	                	// value = value.replaceFirst("^\\s*\\(","");
	               		// value = value.replaceFirst("\\)(\\s+)$",""); 
	               		
	               		quoted = "false";
	               		element = (Element) _document.createElement("item");
	                	element.setAttribute(keyTag,key);
	                	element.setAttribute("quoted",quoted);
	                	
	                	if (useUnitsAttribute && !units.equals("")) {
	                		element.setAttribute(unitsTag,units);
	                	}	
	               			
	               		
	                
	                	// no subitems, just use trhe backeted value quotes and all     
	                	Text text = (Text) _document.createTextNode(value);	                        
	                	element.appendChild(text);
	                
	                	currentNode.appendChild(element);
	               	}
	               	else { // useSubitems == TRUE
	               	// remove the brackets
	               	value = value.replaceFirst("\\(","");
	               	value = value.replaceFirst("\\)","");
	               	
	               	String[] subitems = value.split(",");
	               	
	               	if (value.indexOf("\"") == -1) {
	               		quoted = "false";
	               	}
	               	else {
	               		quoted = "true";
	               	}
	               	element = (Element) _document.createElement("item");
	                element.setAttribute(keyTag,key);
	                element.setAttribute("quoted",quoted);
	                
	                if (useUnitsAttribute && !units.equals("")) {
	                		element.setAttribute(unitsTag,units);
	                	}
	                
	                // no text if we have subitems       
	                // Text text = (Text) _document.createTextNode(value);	                        
	                // element.appendChild(text);
	                
	                currentNode.appendChild(element);
	                
	                String subv = "";
	                Element subElement ;
	                for (int i =0; i<subitems.length ; i++) {
	                	subv = subitems[i];	               	
	               		// remove quotes and spaces before or after quotes
	               		subv = subv.replaceFirst("^\\s*\"","");
	               		subv = subv.replaceFirst("\"(\\s*)$",""); 
	               		subElement = (Element) _document.createElement("subitem");
	                    subElement.setAttribute(keyTag,key);
	                    subElement.setAttribute("quoted",quoted);
	                    
	                    if (useUnitsAttribute && !units.equals("")) {
	                		subElement.setAttribute(unitsTag,units);
	                	}
	                        
	                    Text text2 = (Text) _document.createTextNode(subv);
	                        
	                    subElement.appendChild(text2);
	                    element.appendChild(subElement);
	                 }
	               	} // end of if useSubitem
	               }
	               
	               /**
	               else {
	                quoted = "false";
	                // value = v1;
	                if (debug) {
	               		for (int i=0 ; i< level ; i++) { _output.print(" "); }
	               		_output.println("<item key=\""+key+"\" quoted=\""+quoted+"\">"+value+"<item>" );
	                }
	                
	                
	               
	               	// add nodes for all this stuff
	                        element = (Element) _document.createElement("item");
	                        element.setAttribute(keyTag,key);
	                        element.setAttribute("quoted",quoted);
	                        if (useUnitsAttribute && !units.equals("")) {
	                			element.setAttribute(unitsTag,units);
	                		}
	                        
	                        element.setAttribute("DEBUG","bracket_noQuotes?"); 
	                        
	                        Text text = (Text) _document.createTextNode(value);
	                        
	                        element.appendChild(text);
	                        currentNode.appendChild(element);
	               		}
	               		// ---------- bracket subitem 
	               		
	               	****/	
	               		
	               }
	               
	               // single value with quotes
	               else if (v1.indexOf("\"") != -1) {
	                                 
	                    // remove the quotes from the quoted string
	                    startPos = -1;
	                    endPos = -1;
	                    quoted = "true";
	                    startPos = v1.indexOf("\"");
	                    // endPos = v.lastIndexOf("\"");
	                    endPos = v1.indexOf("\"", startPos +1);
	                    String s="";
	                    String ss="";
	                    if (endPos != -1) { // closing quote is in this string
	                       s = v1.substring(startPos+1, endPos); 
	                       value = s;
	                    }
	                    else { // no closing quote, go to the next line
	                        boolean keepReading = true;
	                        int qPos = -1;
	                        s = v1.substring(startPos+1); 
	                        value = s;
	                        do { 
	                        	s = readLine();
	                            
	                            // s = perl.substitute("s/^\\s*//", s); 
	                            s = s.replaceAll("^\\s*","");
	                            // s = s.replaceAll("(\\s+)$","");
	                            qPos = s.indexOf("\"");
	                            
	                            if ( qPos != -1) {
	                                ss = s.substring(0,qPos);
	                                value += " " + ss;
	                                keepReading = false;
	                            }
	                            else {
	                                value += " " + s;
	                            }
	                        } while (keepReading) ;
	                         
	                    }
	               if (debug) {     
	               	for (int i=0 ; i< level ; i++) { _output.print(" "); }
	               	_output.println("<item key=\""+key+"\" quoted=\""+quoted+"\">"+value+"<item>" );
	               }
	               // add nodes for all this stuff
	                        element = (Element) _document.createElement("item");
	                        element.setAttribute(keyTag,key);
	                        element.setAttribute("quoted",quoted);
	                        
	                        Text text = (Text) _document.createTextNode(value);
	                        
	                        element.appendChild(text);
	                        currentNode.appendChild(element);
	                
	               }
	               else { // single value with no quotes in this value
	               	    
	                    // System.out.println("noQuotes = key <"+key+">");
	                    value = sv[1];
	                    // value = perl.substitute("s/^\\s*//", value);   // remove leading spaces
	                    value = value.replaceAll("^\\s*","");
	                    // value = perl.substitute("s/(\\s+)$//", value); // remove trailing spaces
	                    value = value.replaceAll("(\\s+)$","");
	                    
	                    if (key.matches( "END_OBJECT")) {
							if (debug) System.out.println("END_OBJECT *****************************");
	                        // close the current object. Move UP one level
	                        level--;
	                        if (debug) {
	                         for (int i=0 ; i< level ; i++) { _output.print(" "); }
	                         _output.println("</OBJECT>" );
	                        }
	                        
	                        // move back up one level
	                        currentNode = currentNode.getParentNode();
	                        
	                    }
	                    else if (key.matches( "OBJECT")) {
	                        // open a new object, put new items inside this one
	                        if (debug) {
	                        	for (int i=0 ; i< level ; i++) { _output.print(" "); }
	                        	_output.println("<OBJECT name=\""+value+"\">" );
	                        }
	                        level++;
	                        // add nodes for all this stuff
	                        element = (Element) _document.createElement(key);
	                        element.setAttribute("name",value);
	                        // the xsl to filter unwanted OBJECT nodes is easier if attributes are used
	                        // use an attribute to carry the OBJECT name instead of a TEXT node
	                        // Text text = (Text) _document.createTextNode(value);
	                        // element.appendChild(text);
	                        currentNode.appendChild(element);
	                        currentNode = (Node) element;
	                    }
	                    else if (key.matches( "END_GROUP")) {
	                        // close the current object. Move UP one level
	                        level--;
	                        if (debug) {
	                        	for (int i=0 ; i< level ; i++) { _output.print(" "); }
	                        	_output.println("</GROUP>" );
	                        }
	                        
	                        // move back up one level
	                        currentNode = currentNode.getParentNode();
	                        
	                    }
	                    else if (key.matches( "GROUP")) {
	                        // open a new object, put new items inside this one
	                        if (debug) {
	                        	for (int i=0 ; i< level ; i++) { _output.print(" "); }
	                        	_output.println("<GROUP name=\""+value+"\">" );
	                        }
	                        level++;
	                        // add nodes for all this stuff
	                        element = (Element) _document.createElement(key);
	                        element.setAttribute("name",value);
	                        // the xsl to filter unwanted OBJECT nodes is easier if attributes are used
	                        // use an attribute to carry the OBJECT name instead of a TEXT node
	                        // Text text = (Text) _document.createTextNode(value);
	                        // element.appendChild(text);
	                        currentNode.appendChild(element);
	                        currentNode = (Node) element;
	                    }
	                    else { // this is an "item"
	                    	
	                    	if (debug) {
	                    		System.out.println("+++ item key <"+key+">"+value+"</>");
	                        	for (int i=0 ; i< level ; i++) { _output.print(" "); }
	                        	_output.println("<item key=\""+key+"\" quoted=\""+quoted+"\">"+value+"<item>" );
	                    	}
	                        
	                    	// check for a value across multiple lines 
	                    	// no quotes so all spaces are thrown away	                    	
	                    	String s,vtrim;
	                    	vtrim = value.trim();
	                    	if (vtrim == "") {
	                    		if (debug) { 
	                    			System.out.println("+++ empty value >"+value+"</> ???????????????????????????");
	                    		}
	                    	boolean keepReading = true;
	                    		do {
	                    			s = readLine();
	                    			s = s.trim(); // remove spaces front and back
	                    			int slen = s.length();
	                    			int hPos = s.indexOf("-");
	                    			if (hPos != -1 && hPos == slen) {
	                    				value += s.replaceFirst("-$","");
	                    			} else {
	                    				value += s;
	                    				keepReading = false;
	                    			}
	                    		
	                    		} while (keepReading);
	                    		System.out.println("+++ "+value );
	                    	}
	                    	
	                        if(useUnitsAttribute) {
	                		// String units = "";
	                		// look for <units> pattern
	                		int u1 = value.indexOf("<");
	                		int u2 = value.indexOf(">");
	                		if (u1 != -1 && u2 != -1 && u1 < u2) {
	                			// get the units value, remove all instances from the value string
	                			try {
	                				units = value.substring(u1+1, u2);
	                				
	                				// remove all the instances from the value string
	                				if (debug) {
	                					System.out.println("UNITS units="+units);
	                					System.out.println("UNITS value="+value);
	                				}
	                				value = value.replaceAll("(\\s*)<\\w+>",""); 
	                				if (debug) System.out.println("UNITS value="+value+" ******");
	                			}
	                			catch (IndexOutOfBoundsException e) {
	                				units = "null";
	                				// print an error message ??
	                			}
	                		  }
	                 		}
	                 		else {
	               	   			units = "";
	                 		}
	                 
	                        // add nodes for all this stuff
	                        element = (Element) _document.createElement("item");
	                        element.setAttribute(keyTag,key);
	                        element.setAttribute("quoted",quoted);
	                        if (useUnitsAttribute && !units.equals("")) {
	                			element.setAttribute(unitsTag,units);
	                		}
	                        
	                        
	                        
	                        Text text = (Text) _document.createTextNode(value);
	                        
	                        element.appendChild(text);
	                        currentNode.appendChild(element);
	                        
	                        // ISIS only ?? look for some PDS specific items ???
	                        // labelSize = -1; // don't check until labelSize != -1
    						// recordBytes = -1;
    						// qube = -1;
    						if (key.equalsIgnoreCase("qube") || key.equalsIgnoreCase("^qube") ) { // "^QUBE"
    							qube = Integer.parseInt(value);
    							if (recordBytes != -1) {
    								labelSize = qube * recordBytes;
    								if (debug) {
    									System.out.println("labeSize "+labelSize);
    								}
    							}
    						}
    						else if (key.equalsIgnoreCase("record_bytes")) {
    							recordBytes = Integer.parseInt(value);
    							
    						}
    						// look for "^HISTORY" too ??
    						                       
	                    }                 
	               }

	            }
	           else {
	           	if (debug) {
	           		System.out.println("========== sv.length="+sv.length);
	           	}
	           }
	        }
	        else { // something on the line but no = "
				line = line.replaceFirst("^\\s*","");
				line = line.replaceFirst("(\\s+)$",""); 
				if (line.matches( "END_OBJECT")) {
					if (debug) System.out.println("END_OBJECT *****************************");
					// close the current object. Move UP one level
					level--;
					if (debug) {
					  for (int i=0 ; i< level ; i++) { _output.print(" "); }
						_output.println("</OBJECT>" );
					}
	                        
						// move back up one level
						currentNode = currentNode.getParentNode();	                        
					}										
				else if (line.matches( "END_GROUP")) {
					// close the current object. Move UP one level
					if (debug) System.out.println("END_GROUP *****************************");
					level--;
					if (debug) {
						for (int i=0 ; i< level ; i++) { _output.print(" "); }
							_output.println("</GROUP>" );
						}	                        
					// move back up one level
					currentNode = currentNode.getParentNode();	                        
					}
	        	else if (line.matches( "END") ){
	            // end the ISIS_LABEL tag
	            if (debug) _output.println("</PDS_LABEL>");
	            return _document;
	            }
	      }
	    }
	    // check for labelSize Bytes read
	    if (labelSize != -1 && bytesRead >= labelSize) {
	    	return _document;
	    }
      } // end of the while
     
    
   return  _document;
  }


public static final void main(String args[]) {
    String line;
    BufferedReader input = null;
    PrintWriter output    = null;
    // Perl5Util perl;

    if(args.length < 2) {
      System.err.println("Usage: pds2dom input output");
      System.exit(1); // error return
    }

    try {
      input = 
	new BufferedReader(new FileReader(args[0]));
    } catch(IOException e) {
      System.err.println("Error opening input file: " + args[0]);
      e.printStackTrace();
      System.exit(1);  // error return
    }

    try {
      output =
	new PrintWriter(new FileWriter(args[1]));
    } catch(IOException e) {
      System.err.println("Error opening output file: " + args[1]);
      e.printStackTrace();
      System.exit(1);  // error return
    } 

    PDSLabelToDOM pds = new PDSLabelToDOM(input, output) ;
    pds.setDebug(true);
    Document d = pds.getDocument();
  }  /// end of main. Used for testing



} // end of the class
