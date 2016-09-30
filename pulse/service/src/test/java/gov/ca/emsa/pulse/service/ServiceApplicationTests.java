package gov.ca.emsa.pulse.service;
import gov.ca.emsa.pulse.ServiceApplicationTestConfig;
import gov.ca.emsa.pulse.common.domain.Document;
import gov.ca.emsa.pulse.common.domain.DocumentQuery;
import gov.ca.emsa.pulse.common.domain.DocumentRetrieve;
import gov.ca.emsa.pulse.common.domain.DocumentWrapper;
import gov.ca.emsa.pulse.common.domain.Patient;
import gov.ca.emsa.pulse.common.domain.PatientRecord;
import gov.ca.emsa.pulse.common.domain.PatientSearch;
import gov.ca.emsa.pulse.common.domain.QueryOrganization;
import gov.ca.emsa.pulse.common.soap.JSONToSOAPService;
import gov.ca.emsa.pulse.common.soap.SOAPToJSONService;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;

import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201310UV02;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.common.SAMLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import static org.junit.Assert.*;

import org.springframework.security.saml.SAMLCredential;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=ServiceApplicationTestConfig.class)
@WebAppConfiguration
public class ServiceApplicationTests {

	private static final String jwt = "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJDQUxFTVNBIiwiYXVkIjoiQ0FMRU1TQSIsImV4cCI6MTQ3OTc3MDcwMSwianRpIjoiVFcxbEVLTFZ4X3NRTGR5NnQtY3RyQSIsImlhdCI6MTQ3Mzc3MDcwMSwibmJmIjoxNDczNzcwNDYxLCJzdWIiOiJmYWtlQHNhbXBsZS5jb20iLCJBdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXSwiSWRlbnRpdHkiOlsiRmFrZSIsIlBlcnNvbiIsImZha2VAc2FtcGxlLmNvbSJdfQ.N6Kxy9m0ZXagBsBMy3egOT4Vk9pqM3C6ujFgUlok-8kaupwSF_qt-0Q7_oV6eYOgiaUuUuDTHoU7VkFwKG51DeJFZJYb_rSvy3gAAwDEahePYxNlQGpoc39LlN3EltM1U9rZ9Vj8HlgtE8DMUcmG080D0cZDpFDEdADbEchl8KkIMs_tnhs6Rour4KGymcB5lCHhdVdYH_AMKqZ6qWk2Rh2Ejd6SzRPydmcdR_7jIUKdbsgvjFr0I_l2G_tZggJssGx2zmJmyaMsWGnQOfibojugQiZ16qkpHKPAnG9sdGV6_yfHg8gU9dKx1MJOXkvcatJ3cuLRZIwHJQ-k8zLocA";
	private static final String brokerSearchCompletedQueryResponse = "{\"id\":1,\"userToken\":\"fake@sample.com\",\"status\":\"COMPLETE\",\"terms\":{\"givenName\":\"John\",\"familyName\":\"Doe\",\"dob\":null,\"ssn\":null,\"gender\":null,\"zip\":null},\"lastRead\":1473775608988,\"orgStatuses\":[{\"id\":141,\"queryId\":1,\"org\":{\"name\":\"eHealthExchangeOrg3\",\"id\":3,\"organizationId\":3,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522671,\"endDate\":1473775555980,\"success\":true,\"results\":[{\"id\":85,\"givenName\":\"John\",\"familyName\":\"Doe\",\"dateOfBirth\":-461030400000,\"gender\":\"M\",\"phoneNumber\":\"3517869574\",\"address\":null,\"ssn\":\"451674563\"}]},{\"id\":139,\"queryId\":24,\"org\":{\"name\":\"eHealthExchangeOrg\",\"id\":1,\"organizationId\":1,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522668,\"endDate\":1473775608969,\"success\":true,\"results\":[{\"id\":87,\"givenName\":\"John\",\"familyName\":\"Doe\",\"dateOfBirth\":-461030400000,\"gender\":\"M\",\"phoneNumber\":\"3517869574\",\"address\":null,\"ssn\":\"451674563\"}]},{\"id\":140,\"queryId\":24,\"org\":{\"name\":\"eHealthExchangeOrg2\",\"id\":2,\"organizationId\":2,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522670,\"endDate\":1473775607975,\"success\":true,\"results\":[{\"id\":86,\"givenName\":\"John\",\"familyName\":\"Doe\",\"dateOfBirth\":-461030400000,\"gender\":\"M\",\"phoneNumber\":\"3517869574\",\"address\":null,\"ssn\":\"451674563\"}]},{\"id\":144,\"queryId\":24,\"org\":{\"name\":\"IHEOrg3\",\"id\":6,\"organizationId\":6,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522676,\"endDate\":1473775561522,\"success\":true,\"results\":[]},{\"id\":142,\"queryId\":24,\"org\":{\"name\":\"IHEOrg\",\"id\":4,\"organizationId\":4,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522673,\"endDate\":1473775592713,\"success\":true,\"results\":[]},{\"id\":143,\"queryId\":24,\"org\":{\"name\":\"IHEOrg2\",\"id\":5,\"organizationId\":5,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"COMPLETE\",\"startDate\":1473775522674,\"endDate\":1473775587889,\"success\":true,\"results\":[]}]}";
	private static final String brokerSearchActiveQueryResponse = "{\"id\":1,\"userToken\":\"fake@sample.com\",\"status\":\"ACTIVE\",\"terms\":{\"givenName\":\"John\",\"familyName\":\"Doe\",\"dob\":null,\"ssn\":null,\"gender\":null,\"zip\":null},\"lastRead\":1473775555673,\"orgStatuses\":[{\"id\":144,\"queryId\":1,\"org\":{\"name\":\"IHEOrg3\",\"id\":6,\"organizationId\":6,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522676,\"endDate\":null,\"success\":null,\"results\":[]},{\"id\":143,\"queryId\":24,\"org\":{\"name\":\"IHEOrg2\",\"id\":5,\"organizationId\":5,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522674,\"endDate\":null,\"success\":null,\"results\":[]},{\"id\":141,\"queryId\":24,\"org\":{\"name\":\"eHealthExchangeOrg3\",\"id\":3,\"organizationId\":3,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522671,\"endDate\":null,\"success\":null,\"results\":[]},{\"id\":140,\"queryId\":24,\"org\":{\"name\":\"eHealthExchangeOrg2\",\"id\":2,\"organizationId\":2,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522670,\"endDate\":null,\"success\":null,\"results\":[]},{\"id\":139,\"queryId\":24,\"org\":{\"name\":\"eHealthExchangeOrg\",\"id\":1,\"organizationId\":1,\"adapter\":\"eHealth\",\"ipAddress\":\"127.0.0.1\",\"username\":\"org1User\",\"password\":\"password1\",\"certificationKey\":null,\"endpointUrl\":\"http://localhost:9080/mock/ehealthexchange\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522668,\"endDate\":null,\"success\":null,\"results\":[]},{\"id\":142,\"queryId\":24,\"org\":{\"name\":\"IHEOrg\",\"id\":4,\"organizationId\":4,\"adapter\":\"IHE\",\"ipAddress\":\"127.0.0.1\",\"username\":null,\"password\":null,\"certificationKey\":\"1234567\",\"endpointUrl\":\"http://localhost:9080/mock/ihe\",\"active\":true},\"status\":\"ACTIVE\",\"startDate\":1473775522673,\"endDate\":null,\"success\":null,\"results\":[]}]}";

	private static final String PATIENT_DISCOVERY_REQUEST_RESOURCE_FILE_NAME = "ValidXcpdRequest.xml";
	private static final String DOCUMENT_QUERY_REQUEST_RESOURCE_FILE_NAME = "ValidStoredQueryRequest.xml";
	private static final String DOCUMENT_SET_REQUEST_RESOURCE_FILE_NAME = "ValidStoredQueryRequest.xml";

	@Value("${samlServiceUrl}")
	private String samlServiceUrl;
	@Value("${server.port}")
	private String port;

	@Autowired ResourceLoader resourceLoader;
	@Autowired EHealthQueryConsumerService consumerService;
	@Autowired SOAPToJSONService SOAPconverter;
	@Autowired JSONToSOAPService JSONconverter;
	@Autowired PatientSearchService pss;

	private MockRestServiceServer mockServer;
	@Mock private RestTemplate mockRestTemplate;

	private ExecutorService executor = Executors.newFixedThreadPool(100);

	@Before
	public void setUp() {
		mockRestTemplate = new RestTemplate();
		mockServer = MockRestServiceServer.createServer(mockRestTemplate);
	}
	
	@Test 
	public void testGetSAMLAssertion() throws SOAPException, SAMLException{
		Resource documentsFile = resourceLoader.getResource("classpath:" + PATIENT_DISCOVERY_REQUEST_RESOURCE_FILE_NAME);
		String xml = null;
		try {
			xml = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		MessageFactory factory = null;
		try {
			factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		} catch (SOAPException e1) {
			e1.printStackTrace();
		}
		SOAPMessage soapMessage = null;
		try {
			soapMessage = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException | SOAPException e) {
			e.printStackTrace();
		}
		SaajSoapMessage saajSoap = new SaajSoapMessage(soapMessage);

		SAMLCredential saml = consumerService.getSAMLAssertion(soapMessage);
		assertNotNull(saml);
	}

	@Test
	public void testConsumerServicePatientDiscoveryRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + PATIENT_DISCOVERY_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		PRPAIN201305UV02 requestObj = null;
		try {
			requestObj = consumerService.unMarshallPatientDiscoveryRequestObject(request);
		} catch (SOAPException | SAMLException e) {
			e.printStackTrace();
		}
		
		assertNotNull(requestObj);
		
		PRPAIN201310UV02 responseObj = createPatientDiscoveryResponse();
		
		String responseString = null;
		try {
			responseString = consumerService.marshallPatientDiscoveryResponse(responseObj);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		
		assertNotNull(responseString);
		
		if(responseString.contains("Envelope")){
			assertTrue(true);
		}else{
			assertTrue(false);
		}
	}
	
	@Test
	public void testConsumerServiceDocumentQueryRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + DOCUMENT_QUERY_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		AdhocQueryRequest requestObj = null;
		try {
			requestObj = consumerService.unMarshallDocumentQueryRequestObject(request);
		} catch (SAMLException e) {
			e.printStackTrace();
		}
		
		assertNotNull(requestObj);
		
		AdhocQueryResponse responseObj = createAdhocQueryResponse();
		
		String responseString = null;
		try {
			responseString = consumerService.marshallDocumentQueryResponse(responseObj);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		assertNotNull(responseString);
		
		if(responseString.contains("Envelope")){
			assertTrue(true);
		}else{
			assertTrue(false);
		}
	}
	
	@Test
	public void testConsumerServiceDocumentSetRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + DOCUMENT_SET_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		RetrieveDocumentSetRequestType requestObj = null;
		try {
			requestObj = consumerService.unMarshallDocumentSetRetrieveRequestObject(request);
		} catch (SAMLException e) {
			e.printStackTrace();
		}
		
		assertNotNull(requestObj);
		
		RetrieveDocumentSetResponseType responseObj = createDocumentSetResponse();
		
		String responseString = null;
		try {
			responseString = consumerService.marshallDocumentSetResponse(responseObj);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		//assertNotNull(responseString);
	}
	
	// JSONToSOAPService tests
	@Test 
	public void testConsumerServicePatientDiscoveryResponse(){
		List<PatientRecord> patientRecords = createPatientRecordList();
		PRPAIN201310UV02 responseObj = JSONconverter.convertPatientRecordListToSOAPResponse(patientRecords);
		
		assertNotNull(responseObj);
	}
	
	@Test 
	public void testDocumentQueryResponse(){
		List<Document> docs = createDocumentList();
		AdhocQueryResponse responseObj = JSONconverter.convertDocumentListToSOAPResponse(docs, "1");
		
		assertNotNull(responseObj);
	}
	
	@Test
	public void testDocumentRetrieveResponse(){
		List<DocumentWrapper> docs = createDocumentWrapperList();
		RetrieveDocumentSetResponseType responseObj = JSONconverter.convertDocumentSetToSOAPResponse(docs);
		
		assertNotNull(responseObj);
	}

	// SOAPToJSONService tests
	@Test
	public void testPatientDiscoveryRequest(){
		PRPAIN201305UV02 requestObj = createPatientDiscoveryRequest();
		PatientSearch patientSearch = SOAPconverter.convertToPatientSearch(requestObj);
		assertNotNull(patientSearch);
	}
	
	@Test
	public void testDocumentQueryRequest(){
		AdhocQueryRequest requestObj = createDocumentQueryRequest();
		DocumentQuery documentQuery = SOAPconverter.convertToDocumentQuery(requestObj);
		assertNotNull(documentQuery);
	}
	
	@Test
	public void testDocumentSetRequest(){
		RetrieveDocumentSetRequestType requestObj = createDocumentSetRequest();
		DocumentRetrieve docRetrieve = SOAPconverter.convertToDocumentSetRequest(requestObj);
		assertNotNull(docRetrieve);
	}

	@Test
	public void testPatientSearch() throws Exception{

		mockServer
		.expect(MockRestRequestMatchers.requestTo(samlServiceUrl + "/jwt"))
		.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
		.andRespond(MockRestResponseCreators.withSuccess(
				jwt
				,MediaType.APPLICATION_JSON));

		mockServer
		.expect(MockRestRequestMatchers.requestTo("http://localhost:" + port + "/search"))
		.andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
		.andExpect(MockRestRequestMatchers.header("Authorization", jwt))
		.andRespond(MockRestResponseCreators.withSuccess(
				brokerSearchActiveQueryResponse
				,MediaType.APPLICATION_JSON));

		mockServer
		.expect(MockRestRequestMatchers.requestTo("http://localhost:" + port + "/queries/1"))
		.andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
		.andExpect(MockRestRequestMatchers.header("Authorization", jwt))
		.andRespond(MockRestResponseCreators.withSuccess(
				brokerSearchCompletedQueryResponse
				,MediaType.APPLICATION_JSON));

		PatientSearch patientSearch = createPatientSearchObject();

		pss.searchForPatientWithTerms(mockRestTemplate, patientSearch);

		Future<List<QueryOrganization>> future = executor.submit(pss);

		assertNotNull(future.get());
		assertEquals(future.get().get(0).getResults().get(0).getFamilyName(),"Doe");
		assertEquals(future.get().get(0).getResults().get(0).getGivenName(),"John");
		assertEquals(future.get().get(0).getResults().get(0).getPhoneNumber(),"3517869574");
	}

	public PatientSearch createPatientSearchObject(){
		PRPAIN201305UV02 requestObj = createPatientDiscoveryRequest();
		PatientSearch patientSearch = SOAPconverter.convertToPatientSearch(requestObj);
		return patientSearch;
	}

	public List<PatientRecord> createPatientRecordList(){
		List<PatientRecord> records = new ArrayList<PatientRecord>();
		PatientRecord pr1 = new PatientRecord();
		PatientRecord pr2 = new PatientRecord();
		pr1.setDateOfBirth("2016-09-19");
		pr1.setFamilyName("Lindsey");
		pr1.setGivenName("Brian");
		pr1.setGender("M");
		pr2.setDateOfBirth("2016-09-19");
		pr2.setFamilyName("Snow");
		pr2.setGivenName("John");
		pr2.setGender("M");
		records.add(pr1);
		records.add(pr2);
		return records;
	}

	public PRPAIN201305UV02 createPatientDiscoveryRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + PATIENT_DISCOVERY_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		PRPAIN201305UV02 requestObj = null;
		try {
			requestObj = consumerService.unMarshallPatientDiscoveryRequestObject(request);
		} catch (SOAPException | SAMLException e) {
			e.printStackTrace();
		}
		return requestObj;
	}
	
	public List<Document> createDocumentList(){
		Document doc1 = new Document();
		Document doc2 = new Document();
		List<Document> documents = new ArrayList<Document>();
		doc1.setId("1");
		doc1.setName("Document1");
		doc1.setOrgMapId(new Long(1));
		doc1.setPatient(new Patient());
		doc2.setId("2");
		doc2.setName("Document2");
		doc2.setOrgMapId(new Long(2));
		doc2.setPatient(new Patient());
		documents.add(doc1);
		documents.add(doc2);
		return documents;
	}
	
	public List<DocumentWrapper> createDocumentWrapperList(){
		DocumentWrapper doc1 = new DocumentWrapper("eurfiuoisdhfiebdfodsofihf");
		DocumentWrapper doc2 = new DocumentWrapper("drlkfsoisdhfncslkcxnv;ob");
		List<DocumentWrapper> documents = new ArrayList<DocumentWrapper>();
		doc1.setDocumentUniqueId("1.3.6.1.4...2300");
		doc1.setHomeCommunityId("urn:oid:1.2.3.4");
		doc1.setRepositoryUniqueId("1.3.6.1.4...1000");
		doc2.setDocumentUniqueId("1.3.6.1.4...2300");
		doc2.setHomeCommunityId("urn:oid:1.2.3.4");
		doc2.setRepositoryUniqueId("1.3.6.1.4...1000");
		documents.add(doc1);
		documents.add(doc2);
		return documents;
	}
	
	public AdhocQueryRequest createDocumentQueryRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + DOCUMENT_QUERY_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		AdhocQueryRequest requestObj = null;
		try {
			requestObj = consumerService.unMarshallDocumentQueryRequestObject(request);
		} catch (SAMLException e) {
			e.printStackTrace();
		}
		return requestObj;
	}
	
	public RetrieveDocumentSetRequestType createDocumentSetRequest(){
		Resource documentsFile = resourceLoader.getResource("classpath:" + DOCUMENT_SET_REQUEST_RESOURCE_FILE_NAME);
		String request = null;
		try {
			request = Resources.toString(documentsFile.getURL(), Charsets.UTF_8);
		}catch (IOException e){
			e.printStackTrace();
		}
		RetrieveDocumentSetRequestType requestObj = null;
		try {
			requestObj = consumerService.unMarshallDocumentSetRetrieveRequestObject(request);
		} catch (SAMLException e) {
			e.printStackTrace();
		}
		return requestObj;
	}
	
	public PRPAIN201310UV02 createPatientDiscoveryResponse(){
		List<PatientRecord> patientRecords = createPatientRecordList();
		PRPAIN201310UV02 responseObj = JSONconverter.convertPatientRecordListToSOAPResponse(patientRecords);
		return responseObj;
	}
	
	public AdhocQueryResponse createAdhocQueryResponse(){
		List<Document> docs = createDocumentList();
		AdhocQueryResponse responseObj = JSONconverter.convertDocumentListToSOAPResponse(docs, "1");
		return responseObj;
	}
	
	public RetrieveDocumentSetResponseType createDocumentSetResponse(){
		List<DocumentWrapper> docs = createDocumentWrapperList();
		RetrieveDocumentSetResponseType responseObj = JSONconverter.convertDocumentSetToSOAPResponse(docs);
		return responseObj;
	}
}
