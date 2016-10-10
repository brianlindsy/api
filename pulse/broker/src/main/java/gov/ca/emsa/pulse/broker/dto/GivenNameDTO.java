package gov.ca.emsa.pulse.broker.dto;

import gov.ca.emsa.pulse.broker.entity.GivenNameEntity;


public class GivenNameDTO {
	
	private Long id;
	
	private String givenName;
	
	private Long patientNameId;
	
	public GivenNameDTO(){
		
	}
	
	public GivenNameDTO(GivenNameEntity entity){
		this.id = entity.getId();
		this.givenName = entity.getGivenName();
		this.patientNameId = entity.getPatientNameId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public Long getPatientNameId() {
		return patientNameId;
	}

	public void setPatientNameId(Long patientNameId) {
		this.patientNameId = patientNameId;
	}

}
