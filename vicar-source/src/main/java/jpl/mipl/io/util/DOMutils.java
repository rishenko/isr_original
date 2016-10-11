
/*
 * @(#)DOMutils.java 1.02 10-7-2002
 *
 * class to make using Apache xerces and xalan DOM creation and 
 * XSL processing convenient
 *
 * Steve Levoe
 * NASA/JPL
 * 6-3-2002
 *
 * Some parts of this class is derived from 
 * xalan-j_2_0_0/samples/dom2dom/dom2dom.java
 * 
 * Included in JAR file for IIO support
 *  8-2002 IIOsupport for jdk 1.4
 * 
 * 12-17-2002
 * moved getNodeValues inside a debug bracket since it
 * is expensive and only use for debug output.
 * 
 * next version will check version of JDK and in 1.4 and above
 * apache XML libraries (xerces, xalan) will be dropped in favor 
 * of SUN versions.
 * org.apache.oro.text.perl.*; will be dorpped for the SUN 
 * regex built into String.
 */

package jpl.mipl.io.util;

// Imported TraX classes
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;

// Imported java.io classes
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.OutputStream;

// Imported SAX classes
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// Imported DOM classes
import org.w3c.dom.Document;
import org.w3c.dom.Node;

// Imported Serializer classes
import org.apache.xalan.serialize.Serializer;
import org.apache.xalan.serialize.SerializerFactory;

import org.apache.xalan.templates.OutputProperties;

// Imported JAVA API for XML Parsing classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException; 


import java.util.Hashtable;
import java.awt.image.BufferedImage;

// Properties
import java.util.Properties;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.*;

// from XMLconfigure
import org.w3c.dom.*;
import org.w3c.dom.traversal.NodeIterator;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.apache.xml.utils.TreeWalker;
import org.apache.xml.utils.DOMBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.xpath.NodeSet;

// for Object usre data
// xxx import org.apache.xerces.dom.ElementImpl;

import jpl.mipl.io.plugins.*;

// for entity replacement
import org.apache.oro.text.perl.*;
// may replace this with the 1.4 pattern methods

  /**
   * A set of Utilities for working with DOM trees.
   * The hope is to hide all the details of using XML inside here.
   * The calling classes won't need to have any XML related classes
   * in their code.<br>
   * Example XPath: <br>
   * xpath = "//PDS/item[@key='IMAGE_SIZE']" ; <br>
   * will return the "item" node with a "key" attribute of 'IMAGE_SIZE' inside PDS 
   * <p>
   * 
   * xpath = "//STATUS/item" ; <br>
   * will a return a nodeList of all "item" nodes inside the "STATUS" node
   * <p>
   * Currently the Apache xalan classes are used to implement DOM related
   * stuff. In JDK 1.4 from SUN there may be implementations of the same API's
   * which will be substituted. <br>
   * One of the things this set of utilities addresses is that if all programs 
   * working with DOM's get their Documents from here then they will all be 
   * compatable. If a new implementation is substituted here, the comapatability is
   * kept in tact. <p>
   * 
   * @author Steve Levoe
   * @version 1.0
   */
public class DOMutils
{
    
    Document startDocument = null;
    Document resultDocument = null;
    String xmlFile = null;
    String outFile = null;
    String serializerType = "xml" ;
    String xslFile = null;
    
    String DocumentImplElementName = "DocumentImpl" ;
    String CreatorElementName = "CREATOR";
    
    boolean debug = false; // true turns on debug print statements
    // boolean debug = true; // true turns on debug print statements
   
    public boolean useAttributeForValue = false;
   
   // codes for Element types.
   // should find a class where these are defined and use that
    static final int ELEMENT_TYPE =   1;
    static final int ATTR_TYPE =      2;
    static final int TEXT_TYPE =      3;
    static final int CDATA_TYPE =     4;
    static final int ENTITYREF_TYPE = 5;
    static final int ENTITY_TYPE =    6;
    static final int PROCINSTR_TYPE = 7;
    static final int COMMENT_TYPE =   8;
    static final int DOCUMENT_TYPE =  9;
    static final int DOCTYPE_TYPE =  10;
    static final int DOCFRAG_TYPE =  11;
    static final int NOTATION_TYPE = 12;
    
    // Constructor
    
    public DOMutils() {
        startDocument = null;
        resultDocument = null;
        xmlFile = null;
        outFile = null;
        serializerType = "xml" ;
        xslFile = null;
    }
    
  /**
  * given an XML file, a Document will be created
  *
  * @param xml name of an XML file to read in and create a Document
  * @return The Document created from the file
  ***/
  public Document buildDocument(String xml) {
    
    // System.out.println("buildDocument "+xml );
    Document doc = null;
    if (debug) System.out.println("buildDocument "+xml);
     
    try {
      //Instantiate a DocumentBuilderFactory.
      DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      
      //Use the DocumentBuilderFactory to create a DocumentBuilder.
      dFactory.setNamespaceAware(true);
      
      if (debug)  {
        System.out.println("buildDocument from "+ xml);
        System.out.println("dFactory: "+dFactory);
      }
      
      DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
      
      if (debug) System.out.println("-------------------");
      //Use the DocumentBuilder to parse the XML input.
      doc = dBuilder.parse(xml);
      // startDocument = doc;
      xmlFile = xml;
      
    }
    catch (ParserConfigurationException pce) {
        System.out.println("buildDocument ParserConfigurationException "+ pce );
    }
    catch (IOException ioe) {
        System.out.println("buildDocument IOException "+ ioe );
    }
    catch (SAXException saxe) {
        System.out.println("buildDocument SAXException "+ saxe );
    }
    catch (Exception e) {
        System.out.println("buildDocument Exception "+ e );
    }
    return doc;
  }
   
   public void setDebug(boolean d) {
   	debug = d;
   }
   
   /**
   * Test a document we have received from somewhere to see if is compatable
   * with the DocumentImplentation we are using here. If it isn't compatable
   * when we try to run XSL transforms we will get exceptions.
   * <br>
   * We should have a converter some arbtrary Document can be converted to a useable one.
   * Document convertDocument(Document indoc);
   *
   * @param indoc a Document to check against the ones in use by DOMUtils
   * @return true if they are compatable
   **/
   boolean isCompatableDocument(Document indoc) {
        boolean isCompatable = false;
       // test the document to see if it the same (or some usable type)
       
       try {
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      
        //Use the DocumentBuilderFactory to create a DocumentBuilder.
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
       
        // compare two classes to see if they are the same ???
        Class c = doc.getClass();
        if (c.isInstance(indoc) ) {
             isCompatable = true;
         }
       }
       catch (ParserConfigurationException pce) {
            System.out.println("buildDocument ParserConfigurationException "+ pce );
       }
       // DOMImplementation domImpl = dBuilder.getDOMImplementation();
       
       return isCompatable;
   }
   
   /**
   * Creates a node in the Document which is a UserNode. This is a Non-portable Node which 
   * contains an arbitrary object. The Node is created but is NOT added to the Document.
   * The use is responsible for adding the Node to the Document at the location 
   * required.
   * This was always a hack to be able to store an arbitrary Object in a node.
   * It is no longer supported.
   *
   * @param doc the Document to put the UserNode into, 
   * The Node needs the Document to create a new Element
   * @param elmentName name of the Element to create
   * @param obj the Object to put as the data for the Element
   */
   public Node createUserNode(Document doc, String elementName, Object obj) {
    // make sure Document is compatable ???
        Element userNode = doc.createElement(elementName); 
          // this will only work for the xerces Implementation
          // if we go to JDK1.4 see if this is supported
          // org.apache.xerces.dom.ElementImpl e = (org.apache.xerces.dom.ElementImpl) userNode;
          // test to see if setUserData exists ???
          // e.setUserData(obj);
        // return e;
        return null;
   }
   
   /**
   * Create a new Document.
   *
   * @return the Document created
   **/
   public Document getNewDocument() {
   // DOMutils domUtil = new DOMutils();
          // _document = domUtils.getNewDocument();
          Document doc = null;
    if (debug) System.out.println("DOMUtils.getNewDocument()"); 
    try {
      //Instantiate a DocumentBuilderFactory.
      if (debug) System.out.println("DOMUtils.getNewDocument() ** 1 **"); 
      DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      
      dFactory.setNamespaceAware(true);
      
      //Use the DocumentBuilderFactory to create a DocumentBuilder.
      if (debug) System.out.println("DOMUtils.getNewDocument() ** 2 **"); 
      DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
      
      //Use the DocumentBuilder to parse the XML input.
      if (debug) System.out.println("DOMUtils.getNewDocument() ** 3 **"); 
      doc = dBuilder.newDocument();
      if (debug) System.out.println("DOMUtils.getNewDocument() ** 4 **"); 
      // startDocument = doc;
      // xmlFile = xml;
      // doc = new org.apache.xerces.dom.DocumentImpl();
          
          // we should have a version which adds this info
          /**
          Class c = doc.getClass();
          String documentName = c.getName();
          
          Element documentNameNode = (Element) doc.createElement(documentName); 
          doc.appendChild (documentNameNode);
          **/
          
        }
        catch (ParserConfigurationException pce) {
            System.out.println("buildDocument ParserConfigurationException "+ pce );
        }
        catch (Exception e) {
           System.out.println("buildDocument Exception "+ e );
         }
        /**
        catch (IOException ioe) {
           System.out.println("buildDocument IOException "+ ioe );
         }
        catch (SAXException saxe) {
            System.out.println("buildDocument SAXException "+ saxe );
        }
        ***/
    if (debug) System.out.println("DOMUtils.getNewDocument() returning Document"); 
    return doc;
          
   }
   
   /**
   * add a node to the document with a String of DocumentImpl class name.
   * The Element osis added at the root level.
   * useful only for debugging
   *
   * @param doc the Document to use
   * @return the Node created
   **/
   public Node setDocumentImplNode(Document doc) {
       
          Class c = doc.getClass();
          String documentName = c.getName();
          
          Element documentNameNode = (Element) doc.createElement(documentName); 
          doc.appendChild (documentNameNode);
          return documentNameNode;
   }
   
   
   /**
   * add a node to the document with a String of DocumentImpl class name
   * useful only for debugging
   *
   * @param doc the Document to use
   * @param node the Node in the Document to add the new Element to
   * @return the Node created
   **/
   public Node setDocumentImplNode(Document doc, Node node) {
       
          Class c = doc.getClass();
          String name = c.getName();
          
          Element nameNode = (Element) doc.createElement("DocumentImp"); 
          node.appendChild (nameNode);
          // now set the value of the creator node
          // should we call entity encoder ??
	    // String eValue = domUtil.encodeEnitiy(value);
	    if (useAttributeForValue) {
	        nameNode.setAttribute("value", name);
	        }
	    else {
	        Text text = (Text) doc.createTextNode(name);
	        nameNode.appendChild(text);
	    }
          return nameNode;
   }
   
   /**
   * Add a node to the document with a String of creator name. <br>
   * Useful for debugging to find where a Document was created.
   *
   * @param doc the Document to use
   * @param node the Node in the Document to add the new Element to
   * @param name the name of the creator of this Document
   * @return the Node created
   **/
   public Node setCreatorName(Document doc, Node node, String name) {
       
          
          
          Element creatorNameNode = (Element) doc.createElement(CreatorElementName); 
          node.appendChild (creatorNameNode);
          // now set the value of the creator node
          // should we call entity encoder ??
	    // String eValue = domUtil.encodeEnitiy(value);
	    if (useAttributeForValue) {
	        creatorNameNode.setAttribute("value", name);
	        }
	    else {
	        Text text = (Text) doc.createTextNode(name);
	        creatorNameNode.appendChild(text);
	    }
          return creatorNameNode;
   }
   
   /**
   * Add a node to the document with a String of creator name. <br>
   * Useful for debugging to find where a Document was created.
   *
   * @param doc the Document to use
   * @param node the Node in the Document to add the new Element to
   * @param creator the clasName of the creator Object is used as the name of
   * the creator
   * @return the Node created
   **/
   public Node setCreatorName(Document doc, Node node, Object creator) {
       
          Class c = creator.getClass();
          String creatorName = c.getName();
          
          Element creatorNameNode = (Element) doc.createElement(CreatorElementName); 
          
          node.appendChild (creatorNameNode);
          // now set the value of the creator node
          // should we call entity encoder ??
	    // String eValue = domUtil.encodeEnitiy(value);
	    if (useAttributeForValue) {
	        creatorNameNode.setAttribute("value", creatorName);
	        }
	    else {
	        Text text = (Text) doc.createTextNode(creatorName);
	        creatorNameNode.appendChild(text);
	    }
          return creatorNameNode;
   }
   
   /**
   * Adds a node to the document.
   *
   * @param doc the Document to use
   * @param node the Node in the Document to add the new Element to
   * @param nodeName the String to be used as the new Element name
   * @param value the String to be used as the valuie of this Element
   * @return the Node created
   **/
   public Node addNode(Document doc, Node node, String nodeName, String value) {
       
          
          
          Element newNode = (Element) doc.createElement(nodeName); 
          node.appendChild (newNode);
          // now set the value of the creator node
          // should we call entity encoder ??
	    // String eValue = domUtil.encodeEnitiy(value);
	    if (useAttributeForValue) {
	        newNode.setAttribute("value", value);
	        }
	    else {
	        Text text = (Text) doc.createTextNode(value);
	        newNode.appendChild(text);
	    }
          return newNode;
   }
   
   /**
   * Adds a node to the document with no value. 
   * The node will probably be used as a container for other nodes.
   *
   * @param doc the Document to use
   * @param node the Node in the Document to add the new Element to
   * @param nodeName the String to be used as the new Element name
   * @return the Node created
   **/
   public Node addEmptyNode(Document doc, Node node, String nodeName) {
         
          Element newNode = (Element) doc.createElement(nodeName); 
          node.appendChild (newNode);
          
          return newNode;
   }
   
   /**
   * Create a new Document with the rootName as the root Element.
   * 2 other elemnts will be added to the root to be used for identification
   * The implementation will be added as the DOCUMENT_IMPLEMENTATION node.
   * creatorName will be added as CREATOR node
   * If we create all our documents here and put the class name in we
   * can test to see if they are compatable for use with the other 
   * parsing xsl etc methods in DOMutils.
   * If we get them from here they will always be compatable with each other.
   * <p>
   * example <br>
   * <jpl.mipl.io.plugins.vicar.pdsimage_1.0> <br>
   *   <DocumentImpl>org.apache.xerces.dom.DocumentImpl</DocumentImpl> <br>
   *   <CREATOR>PDSImageReader</CREATOR> <br>
   * </jpl.mipl.io.plugins.vicar.pdsimage_1.0> 
   * <p>
   * This Document was created to allow a few conveniences.<br>
   * 1) It is easy to check implemntation and therefore compatability in the 
   * case where documents must be merged, compared or other processing applied,
   * In the case of serializers and XSLT processors an incompatable DocumentImpl will 
   * cause and exception to be thrown. <br>
   * The <DocumemtImpl> Node could be used to help locate a compatable implmentation
   * so processing can proceed, or at least help to identify the problem. <br>
   * 2) The CREATOR node can be used to identify multiple Documents associated with
   * some processing. For example if an ImageReader creates a Document for the image 
   * then it's creator would be ImageReader, Then for writing ImageToDOM might create
   * Doucment describing the system parameters of the image and how it will be
   * written to a file. Before writing the file out the 2 Documents could be merged so
   * that all the Metadadta from the reader is preserved. The System values for the 
   * write should come from the ImageToDOM created Document. A merge process will be able
   * to determine how to determine which value to use when duplicate elements are found 
   * during a merge operation.<br>
   * Other processing will no doubt find it useful to know the Creator of the Document.
   * 
   **/
   Document getNewDocument(String rootName, String creatorName) {
   // DOMutils domUtil = new DOMutils();
          // _document = domUtils.getNewDocument();
          Document doc = null;
     
    try {
      //Instantiate a DocumentBuilderFactory.
      DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
      
      //Use the DocumentBuilderFactory to create a DocumentBuilder.
      DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
      
      //Use the DocumentBuilder to parse the XML input.
      doc = dBuilder.newDocument();
      // startDocument = doc;
      // xmlFile = xml;
      // doc = new org.apache.xerces.dom.DocumentImpl();
          
          Class c = doc.getClass();
          String documentName = c.getName();
          
          Element documentNameNode = (Element) doc.createElement(documentName); 
          
          setCreatorName(doc, documentNameNode, creatorName);
      
        }
        catch (ParserConfigurationException pce) {
            System.out.println("buildDocument ParserConfigurationException "+ pce );
        }
        /**
        catch (IOException ioe) {
           System.out.println("buildDocument IOException "+ ioe );
         }
        catch (SAXException saxe) {
            System.out.println("buildDocument SAXException "+ saxe );
        }
        ***/
    
    return doc;
          
   }
   
   
   /** ideas
   method to return Node for a given Element name
   another item
   xpath = "//PDS/item[@key='IMAGE_SIZE']"
   
   see about using returned xpath from a node to see if there is a
   Node in the other tree to match it ???
   
   method to copy/clone a node, put the copy into the new tree
   **/
   /**
   * Merge the contents of 2 Documents.
   * return the merged Document (which is a new Document)
   * find the xpath node in each document, take the comntents of one node and add it 
   * at that node in the other Document. <BR>
   * Whole new idea.... <BR>
   * Use XSLT to do the merge. Then the merge is flexible.
   * We will need a default XSLT merge script and a way of loading it automatically.
   * This is still under development. To be used with the ImageIO Transcoders.
   * 
   * @param doc1 the first Document 
   * @param doc2 the second Document
   * @param xpath an XPath statement used the nodes in each Document to0 use as the root
   * nodes during the merge
   * @return the resulting merged Document
   **/
   public Document mergeDocuments(Document doc1, Document doc2, String xpath) {
        // the contents of doc2 is added to doc1
        // add checking to determine duplicates and not add one of them
   
        // check to see that both Documents are compatable
        if (doc1 != null && doc2 != null) {
            
            if (debug) {
                System.out.println("doc1 > " + doc1.getImplementation() );
                System.out.println("doc2 > " + doc2.getImplementation() );
            }
        
            Node root1 = doc1.getDocumentElement();
            // strip off the documnet node??
            // check if there is onlt one child??
            // child the name of the root??
            Node childOfDocRoot1 = root1.getFirstChild();
            
            Node root2 = doc2.getDocumentElement();
            // strip off the document node??
            // check if there is only one child??
            // child the name of the root??
            Node childOfDocRoot2 = root2.getFirstChild();
            
            // mergeNodes(childOfDocRoot1, childOfRootDoc2);
           
             if (debug)  System.out.println("mergeDocuments("+xpath+")" );
            // now use xpath to find the nodes in each docuument to start from
            
            Node x1 = getResultNode( root1, xpath) ;
            
            if (debug)  { 
            	System.out.println("mergeDocuments() after getResultNode root1" );
            	System.out.println("x1 "+x1);
            }
            
            Node x2 = getResultNode( root2, xpath) ;
            if (debug)  { 
            	System.out.println("mergeDocuments() after getResultNode root2" );
            	System.out.println("x2 "+x2);
            }
            // Node x1 = getResultNode( childOfDocRoot1, xpath) ;
            
            // Node x2 = getResultNode( childOfDocRoot2, xpath) ;
            
            mergeNodes(x1, x2);
             if (debug)  { 
            	System.out.println("mergeDocuments() after mergeNodes" );
            }
            
        }
        return doc1 ;
    }
    
    /** 
    * Could allow user to set the IIOMetadataNode to convert into.
    * This would allow a document to added to the supplied node as 
    * a child of that node.
    * Called from MergeDocuments.
    *
    * @param root the node to merge into
    * @param n2 this node will be merged into the root
    **/
    public void mergeNodes(Node root, Node n2) {
        if (root == null) {
            System.out.print("mergeNodes: null root");
        }
        else {
            mergeNodes(root, n2, 0);
        }
    }
    
    /**
    * input Node node, n2 <br>
    * output Node node with n2 contnts merged in <br> 
    * int level - used only for the formatting of the StringBuffer used by toString
    * This is a recursive method called as we walk through the tree.
    *
    * @param n1 the node to merge into
    * @param n2 this node will be merged into the root
    **/
    private void mergeNodes(Node n1, Node n2, int level) {
        
        
        /** 
        * implementation #1
        * assume that all of n2 should be added to n1
        * go thru all the nodes of node, clone them and add them to node
        * This assumes that node and n2 are at the same level, and so we really 
        * want to add all of n2's children to n1.
        * There should be some preprocessing of node and n2 to see how they
        * fit together before they are handed off to this method.
        *******/
        if (debug) {
        System.out.println("n1 > " + n1.getNodeName()+" * "+n1.getNodeValue());
        System.out.println("n2 > " + n2.getNodeName()+" * "+n2.getNodeValue());
        
        System.out.println("n1 children > " + n1.hasChildNodes());
        System.out.println("n2 children > " + n2.hasChildNodes());
        }
        
        Document n1ParentDoc = n1.getOwnerDocument();
        
        Node child = n2.getFirstChild();
        Node cloneNode = null;
        String[] s;
        String name;
        NodeList nl = n2.getChildNodes();
        int len = nl.getLength();
        for (int i=0 ; i<len ; i++) {
            child = nl.item(i);
            if (debug) System.out.println("child "+i+" > " + child.getNodeName()+" * "+child.getNodeValue());
            cloneNode = n1ParentDoc.importNode(child, true); // deep copy, includes all children in the clone
            // this node will have the proper Document parent so we can add it to that Document
            // System.out.println("  cloneNode   > " + cloneNode.getNodeName()+" * "+cloneNode.getNodeValue());
            if (cloneNode != null) {
                // get the xpath for this node
                // check for one in the original Node
                // don't add if they are the same
                boolean includeAtributes = true;
                
                name = cloneNode.getNodeName();
                if (name.equals("#text")) {
                    // only add the text node that is the child of an Element we want ???
                    // n1.appendChild(cloneNode);
                }
                else {
                    s = getNodeXPath(cloneNode, includeAtributes );
                    
                    
                    if (s != null) {
                    for (int j=0 ; j<s.length ; j++) {
                        String ss = s[j];
                        int ix = ss.indexOf("@quoted");
                        if (ix != -1) {
                            // do nothing 
                        }
                        else {
                            String cval = getNodeValue(cloneNode, s[j]);
                            String cval2 = getNodeValue(cloneNode);
                            if (debug) System.out.println("cloneNode "+name+" xpath="+s[j]+" = "+cval+" - "+cval2); 
                            // see if we can find the same node in the input Document
                            
                            if (debug) {
                            	String[] val = getNodeValues(n1, s[j]);
                                for (int k=0 ;k<val.length ; k++) {
                                    System.out.println("  n1 xpath="+s[j]+" = "+val[k]);
                                }
                            }
                        
                            Node rn = getSingleNode(n1, s[j]);
                            String rv = null;
                            String rv2 = null;
                            String rname = null;
                            if (rn != null) {
                                rv = rn.getNodeValue();
                                rv2 = getNodeValue(rn);
                                }
                            if (debug) System.out.println("    rn="+rn+" "+rname+"  value="+rv+" - "+rv2); 
                            }
                        }
                    }
                 n1.appendChild(cloneNode);
                }
            }
        }
        
        /****
        while (child != null && n1ParentDoc != null) {
            System.out.println("  child   > " + child.getNodeName()+" * "+child.getNodeValue());
            // cloneNode = child.cloneNode(true) ; // deep clone, includes all children in the clone
            
            // a clone won't work because the clone will have the original Document as the 
            // owner Document. When we try to add this node to a different Document you 
            // get an Exception.
            cloneNode = n1ParentDoc.importNode(child, true); // deep copy, includes all children in the clone
            // this node will have the proper Document parent so we can ad it to that Document
            System.out.println("  cloneNode   > " + cloneNode.getNodeName()+" * "+cloneNode.getNodeValue());
            if (cloneNode != null) {
                n1.appendChild(cloneNode);
            }
            child = n2.getNextSibling();
        }
        ****/
        
        /***
        // Print node name and attribute names and values
        // indent(level);
        // System.out.print("<" + node.getNodeName());
        String nodeName = n1.getNodeName();
        String nodeValue = n1.getNodeValue();
        String pad ;
        
        String attrNodeName = null;
        String attrKey = null;
        String attrNodeValue = null;
        
        String objectName = null;
        IIOMetadataNode iioNode;
        
        int type = node.getNodeType();
          // create the same type of node??
          // I don't know how to do that with IIOMetadaNode
          // if this node is ANYTHING but an Element we should ignore it
          // Attribute nodes should fall out below. We sghould never see
          // an Attribute node at this level.
          iioNode = new IIOMetadataNode(nodeName); // this IS an Element nodeType
          iioNode.setNodeValue(nodeValue);
          rootIioNode.appendChild(iioNode);
          
          // this should always be Element
          // I'll throw this away after some testing
          // iioNode.setAttribute("nodeType", ""+nodeType);
          if (addNodeTypeAttribute) {
            iioNode.setAttribute("nodeType", nodeType[type]); 
          }
          
            
          // get all the attributes and add them to this node
          NamedNodeMap map = node.getAttributes();
          if (map != null) {
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                attrNodeName = attr.getNodeName();
                attrNodeValue = attr.getNodeValue();
                // this will be an Attribute nodeType
                iioNode.setAttribute(attrNodeName, attrNodeValue);
             } 
            }

            // org.apache.xerces.dom.NodeImpl
            // setUserData(java.lang.Object data)
            // xerces nodes have something equivalent to userObject
            // look for them and add them to the tree
            
            // Visit the children recursively
            // I THINK a node which has TEXT or CDATA children will not have
            // other children
            // check somewhere to be sure. I will not check for more children  ???
            Node child = node.getFirstChild();
            if (child != null) {
                // nodeValue = "";
                // System.out.println(">");
                while (child != null) {
                    int nType = child.getNodeType();
                    // check node type - store the data in a TEXT or CDATA node
                    // as the node value (IIOMetadata doesn't implement TEXT and CDATA nodes)
                    // these aren't "REAL" children
                    // 
                    if (type == TEXT_TYPE) {
                        nodeValue = nodeValue+child.getNodeValue(); 
                        iioNode.setNodeValue(nodeValue); // replace nodeValue
                    } else if (type == CDATA_TYPE) {
                        nodeValue = nodeValue+child.getNodeValue();
                        iioNode.setNodeValue(nodeValue); // replace nodeValue
                    }else {
                        // IIOMetadataNode iioChild = new 
                        convertMetadata(child, iioNode, level + 1);
                    }
                    child = child.getNextSibling();
                    nodeValue = "";
                }
                
            } 
        ***********************/
    
    } 
   
   
    /**
    * input Node n1, n2 <br>
    * output Node n1 with n2 contents merged in <br> 
    * 
    * This is a recursive method called as we walk through the tree.<br>
    * If a node from n1 and n2 both have the same ElementName and share the same value
    * for the attribute name, then the Element from n2 will be used. 
    * This means the n1 Element is replaced by n2's.
    * Otherwise Elements from n1 and n2 are combined into n1.<br
    * Node n1 is the output.
    *
    * @param n1 the node to merge into
    * @param n2 this node will be merged into the root
    * @param elementName - name of the Element to search for Unique nodes
    * @param attributeName - name of Attribute to search for Unique nodes
    **/
    public void mergeNodesOnElAttrUniq(Node n1, 
    								Node n2, 
    								String elementName, 
    								String attributeName, 
    								String xPath) 
    {       
        boolean saveDebug = debug;  
    	// debug = true;    
        if (debug) {
        	System.out.println("mergeNodesOnElAttrUniq elementName "+
        			elementName+"   attributeName "+attributeName+"  "+xPath);
        }
        	
        
        if (n1 == null || n2 == null) {
        	if (debug) {
        		System.out.println("n1 or n2 is null. returning");
        	}	
        	return ;
        }
        
        if (debug) {
        	System.out.println("n1 > " + n1.getNodeName()+" * "+n1.getNodeValue());
        	System.out.println("n2 > " + n2.getNodeName()+" * "+n2.getNodeValue());
        
        	System.out.println("n1 children > " + n1.hasChildNodes());
        	System.out.println("n2 children > " + n2.hasChildNodes());
        }
        
        
        Document n1ParentDoc = n1.getOwnerDocument();
        
        Node n1firstChild = n1.getFirstChild();
        Node child = n2.getFirstChild();
        Node cloneNode = null;
        Node origNode = null;
        
        String[] s;
        String name;
        NodeList nl = n2.getChildNodes();
        int len = nl.getLength();
        for (int i=0 ; i<len ; i++) {
            child = nl.item(i);
            if (debug) System.out.println("child "+i+" > " + child.getNodeName()+" * "+child.getNodeValue());
            cloneNode = n1ParentDoc.importNode(child, true); // deep copy, includes all children in the clone
            // this node will have the proper Document parent so we can add it to that Document
            // System.out.println("  cloneNode   > " + cloneNode.getNodeName()+" * "+cloneNode.getNodeValue());
            if (cloneNode != null) {
                // get the xpath for this node
                // check for one in the original Node
                // don't add if they are the same
                boolean includeAtributes = true;
                boolean includeParent = true;
                boolean matchFound = false;
                Node nodeToReplace = null;
                
                name = cloneNode.getNodeName();
                if (name.equals("#text")) {
                    // only add the text node that is the child of an Element we want ???
                    // n1.appendChild(cloneNode);
                }
                else {
                    s = getNodeXPath(cloneNode, includeAtributes );
                    
                    // check for a node with the same xPath in n1
                    
                    
                    
                    if (s != null) {
                    for (int j=0 ; j<s.length ; j++) {
                        String ss = s[j];
                        int ix = ss.indexOf("@quoted");
                        if (ix != -1) {
                            // do nothing 
                        }
                        else {
                            String cval = getNodeValue(cloneNode, s[j]);
                            String cval2 = getNodeValue(cloneNode);
                            if (debug) System.out.println(j+")cloneNode "+name+" xpath="+s[j]+" = "+cval+" - "+cval2); 
                            // see if we can find the same node in the input Document
                            NodeList inList = getNodeList(n1, s[j]);
                            
                                for (int x=0 ;x < inList.getLength() ; x++) {
                                	Node n = inList.item(x);
                                	String[] xp = getNodeXPath(n, includeAtributes, includeParent );
                                	for (int z=0 ; z < xp.length ; z++) {
                                		if (debug) System.out.println(j+"*"+x+"*"+z+"  n1 xpath="+s[j]+" ? "+xp[z]);
                                    	if (xp[z].indexOf(xPath) != -1) {
                                    		matchFound = true;
                                    		nodeToReplace = n;
                                    		if (debug) System.out.println("match FOUND "+xPath+" "+s[j]);
                                    	}
                                	}
                                
                            }
                            
                            String[] val = getNodeValues(n1, s[j]);
                            if (debug) {
                                for (int k=0 ;k<val.length ; k++) {
                                    System.out.println("  n1 xpath="+s[j]+" = "+val[k]);
                                }
                            }
                        
                            origNode = getSingleNode(n1, s[j]);
                            String rv = null;
                            String rv2 = null;
                            String rname = null;
                            if (origNode != null) {
                                rv = origNode.getNodeValue();
                                rv2 = getNodeValue(origNode);
                                }
                            if (debug) System.out.println("    origNode="+origNode+" "+rname+"  value="+rv+" - "+rv2); 
                            }
                        }
                    }
                 if (matchFound && nodeToReplace != null) {
                 	// is nodeToReplace the same as origNode ???
                 	if (debug) System.out.println(" replaceChild ----------------------- ");
                 	n1.replaceChild(cloneNode, nodeToReplace );
                 	// n1.insertBefore(cloneNode, nodeToReplace );
                 	// n1.removeChild(nodeToReplace);
                 	matchFound = false;
                 	nodeToReplace = null;
                 }
                 else {
                 	// this puts these in the beginning of the group
                 	if (debug) {
                 		System.out.println(" insertBefore ++++++++++++++++++++++++++ ");
                 		System.out.println(" cloneNode = " +cloneNode);
                 		System.out.println(" n1firstChild = "+n1firstChild);
                 	}
                 	try {
                 		n1.insertBefore(cloneNode, n1firstChild);
                 	}
                 	catch (org.w3c.dom.DOMException e) {
                 	  System.out.println(" Exception "+e);
                 	  // try again
                 	  n1.appendChild(cloneNode);
                 	}
                 	
                 }
                }
            }
        }
        
       
     debug = saveDebug ;   
    } 
    
     
    
    
   /**
   * These methods are conveniences to help a user determine the CREATOR
   * and DocumentImpl. The correct XPath (consistent with the method that created the
   * Document) is provided inside the methods
   **/
    /**
    * Transform a DOM to a new DOM using an XSL file.
    * Use: <br> 
    * public Document buildDocument(String xml)
    * <BR>
    * to create the Document from a file.
    * The Document may be created elsewhere and then transformed with this method.
    * There may be compatability problems with the DocumentImpl class used.
    * An exception will be seen if there is a problem.
    * <br>
    * User could run 
    * if (isCompatableDocument(indoc)) == false ) {
        newDoc = convertDocument(indoc);
        // newdoc will be compatable
    *   }
    * <p>
    * 
    **/
    /* transform a Document using an XSL file
     * @param Doc - the Document to transform
     * @param xsl - a String of the filename of the XSL file
     */
   public Document transformDocument(Document doc, String xsl) {
   		StreamSource xslss = new StreamSource(xsl);
   		return transformDocument((Node)  doc, xslss) ;
   }
   
   /* transform a Document using an XSL file
     * @param Doc - the Document to transform
     * @param xslIS - an input Stream for an XSL file<br>
     * The xsl file probably isthe default XSL read in from the jar file
     */
   	public Document transformDocument(Document doc, InputStream xslIS) {
   		StreamSource xslSS = new StreamSource(xslIS);
   		return transformDocument((Node)  doc, xslSS) ;
   	}
   	
   	/* transform a Document using an XSL file
     * @param doc - the Node which shoudl be a Document to transform
     * @param xsl - a String of the filename of the XSL file
     */	
   public Document transformDocument(Node doc, String xsl) {
   		StreamSource xslSS = new StreamSource(xsl);
   		return transformDocument((Node)  doc, xslSS) ;
   }
   	
   	/* transform a Document using an XSL file
     * @param doc - the Node which should be a Document to transform
     * @param xslIS - an input Stream for an XSL file<br>
     * The xsl file probably isthe default XSL read in from the jar file
     */		
   	public Document transformDocument(Node doc, InputStream xslIS) {
   		StreamSource xslSS = new StreamSource(xslIS);
   		return transformDocument((Node)  doc, xslSS) ;
   	}
   	
   // 	public Document transformDocument(Node doc, String xsl) {
    
    	/* transform a Document using an XSL file
     * @param doc - the Node which should be a Document to transform
     * @param xslIS - an input StreamSource for an XSL file<br>
     * 
     */		
    public Document transformDocument(Node doc, StreamSource xslStreamSource) {
    	
    TransformerFactory tFactory = TransformerFactory.newInstance();

    // System.out.println("transformDocument  TransformerFactory tFactory="+tFactory );
    
   
    if(tFactory.getFeature(DOMSource.FEATURE) && tFactory.getFeature(DOMResult.FEATURE))
    {
      // Process the stylesheet StreamSource and generate a Transformer.
      // Transformer transformer = tFactory.newTransformer(new StreamSource("birds.xsl"));
      DOMResult domResult = null;
      try {
      // Transformer transformer = tFactory.newTransformer(new StreamSource(xsl));
      Transformer transformer = tFactory.newTransformer(xslStreamSource);
      
      // Use the DOM Document to define a DOMSource object.
      DOMSource domSource = new DOMSource(doc);
      
      // Set the base URI for the DOMSource so any relative URIs it contains can
      // be resolved. This is optional.
      /** could get URI or URL from the xslStreamSource ???
      if (xmlFile != null) {
        domSource.setSystemId(xmlFile);
      } 
      ***/
      
      // if the Document was created elsewhere there will be no xmlFile
      // but also no need for relative URI's
      
      // Create an empty DOMResult for the Result.
      domResult = new DOMResult();
  
  	  // Perform the transformation, placing the output in the DOMResult.
  	  
      transformer.transform(domSource, domResult);
      
      } catch (TransformerConfigurationException tce) {
        System.out.println("transformDocument TransformerException "+ tce );
      
      } catch (TransformerException te) {
        System.out.println("transformDocument TransformerException "+te );
      }
	  
	 return (Document) domResult.getNode();
	}
    else
    {
        System.out.println("SAX Not Supported" );
      // throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
    }
    return null;
   }
   
   
   // could also have:
   // public void serializeDocument(Document doc, OutputStream out, String type) { 
   // the user supplied the output stream to serialize to.
   /**
   * Serialize the Document. In this case serializing the Document means printing it to
   * an ASCII file. <br>
   * Valid types are "xml" "text" "html"
   *
   * @param node Node to serialize
   * @param file the name of file to write to
   * @param type the type of serializer to use, Valid types are "xml" "text" "html"
   **/
   public void serializeNode(Node node, String file, String type) { 
   	
   	
   	if (node instanceof Document) {
   		serializeDocument((Document)  node, file, type) ;
   		return;
   	}
       //Instantiate an XML serializer and use it to serialize the output DOM to System.out
	    // using a default output format.
	   if (debug) System.out.println("serializeNode as "+type+" to "+file); 
	    // valid types are "xml" "text" "html"
	    // At the moment, anything other than 'text', 'xml', and 'html', 
	    // will use the output_xml.properties file
      Serializer serializer = SerializerFactory.getSerializer 
                            (OutputProperties.getDefaultMethodProperties(type));
                              //      (OutputProperties.getDefaultMethodProperties("xml"));
     
       
       // put the node into a Document
       
    Document newDoc;  
    Node nodeClone;
    try {                       
      if (file != null) { 
        FileOutputStream out = null;
        
        out = new FileOutputStream(file);
        
        
        serializer.setOutputStream(out);
         /**
         * this makes a clone of the node which now belongs to newDoc
         * now we must add the node to the Document newDoc
         * if importNode isn't run then appendChild will throw an exception
         * because the node has a different parent Document
         **/
        newDoc = getNewDocument();
        nodeClone = newDoc.importNode(node, true); 
        newDoc.appendChild(nodeClone);
        serializer.asDOMSerializer().serialize(newDoc);
      }
      else {
        serializer.setOutputStream(System.out);
        /**
         * this makes a clone of the node which now belongs to newDoc
         * now we must add the node to the Document newDoc
         * if importNode isn't run then appendChild will throw an exception
         * because the node has a different parent Document
         **/
        newDoc = getNewDocument();      
        nodeClone = newDoc.importNode(node, true); 
        newDoc.appendChild(nodeClone);
        serializer.asDOMSerializer().serialize(newDoc);
        }
      
      } catch (FileNotFoundException te) {
        System.out.println("serializeDocument FileNotFoundException "+te );
     
      } catch (IOException ioe) {
        System.out.println("serializeDocument IOException "+ioe );
        
      } catch (DOMException dome) {
        System.out.println("serializeDocument DOMException "+dome );
      }
   }
   
   public void serializeDocument(Document doc, String file, String type) { 
       //Instantiate an XML serializer and use it to serialize the output DOM to System.out
	    // using a default output format.
	   if (debug) System.out.println("serializeDocument as "+type+" to "+file); 
	    // valid types are "xml" "text" "html"
	    // At the moment, anything other than 'text', 'xml', and 'html', 
	    // will use the output_xml.properties file
      Serializer serializer = SerializerFactory.getSerializer 
                            (OutputProperties.getDefaultMethodProperties(type));
                              //      (OutputProperties.getDefaultMethodProperties("xml"));
       
    try {                       
      if (file != null) { 
        FileOutputStream out = null;
        
        out = new FileOutputStream(file);
        
        
        serializer.setOutputStream(out);
        // serializer.asDOMSerializer().serialize(domResult.getNode());
        serializer.asDOMSerializer().serialize(doc);
      }
      else {
        serializer.setOutputStream(System.out);
        // serializer.asDOMSerializer().serialize(domResult.getNode());
        serializer.asDOMSerializer().serialize(doc);
        }
      
      } catch (FileNotFoundException te) {
        System.out.println("serializeDocument FileNotFoundException "+te );
     
      } catch (IOException ioe) {
        System.out.println("serializeDocument IOException "+ioe );
      }
   }
   
   /**
    * Serialize the Document to the OutputStream provided.
    * 
    * @param doc Document to serialize
   * @param out the OutputStream to write to
   * @param type the type of serializer to use, Valid types are "xml" "text" "html"
    */
   public void serializeDocument(Document doc, OutputStream out, String type) { 
       //Instantiate an XML serializer and use it to serialize the output DOM to System.out
	    // using a default output format.
	   if (debug)  System.out.println("serializeDocument as "+type+" to OutputStream"); 
	    // valid types are "xml" "text" "html"
	    // At the moment, anything other than 'text', 'xml', and 'html', 
	    // will use the output_xml.properties file
      Serializer serializer = SerializerFactory.getSerializer 
                            (OutputProperties.getDefaultMethodProperties(type));
                              //      (OutputProperties.getDefaultMethodProperties("xml"));
       
    try {                       
      if (out != null) { 
        // FileOutputStream out = null;
        // out = new FileOutputStream(file);
        
        
        serializer.setOutputStream(out);
        // serializer.asDOMSerializer().serialize(domResult.getNode());
        serializer.asDOMSerializer().serialize(doc);
      }
      else {
        serializer.setOutputStream(System.out);
        // serializer.asDOMSerializer().serialize(domResult.getNode());
        serializer.asDOMSerializer().serialize(doc);
        }
      
      } catch (FileNotFoundException te) {
        System.out.println("serializeDocument FileNotFoundException "+te );
     
      } catch (IOException ioe) {
        System.out.println("serializeDocument IOException "+ioe );
      }
   }
   
   
 // -------------------------------------------------------------------
 // Xpath utilities
 
 /**
 *
 *
 * other versions:
 * add Document as an argument
 * no attribute - return node values (instead of attribute)
 *
 * getNodeAttributes( xpath with an attribute, gets all the attributes values
 * which isn't the node value, just the value of that atribute
 **/
 /**
  * Returns an array of values for all the attributes matching the xpath search String.
  * If returnXPath is true, the XPath statement is returned for the attribute value located.
  * This allows the user to find a value or list of values of a certain node and attribute.
  * Then the XPath returned can be used to get the value for that particular node.
  * Otherwise the value of the attribute is returned.
  * 
  * @param doc 
  * change to Node, this makes it more general purpose, Document implements Node
  * so a Document will still work
  * @param xpath
  * @param attribute
  * @param returnXPath
  */
 // public String[] getNodeValues(Document doc, String xpath, String attribute, boolean returnXPath) {
 public String[] getNodeValues(Node node, String xpath, String attribute, boolean returnXPath) {  
    Node n;
    int nodeCt = 0;
    String value;
    String values[] = null;
    // String attr = attribute;
    
    // xpath = "//SOUND" ;
    // System.out.println("Querying Node using "+xpath+" + "+attribute);
    NodeIterator nl = null;
    try {
        // nl = XPathAPI.selectNodeIterator(doc, xpath);
        nl = XPathAPI.selectNodeIterator(node, xpath, node);
        while ((n = nl.nextNode())!= null)
            { 
            nodeCt++;
            // this puts the index at the end of the list
            // get it again to "reset" the list
            }
        // nl = XPathAPI.selectNodeIterator(doc, xpath);
        nl = XPathAPI.selectNodeIterator(node, xpath, node);
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
    // this is a Global variable
    values = new String[nodeCt];
    int j=0;
    // NodeSet ns = new NodeSet(nl);
    // int ode n;
    // System.out.println("found  nodes");
    while ((n = nl.nextNode())!= null)
        {  
        // value = n.getLocalName()+" - ";
        // value += n.getPrefix()+" - ";
        // System.out.println("nodeName "+n.getNodeName() );
          NamedNodeMap map = n.getAttributes();
          if (map != null) {
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                String attrNodeName = attr.getNodeName();
                if (attrNodeName.equalsIgnoreCase(attribute)) {
                    if (returnXPath) {
                        value = xpath+"[@"+attribute+"='"+attr.getNodeValue()+"']";
                    }
                    else {
                        value = attr.getNodeValue();
                    }
                    
                    values[j] = value;
                }
                
             } 
            }
            j++;
        }
        
     return values ;        
  }
  
  /**
  *
  *  Returns the first value, many times there will only be one value 
  * so the first is quite convenient.
  *
  * @param doc the Document to search
  * @param xpath the xpath expression to search for
  *
  * @return the value (or null) of the located node
  **/
  // public String getNodeValue(Document doc, String xpath) {
  public String getNodeValue(Node node, String xpath) {
    // String v[] = getNodeValues(doc, xpath);
    String v[] = getNodeValues(node, xpath);
    if (v.length >= 1) {
        return v[0];
    }
    else {
        return null;
    }   
  }
  
  
  // xpath = "//item[@key='FIRST_LINE']" ;
  /**
  * Searches for node[s] which match an XPath expression. An array of values 
  * is returned. The values are the node values, which is the text() node of the Element.
  *
  * @param doc the Document to search
  * @param xpath the xpath expression to search for
  *
  * @return the array of values (or null) of the located node(s)
  **/
    // public String[] getNodeValues(Document doc, String xpath) {
    public String[] getNodeValues(Node node, String xpath) {
        
    Node n;
    int nodeCt = 0;
    String value;
    String values[] = null;
    // String attr = attribute;
    
    // xpath = "//SOUND" ;
    // System.out.println("getNodeValues using "+xpath);
    NodeIterator nl = null;
    try {
        // nl = XPathAPI.selectNodeIterator(doc, xpath);
        nl = XPathAPI.selectNodeIterator(node, xpath, node);
        while ((n = nl.nextNode())!= null)
            { 
            nodeCt++;
            // this puts the index at the end of the list
            // get it again to "reset" the list
            // wasteful but I don't know any other way
            }
        // nl = XPathAPI.selectNodeIterator(doc, xpath);
        nl = XPathAPI.selectNodeIterator(node, xpath, node);
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
    // this is a Global variable
    values = new String[nodeCt];
    int j=0;
    
    while ((n = nl.nextNode())!= null)
        {                       
        value = n.getNodeValue();
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        
        String nodeValue = "";
        Node child = n.getFirstChild();
            if (child != null) {
                nodeValue = "";
                // put together all the text/CDATA nodes as the value of this node 
                // System.out.println(">");
                while (child != null) {
                    int nodeType = child.getNodeType();
                    // displayMetadata(child, level + 1);
                    if (nodeType == TEXT_TYPE) {
                       nodeValue = nodeValue+child.getNodeValue(); 
                    } else if (nodeType == CDATA_TYPE) {
                        nodeValue = nodeValue+child.getNodeValue(); 
                    }
                    child = child.getNextSibling();
                }
                        
            value = nodeValue;
            }
        
        
        
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        values[j] = value;  
        j++;
        }
        
     return values ;        
  }
  
  /**
  * Get the value of this node. 
  * Better than Node.getNodeValue() since this will look into any #text 
  * node child of this node and get that value out
  **/
  public String getNodeValue(Node node) {
        
    Node n;
    int nodeCt = 0;
    String value;
    
    // this is uslually empty (null) instead the real value is stored in a #text node 
    // of CData node
    value = node.getNodeValue();
    // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        
    String nodeValue = "";
    Node child = node.getFirstChild();
    if (child != null) {
        nodeValue = "";
        // put together all the text/CDATA nodes as the value of this node 
        // System.out.println(">");
        while (child != null) {
            int nodeType = child.getNodeType();
            
            if (nodeType == TEXT_TYPE) {
                nodeValue = nodeValue+child.getNodeValue(); 
            } else if (nodeType == CDATA_TYPE) {
                nodeValue = nodeValue+child.getNodeValue(); 
            }
            child = child.getNextSibling();
         }
                        
        value = nodeValue;
      }
        
        
        
     return value ;        
  }
  
  /**
   * Get a NodeIterator for a given xPath.
   **/
    // public NodeIterator getNodeIterator(Document doc, String xpath) {
    public NodeIterator getNodeIterator(Node node, String xpath) {
        
    Node n;
    int nodeCt = 0;
    String value;
    String values[] = null;
    // String attr = attribute;
    
    // xpath = "//SOUND" ;
    // System.out.println("getNodeIterator using "+xpath);
    NodeIterator nl = null;
    try {
        // nl = XPathAPI.selectNodeIterator(doc, xpath); 
        nl = XPathAPI.selectNodeIterator(node, xpath, node);      
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
  return nl;
  }
   
   /**
   * Get a NodeList for a given xPath.
   **/
    // public NodeList getNodeList(Document doc, String xpath) {
    public NodeList getNodeList(Node node, String xpath) {
        
    Node n;
    int nodeCt = 0;
    String value;
    String values[] = null;
    // String attr = attribute;
    
    // xpath = "//SOUND" ;
    // System.out.println("getNodeList using "+xpath);
    NodeList nl = null;
    try {
        // nl = XPathAPI.selectNodeList(doc, xpath);  
        nl = XPathAPI.selectNodeList(node, xpath, node); 
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
  return nl;
  }
  
    /**
   * Get a Node for a given xPath.
   * Matches the FIRST node found
   * Call getNodeList or getNodeIterator to get all matching nodes
   * Returns a Node
   **/
    // public Node getSingleNode(Document doc, String xpath) {
    public Node getSingleNode(Node node, String xpath) {
        
    Node n = null;
    
    
    // xpath = "//SOUND" ;
    // System.out.println("getSingleNode using "+xpath);
    
    try {
        // n = XPathAPI.selectSingleNode(doc, xpath); 
        n = XPathAPI.selectSingleNode(node, xpath, node);      
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
  return n;
  }
  
  /* 
   * Delete this node from its parent
   */
  public boolean deleteNode(Node target) {
  	
  		boolean ret = false;
  		if (target == null) return ret;
  		
  		Node parent = target.getParentNode();
  		Node removedNode = parent.removeChild(target);
  		if (removedNode != null) {
  			if (debug) 
  				serializeNode(removedNode, "deleteNode.xml","xml");
  			ret = true;
  		}
  		else {
  			if (debug) System.out.println("could NOT delete the node ");
  		}
  		
  	
  	return ret;
  }
  
  /* 
   * find a child Node contained in the Node using xPath. If a child node is found,
   * delete it from the supplied Node   */
  public boolean deleteNode(Node node, String xpath) {
  	
  	if (node == null) return false;
  	
  	Node target = getSingleNode(node, xpath);
  	boolean ret = false;
  	if (debug) System.out.println("DOMutils.deleteNode "+xpath);
  	if (target != null) {
  		if (debug) {
  			System.out.println("found the node "+xpath);
  			serializeNode(target, "foundNode.xml","xml");
  		}  		//now remove the node from the document
  		Node parent = target.getParentNode();
  		Node removedNode = parent.removeChild(target);
  		if (removedNode != null) {
  			if (debug) serializeNode(removedNode, "removedNode.xml","xml");
  			ret = true;
  		}
  		else {
  			if (debug) System.out.println("could NOT find the node "+xpath);
  		}
  		
  	}
  	return ret;
  }
  
  /**
  * Construct an array of all the xPath expressions for this node.
  * Each attrubute results in a different xPath expression.
  * IncludeAttributes will return a String with an attribute.
  * If there are multiple attributes there will be multile xPath
  * expressions returned, each with a DIFFERENT attribute.
  * IncludeAttributeValue will include the attribute's value in the xPath String if there
  * are attributes.
  * I don't think an xPath expression can hold a value.
  * // xpath = "//item[@key='FIRST_LINE']" ;
  **/
   public String[] getNodeXPath(Node node, boolean includeAttributes ) {
                // boolean includeValue, 
                // boolean includeAttributes, boolean includeAttributeValue) {
       
       String values[] = null; // return
       String value;
       String attributeNames[] = null;
       int nodeCt = 1;
       String nodeName = node.getNodeName();
       String nodeValue = node.getNodeValue();
       String localName = node.getLocalName();
       String prefix = node.getPrefix();
       
       // System.out.println("getNodeXPath "+nodeName+" - "+nodeValue);
       // System.out.println("localName "+localName+" - "+prefix);
       
       if (nodeName.equals("#text")) {
        return null;
       }
       
       boolean includeAttributeValue = true;
       
       if (node.hasAttributes() && includeAttributes) {
            if (includeAttributeValue == true ) {
                attributeNames = getNodeAttributesXPath(node);
            }
            else {
                attributeNames = getNodeAttributes(node);
            }
            
            // attributeNames = getNodeAttributes(node);
            // check for null ???
            nodeCt = attributeNames.length;
            values = new String[nodeCt];
            for (int i=0 ; i<nodeCt ; i++) {
                value = "//"+nodeName+attributeNames[i] ;
                values[i] = value;
            }
       }
       else {
        // no attributes only one xPath will be returned
        
    
        values = new String[nodeCt];
        value = "//"+nodeName ;
        values[0] = value;
    
       }
    return values;
   }
   
   
   /**
  * Construct an array of all the xPath expressions for this node.
  * Each attrubute results in a different xPath expression.
  * IncludeAttributes will return a String with an attribute.
  * If there are multiple attributes there will be multile xPath
  * expressions returned, each with a DIFFERENT attribute.
  * IncludeAttributeValue will include the attribute's value in the xPath String if there
  * are attributes.
  * I don't think an xPath expression can hold a value.
  * // xpath = "//item[@key='FIRST_LINE']" ;
  **/
   public String[] getNodeXPath(Node node, boolean includeAttributes, boolean includeParent ) {
                // boolean includeValue, 
                // boolean includeAttributes, boolean includeAttributeValue) {
       
       String values[] = null; // return
       String value;
       String attributeNames[] = null;
       int nodeCt = 1;
       String nodeName = node.getNodeName();
       String nodeValue = node.getNodeValue();
       String localName = node.getLocalName();
       String prefix = null;
       
       String[] parentNames = new String[0];
       Node parent = null;
       if (includeParent) {
       		prefix = node.getPrefix();
       		if (prefix == null) prefix = "";
       		parent = node.getParentNode();
       		if (parent != null) {
       			parentNames = getNodeXPath(parent, true);
       		}
       		else {
       			parentNames = new String[0];
       		}
       }
       		
       
       // System.out.println("getNodeXPath "+nodeName+" - "+nodeValue);
       // System.out.println("localName "+localName+" - "+prefix);
       
       if (nodeName.equals("#text")) {
        return null;
       }
       
       boolean includeAttributeValue = true;
       int parentCt = parentNames.length;
       
       if (node.hasAttributes() && includeAttributes) {
            if (includeAttributeValue == true ) {
                attributeNames = getNodeAttributesXPath(node);
            }
            else {
                attributeNames = getNodeAttributes(node);
            }
            
            // attributeNames = getNodeAttributes(node);
            // check for null ???
            nodeCt = attributeNames.length;
            values = new String[nodeCt*parentCt];
            for (int j=0 ; j < parentCt ; j++) {
            	for (int i=0 ; i<nodeCt ; i++) {
                	value = "/"+parentNames[j]+"/"+nodeName+attributeNames[i] ;
                	values[i] = value;
            	}
            }
       }
       else {
        // no attributes only one xPath will be returned
        
    
        values = new String[nodeCt*parentCt];
        for (int j=0 ; j < parentCt ; j++) {
        	value = "/"+parentNames[j]+"/"+nodeName ;
        	values[j] = value;
        }
    
       }
    return values;
   }
   
   /**
   * Get a NodeIterator for a given node.
   * Returns an array of Strings. Each one is an attribute name and the attributes value
   * in xPath style: <br>
   * / xpath = "//item[@key='FIRST_LINE']" ;
   * so [@key='FIRST_LINE'] would be returned 
   * item is the Element name
   * 
   **/
   public String[] getNodeAttributesXPath(Node n) {
    
    String value;
    String values[] = null;
    // String attr = attribute;
    String name, xpath;
    Node node;
    
    NamedNodeMap map = n.getAttributes();
    int nodeCt = map.getLength();
    values = new String[nodeCt];
    for (int i=0 ; i<nodeCt ; i++) { 
        node = map.item(i);
        value = node.getNodeValue();
        name = node.getNodeName();
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );       
        // value is probably in a TEXT node
        String nodeValue = "";
        Node child = node.getFirstChild();
            if (child != null) {
                nodeValue = "";
                // put together all the text/CDATA nodes as the value of this node 
                // System.out.println(">");
                while (child != null) {
                    int nodeType = child.getNodeType();
                    // displayMetadata(child, level + 1);
                    if (nodeType == TEXT_TYPE) {
                       nodeValue = nodeValue+child.getNodeValue(); 
                    } else if (nodeType == CDATA_TYPE) {
                        nodeValue = nodeValue+child.getNodeValue(); 
                    }
                    child = child.getNextSibling();
                }
                        
            value = nodeValue;
            }
        
        
        xpath = "[@"+name+"=\'"+value+"\']" ;
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        values[i] = xpath;
        }
        
     return values ;     
    
    
   }
   
   
   /**
   * Get a NodeIterator for a given node.
   * Returns an array of Strings. Each one is an attribute name
   * 
   **/
    public String[] getNodeAttributes(Node node) {
    
    Node n;
    String value;
    String values[] = null;
    // String attr = attribute;
    String name;
    
    NamedNodeMap map = node.getAttributes();
    int nodeCt = map.getLength();
    values = new String[nodeCt];
    for (int i=0 ; i<nodeCt ; i++) {
        n = map.item(i);
        name = n.getNodeName();       
        values[i] = name; 
        }
        
     return values ;        
  }
  
  /**
   * Get a NodeIterator for a given node.
   * Returns a Hashtable. 
   * Each hash is an attribute name, value pair
   * 
   **/
    public Hashtable getNodeAttributesHash(Node node) {
    
    Node n;
    String value;
    String name;
    Hashtable hash = new Hashtable();
    
    if (debug) System.out.println("  getNodeAttributesHash -----------");
    NamedNodeMap map = node.getAttributes();
    int nodeCt = map.getLength();
    for (int i=0 ; i<nodeCt ; i++) {
        n = map.item(i);
        name = n.getNodeName();  
        value = n.getNodeValue();
        hash.put(name, value);
        if (debug) System.out.println("    "+name+"  "+value);
        }
        
     return hash ;        
  }
  /***/
  
   /**
   * Get a NodeIterator for a given xPath.
   * Returns an array of Strings.
   **/
    // public String[] getNodeAttribues(Document doc, String xpath) {
    public String[] getNodeAttribues(Node node, String xpath) {
        
    Node n;
    int nodeCt = 0;
    String value;
    String values[] = null;
    // String attr = attribute;
    
    // xpath = "//SOUND" ;
    if (debug) System.out.println("Querying Node using "+xpath);
    NodeIterator ni = null;
    try {
       // ni = XPathAPI.selectNodeIterator(doc, xpath);
       ni = XPathAPI.selectNodeIterator(node, xpath, node);
        while ((n = ni.nextNode())!= null)
            { 
            nodeCt++;
            // this puts the index at the end of the list
            // get it again to "reset" the list
            // wasteful but I don't know any other way
            }
        // ni = XPathAPI.selectNodeIterator(doc, xpath);
        ni = XPathAPI.selectNodeIterator(node, xpath, node);
    }
    catch (TransformerException e) {
        System.err.println( "TransformerException "+ e);
        e.printStackTrace();
    }
    
    // this is a Global variable
    values = new String[nodeCt];
    int j=0;
    
    while ((n = ni.nextNode())!= null)
        {                       
        value = n.getNodeValue();
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        
        String nodeValue = "";
        Node child = n.getFirstChild();
            if (child != null) {
                nodeValue = "";
                // put together all the text/CDATA nodes as the value of this node 
                // System.out.println(">");
                while (child != null) {
                    int nodeType = child.getNodeType();
                    // displayMetadata(child, level + 1);
                    if (nodeType == TEXT_TYPE) {
                       nodeValue = nodeValue+child.getNodeValue(); 
                    } else if (nodeType == CDATA_TYPE) {
                        nodeValue = nodeValue+child.getNodeValue(); 
                    }
                    child = child.getNextSibling();
                }
                        
            value = nodeValue;
            }
        
        
        
        // System.out.println("nodeName "+n.getNodeName()+" = "+value );
        values[j] = value;  
        j++;
        }
        
     return values ;        
  }
   
/**
*
* Special Vicar Label item accessor
*
**/

/**
* This method 

* 3 possible locations for the "VALUE"
* 1) an attribute VALUE="somthing"
* 2) attribute NELEMENTS="X"
*     SUBITEM nodes an attribute VALUE="somthing"
* 3) where the actual value we seek is a TEXT or CDATA node which
* is a child of the "item" node.
*
**/

    public String[] getVicarLabelItemValue( Node root, String itemName) {
        String nodeValue = "";
        
        // this is the return array
        String[] values = null; // allocate when we know how many items we have
        
        int iValue;
        Node result;
        int nelements = 0;
        boolean foundValue = false;
        
        
        // we need to standardize on capitalization
        
        // construct xPath expression to locate the correct Node
        // what about multiple nodes of the same type (not allowed??)
        String xPath = "//ITEM[@KEY='"+itemName+"']" ;
        if (debug) System.out.println("PDSInputFile.getVicarLabelItemValue("+xPath+")" );
        
       try {
        result = XPathAPI.selectSingleNode(root,xPath,root);
        // get the needed data from the result node
        if (debug) System.out.println("getVicarLabelItemValue() result=" + result);
        if (result == null) {
            if (debug) System.out.println("getItemValue() UNABLE TO LOCATE NODE FOR: "+xPath);
            return values;
        } else {
            
            // check for the value in an attribute
            
            String name;
            String value;
            Node n;
    
            NamedNodeMap map = result.getAttributes();
            int nodeCt = map.getLength();
            values = new String[nodeCt];
            for (int i=0 ; i<nodeCt ; i++) {
                n = map.item(i);
                name = n.getNodeName();  
                // attribute nodes do have values
                value = n.getNodeValue();
                // values[i] = name; 
                if (name.equalsIgnoreCase("value") ) {
                    nodeValue = value;
                    foundValue = true; // prevent further searches for a value
                    values = new String[1];
                    values[0] = nodeValue;
                }
                else if (name.equalsIgnoreCase("nelements") ) {                   
                    nelements = Integer.parseInt(value);
                } 
                else if (name.equalsIgnoreCase("type") ) {
                    // here so we know how to get the type attribute
                }
            }
            
            // see if the values are in SUBITEM elements
            if (nelements > 0 ) {
                // go get sub elements
                values = new String[nelements]; // assume nelements is correct
                int nel = 0; // counter used for the values array
                
                Node child = result.getFirstChild();
                // nodeValue = result.getNodeValue();
                if (debug) System.out.println("getItemValue() nodeValue=" + nodeValue);
                if (child != null) {
                
                  while (child != null) {
                    
                    name = child.getNodeName(); 
                    if (name.equalsIgnoreCase("SUBITEM") ) {
                        // NamedNodeMap map
                        map = child.getAttributes();
                        nodeCt = map.getLength();
                        // values = new String[nodeCt];
                        // use nelements above instead
                        
                        // the only attribute we expect is "VALUE"
                        // others will be ignored for now, thats why we use nel
                        // instead of i as the index into values[]
                        // since these are attribute nodes it will hold the value
                        for (int i=0 ; i<nodeCt ; i++) {
                            n = map.item(i);
                            name = n.getNodeName();  
                            // attribute nodes do have values
                            value = n.getNodeValue();
                            // values[i] = name; 
                            if (name.equalsIgnoreCase("value") ) {
                                values[nel++] = value;
                            }  
                        }
                    }
                
                    }
                    child = child.getNextSibling();
                }   
                
                return values;
              } // matches: if (nelements > 0 )
                
            
            
            
            // check for values in SUBITEM nodes
            
            
            if (foundValue == false) {
             // check for the value in a TEXT or CDATA node
             Node child = result.getFirstChild();
             nodeValue = result.getNodeValue();
             if (debug) System.out.println("getItemValue() nodeValue=" + nodeValue);
             if (child != null && nodeValue == null) {
                
                while (child != null) {
                    int type = child.getNodeType();
                    // displayMetadata(child, level + 1);
                    if (type == TEXT_TYPE) {
                       nodeValue = child.getNodeValue(); 
                    } else if (type == CDATA_TYPE) {
                        nodeValue = child.getNodeValue(); 
                    }
                    child = child.getNextSibling();
                }           
              } 
              
            values = new String[1];
            values[0] = nodeValue;
            } 
        }
       }
        catch (TransformerException se) {
            System.out.println("TransformerException "+se);
        }
        
         if (debug) System.out.println("getVicarLabelItemValue() return nodeValue=" + nodeValue);
        return values;
    }

  
/**
* This method assumes the value we want is from an "item" node
* where the actual value we seek is a TEXT or CDATA node which
* is a child of the "item" node.
*
**/

    public String getItemValue( Node root, String xPath) {
        String nodeValue = "";
        String childNodeValue = "";
        String name;
        int iValue;
        Node result;
        
        if (debug)  {
        	name = root.getNodeName();
        	Hashtable attr = getNodeAttributesHash(root);
        	System.out.println("node name "+name );
        	
        	System.out.println("attr "+attr);
        
        	System.out.println("DOMUtils.getItemValue("+xPath+")" );
        	serializeNode(root, name+".xml","xml");
        }
       try {
       	// arg 3 is the namespace node
        result = XPathAPI.selectSingleNode(root,xPath, root);
        // get the needed data from the result node
         if (debug) System.out.println("getItemValue() result=" + result);
        if (result == null) {
            if (debug) System.out.println("getItemValue() UNABLE TO LOCATE NODE FOR: "+xPath);
            return nodeValue;
        } else {
            
        	
            Node child = result.getFirstChild();
            nodeValue = result.getNodeValue();
            childNodeValue = child.getNodeValue();
             if (debug) {
             	System.out.println("getItemValue() nodeValue=" + nodeValue);
             	System.out.println("getItemValue() childNodeValue=" + childNodeValue);
             }
            if (child != null && nodeValue == null) {
                
                while (child != null) {
                    int type = child.getNodeType();
                    
                    // displayMetadata(child,  level + 1);
                    if (type == TEXT_TYPE) {
                       nodeValue = child.getNodeValue(); 
                    } else if (type == CDATA_TYPE) {
                        nodeValue = child.getNodeValue(); 
                    }
                    child = child.getNextSibling();
                }
                
            } 
            
        }
       }
        catch (TransformerException se) {
            System.out.println("TransformerException "+se);
        }
        
        if (debug) System.out.println("getItemValue() return nodeValue=" + nodeValue);
        return nodeValue;
    }

    
    /**
    * This method assumes the value we want is from an "item" node
    * where the actual value we seek is a TEXT or CDATA node which
    * is a child of the "item" node.
    *
    **/

        public String getSubitemValue( Node root, String xPath) {
            String nodeValue = "";
            int iValue;
            Node result;
            
            if (debug)  {
            	String name = root.getNodeName();
            	Hashtable attr = getNodeAttributesHash(root);
            	System.out.println("node name "+name );
            	
            	System.out.println("attr "+attr);
            
            	System.out.println("DOMUtils.getSubitemValue("+xPath+")" );
            }
           try {
           	// arg 3 is the namespace node
            result = XPathAPI.selectSingleNode(root,xPath, root);
            // get the needed data from the result node
             if (debug) System.out.println("getSubitemValue() result=" + result);
            if (result == null) {
                if (debug) System.out.println("getSubitemValue() UNABLE TO LOCATE NODE FOR: "+xPath);
                return nodeValue;
            } else {
                
                Node child = result.getFirstChild();
                nodeValue = result.getNodeValue();
                 if (debug) System.out.println("getSubitemValue() nodeValue=" + nodeValue);
                if (child != null && nodeValue == null) {
                    
                    while (child != null) {
                        int type = child.getNodeType();
                        // displayMetadata(child, level + 1);
                        if (type == TEXT_TYPE) {
                           nodeValue = child.getNodeValue(); 
                        } else if (type == CDATA_TYPE) {
                            nodeValue = child.getNodeValue(); 
                        }
                        child = child.getNextSibling();
                    }
                    
                } 
                
            }
           }
            catch (TransformerException se) {
                System.out.println("TransformerException "+se);
            }
            
            if (debug) System.out.println("getSubitemValue() return nodeValue=" + nodeValue);
            return nodeValue;
        }

/**
* This method is a convenience so we don't need to catch the exceptions
* in the main code.
*
**/

    public Node getResultNode( Node root, String xPath) {
        
        Node result = null;
        Node result1 = null;
        Node result2 = null;
        Node result3 = null;
       
       if (root == null) {
       	if (debug) System.out.println("DOMutils.getResultNode( root is NULL )" );
       	return result;
       } 
       if (debug) {
       	System.out.println("DOMutils.getResultNode("+xPath+")" );
       	System.out.println("DOMutils.getResultNode("+root+")" );
       	serializeNode(root,"getResultNode_root.xml","xml");
       }
       try {
       	// there are 3 ways I have tried, they all work the same. Just do one
       	// don't waste time doing it more than once
       	/*
       	XPathAPI xp = new XPathAPI();
       	if (debug) System.out.println("DOMutils.getResultNode() xp "+xp +" *****************");
       	XObject xo = xp.eval(root, xPath);
       	if (debug) System.out.println("DOMutils.getResultNode() xo "+xo);
       	
       	NodeList nl = xo.nodelist();
       	if (debug) System.out.println("DOMutils.getResultNode() nl.length() "+nl.getLength() );
       	if (nl.getLength() > 0) {
        	result1 = nl.item(0);
        	// get the needed data from the result node
        	if (result1 != null) {
        		result = result1;
        		if (debug) {
        		System.out.println("DOMutils.getResultNode() result1  "+result1);
        		serializeNode(result1,"getResultNode_result1.xml","xml");
        		}
        	}
        		
       	}
       	**/
       	
        result2 = XPathAPI.selectSingleNode(root,xPath, root);
        // result = xp.selectSingleNode(root,xPath, root);
        if (result2 != null) {
        	result = result2;
        	
        	if (debug) {
        		System.out.println("DOMutils.getResultNode() result2 "+result2);
        		serializeNode(result2,"getResultNode_result2.xml","xml");
        	}
        }
        
        /*
        nl = xp.selectNodeList(root, xPath);
        if (debug) System.out.println("DOMutils.getResultNode() nl.length() "+nl.getLength() );
        if (nl.getLength() > 0) {
        	result3 = nl.item(0);
        	if (result3 != null) {
        		result = result3;
        		// get the needed data from the result node
        		if (debug) {
        			System.out.println("DOMutils.getResultNode() result3 "+result3);
        			serializeNode(result3,"getResultNode_result3.xml","xml");
        		}
        	}
        }
        */
       }
        catch (TransformerException se) {
            System.out.println("TransformerException "+se);
        }
        
        if (debug) {
        	System.out.println("DOMutils.getResultNode() returning: result "+result);
        }
        return result;
    }
  
  
  /**
    * encodeEntities
    * convert a string with values which are not allowed in a CDATA
    * into the standard entity representations
    **/
    public String encodeEntities(String s) {
    
        // &amp;  & 
        // &lt;   <
        // &gt;   >
        // &quot; "
        // &apos; '
        // do string substitution for each entity
        
        // use the pattern matching perl utilities to do this
        Perl5Util perl = new Perl5Util();
        String newS = s;
        
        newS = perl.substitute("s/&/&amp;/g", newS); 
        newS = perl.substitute("s/</&lt;/g", newS); 
        newS = perl.substitute("s/>/&gt;/g", newS); 
        newS = perl.substitute("s/\"/&quot;/g", newS); 
        newS = perl.substitute("s/'/&apos;/g", newS); 
        return newS;
    }
  
  /**
    * decodeEntities
    * convert a string with values which are not allowed in a XML
    * entities are NOT alowed in CDATA
    * into the standard entity representations
    **/
    public String decodeEntities(String s) {
        Perl5Util perl = new Perl5Util();
        // &amp;  & 
        // &lt;   <
        // &gt;   >
        // &quot; "
        // &apos; '
        String newS = s;
        newS = perl.substitute("s/&amp;/&/g", newS); 
        newS = perl.substitute("s/&lt;/</g", newS); 
        newS = perl.substitute("s/&gt;/>/g", newS); 
        newS = perl.substitute("s/&quot;/\"/g", newS); 
        newS = perl.substitute("s/&apos;/'/g", newS); 
        return newS;
    }
  
  // ----------------- main ------------------------------------------------ 
  /**
  * 
  * example command lines:
  *
  * 
  * xslMerge file1.xml file2.xml file.xsl XPath
  *
  * merge 2 xml files into a single one and serialize the result
  * merge file1.xml file2.xml xPath
  *
  * run an xsl transform on an xml Doc and serialize the result
  * xsl file.xml file.xsl
  *
  * search a Document using XPath expressions
  * xpath file.xml xpath attribute
  *
  * * simple reserialize
  * xml infile outfile
  **********
  * to do an XSL transfer and also do some xpath
  * i942299r.vicar_label.xml a.xsl out.xml //PDS/item key false
  *
  * merge two documents
  * i942299r.vicar_label.xml other.xml out.xml merge
  *
  **/
	public static void main(String[] args)
    throws TransformerException, TransformerConfigurationException, FileNotFoundException,
           ParserConfigurationException, SAXException, IOException
  {    
  	
    String operation = "help";
    if (args.length != 0) {
   		operation = args[0];
    }
    
    System.out.println("Properties  ----------------------------");
    Properties prop = new Properties(System.getProperties());
    prop.list(System.out);
    System.out.println("---------------------------- "+operation);
    
    String outFile1 = "out1.xml";
    String outFile2 = "out2.xml" ;
    String outFile3 = "out3.xml" ;
    String outFile4 = "out4.xml" ;
    
    DOMutils domUtil = new DOMutils();
    
    
    boolean doMerge = false;
    // use operation to decide how we proceed
	if (operation.equals("help") || operation.equals("?") ||
		args.length == 0) {
		System.out.println("DOMutils help:");
		System.out.println("xml");
		System.out.println("read in then serialize an xml file (check that it is read ok)");
		System.out.println("> DOMutils  xml in.xml out.xml");
		 System.out.println("xslMerge");
        System.out.println("read in the first xml file and create an in memory Document");
        System.out.println("serialize to out1.xml for debugging purposes");
       System.out.println("read in the second xml file, apply an xsl transform to the Doc");
       System.out.println("serialize to file2.xml out2.xml ");
        System.out.println("serialize to trasformed xml to out3.xml");
        System.out.println("merge the first Doc with the transformed Doc");
        System.out.println(" if XPath isn't empty use the XPath to decide what Nodes to merge");
        System.out.println( "if XPath is \"-\" then it will not be used");
        System.out.println( "serialize the merged Doc to out4.xml");
       System.out.println(">DOMutils xslMerge file1.xml file2.xml file.xsl XPath");
        
		System.out.println(" merge");
       System.out.println(" merge 2 xml files into a single one and serialize the result");
       System.out.println(" results are sent to out1.xml out2.xml out3.xml");
        System.out.println("> DOMutils  merge file1.xml file2.xml ");
        System.out.println("pds");
        System.out.println("input a PDS label xml file, write as a pds label");
		System.out.println("> DOMutils pds xmlFile outFile");
        System.out.println("xsl");
        System.out.println("input an xml file, apply xsl and serialize the result");
        System.out.println("> DOMutils xsl in.xml in.xsl out.xml");
        System.out.println("pds2");
        System.out.println("serialize a pds xml label made from a null BuffereImage");
        System.out.println("then format as a PDS label");
        System.out.println("> DOMutils pds2 in.xml out.pds");
		System.out.println("entity arg");
		}
    else if (operation.equals("entity")) {
        String s = args[1];
        System.out.println("---enity replacement "+s);
        s = domUtil.encodeEntities(s);
        System.out.println("---encode "+s);
        s = domUtil.decodeEntities(s);
        System.out.println("---decode "+s);
        
        
    }
    else if (operation.equals("xml")) {
        // xslMerge
        // read in the first xml file and create an in memory Document
        // read in the second xml file, apply an xsl transform to the Doc
        // serialize for debugging purposes
        // merge the first Doc with the transformed Doc
        // if XPath isn't empty use the XPath to decide what Nodes to merge
        // serialize the merged Doc
        // xslMerge file1.xml file2.xml file.xsl XPath
        // if XPath is "-" then it will not be used
        String xmlFile1 = args[1];
		outFile1 = args[2];
        
        System.out.println("--- xml");
        System.out.println("--- doc1 from "+xmlFile1);
        System.out.println("--- buildDocument "+xmlFile1);
        Document doc1 = domUtil.buildDocument(xmlFile1);
        System.out.println("----------------------------");
        domUtil.serializeDocument(doc1, outFile1, "xml");
        
        System.out.println("----------------------------");

        
        System.out.println("--- result is in "+outFile3);
    }
    else if (operation.equals("xslMerge")) {
        // xslMerge
        // read in the first xml file and create an in memory Document
        // read in the second xml file, apply an xsl transform to the Doc
        // serialize for debugging purposes
        // merge the first Doc with the transformed Doc
        // if XPath isn't empty use the XPath to decide what Nodes to merge
        // serialize the merged Doc
        // xslMerge file1.xml file2.xml file.xsl XPath
        // if XPath is "-" then it will not be used
        String xmlFile1 = args[1];
        String xmlFile2 = args[2];
        String xslFile1 = args[3];
        String xPath = args[4];
        
        
        System.out.println("--- merge");
        System.out.println("--- doc1 from "+xmlFile1);
        System.out.println("--- buildDocument "+xmlFile1);
        Document doc1 = domUtil.buildDocument(xmlFile1);
        System.out.println("----------------------------");
        domUtil.serializeDocument(doc1, outFile1, "xml");
        System.out.println("--- serlaize to "+outFile1);
        
        System.out.println("----------------------------");
        System.out.println("--- buildDocument "+xmlFile2);
        Document doc2 = domUtil.buildDocument(xmlFile2); 
        domUtil.serializeDocument(doc2, outFile2, "xml");
        System.out.println("--- serlaize to "+outFile2);
        System.out.println("--- transform "+xmlFile2+" with "+xslFile1);
        Document doc3 = domUtil.transformDocument(doc2, xslFile1);
        domUtil.serializeDocument(doc3, outFile3, "xml");
        System.out.println("--- serlaize to "+outFile3);
        System.out.println("----------------------------");
        // merge the two 
        // Document doc3 = domUtil.merge(doc1, doc2);
        // add an argument (xpath) of the node to merge on 
        // xPath = "//PDS_LABEL" ;
        System.out.println("--- merge doc1 with doc3 using xPath "+xPath);
        Document doc4 = domUtil.mergeDocuments(doc1, doc3, xPath);
        
        domUtil.serializeDocument(doc4, outFile4, "xml");
        System.out.println("--- result is in "+outFile4);
    }
    else if (operation.equals("merge")) {
        // merge
        // merge 2 xml files into a single one and serialize the result
        // merge file1.xml file2.xml xPath
        String xmlFile1 = args[1];
        String xmlFile2 = args[2];
        String xPath = args[3];
        
        System.out.println("--- merge");
        System.out.println("--- buildDocument "+xmlFile1);
        Document doc1 = domUtil.buildDocument(xmlFile1);
        
        System.out.println("----------------------------");
        domUtil.serializeDocument(doc1, outFile1, "xml");
        System.out.println("----------------------------");
        System.out.println("--- buildDocument "+xmlFile2);
        Document doc2 = domUtil.buildDocument(xmlFile2); // really a second xml file
        domUtil.serializeDocument(doc2, outFile2, "xml");
        System.out.println("----------------------------");
        
        // merge the two 
        // xPath = "//PDS_LABEL" ;
        Document doc3 = domUtil.mergeDocuments(doc1, doc2, xPath); 
        domUtil.serializeDocument(doc3, outFile3, "xml");
    }
     else if (operation.equals("serialize")) {
        // xsl
        // apply an xsl transform to an xml file and serialize the result
        // xsl file.xml file.xsl
        String xmlFile = args[1];
        String outFile = args[2];
        
        Document startDoc = domUtil.buildDocument(xmlFile);
	
	    domUtil.serializeDocument(startDoc, outFile, "xml");
    }
    else if (operation.equals("pds2")) {
        // xsl
        // apply an xsl transform to an xml file and serialize the result
        // xsl file.xml file.xsl
        
        String xmlFile = args[1];
        String outFile = args[2];
        
        // Document startDoc = domUtil.buildDocument(xmlFile);
        BufferedImage bi = null;
        ImageToPDS_DOM  im2DOM = new ImageToPDS_DOM(bi);
        Document pdsDoc = im2DOM.getDocument();
	
	    domUtil.serializeDocument(pdsDoc,xmlFile, "xml");
	    
	    // now create the PDS label
	    
	     DOMtoPDSlabel dom2pds = new DOMtoPDSlabel(pdsDoc, outFile);
	     String s = dom2pds.toString();
	     // open a file
	    // write to the file
	    System.out.println(s);
	    
	    PrintWriter out = new PrintWriter(new FileOutputStream(outFile));
	    out.println(s);
	    out.close();
	    
	     
	     
	    
    }
    else if (operation.equals("pds")) {
        // xsl
        // apply an xsl transform to an xml file and serialize the result
        // xsl file.xml file.xsl
        String xmlFile = args[1];
        String outFile = args[2];
        
        Document pdsDoc = domUtil.buildDocument(xmlFile);
        
	
	   //  domUtil.serializeDocument(pdsDoc,xmlFile, "xml");
	    
	    // now create the PDS label
	    
	     DOMtoPDSlabel dom2pds = new DOMtoPDSlabel(pdsDoc, outFile);
	     String s = dom2pds.toString();
	     // open a file
	    // write to the file
	    System.out.println(s);
	    
	    PrintWriter out = new PrintWriter(new FileOutputStream(outFile));
	    out.println(s);
	    out.close();
	    
	     
	     
	    
    }
    else if (operation.equals("xsl")) {
        // xsl
        // apply an xsl transform to an xml file and serialize the result
        // xsl file.xml file.xsl
        String xmlFile = args[1];
        String xslFile = args[2];
        outFile1 = args[3];
        
        Document startDoc = domUtil.buildDocument(xmlFile);
    
        Document resultDoc = domUtil.transformDocument(startDoc, xslFile);
	
	    domUtil.serializeDocument(resultDoc, outFile1, "xml");
    }
    else if (operation.equalsIgnoreCase("xpath")) {
        // xpath
        // search a Document using XPath expressions
        // xpath file.xml xpath attribute
        String xmlFile = args[1];
        String xpath = args[2];
        String attribute = args[3];
        if (attribute.equals("-")) attribute = null;
        boolean returnXPath = true;
        
        Document startDoc = domUtil.buildDocument(xmlFile);
        
        
        if (xpath != null) {
	        String v[]= null;
	        if (attribute != null) {
	            v = domUtil.getNodeValues(startDoc, xpath, attribute, returnXPath);
	        }
	        else {
	            v = domUtil.getNodeValues(startDoc, xpath);
	        }
	        if (v != null) {
	            int len = v.length;
	            System.out.println(len+" values for "+xpath+" "+attribute);
	            // print out all the returned node values
	            for (int i=0 ; i< len ; i++) {
	                System.out.println(i+") "+v[i]);
	            
	                if (returnXPath) {
	                    // use v[i] as an xPath expression to get the values for each item returned
	                    String xp = v[i];     
	                    String vv[] = domUtil.getNodeValues(startDoc, xp);
	                    System.out.println("   "+vv.length+"  > "+vv[0]);
	            
	                    // this is a convenience methos when there should only be 1 value
	                    String vvv = domUtil.getNodeValue(startDoc, xp);
	                    System.out.println("   "+vvv);
	                    }
	                }
	            }
	        }
    } 
    else {
        System.out.println("invalid arguments");
    }
    
  } // end of main
} // end of DOMUtils

