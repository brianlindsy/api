package gov.ca.emsa.pulse.xcpd.prpa.cap.subj;

import javax.xml.bind.annotation.XmlAttribute;

import gov.ca.emsa.pulse.xcpd.Id;

public class ProviderOrganization {
	@XmlAttribute private String classCode;
	@XmlAttribute private String determinderCode;
	public Id id;
	public String name;
	public ContactParty contactParty;

	public ProviderOrganization() {
	}

}