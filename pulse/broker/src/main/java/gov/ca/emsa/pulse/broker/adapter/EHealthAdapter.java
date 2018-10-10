package gov.ca.emsa.pulse.broker.adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.opensaml.common.SAMLException;
import org.opensaml.xml.io.MarshallingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.DOMException;

import com.google.common.collect.MultimapBuilder;

import gov.ca.emsa.pulse.auth.user.CommonUser;
import gov.ca.emsa.pulse.broker.adapter.service.EHealthQueryProducerService;
import gov.ca.emsa.pulse.broker.dao.NameTypeDAO;
import gov.ca.emsa.pulse.broker.dto.AuditDocumentDTO;
import gov.ca.emsa.pulse.broker.dto.AuditEventDTO;
import gov.ca.emsa.pulse.broker.dto.AuditHumanRequestorDTO;
import gov.ca.emsa.pulse.broker.dto.AuditPatientDTO;
import gov.ca.emsa.pulse.broker.dto.AuditQueryParametersDTO;
import gov.ca.emsa.pulse.broker.dto.AuditRequestDestinationDTO;
import gov.ca.emsa.pulse.broker.dto.AuditRequestSourceDTO;
import gov.ca.emsa.pulse.broker.dto.AuditSourceDTO;
import gov.ca.emsa.pulse.broker.dto.DocumentDTO;
import gov.ca.emsa.pulse.broker.dto.DocumentQueryResults;
import gov.ca.emsa.pulse.broker.dto.DomainToDtoConverter;
import gov.ca.emsa.pulse.broker.dto.DtoToDomainConverter;
import gov.ca.emsa.pulse.broker.dto.EndpointDTO;
import gov.ca.emsa.pulse.broker.dto.NameTypeDTO;
import gov.ca.emsa.pulse.broker.dto.PatientEndpointMapDTO;
import gov.ca.emsa.pulse.broker.dto.PatientRecordDTO;
import gov.ca.emsa.pulse.broker.dto.PatientRecordResults;
import gov.ca.emsa.pulse.broker.manager.AuditEventManager;
import gov.ca.emsa.pulse.broker.saml.SAMLInput;
import gov.ca.emsa.pulse.common.domain.Document;
import gov.ca.emsa.pulse.common.domain.Patient;
import gov.ca.emsa.pulse.common.domain.PatientEndpointMap;
import gov.ca.emsa.pulse.common.domain.PatientRecord;
import gov.ca.emsa.pulse.common.domain.PatientSearch;
import gov.ca.emsa.pulse.common.soap.JSONToSOAPService;
import gov.ca.emsa.pulse.common.soap.SOAPToJSONService;
import gov.ca.emsa.pulse.cten.IheStatus;
import gov.ca.emsa.pulse.service.AuditUtil;
import gov.ca.emsa.pulse.service.UserUtil;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType.DocumentResponse;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.mime.MimeMessage;
import org.springframework.xml.transform.StringSource;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

@Component
public class EHealthAdapter implements Adapter {
	public static final String HOME_COMMUNITY_ID = "2.16.840.1.113883.9.224";
	private static final Logger logger = LogManager.getLogger(EHealthAdapter.class);

	@Value("${mtomMutltipartRequest}")
	private String mtomMutltipartRequest;

	@Value("${defaultRequestTimeoutSeconds}")
	private Long defaultRequestTimeoutSeconds;

	@Value("${defaultConnectTimeoutSeconds}")
	private Long defaultConnectTimeoutSeconds;

	@Value("${pulseOID}")
	private String pulseOID;

	@Value("${ocprhioOID}")
	private String ocprhioOID;

	@Value("${schieOID}")
	private String santaCruzOID;

	@Value("${ucdavisOID}")
	private String ucdavisOID;

	@Autowired JSONToSOAPService jsonConverterService;
	@Autowired SOAPToJSONService soapConverterService;
	@Autowired EHealthQueryProducerService queryProducer;
	@Autowired NameTypeDAO nameTypeDao;
	@Autowired AuditEventManager auditManager;
	private RestTemplate restTemplate;
	class MyMultiValueMap extends LinkedMultiValueMap<String, Object>
	{}

	@PostConstruct
	public void initRestTemplate() {
		restTemplate = new RestTemplate();
		SimpleClientHttpRequestFactory rf =
				(SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		rf.setConnectTimeout(defaultConnectTimeoutSeconds.intValue() * 1000);
		rf.setReadTimeout(defaultRequestTimeoutSeconds.intValue() * 1000);
	}

	public String getOrganizationOID(String managingOrganization){
		if(managingOrganization.contains("Santa Cruz")){
			return santaCruzOID;
		}else if(managingOrganization.contains("OCPRHIO")){
			return ocprhioOID;
		}else if(managingOrganization.contains("UC Davis")){
			return ucdavisOID;
		}else{
			return HOME_COMMUNITY_ID;
		}
	}

	@Override
	public PatientRecordResults queryPatients(CommonUser user, EndpointDTO endpoint, PatientSearch toSearch, String assertion) throws Exception {
		String orgOID = getOrganizationOID(endpoint.getManagingOrganization());
		PRPAIN201305UV02 requestBody = jsonConverterService.convertFromPatientSearch(toSearch, pulseOID, orgOID);
		String requestBodyXml = null;
		try {
			requestBodyXml = queryProducer.marshallPatientDiscoveryRequest(endpoint, assertion, requestBody);
		} catch(JAXBException ex) {
			logger.error(ex.getMessage(), ex);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/soap+xml; charset=utf-8");   
		HttpEntity<String> request = new HttpEntity<String>(requestBodyXml, headers);
		String searchResults = null;
		try {
			logger.trace("Querying " + endpoint.getUrl() + " with timeout " + defaultRequestTimeoutSeconds + " seconds");
			searchResults = restTemplate.postForObject(endpoint.getUrl(), request, String.class); // TODO: the request that is going out here mock does not like
			logger.trace("Search results for " + endpoint.getUrl() + ": " + searchResults);
		} catch(Exception ex) {
			auditManager.createAuditEventIG("FAILURE" , user, endpoint.getUrl(), queryProducer.marshallQueryByParameter(jsonConverterService.getQueryByParameter(requestBody).getValue()), HOME_COMMUNITY_ID);
			throw ex;
		}

		PatientRecordResults results = new PatientRecordResults();
		results.setStatus(IheStatus.Success);
		if(!StringUtils.isEmpty(searchResults)) {
			try {
				PRPAIN201306UV02 resultObj = queryProducer.unMarshallPatientDiscoveryResponseObject(searchResults);
				auditManager.createAuditEventIG("SUCCESS", user, endpoint.getUrl(), queryProducer.marshallQueryByParameter(jsonConverterService.getQueryByParameter(requestBody).getValue()), HOME_COMMUNITY_ID);

				List<PatientRecord> patientResults = soapConverterService.convertToPatientRecords(resultObj);
				for(int i = 0; i < patientResults.size(); i++) {
					PatientRecordDTO record = DomainToDtoConverter.convertToPatientRecord(patientResults.get(i));
					for(int j = 0; j < record.getPatientRecordName().size(); j++){
						NameTypeDTO nameType = nameTypeDao.getByCode("L");
						record.getPatientRecordName().get(j).setNameType(nameType);
					}
					results.getResults().add(record);
				}
			} catch(Exception ex) {
				logger.error("Exception unmarshalling patient discovery response", ex);
				ex.printStackTrace();
				results.setStatus(IheStatus.Failure);
			}

			if(results.getStatus() != IheStatus.Success) {
				logger.info("Trying to unmarshal response as an AdHocQueryRequest object to look for errors.");
				try {
					AdhocQueryResponse resultObj = queryProducer.unmarshallErrorQueryResponse(searchResults);
					results.setStatus(soapConverterService.getErrorStatus(resultObj));
					logger.info("Got error back from " + endpoint.getUrl() + ". Status: " + results.getStatus().name());
					auditManager.createAuditEventIG("FAILURE", user, endpoint.getUrl(), queryProducer.marshallQueryByParameter(jsonConverterService.getQueryByParameter(requestBody).getValue()), HOME_COMMUNITY_ID);
				} catch(Exception ex) {
					auditManager.createAuditEventIG("FAILURE", user, endpoint.getUrl(), queryProducer.marshallQueryByParameter(jsonConverterService.getQueryByParameter(requestBody).getValue()), HOME_COMMUNITY_ID);
					logger.error("Exception unmarshalling patient discovery response as error", ex);
				}
			}
		}

		return results;
	}

	@Override
	public DocumentQueryResults queryDocuments(CommonUser user, EndpointDTO endpoint, PatientEndpointMapDTO toSearch, String assertion) throws UnknownHostException, UnsupportedEncodingException, DOMException, MarshallingException, SAMLException, SOAPException {
		String patientId = toSearch.getExternalPatientRecordId();
		AdhocQueryRequest requestBody = jsonConverterService.convertToDocumentRequest(patientId);
		String requestBodyXml = null;
		try {
			requestBodyXml = queryProducer.marshallDocumentQueryRequest(endpoint, assertion, requestBody);
		} catch(JAXBException | WSSecurityException ex) {
			logger.error(ex.getMessage(), ex);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/soap+xml");
		headers.set("action", "urn:ihe:iti:2007:CrossGatewayQuery");
		HttpEntity<String> request = new HttpEntity<String>(requestBodyXml, headers);

		String searchResults = null;
		try {
			logger.trace("Querying " + endpoint.getUrl() + " with request " + request + " and timeout " + defaultRequestTimeoutSeconds + " seconds");
			searchResults = restTemplate.postForObject(endpoint.getUrl(), request, String.class);
			logger.trace("Search results for " + endpoint.getUrl() + ": " + searchResults);
		} catch(Exception ex) {
			logger.error("Exception when querying " + endpoint.getUrl() + ": " + ex.getMessage(), ex);
			auditManager.createAuditEventDCXGatewayQuery("FAILURE", user, endpoint.getUrl(),"", "", patientId);
			throw ex;
		}

		DocumentQueryResults results = new DocumentQueryResults();
		results.setStatus(IheStatus.Success);
		if(!StringUtils.isEmpty(searchResults)) {
			try {
				AdhocQueryResponse resultObj = queryProducer.unMarshallDocumentQueryResponseObject(searchResults);
				List<Document> documentResults = soapConverterService.convertToDocumentQueryResponse(resultObj);
				for(int i = 0; i < documentResults.size(); i++) {
					DocumentDTO record = DomainToDtoConverter.convert(documentResults.get(i));
					auditManager.createAuditEventDCXGatewayQuery("SUCCESS", user, endpoint.getUrl(),record.getRepositoryUniqueId(), record.getDocumentUniqueId(), patientId);
					results.getResults().add(record);
				}
			} catch(Exception ex) {
				logger.error("Exception unmarshalling document discovery response", ex);
				results.setStatus(IheStatus.Failure);
			}

			if(results.getStatus() != IheStatus.Success) {
				logger.error("Trying to unmarshal response as an AdHocQueryRequest object to look for errors.");
				try {
					AdhocQueryResponse resultObj = queryProducer.unmarshallErrorQueryResponse(searchResults);
					results.setStatus(soapConverterService.getErrorStatus(resultObj));
					logger.error("Got error back from " + endpoint.getUrl() + ". Status: " + results.getStatus().name());
					auditManager.createAuditEventDCXGatewayQuery("FAILURE", user, endpoint.getUrl(),"", "", patientId);
				} catch(Exception ex) {
					logger.error("Exception unmarshalling documents discovery response as error", ex);
				}
			}
		}
		return results;
	}

	/**
	 * get all of the document contents available from the given organization
	 * @param orgMap
	 * @param documents
	 * @return
	 * @throws Exception 
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	@Override

	public void retrieveDocumentsContents(CommonUser user, EndpointDTO endpoint, List<DocumentDTO> documents, PatientEndpointMapDTO patientMap, String assertion) 
			throws Exception {
		List<Document> docsToSearch = new ArrayList<Document>();
		for(DocumentDTO docDto : documents) {
			Document doc = DtoToDomainConverter.convert(docDto);
			docsToSearch.add(doc);
		}
		RetrieveDocumentSetRequestType requestBody = jsonConverterService.convertToRetrieveDocumentSetRequest(docsToSearch);
		String requestBodyXml = null;
		try {
			requestBodyXml = queryProducer.marshallDocumentSetRequest(endpoint, assertion, requestBody);
		} catch(JAXBException ex) {
			logger.error(ex.getMessage(), ex);
		}
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> request = null;
		if(Boolean.getBoolean(mtomMutltipartRequest)){
			String boundary = UUID.randomUUID().toString().replace("-", "");

			headers.set("Content-Type", "multipart/related;" + " boundary=" + boundary + "; " + "start=\"<" + boundary + ">\"; type=\"application/xop+xml\"");

			String part0Header = "Content-Type: application/soap+xml\n" +
					"Content-ID: <" + boundary + ">\n";
			String requestStringXml = "--" + boundary + "\n" +
					part0Header + "\n" +
					requestBodyXml + "\n" +
					"--" + boundary + "--";
			request = new HttpEntity<String>(requestStringXml, headers);
		}else{
			headers.set("Content-Type", "application/soap+xml");
			request = new HttpEntity<String>(requestBodyXml, headers);
		}
		ResponseEntity<String> searchResults = null;
		String returnBody = null;
		String returnEnvelope = null;
		String ct = null;
		AttachmentDeserializer deserializer = null;
		try {
			logger.trace("Querying " + endpoint.getUrl() + " with request " + request + " and timeout " + defaultRequestTimeoutSeconds + " seconds");
			searchResults = restTemplate.postForEntity(endpoint.getUrl(), request, String.class);
			returnBody = searchResults.getBody();
			ct = searchResults.getHeaders().getFirst("Content-Type");
			InputStream is = new ByteArrayInputStream(returnBody.getBytes());
			MessageImpl msg = new MessageImpl();
			msg.put(Message.CONTENT_TYPE, ct);
			msg.setContent(InputStream.class, is);
			if(!documents.get(0).getHomeCommunityId().equals("sacvalleyOID")){
			    deserializer = new AttachmentDeserializer(msg);
			    deserializer.initializeAttachments();
			}
			deserializer = new AttachmentDeserializer(msg);
			deserializer.initializeAttachments();
			InputStream attBody = msg.getContent(InputStream.class);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(attBody, out);
			returnEnvelope = out.toString();
			logger.trace("Search results for " + endpoint.getUrl() + ": " + returnEnvelope);
		} catch(Exception ex) {
			logger.error("Exception when querying " + endpoint.getUrl() + ": " + ex.getMessage(), ex);
			for(Document doc : docsToSearch){
				auditManager.createAuditEventDCXGatewayRetrieve("FAILURE", user, endpoint.getUrl(), doc.getIdentifier().getRepositoryUniqueId(), doc.getIdentifier().getDocumentUniqueId(), doc.getIdentifier().getHomeCommunityId(), patientMap.getExternalPatientRecordId());
			}
			throw ex;
		}

		if(!StringUtils.isEmpty(searchResults)) {
			IheStatus resultStatus = IheStatus.Success;
			try {
				RetrieveDocumentSetResponseType resultObj = queryProducer.unMarshallDocumentSetRetrieveResponseObject(returnEnvelope);
				List<DocumentResponse> documentResponses = soapConverterService.convertToDocumentSetResponse(resultObj);
				for(DocumentResponse docResponse : documentResponses) {
					//find the matching DocumentDTO that we sent in
					DocumentDTO matchingDto = null;
					for(int i = 0; i < documents.size() && matchingDto == null; i++) {
						DocumentDTO currDto = documents.get(i);
						auditManager.createAuditEventDCXGatewayRetrieve("SUCCESS", user, endpoint.getUrl(), docResponse.getRepositoryUniqueId(), docResponse.getDocumentUniqueId(), docResponse.getHomeCommunityId(), patientMap.getExternalPatientRecordId());
						if(currDto.getRepositoryUniqueId().equals(docResponse.getRepositoryUniqueId()) && 
								currDto.getHomeCommunityId().equals(docResponse.getHomeCommunityId()) &&
								currDto.getDocumentUniqueId().equals(docResponse.getDocumentUniqueId())) {
							matchingDto = currDto;
						}
					}

					if(matchingDto != null) {
					    //read the binary document data from this DocumentResponse
					    InputStream in = null;
					    if(matchingDto.getHomeCommunityId().equals("urn:oid:2.16.840.1.113883.3.432.0.31.500") &&
					            docResponse.getDocument() != null){
					        // this is a sacvalley doc
					        in = docResponse.getDocument().getDataSource().getInputStream();
					    }else{
					        if(deserializer.hasNext()){
					            in = deserializer.readNext().getDataHandler().getDataSource().getInputStream();
					        }
					    }
					    StringWriter writer = new StringWriter();
					    IOUtils.copy(in, writer, Charset.forName("UTF-8"));
					    String dataStr = writer.toString();
						logger.trace("Converted binary to " + dataStr);
						matchingDto.setContents(new String(dataStr.getBytes()));
					}
				}
			} catch(Exception ex) {
				logger.error("Exception unmarshalling document retrieve response", ex);
				resultStatus = IheStatus.Failure;
			}

			if(resultStatus != IheStatus.Success) {
				logger.error("Trying to unmarshal response as an AdHocQueryRequest object to look for errors.");
				try {
					AdhocQueryResponse resultObj = queryProducer.unmarshallErrorQueryResponse(searchResults.getBody());
					resultStatus = soapConverterService.getErrorStatus(resultObj);
					logger.error("Got error back from " + endpoint.getUrl() + ". Status: " + resultStatus.name());
					for(Document doc : docsToSearch){
						auditManager.createAuditEventDCXGatewayRetrieve("FAILURE", user, endpoint.getUrl(), doc.getIdentifier().getRepositoryUniqueId(), doc.getIdentifier().getDocumentUniqueId(), doc.getIdentifier().getHomeCommunityId(), patientMap.getExternalPatientRecordId());
					}
					throw new IheErrorException("Got error code: " + resultStatus.name());
				} catch(Exception ex) {
					logger.error("Exception unmarshalling document set response as error", ex);
					throw ex;
				}
			}
		}
	}

	private String base64DecodeMessage(String encodedMessage){       
		byte[] decoded = Base64.getDecoder().decode(encodedMessage);
		String decodedMessage = new String(decoded, Charset.forName("UTF-8"));
		return decodedMessage;
	}
}
