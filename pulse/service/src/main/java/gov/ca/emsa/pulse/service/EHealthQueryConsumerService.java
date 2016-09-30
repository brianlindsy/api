package gov.ca.emsa.pulse.service;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;

import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;

import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201310UV02;
import org.opensaml.common.SAMLException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

public interface EHealthQueryConsumerService {
	public PRPAIN201305UV02 unMarshallPatientDiscoveryRequestObject(String xml) throws SOAPException, SAMLException;
	public AdhocQueryRequest unMarshallDocumentQueryRequestObject(String xml) throws SAMLException;
	public RetrieveDocumentSetRequestType unMarshallDocumentSetRetrieveRequestObject(String xml) throws SAMLException;
	public String marshallPatientDiscoveryResponse(PRPAIN201310UV02 response) throws JAXBException;
	public String createSOAPFault();
	public String marshallDocumentQueryResponse(AdhocQueryResponse responseObj) throws JAXBException;
	public String marshallDocumentSetResponse(RetrieveDocumentSetResponseType responseObj) throws JAXBException;
	public SAMLCredential getSAMLAssertion(SOAPMessage saajSoap) throws SOAPException, SAMLException;
	public SAMLCredential unMarshallSAMLCredential(String saml) throws SOAPException, SAMLException;
}