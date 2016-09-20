package gov.ca.emsa.pulse.service;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.opensaml.xml.io.MarshallingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.ca.emsa.pulse.common.domain.Document;
import gov.ca.emsa.pulse.common.domain.Patient;
import gov.ca.emsa.pulse.auth.user.CommonUser;
import gov.ca.emsa.pulse.broker.domain.QueryType;
import gov.ca.emsa.pulse.broker.dto.AlternateCareFacilityDTO;
import gov.ca.emsa.pulse.broker.dto.DocumentDTO;
import gov.ca.emsa.pulse.broker.dto.DtoToDomainConverter;
import gov.ca.emsa.pulse.broker.dto.PatientDTO;
import gov.ca.emsa.pulse.broker.manager.AlternateCareFacilityManager;
import gov.ca.emsa.pulse.broker.manager.AuditManager;
import gov.ca.emsa.pulse.broker.manager.DocumentManager;
import gov.ca.emsa.pulse.broker.manager.PatientManager;
import gov.ca.emsa.pulse.broker.saml.SAMLInput;
import gov.ca.emsa.pulse.broker.saml.SamlGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "patients")
@RestController
@RequestMapping("/patients")
public class PatientService {
	private static final Logger logger = LogManager.getLogger(PatientService.class);
	@Autowired SamlGenerator samlGenerator;
	@Autowired private PatientManager patientManager;
	@Autowired private DocumentManager docManager;
	@Autowired private AlternateCareFacilityManager acfManager;
	@Autowired private AuditManager auditManager;

	public PatientService() {
	}

	@ApiOperation(value="Get all patients at the logged-in user's ACF")
	@RequestMapping("")
	public List<Patient> getPatientsAtAcf() throws InvalidParameterException {
		CommonUser user = UserUtil.getCurrentUser();
		auditManager.addAuditEntry(QueryType.GET_ALL_PATIENTS, "/patients", user.getSubjectName());
		if(user.getAcf() == null || user.getAcf().getId() == null) {
			throw new InvalidParameterException("There was no ACF supplied in the User header, or the ACF had a null ID.");
		}

		List<PatientDTO> queryResults = patientManager.getPatientsAtAcf(user.getAcf().getId());
		List<Patient> results = new ArrayList<Patient>(queryResults.size());
        for(PatientDTO patientDto : queryResults) {
        	Patient patient = DtoToDomainConverter.convert(patientDto);
        	results.add(patient);
        }

        return results;
	}

	@ApiOperation(value="Get a list of documents associated with the given patient")
	@RequestMapping("/{patientId}/documents")
	public List<Document> getDocumentListForPatient(@PathVariable("patientId")Long patientId) {
		CommonUser user = UserUtil.getCurrentUser();
		auditManager.addAuditEntry(QueryType.SEARCH_DOCUMENT, "/" + patientId + "/documents", user.getSubjectName());
		List<DocumentDTO> docDtos = docManager.getDocumentsForPatient(patientId);
		List<Document> results = new ArrayList<Document>(docDtos.size());
		for(DocumentDTO docDto : docDtos) {
			results.add(DtoToDomainConverter.convert(docDto));
		}
		return results;
	}

	@ApiOperation(value="Retrieve a specific document from an organization.")
	@RequestMapping(value = "/{patientId}/documents/{documentId}")
	public @ResponseBody String getDocumentContents(@PathVariable("patientId") Long patientId,
			@PathVariable("documentId") Long documentId,
			@RequestParam(value="cacheOnly", required= false, defaultValue="true") Boolean cacheOnly) {

		CommonUser user = UserUtil.getCurrentUser();
		auditManager.addAuditEntry(QueryType.CACHE_DOCUMENT, "/" + patientId + "/documents/" + documentId, user.getSubjectName());
		SAMLInput input = new SAMLInput();
		input.setStrIssuer("https://idp.dhv.gov");
		input.setStrNameID(user.getFirstName());
		input.setStrNameQualifier("My Website");
		input.setSessionId("abcdedf1234567");

		HashMap<String, String> customAttributes = new HashMap<String,String>();
		customAttributes.put("RequesterName", user.getFirstName());
		customAttributes.put("RequestReason", "Patient is bleeding.");
		customAttributes.put("PatientGivenName", "Hodor");
		customAttributes.put("PatientFamilyName", "Guy");
		customAttributes.put("PatientSSN", "123456789");
		input.setAttributes(customAttributes);

		String samlMessage = null;

		try {
			samlMessage = samlGenerator.createSAML(input);
		} catch (MarshallingException e) {
			e.printStackTrace();
		}

		String result = "";
		if(cacheOnly == null || cacheOnly.booleanValue() == false) {
			result = docManager.getDocumentById(samlMessage, documentId);
		} else {
			docManager.getDocumentById(samlMessage, documentId);
		}
		return result;
	}
	
	@ApiOperation(value = "Delete a patient")
	@RequestMapping(value="/{patientId}/delete", method = RequestMethod.POST)
	public void deletePatient(@PathVariable(value="patientId") Long patientId) {
		patientManager.delete(patientId);
	}
}
