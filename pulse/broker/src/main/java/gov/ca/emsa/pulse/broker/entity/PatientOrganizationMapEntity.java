package gov.ca.emsa.pulse.broker.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="patient_organization_map")
public class PatientOrganizationMapEntity {
	
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column( name = "id", nullable = false )
	private Long id;
	
	@Column(name = "patient_id")
	private Long patientId;
	
	@OneToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
	@JoinColumn(name = "patient_id", unique=true, nullable = true, insertable=false, updatable= false)
	private PatientEntity patient;
	
	@Column(name = "organization_id")
	private Long organizationId;
	
	@OneToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
	@JoinColumn(name = "organization_id", unique=true, nullable = true, insertable=false, updatable= false)
	private OrganizationEntity organization;
	
	@Column(name = "organization_patient_id")
	private String organizationPatientId;
	
	@Column(name = "documents_query_status")
	private String documentsQueryStatus;
	
	@Column(name = "documents_query_success")
	private Boolean documentsQuerySuccess;
	
	@Column(name = "documents_query_start")
	private Date documentsQueryStart;
	
	@Column(name = "documents_query_end")
	private Date documentsQueryEnd;
	
	@Column( name = "creation_date", insertable = false, updatable = false)
	private Date creationDate;
	
	@Column( name = "last_modified_date", insertable = false, updatable = false)
	private Date lastModifiedDate;
	
	@OneToMany( fetch = FetchType.LAZY, mappedBy = "patientOrgMapId"  )
	@Column( name = "patient_organization_map_id", nullable = false  )
	private Set<DocumentEntity> documents = new HashSet<DocumentEntity>();
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPatientId() {
		return patientId;
	}

	public void setPatientId(Long patientId) {
		this.patientId = patientId;
	}

	public PatientEntity getPatient() {
		return patient;
	}

	public void setPatient(PatientEntity patient) {
		this.patient = patient;
	}

	public Long getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Long organizationId) {
		this.organizationId = organizationId;
	}

	public OrganizationEntity getOrganization() {
		return organization;
	}

	public void setOrganization(OrganizationEntity organization) {
		this.organization = organization;
	}

	public String getOrganizationPatientId() {
		return organizationPatientId;
	}

	public void setOrganizationPatientId(String organizationPatientId) {
		this.organizationPatientId = organizationPatientId;
	}

	public String getDocumentsQueryStatus() {
		return documentsQueryStatus;
	}

	public void setDocumentsQueryStatus(String documentsQueryStatus) {
		this.documentsQueryStatus = documentsQueryStatus;
	}

	public Boolean getDocumentsQuerySuccess() {
		return documentsQuerySuccess;
	}

	public void setDocumentsQuerySuccess(Boolean documentsQuerySuccess) {
		this.documentsQuerySuccess = documentsQuerySuccess;
	}

	public Date getDocumentsQueryStart() {
		return documentsQueryStart;
	}

	public void setDocumentsQueryStart(Date documentsQueryStart) {
		this.documentsQueryStart = documentsQueryStart;
	}

	public Date getDocumentsQueryEnd() {
		return documentsQueryEnd;
	}

	public void setDocumentsQueryEnd(Date documentsQueryEnd) {
		this.documentsQueryEnd = documentsQueryEnd;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public Set<DocumentEntity> getDocuments() {
		return documents;
	}

	public void setDocuments(Set<DocumentEntity> documents) {
		this.documents = documents;
	}
}