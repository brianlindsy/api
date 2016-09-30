package gov.ca.emsa.pulse.service.impl;

import gov.ca.emsa.pulse.service.EHealthQueryConsumerService;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import gov.ca.emsa.pulse.service.controller.PatientDiscoveryController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;

import org.w3c.dom.Node;

import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElementFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;

import org.hl7.v3.*;
import org.opensaml.common.SAMLException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.parser.SAMLObject;
import org.springframework.stereotype.Service;
import org.springframework.ws.soap.SoapElement;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Service
public class EHealthQueryConsumerServiceImpl implements EHealthQueryConsumerService{

	private static final Logger logger = LogManager.getLogger(EHealthQueryConsumerServiceImpl.class);

	public String createSOAPFault(){
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage();
			soapMessage.getSOAPBody().addFault(SOAPConstants.SOAP_SENDER_FAULT, "There was no SAML security header in the SOAP message.");
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			soapMessage.writeTo(outputStream);
		} catch (SOAPException | IOException e) {
			
		}
		String fault = new String(outputStream.toByteArray());

		return fault;
	} 

	public boolean checkSecurityHeading(SaajSoapMessage saajSoap){
		Iterator<SoapHeaderElement> security = saajSoap.getSoapHeader().examineAllHeaderElements();
		while(security.hasNext()){
			SoapHeaderElement headerElem = security.next();
			if(headerElem.getName().getLocalPart().equals("Security")){
				SoapHeaderElement wsse = headerElem;
				return true;
			}
		}
		return false;
	}
	
	public SAMLCredential getSAMLAssertion(SOAPMessage soap) throws SOAPException, SAMLException{
		Iterator<SOAPHeaderElement> security = soap.getSOAPHeader().examineAllHeaderElements();
		while(security.hasNext()){
			SOAPHeaderElement headerElem = security.next();
			if(headerElem.getElementName().getLocalName().equals("Security")){
				Node contents = headerElem.getFirstChild().getNextSibling();
				String string = nodeToString(contents);
				SAMLCredential samlCred = unMarshallSAMLCredential(string);
				return samlCred;
			}
		}
		return null;
	}
	
	private String nodeToString(Node node) {
		  StringWriter sw = new StringWriter();
		  try {
		    Transformer t = TransformerFactory.newInstance().newTransformer();
		    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		    t.transform(new DOMSource(node), new StreamResult(sw));
		  } catch (TransformerException te) {
		    System.out.println("nodeToString Transformer Exception");
		  }
		  return sw.toString();
		}
	
	public String marshallPatientDiscoveryResponse(PRPAIN201310UV02 response) throws JAXBException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage();
		} catch (SOAPException e) {
			logger.error(e);
		}
		JAXBElement<PRPAIN201310UV02> je = new JAXBElement<PRPAIN201310UV02>(new QName("PRPAIN201310UV02"), PRPAIN201310UV02.class, response);
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Marshaller documentMarshaller = createMarshaller(createJAXBContext(response.getClass()));
		documentMarshaller.marshal(je, document);
		try {
			soapMessage.getSOAPBody().addDocument(document);
		} catch (SOAPException e1) {
			e1.printStackTrace();
		}
		OutputStream sw = new ByteArrayOutputStream();
		try {
			soapMessage.writeTo(sw);
		} catch (IOException | SOAPException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	public String marshallDocumentQueryResponse(AdhocQueryResponse response) throws JAXBException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage();
		} catch (SOAPException e) {
			logger.error(e);
		}
		JAXBElement<AdhocQueryResponse> je = new JAXBElement<AdhocQueryResponse>(new QName("AdhocQueryResponse"), AdhocQueryResponse.class, response);
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Marshaller documentMarshaller = createMarshaller(createJAXBContext(response.getClass()));
		documentMarshaller.marshal(je, document);
		try {
			soapMessage.getSOAPBody().addDocument(document);
		} catch (SOAPException e1) {
			e1.printStackTrace();
		}
		OutputStream sw = new ByteArrayOutputStream();
		try {
			soapMessage.writeTo(sw);
		} catch (IOException | SOAPException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	public String marshallDocumentSetResponse(RetrieveDocumentSetResponseType response) throws JAXBException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage();
		} catch (SOAPException e) {
			logger.error(e);
		}
		JAXBElement<RetrieveDocumentSetResponseType> je = new JAXBElement<RetrieveDocumentSetResponseType>(new QName("RetrieveDocumentSetResponseType"), RetrieveDocumentSetResponseType.class, response);
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Marshaller documentMarshaller = createMarshaller(createJAXBContext(response.getClass()));
		documentMarshaller.marshal(je, document);
		try {
			soapMessage.getSOAPBody().addDocument(document);
		} catch (SOAPException e1) {
			e1.printStackTrace();
		}
		OutputStream sw = new ByteArrayOutputStream();
		try {
			soapMessage.writeTo(sw);
		} catch (IOException | SOAPException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	public Marshaller createMarshaller(JAXBContext jc){
		// Create JAXB marshaller
		Marshaller marshaller = null;
		try {
			marshaller = jc.createMarshaller();
		}
		catch (Exception ex) {
			logger.error(ex);
		}
		return marshaller;
	}

	public JAXBContext createJAXBContext(Class<?> classObj){
		// Create a JAXB context
		JAXBContext jc = null;
		try {
			jc = JAXBContext.newInstance(classObj);
		}
		catch (Exception ex) {
			logger.error(ex);
		}
		return jc;
	}

	public Unmarshaller createUnmarshaller(JAXBContext jc){
		// Create JAXB unmarshaller
		Unmarshaller unmarshaller = null;
		try {
			unmarshaller = jc.createUnmarshaller();
		}
		catch (Exception ex) {
			logger.error(ex);
		}
		return unmarshaller;
	}
	
	public SAMLCredential unMarshallSAMLCredential(String saml) throws SOAPException, SAMLException{

		// Create a JAXB context
		JAXBContext jc = createJAXBContext(SAMLObject.class);

		// Create JAXB unmarshaller
		Unmarshaller unmarshaller = createUnmarshaller(jc);

		JAXBElement<?> samlObj = null;
		try {
			samlObj = (JAXBElement<?>) unmarshaller.unmarshal(new StreamSource(new StringReader(saml)), SAMLObject.class);
		} catch (JAXBException e) {
			logger.error(e);
		}
		return (SAMLCredential) samlObj.getValue();
	}

	public PRPAIN201305UV02 unMarshallPatientDiscoveryRequestObject(String xml) throws SOAPException, SAMLException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException | SOAPException e) {
			logger.error(e);
		}
		SaajSoapMessage saajSoap = new SaajSoapMessage(soapMessage);
		
		SoapHeaderElement wsse = null;

		if(checkSecurityHeading(saajSoap)){
			
			//SAMLCredential saml = getSAMLAssertion(soapMessage);
			//logger.info("SAML: " + saml);
			Source requestSource = saajSoap.getSoapBody().getPayloadSource();
			// Create a JAXB context
			JAXBContext jc = createJAXBContext(PRPAIN201305UV02.class);

			// Create JAXB unmarshaller
			Unmarshaller unmarshaller = createUnmarshaller(jc);

			JAXBElement<?> requestObj = null;
			try {
				requestObj = (JAXBElement<?>) unmarshaller.unmarshal(requestSource, PRPAIN201305UV02.class);
			} catch (JAXBException e) {
				logger.error(e);
			}

			return (PRPAIN201305UV02) requestObj.getValue();
		}else{
			logger.error("SOAP message does not have a SAML header");
			throw new SAMLException();
		}
	}

	public AdhocQueryRequest unMarshallDocumentQueryRequestObject(String xml) throws SAMLException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException | SOAPException e) {
			logger.error(e);
		}
		SaajSoapMessage saajSoap = new SaajSoapMessage(soapMessage);

		if(checkSecurityHeading(saajSoap)){

			Source requestSource = saajSoap.getSoapBody().getPayloadSource();

			// Create a JAXB context
			JAXBContext jc = null;
			try {
				jc = JAXBContext.newInstance(AdhocQueryRequest.class);
			}
			catch (Exception ex) {
				logger.error(ex);
			}

			// Create JAXB unmarshaller
			Unmarshaller unmarshaller = null;
			try {
				unmarshaller = jc.createUnmarshaller();
			}
			catch (Exception ex) {
				logger.error(ex);
			}
			JAXBElement<?> requestObj = null;
			try {
				requestObj = (JAXBElement<?>) unmarshaller.unmarshal(requestSource, AdhocQueryRequest.class);
			} catch (JAXBException e) {
				logger.error(e);
			}
			return (AdhocQueryRequest) requestObj.getValue();
		}else{
			logger.error("SOAP message does not have a SAML header");
			throw new SAMLException();
		}
	}

	public RetrieveDocumentSetRequestType unMarshallDocumentSetRetrieveRequestObject(String xml) throws SAMLException{
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			logger.error(e1);
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException | SOAPException e) {
			logger.error(e);
		}
		SaajSoapMessage saajSoap = new SaajSoapMessage(soapMessage);

		if(checkSecurityHeading(saajSoap)){

			Source requestSource = saajSoap.getSoapBody().getPayloadSource();

			// Create a JAXB context
			JAXBContext jc = null;
			try {
				jc = JAXBContext.newInstance(RetrieveDocumentSetRequestType.class);
			}
			catch (Exception ex) {
				logger.error(ex);
			}

			// Create JAXB unmarshaller
			Unmarshaller unmarshaller = null;
			try {
				unmarshaller = jc.createUnmarshaller();
			}
			catch (Exception ex) {
				logger.error(ex);
			}
			JAXBElement<?> requestObj = null;
			try {
				requestObj = (JAXBElement<?>) unmarshaller.unmarshal(requestSource, RetrieveDocumentSetRequestType.class);
			} catch (JAXBException e) {
				logger.error(e);
			}

			return (RetrieveDocumentSetRequestType) requestObj.getValue();

		}else{
			logger.error("SOAP message does not have a SAML header");
			throw new SAMLException();
		}
	}

}
