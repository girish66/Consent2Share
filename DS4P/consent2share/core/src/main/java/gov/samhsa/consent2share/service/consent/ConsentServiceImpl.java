/*******************************************************************************
 * Open Behavioral Health Information Technology Architecture (OBHITA.org)
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.samhsa.consent2share.service.consent;

import gov.samhsa.acs.common.exception.DS4PException;
import gov.samhsa.consent.ConsentGenException;
import gov.samhsa.consent2share.domain.consent.Consent;
import gov.samhsa.consent2share.domain.consent.ConsentDoNotShareClinicalDocumentSectionTypeCode;
import gov.samhsa.consent2share.domain.consent.ConsentDoNotShareClinicalDocumentTypeCode;
import gov.samhsa.consent2share.domain.consent.ConsentDoNotShareSensitivityPolicyCode;
import gov.samhsa.consent2share.domain.consent.ConsentIndividualProviderDisclosureIsMadeTo;
import gov.samhsa.consent2share.domain.consent.ConsentIndividualProviderPermittedToDisclose;
import gov.samhsa.consent2share.domain.consent.ConsentOrganizationalProviderDisclosureIsMadeTo;
import gov.samhsa.consent2share.domain.consent.ConsentOrganizationalProviderPermittedToDisclose;
import gov.samhsa.consent2share.domain.consent.ConsentPdfGenerator;
import gov.samhsa.consent2share.domain.consent.ConsentRepository;
import gov.samhsa.consent2share.domain.consent.ConsentShareForPurposeOfUseCode;
import gov.samhsa.consent2share.domain.consent.SignedPDFConsent;
import gov.samhsa.consent2share.domain.consent.SignedPDFConsentRevocation;
import gov.samhsa.consent2share.domain.patient.Patient;
import gov.samhsa.consent2share.domain.patient.PatientRepository;
import gov.samhsa.consent2share.domain.provider.AbstractProvider;
import gov.samhsa.consent2share.domain.provider.IndividualProvider;
import gov.samhsa.consent2share.domain.provider.IndividualProviderRepository;
import gov.samhsa.consent2share.domain.provider.OrganizationalProvider;
import gov.samhsa.consent2share.domain.provider.OrganizationalProviderRepository;
import gov.samhsa.consent2share.domain.reference.ClinicalConceptCode;
import gov.samhsa.consent2share.domain.reference.ClinicalDocumentSectionTypeCode;
import gov.samhsa.consent2share.domain.reference.ClinicalDocumentSectionTypeCodeRepository;
import gov.samhsa.consent2share.domain.reference.ClinicalDocumentTypeCode;
import gov.samhsa.consent2share.domain.reference.ClinicalDocumentTypeCodeRepository;
import gov.samhsa.consent2share.domain.reference.PurposeOfUseCode;
import gov.samhsa.consent2share.domain.reference.PurposeOfUseCodeRepository;
import gov.samhsa.consent2share.domain.reference.SensitivityPolicyCode;
import gov.samhsa.consent2share.domain.reference.SensitivityPolicyCodeRepository;
import gov.samhsa.consent2share.infrastructure.ConsentRevokationPdfGenerator;
import gov.samhsa.consent2share.infrastructure.EchoSignSignatureService;
import gov.samhsa.consent2share.infrastructure.TryPolicyService;
import gov.samhsa.consent2share.infrastructure.security.UserContext;
import gov.samhsa.consent2share.service.consentexport.ConsentExportService;
import gov.samhsa.consent2share.service.dto.AbstractPdfDto;
import gov.samhsa.consent2share.service.dto.ConsentDto;
import gov.samhsa.consent2share.service.dto.ConsentListDto;
import gov.samhsa.consent2share.service.dto.ConsentPdfDto;
import gov.samhsa.consent2share.service.dto.ConsentRevokationPdfDto;
import gov.samhsa.consent2share.service.dto.SpecificMedicalInfoDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// TODO: Auto-generated Javadoc
/**
 * The Class ConsentServiceImpl.
 */
@Service
@Transactional
public class ConsentServiceImpl implements ConsentService {
	
	/** The consent repository. */
	@Autowired
    private ConsentRepository consentRepository;
	
	/** The consent pdf generator. */
	@Autowired
	private ConsentPdfGenerator consentPdfGenerator;
	
	/** The patient repository. */
	@Autowired
	private PatientRepository patientRepository;
	
	/** The individual provider repository. */
	@Autowired
	private IndividualProviderRepository individualProviderRepository;
	
	/** The organizational provider repository. */
	@Autowired
	private OrganizationalProviderRepository organizationalProviderRepository;
	
	/** The clinical document type code repository. */
	@Autowired
	private ClinicalDocumentTypeCodeRepository clinicalDocumentTypeCodeRepository;
	
	/** The clinical document section type code repository. */
	@Autowired
	private ClinicalDocumentSectionTypeCodeRepository clinicalDocumentSectionTypeCodeRepository;
	
	/** The sensitivity policy code repository. */
	@Autowired
	private SensitivityPolicyCodeRepository sensitivityPolicyCodeRepository;
	
	/** The purpose of use code repository. */
	@Autowired
	private PurposeOfUseCodeRepository purposeOfUseCodeRepository;
	
	/** The echo sign signature service. */
	@Autowired
	private EchoSignSignatureService echoSignSignatureService;
	
	/** The user context. */
	@Autowired
	private UserContext userContext;
	
	/** The consent revokation pdf generator. */
	@Autowired
	private ConsentRevokationPdfGenerator consentRevokationPdfGenerator; 
	
	/** The consent export service. */
	@Autowired
	private ConsentExportService consentExportService;
	
	/**	Try policy service */
	@Autowired
	private TryPolicyService tryPolicyService;
	
	/** The logger. */
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Count all consents.
	 *
	 * @return the long
	 */
	public long countAllConsents() {
        return consentRepository.count();
    }

	/**
	 * Delete consent.
	 *
	 * @param consent the consent
	 */
	public void deleteConsent(Consent consent) {
        consentRepository.delete(consent);
    }
	

	/**
	 * Delete consent.
	 *
	 * @param consentId the consent id
	 */
	public boolean deleteConsent(Long consentId) {
		Consent consent = null;
		
		try{
			consent = findConsent(consentId);
		}catch(IllegalArgumentException e){
			logger.warn("Attempted to call findConsent(consentId) with null or invalid consentId from deleteConsent(Long consentId) method in ConsentService.");
			logger.warn("The exception stack trace is: " + e);
			return false;
		}
		
		if (consent.getSignedPdfConsent()!=null){
			if (consent.getSignedPdfConsent().getSignedPdfConsentContent()!=null){
				return false;
			}
		}
		
		try{
			consentRepository.delete(consent);
		}catch(IllegalArgumentException e){
			logger.warn("Attempted to call consentRepository.delete(consent) with null or invalid consent from deleteConsent(Long consentId) method in ConsentService.");
			logger.warn("The exception stack trace is: " + e);
			return false;
		}
        
        return true;
    }


	/**
	 * Find consent.
	 *
	 * @param id the id
	 * @return the consent
	 */
	public Consent findConsent(Long id) {
        Consent consent = consentRepository.findOne(id);
        return consent;
    }


	/**
	 * Find all consents.
	 *
	 * @return the list
	 */
	public List<Consent> findAllConsents() {
        return consentRepository.findAll();
    }
	
	

	/**
	 * Sign consent.
	 *
	 * @param consentPdfDto the consent pdf dto
	 */
	@Override
	public boolean signConsent(ConsentPdfDto consentPdfDto){
		Consent consent=consentRepository.findOne(consentPdfDto.getId());
		if(consent==null)
			return false;
		// SignConsent
		SignedPDFConsent signedPdfConsent = makeSignedPdfConsent();
		Patient patient=consent.getPatient();
		String patientEmail=patient.getEmail();
		
		//Email hard-coded and to be changed
		signedPdfConsent.setDocumentId((echoSignSignatureService.sendDocumentToSign(consentPdfDto.getContent(), 
				consentPdfDto.getFilename()+".pdf", consentPdfDto.getConsentName(), 
				patientEmail,"consent2share@gmail.com")));
		signedPdfConsent.setDocumentNameBySender(consentPdfDto.getConsentName());
		signedPdfConsent.setDocumentMessageBySender("This is a hard-coded greeting to be replaced. Hi.");
		signedPdfConsent.setSignerEmail(patientEmail);
		signedPdfConsent.setDocumentSignedStatus("Unsigned");
		consent.setSignedPdfConsent(signedPdfConsent);
		consentRepository.save(consent);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see gov.samhsa.consent2share.service.consent.ConsentService#signConsentRevokation(gov.samhsa.consent2share.service.dto.ConsentRevokationPdfDto)
	 */
	public void signConsentRevokation(ConsentRevokationPdfDto consentRevokationPdfDto){
		Consent consent=consentRepository.findOne(consentRevokationPdfDto.getId());
		// SignConsentRevokation
		SignedPDFConsentRevocation signedPDFConsentRevocation=makeSignedPDFConsentRevocation();
		Patient patient=consent.getPatient();
		String patientEmail=patient.getEmail();
		
		//TODO:Email and Email message hard-coded and to be changed
		signedPDFConsentRevocation.setDocumentId((echoSignSignatureService.sendDocumentToSign(consentRevokationPdfDto.getContent(), 
				consentRevokationPdfDto.getFilename()+".pdf", consentRevokationPdfDto.getConsentName() + " Revocation", 
				patientEmail,"consent2share@gmail.com")));
		signedPDFConsentRevocation.setDocumentNameBySender(consentRevokationPdfDto.getConsentName());
		signedPDFConsentRevocation.setDocumentMessageBySender("This is a hard-coded greeting to be replaced. Hi.");
		signedPDFConsentRevocation.setSignerEmail("consent2share@gmail.com");
		signedPDFConsentRevocation.setDocumentSignedStatus("Unsigned");
		signedPDFConsentRevocation.setDocumentCreatedBy(consent.getPatient().getLastName()+", "+consent.getPatient().getFirstName());
		signedPDFConsentRevocation.setDocumentSentOutForSignatureDateTime(new Date());
		consent.setConsentRevoked(true);
		consent.setSignedPdfConsentRevoke(signedPDFConsentRevocation);
		
		if(consentRevokationPdfDto.getRevokationType().equals("EMERGENCY ONLY"))
			consent.setConsentRevokationType("EMERGENCY ONLY");
		if(consentRevokationPdfDto.getRevokationType().equals("NO NEVER"))
			consent.setConsentRevokationType("NO NEVER");
		
		consentRepository.save(consent);
		}
	
	/**
	 * Make signed pdf consent revocation.
	 *
	 * @return the signed pdf consent revocation
	 */
	public SignedPDFConsentRevocation makeSignedPDFConsentRevocation(){
		return new SignedPDFConsentRevocation();
	}
	
	

	/**
	 * Make signed pdf consent.
	 *
	 * @return the signed pdf consent
	 */
	public SignedPDFConsent makeSignedPdfConsent(){
		return new SignedPDFConsent();
	}
	

	/**
	 * Find consent pdf dto.
	 *
	 * @param consentId the consent id
	 * @return the consent pdf dto
	 */
	public ConsentPdfDto findConsentPdfDto(Long consentId){
		Consent consent=consentRepository.findOne(consentId);
		ConsentPdfDto consentPdfDto=makeConsentPdfDto();
		if (consent.getSignedPdfConsent()!=null){
			if (consent.getSignedPdfConsent().getSignedPdfConsentContent()!=null){
				consentPdfDto.setContent(consent.getSignedPdfConsent().getSignedPdfConsentContent());
			}
			else{
				consentPdfDto.setContent(consent.getUnsignedPdfConsent());
			}
		}
		else{
			consentPdfDto.setContent(consent.getUnsignedPdfConsent());
		}
		consentPdfDto.setFilename(consent.getPatient().getFirstName()+"_"+consent.getPatient().getLastName()+"_Consent"+consent.getId());
		consentPdfDto.setConsentName(consent.getName());
		consentPdfDto.setId(consentId);
		return consentPdfDto;
	}
	
	/* (non-Javadoc)
	 * @see gov.samhsa.consent2share.service.consent.ConsentService#findConsentRevokationPdfDto(java.lang.Long)
	 */
	public ConsentRevokationPdfDto findConsentRevokationPdfDto(Long consentId){
		Consent consent=consentRepository.findOne(consentId);
		ConsentRevokationPdfDto consentRevokationPdfDto=makeConsentRevokationPdfDto();
		if(consent.getSignedPdfConsentRevoke()!=null){
			if(consent.getSignedPdfConsentRevoke().getSignedPdfConsentRevocationContent()!=null){
				consentRevokationPdfDto.setContent(consent.getSignedPdfConsentRevoke().getSignedPdfConsentRevocationContent());
			}
			else{
				consentRevokationPdfDto.setContent(consent.getUnsignedPdfConsentRevoke());
			}
		}
		else{
			consentRevokationPdfDto.setContent(consent.getUnsignedPdfConsentRevoke());
		}
		consentRevokationPdfDto.setFilename(consent.getPatient().getFirstName()+"_"
				+consent.getPatient().getLastName()+"_ConsentRevokation"+consent.getId());
		consentRevokationPdfDto.setConsentName(consent.getName());
		consentRevokationPdfDto.setId(consent.getId());
		return consentRevokationPdfDto;
	}
	
	/**
	 * Make consent revokation pdf dto.
	 *
	 * @return the consent revokation pdf dto
	 */
	public ConsentRevokationPdfDto makeConsentRevokationPdfDto(){
		return new ConsentRevokationPdfDto();
	}

	/**
	 * Make consent pdf dto.
	 *
	 * @return the consent pdf dto
	 */
	public ConsentPdfDto makeConsentPdfDto(){
		return new ConsentPdfDto();
	}
	

	/**
	 * Find all consents dto by patient.
	 *
	 * @param patientId the patient id
	 * @return the list
	 */
	//@PreAuthorize("ROLE_USER")
	public List<ConsentListDto> findAllConsentsDtoByPatient(Long patientId){
		Patient patient=patientRepository.findOne(patientId);
		List<Consent> consents = consentRepository.findByPatient(patient);
		List<ConsentListDto> consentListDtos=makeConsentListDtos();
		if (consents!=null)
		consentListDtos=consentListToConsentListDtosConverter(consents);
		return consentListDtos;
	}
	
	@Override
	public List<ConsentListDto> findAllConsentsDtoByUserName(String userName){
		Patient patient=patientRepository.findByUsername(userName);
		List<Consent> consents = consentRepository.findByPatient(patient);
		List<ConsentListDto> consentListDtos=makeConsentListDtos();
		if (consents!=null)
		consentListDtos=consentListToConsentListDtosConverter(consents);
		return consentListDtos;
	}
	
	/**
	 * Consent list to consent list dtos converter.
	 *
	 * @param consents the consents
	 * @return the list
	 */
	public List<ConsentListDto> consentListToConsentListDtosConverter(List<Consent> consents) {
		List<ConsentListDto> consentListDtos=makeConsentListDtos();
		for (Consent consent:consents){
			ConsentListDto consentListDto=new ConsentListDto();
			//Get fields
			Set<String> isMadeToName=new HashSet<String>();
			for (ConsentIndividualProviderDisclosureIsMadeTo item:consent.getProvidersDisclosureIsMadeTo()){
				String name=item.getIndividualProvider().getLastName()+", "+
						item.getIndividualProvider().getFirstName();
				isMadeToName.add(name);
			}
			Set<String> isMadeToOrgName=new HashSet<String>();
			for (ConsentOrganizationalProviderDisclosureIsMadeTo item:consent.getOrganizationalProvidersDisclosureIsMadeTo()){
				isMadeToOrgName.add(item.getOrganizationalProvider().getOrgName());
			}

			Set<String> toDiscloseName=new HashSet<String>();
			for (ConsentIndividualProviderPermittedToDisclose item:consent.getProvidersPermittedToDisclose()){
				String name=item.getIndividualProvider().getLastName()+", "+
						item.getIndividualProvider().getFirstName();
				toDiscloseName.add(name);
			}

			Set<String> toDiscloseOrgName=new HashSet<String>();
			for (ConsentOrganizationalProviderPermittedToDisclose item:consent.getOrganizationalProvidersPermittedToDisclose()){
				toDiscloseOrgName.add(item.getOrganizationalProvider().getOrgName());
			}

			Set<String> consentDoNotShareClinicalDocumentTypeCode=new HashSet<String>();
			for (ConsentDoNotShareClinicalDocumentTypeCode item:consent.getDoNotShareClinicalDocumentTypeCodes()){
				consentDoNotShareClinicalDocumentTypeCode.add(item.getClinicalDocumentTypeCode().getDisplayName());
			}

			Set<String> consentDoNotShareClinicalDocumentSectionTypeCode=new HashSet<String>();
			for (ConsentDoNotShareClinicalDocumentSectionTypeCode item:consent.getDoNotShareClinicalDocumentSectionTypeCodes()){
				consentDoNotShareClinicalDocumentSectionTypeCode.add(item.getClinicalDocumentSectionTypeCode().getDisplayName());
			}

			Set<String> consentDoNotShareSensitivityPolicyCode=new HashSet<String>();
			for (ConsentDoNotShareSensitivityPolicyCode item:consent.getDoNotShareSensitivityPolicyCodes()){
				consentDoNotShareSensitivityPolicyCode.add(item.getSensitivityPolicyCode().getDisplayName());
			}

			Set<String> consentShareForPurposeOfUseCode=new HashSet<String>();
			for (ConsentShareForPurposeOfUseCode item:consent.getShareForPurposeOfUseCodes()){
				consentShareForPurposeOfUseCode.add(item.getPurposeOfUseCode().getDisplayName());
			}

			Set<String> consentDoNotShareClinicalConceptCodes=new HashSet<String>();
			for (ClinicalConceptCode item:consent.getDoNotShareClinicalConceptCodes()){
				consentDoNotShareClinicalConceptCodes.add(item.getDisplayName());
			}

			if (consent.getSignedPdfConsent()!=null){
				if (consent.getSignedPdfConsent().getSignedPdfConsentContent()!=null){
					consentListDto.setConsentStage("CONSENT_SIGNED");
				}
				else{
					consentListDto.setConsentStage("CONSENT_SUBMITTED");
				}
			}
			else{
				consentListDto.setConsentStage("CONSENT_SAVED");
			}

			if (!consentListDto.getConsentStage().equals("CONSENT_SIGNED")){
				consentListDto.setRevokeStage("NA");
			}
			else{
				if (consent.getSignedPdfConsentRevoke()!=null)
					if(consent.getSignedPdfConsentRevoke().getSignedPdfConsentRevocationContent()!=null)
						consentListDto.setRevokeStage("REVOCATION_REVOKED");
					else
						consentListDto.setRevokeStage("REVOCATION_SUBMITTED");
				else
					consentListDto.setRevokeStage("REVOCATION_NOT_SUBMITTED");
			}


			//Set fields
			isMadeToName.addAll(isMadeToOrgName);
			toDiscloseName.addAll(toDiscloseOrgName);
			consentListDto.setDoNotShareClinicalConceptCodes(consentDoNotShareClinicalConceptCodes);
			consentListDto.setId(consent.getId());
			consentListDto.setIsMadeToName(isMadeToName);
			consentListDto.setToDiscloseName(toDiscloseName);
			consentListDto.setDoNotShareClinicalDocumentSectionTypeCodes(consentDoNotShareClinicalDocumentSectionTypeCode);
			consentListDto.setDoNotShareClinicalDocumentTypeCodes(consentDoNotShareClinicalDocumentTypeCode);
			consentListDto.setShareForPurposeOfUseCodes(consentShareForPurposeOfUseCode);
			consentListDto.setDoNotShareSensitivityPolicyCodes(consentDoNotShareSensitivityPolicyCode);
			
			consentListDto.setConsentStart(consent.getStartDate());
			consentListDto.setConsentEnd(consent.getEndDate());
				
			//Merge all Dtos
			consentListDtos.add(consentListDto);
		}
		return consentListDtos;
	}

	/**
	 * Make consent list dtos.
	 *
	 * @return the array list
	 */
	public ArrayList<ConsentListDto> makeConsentListDtos(){
		return new ArrayList<ConsentListDto>();
	}


	/**
	 * Find consent entries.
	 *
	 * @param firstResult the first result
	 * @param maxResults the max results
	 * @return the list
	 */
	public List<Consent> findConsentEntries(int firstResult, int maxResults) {
        return consentRepository.findAll(new org.springframework.data.domain.PageRequest(firstResult / maxResults, maxResults)).getContent();
    }


	/**
	 * Save consent.
	 *
	 * @param consent the consent
	 */
	public void saveConsent(Consent consent) {
        consentRepository.save(consent);
    }


	/**
	 * Update consent.
	 *
	 * @param consent the consent
	 * @return the consent
	 */
	public Consent updateConsent(Consent consent) {
        return consentRepository.save(consent);
    }
	

    /**
     * Save consent.
     *
     * @param consentDto the consent dto
     * @throws ConsentGenException the consent gen exception
     */
    public void saveConsent(ConsentDto consentDto) throws ConsentGenException {
	Consent consent = makeConsent();
	Patient patient = patientRepository.findByUsername(consentDto.getUsername());
	Map<String, AbstractProvider> providerMap=new HashMap<String, AbstractProvider>();
	for (IndividualProvider o:patient.getIndividualProviders())
		providerMap.put(o.getNpi(),o);
	for (OrganizationalProvider o:patient.getOrganizationalProviders())
		providerMap.put(o.getNpi(), o);
	if (consentDto.getId() != null && consentDto.getId().longValue() > 0)
	    consent = consentRepository.findOne(consentDto.getId());

	// Set Providers
	if (consentDto.getProvidersDisclosureIsMadeTo() != null) {
	    Set<ConsentIndividualProviderDisclosureIsMadeTo> providersDisclosureIsMadeTo = new HashSet<ConsentIndividualProviderDisclosureIsMadeTo>();
	    for (String item : consentDto.getProvidersDisclosureIsMadeTo()) {
		IndividualProvider individualProvider = (IndividualProvider) providerMap.get(item);
		ConsentIndividualProviderDisclosureIsMadeTo consentIndividualProviderPermittedToDisclose = new ConsentIndividualProviderDisclosureIsMadeTo(
			individualProvider);
		providersDisclosureIsMadeTo.add(consentIndividualProviderPermittedToDisclose);
	    }
	    consent.setProvidersDisclosureIsMadeTo(providersDisclosureIsMadeTo);
	} else
	    consent.setProvidersDisclosureIsMadeTo(new HashSet<ConsentIndividualProviderDisclosureIsMadeTo>());

	if (consentDto.getProvidersPermittedToDisclose() != null) {
	    Set<ConsentIndividualProviderPermittedToDisclose> providersPermittedToDisclose = new HashSet<ConsentIndividualProviderPermittedToDisclose>();
	    for (String item : consentDto.getProvidersPermittedToDisclose()) {
		IndividualProvider individualProvider = (IndividualProvider) providerMap.get(item);
		ConsentIndividualProviderPermittedToDisclose consentIndividualProviderPermittedToDisclose = new ConsentIndividualProviderPermittedToDisclose(
			individualProvider);
		providersPermittedToDisclose.add(consentIndividualProviderPermittedToDisclose);
	    }
	    consent.setProvidersPermittedToDisclose(providersPermittedToDisclose);
	}
	else
	    consent.setProvidersPermittedToDisclose(new HashSet<ConsentIndividualProviderPermittedToDisclose>());

	if (consentDto.getOrganizationalProvidersDisclosureIsMadeTo() != null) {
	    Set<ConsentOrganizationalProviderDisclosureIsMadeTo> organizationalProvidersDisclosureIsMadeTo = new HashSet<ConsentOrganizationalProviderDisclosureIsMadeTo>();
	    for (String item : consentDto.getOrganizationalProvidersDisclosureIsMadeTo()) {
		OrganizationalProvider organizationalProvider = (OrganizationalProvider) providerMap.get(item);
		ConsentOrganizationalProviderDisclosureIsMadeTo consentOrganizationalProviderPermittedToDisclose = new ConsentOrganizationalProviderDisclosureIsMadeTo(
			organizationalProvider);
		organizationalProvidersDisclosureIsMadeTo.add(consentOrganizationalProviderPermittedToDisclose);
	    }
	    consent.setOrganizationalProvidersDisclosureIsMadeTo(organizationalProvidersDisclosureIsMadeTo);
	}
	else
	    consent.setOrganizationalProvidersDisclosureIsMadeTo(new HashSet<ConsentOrganizationalProviderDisclosureIsMadeTo>());

	if (consentDto.getOrganizationalProvidersPermittedToDisclose() != null) {
	    Set<ConsentOrganizationalProviderPermittedToDisclose> organizationalProvidersPermittedToDisclose = new HashSet<ConsentOrganizationalProviderPermittedToDisclose>();
	    for (String item : consentDto.getOrganizationalProvidersPermittedToDisclose()) {
		OrganizationalProvider organizationalProvider = (OrganizationalProvider) providerMap.get(item);
		ConsentOrganizationalProviderPermittedToDisclose consentOrganizationalProviderPermittedToDisclose = new ConsentOrganizationalProviderPermittedToDisclose(
			organizationalProvider);
		organizationalProvidersPermittedToDisclose.add(consentOrganizationalProviderPermittedToDisclose);
	    }
	    consent.setOrganizationalProvidersPermittedToDisclose(organizationalProvidersPermittedToDisclose);
	}
	else
	    consent.setOrganizationalProvidersPermittedToDisclose(new HashSet<ConsentOrganizationalProviderPermittedToDisclose>());
	    
	// Set Do Not Shares
	if (consentDto.getDoNotShareClinicalDocumentTypeCodes() != null) {
	    Set<ConsentDoNotShareClinicalDocumentTypeCode> doNotShareClinicalDocumentTypeCodes = new HashSet<ConsentDoNotShareClinicalDocumentTypeCode>();
	    for (String item : consentDto.getDoNotShareClinicalDocumentTypeCodes()) {
		ClinicalDocumentTypeCode clinicalDocumentTypeCode = clinicalDocumentTypeCodeRepository.findByCode(item);
		ConsentDoNotShareClinicalDocumentTypeCode consentDoNotShareClinicalDocumentTypeCode = new ConsentDoNotShareClinicalDocumentTypeCode(
			clinicalDocumentTypeCode);
		doNotShareClinicalDocumentTypeCodes.add(consentDoNotShareClinicalDocumentTypeCode);
	    }
	    consent.setDoNotShareClinicalDocumentTypeCodes(doNotShareClinicalDocumentTypeCodes);
	}
	else
	    consent.setDoNotShareClinicalDocumentTypeCodes(new HashSet<ConsentDoNotShareClinicalDocumentTypeCode>());

	if (consentDto.getDoNotShareClinicalDocumentSectionTypeCodes() != null) {
	    Set<ConsentDoNotShareClinicalDocumentSectionTypeCode> doNotShareClinicalDocumentSectionTypeCodes = new HashSet<ConsentDoNotShareClinicalDocumentSectionTypeCode>();
	    for (String item : consentDto.getDoNotShareClinicalDocumentSectionTypeCodes()) {
		ClinicalDocumentSectionTypeCode clinicalDocumentSectionTypeCode = clinicalDocumentSectionTypeCodeRepository
			.findByCode(item);
		ConsentDoNotShareClinicalDocumentSectionTypeCode consentDoNotShareClinicalDocumentSectionTypeCode = new ConsentDoNotShareClinicalDocumentSectionTypeCode(
			clinicalDocumentSectionTypeCode);
		doNotShareClinicalDocumentSectionTypeCodes.add(consentDoNotShareClinicalDocumentSectionTypeCode);
	    }
	    consent.setDoNotShareClinicalDocumentSectionTypeCodes(doNotShareClinicalDocumentSectionTypeCodes);
	}
	else
	    consent.setDoNotShareClinicalDocumentSectionTypeCodes(new HashSet<ConsentDoNotShareClinicalDocumentSectionTypeCode>());

	if (consentDto.getDoNotShareSensitivityPolicyCodes() != null) {
	    Set<ConsentDoNotShareSensitivityPolicyCode> doNotShareSensitivityPolicyCodes = new HashSet<ConsentDoNotShareSensitivityPolicyCode>();
	    for (String item : consentDto.getDoNotShareSensitivityPolicyCodes()) {
		SensitivityPolicyCode sensitivityPolicyCode = sensitivityPolicyCodeRepository.findByCode(item);
		ConsentDoNotShareSensitivityPolicyCode consentDoNotShareSensitivityPolicyCode = new ConsentDoNotShareSensitivityPolicyCode(
			sensitivityPolicyCode);
		doNotShareSensitivityPolicyCodes.add(consentDoNotShareSensitivityPolicyCode);
	    }
	    consent.setDoNotShareSensitivityPolicyCodes(doNotShareSensitivityPolicyCodes);
	}
	else
	    consent.setDoNotShareSensitivityPolicyCodes(new HashSet<ConsentDoNotShareSensitivityPolicyCode>());
	
	if (consentDto.getShareForPurposeOfUseCodes() != null) {
	    Set<ConsentShareForPurposeOfUseCode> shareForPurposeOfUseCodes = new HashSet<ConsentShareForPurposeOfUseCode>();
	    for (String item : consentDto.getShareForPurposeOfUseCodes()) {
		PurposeOfUseCode purposeOfUseCode = purposeOfUseCodeRepository.findByCode(item);
		ConsentShareForPurposeOfUseCode consentShareForPurposeOfUseCode = new ConsentShareForPurposeOfUseCode(
			purposeOfUseCode);
		shareForPurposeOfUseCodes.add(consentShareForPurposeOfUseCode);
	    }
	    consent.setShareForPurposeOfUseCodes(shareForPurposeOfUseCodes);
	}
	else
	    consent.setShareForPurposeOfUseCodes(new HashSet<ConsentShareForPurposeOfUseCode>());

	if (consentDto.getDoNotShareClinicalConceptCodes() != null) {
	    Set<ClinicalConceptCode> doNotShareClinicalConceptCodes = new HashSet<ClinicalConceptCode>();
	    for (SpecificMedicalInfoDto item : consentDto.getDoNotShareClinicalConceptCodes()) {
		ClinicalConceptCode clinicalConceptCode = new ClinicalConceptCode();
		clinicalConceptCode.setCode(item.getCode());
		clinicalConceptCode.setCodeSystem(item.getCodeSystem());
		clinicalConceptCode.setCodeSystemName(item.getCodeSystem());
		clinicalConceptCode.setDisplayName(item.getDisplayName());
		doNotShareClinicalConceptCodes.add(clinicalConceptCode);
	    }
	    consent.setDoNotShareClinicalConceptCodes(doNotShareClinicalConceptCodes);
	}
	else
	    consent.setDoNotShareClinicalConceptCodes(new HashSet<ClinicalConceptCode>());

	// Set Dates	
	consent.setStartDate(consentDto.getConsentStart());
	consent.setEndDate(consentDto.getConsentEnd());

	consent.setPatient(patient);
	consent.setName("Consent");
	consent.setDescription("This is a consent made by " + patient.getFirstName() + " " + patient.getLastName());
	consent.setUnsignedPdfConsent(consentPdfGenerator.generate42CfrPart2Pdf(consent));
	try {
		consent.setXacmlPolicyFile(consentExportService.exportConsent2XACML(consent).getBytes());
	} catch (ConsentGenException e) {
		logger.error("Error in saving consent in xacml format",e);
		throw new ConsentGenException(e.getMessage());
	}
	consentRepository.save(consent);

    }
	
	

	/* (non-Javadoc)
	 * @see gov.samhsa.consent2share.service.consent.ConsentService#addUnsignedConsentRevokationPdf(java.lang.Long, java.lang.String)
	 */
	public void addUnsignedConsentRevokationPdf(Long consentId,String revokationType){
		Consent consent=consentRepository.findOne(consentId);
		if (revokationType.equals("EMERGENCY ONLY"))
			consent.setConsentRevokationType("EMERGENCY ONLY");
		else if (revokationType.equals("NO NEVER"))
			consent.setConsentRevokationType("NO NEVER");
		consent.setUnsignedPdfConsentRevoke(consentRevokationPdfGenerator.generateConsentRevokationPdf(consent));
		consentRepository.save(consent);
	}
	

	/**
	 * Make consent.
	 *
	 * @return the consent
	 */
	public Consent makeConsent(){
		Consent consent=new Consent();
		consent.setConsentReferenceId(UUID.randomUUID().toString());
		return consent;
	}
	

	/**
	 * Make consent dto.
	 *
	 * @return the consent dto
	 */
	public ConsentDto makeConsentDto(){
		return new ConsentDto();
	}
	

	/**
	 * Checks if is consent belong to this user.
	 *
	 * @param consentId the consent id
	 * @param patientId the patient id
	 * @return true, if is consent belong to this user
	 */
        @Override
        public boolean isConsentBelongToThisUser(Long consentId, Long patientId) {
            if (consentRepository.findOne(consentId) != null) {
        	    Consent consent1 = consentRepository.findOne(consentId);
        	    Patient patient = patientRepository.findOne(patientId);
        
        	    return consent1.getPatient().equals(patient);
    		} else
    	    return false;
        }
	
	/**
	 * Returns consentDto based on consent id.
	 *
	 * @param consentId the consent id
	 * @return ConsentDto
	 */
	public ConsentDto findConsentById(Long consentId) {
		Consent consent = null;
		ConsentDto consentDto = null;
		
		if(consentRepository.findOne(consentId)!=null){
			consent=consentRepository.findOne(consentId);

		 	consentDto=new ConsentDto();
			//Get fields
			Set<String> isMadeToName=new HashSet<String>();
			Set<String> isMadeToNpi=new HashSet<String>();
			for (ConsentIndividualProviderDisclosureIsMadeTo item:consent.getProvidersDisclosureIsMadeTo()){
				String name=item.getIndividualProvider().getLastName()+", "+
						item.getIndividualProvider().getFirstName(); 
				isMadeToName.add(name);
				isMadeToNpi.add(item.getIndividualProvider().getNpi());
			}
			
			Set<String> isMadeToOrgName=new HashSet<String>();
			Set<String> isMadeToOrgNpi =new HashSet<String>();
			for (ConsentOrganizationalProviderDisclosureIsMadeTo item:consent.getOrganizationalProvidersDisclosureIsMadeTo()){
				isMadeToOrgName.add(item.getOrganizationalProvider().getOrgName());
				isMadeToOrgNpi.add(item.getOrganizationalProvider().getNpi());
			}
			
			Set<String> toDiscloseName= new HashSet<String>();
			Set<String> toDiscloseNpi = new HashSet<String>();
			for (ConsentIndividualProviderPermittedToDisclose item:consent.getProvidersPermittedToDisclose()){
				String name=item.getIndividualProvider().getLastName()+", "+
						item.getIndividualProvider().getFirstName();
				toDiscloseName.add(name);
				toDiscloseNpi.add(item.getIndividualProvider().getNpi());
			}
			
			Set<String> toDiscloseOrgName=new HashSet<String>();
			Set<String> toDiscloseOrgNpi=new HashSet<String>();
			for (ConsentOrganizationalProviderPermittedToDisclose item:consent.getOrganizationalProvidersPermittedToDisclose()){
				toDiscloseOrgName.add(item.getOrganizationalProvider().getOrgName());
				toDiscloseOrgNpi.add(item.getOrganizationalProvider().getNpi());
			}
			
			Set<String> consentDoNotShareClinicalDocumentTypeCode=new HashSet<String>();
			for (ConsentDoNotShareClinicalDocumentTypeCode item:consent.getDoNotShareClinicalDocumentTypeCodes()){
				consentDoNotShareClinicalDocumentTypeCode.add(item.getClinicalDocumentTypeCode().getDisplayName()); 
			}
			
			Set<String> consentDoNotShareClinicalDocumentSectionTypeCode=new HashSet<String>();
			for (ConsentDoNotShareClinicalDocumentSectionTypeCode item:consent.getDoNotShareClinicalDocumentSectionTypeCodes()){
				consentDoNotShareClinicalDocumentSectionTypeCode.add(item.getClinicalDocumentSectionTypeCode().getDisplayName()); 
			}
			
			Set<String> consentDoNotShareSensitivityPolicyCode=new HashSet<String>();
			for (ConsentDoNotShareSensitivityPolicyCode item:consent.getDoNotShareSensitivityPolicyCodes()){
				consentDoNotShareSensitivityPolicyCode.add(item.getSensitivityPolicyCode().getDisplayName()); 
			}
			
			Set<String> consentShareForPurposeOfUseCode=new HashSet<String>();
			for (ConsentShareForPurposeOfUseCode item:consent.getShareForPurposeOfUseCodes()){
				consentShareForPurposeOfUseCode.add(item.getPurposeOfUseCode().getDisplayName()); 
			}
			
			Set<SpecificMedicalInfoDto> consentDoNotShareClinicalConceptCodes=new HashSet<SpecificMedicalInfoDto>();
			for (ClinicalConceptCode item:consent.getDoNotShareClinicalConceptCodes()){
				SpecificMedicalInfoDto specificMedicalInfoDto = new SpecificMedicalInfoDto();
				specificMedicalInfoDto.setCode(item.getCode());
				specificMedicalInfoDto.setCodeSystem(item.getCodeSystem());
				specificMedicalInfoDto.setDisplayName(item.getDisplayName());
				consentDoNotShareClinicalConceptCodes.add(specificMedicalInfoDto); 
			}
			
			
			//Set fields
//			isMadeToName.addAll(isMadeToOrgName);
//			toDiscloseName.addAll(toDiscloseOrgName);
			consentDto.setDoNotShareClinicalConceptCodes(consentDoNotShareClinicalConceptCodes);
			consentDto.setId(consent.getId());
			consentDto.setUsername(consent.getPatient().getUsername());

			//
		    // Converting timestamp to Date to resolve
			// rendering the values in IE
		    //
		    DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		    String today = "" ;			
		    try {
				today = formatter.format(consent.getStartDate());
				consentDto.setConsentStart(formatter.parse(today));
			} catch (ParseException e) {
				consentDto.setConsentStart(consent.getStartDate());
				e.printStackTrace();
			}		
			try {
				today = formatter.format(consent.getEndDate());
				consentDto.setConsentEnd(formatter.parse(today));
			} catch (ParseException e) {
				consentDto.setConsentEnd(consent.getEndDate());
				e.printStackTrace();
			}	
			
			// TODO: Cleanup: combine name and npi into one object
			// populate consent dto with selected options
			consentDto.setOrganizationalProvidersDisclosureIsMadeTo(isMadeToOrgName);
			consentDto.setOrganizationalProvidersDisclosureIsMadeToNpi(isMadeToOrgNpi);
			
			consentDto.setOrganizationalProvidersPermittedToDisclose(toDiscloseOrgName);
			consentDto.setOrganizationalProvidersPermittedToDiscloseNpi(toDiscloseOrgNpi);
			
			consentDto.setProvidersDisclosureIsMadeTo(isMadeToName);
			consentDto.setProvidersDisclosureIsMadeToNpi(isMadeToNpi);

			consentDto.setProvidersPermittedToDisclose(toDiscloseName);
			consentDto.setProvidersPermittedToDiscloseNpi(toDiscloseNpi);
			
			consentDto.setDoNotShareClinicalDocumentSectionTypeCodes(consentDoNotShareClinicalDocumentSectionTypeCode);
			consentDto.setDoNotShareClinicalDocumentTypeCodes(consentDoNotShareClinicalDocumentTypeCode);
			consentDto.setShareForPurposeOfUseCodes(consentShareForPurposeOfUseCode);
			consentDto.setDoNotShareSensitivityPolicyCodes(consentDoNotShareSensitivityPolicyCode);


		}
		return consentDto;
	}
	
	/**
	 * Are there duplicates.
	 *
	 * @param set1 the set1
	 * @param set2 the set2
	 * @return true, if successful
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean areThereDuplicatesInTwoSets(Set set1,Set set2){
		Set all=new HashSet();
		all.addAll(set1);
		all.addAll(set2);
		if (all.size()==set1.size()+set2.size()){
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see gov.samhsa.consent2share.service.consent.ConsentService#findConsentContentDto(java.lang.Long)
	 */
	public AbstractPdfDto findConsentContentDto(Long consentId){
		Consent consent=findConsent(consentId);
		AbstractPdfDto consentPdfDto;
		if (consent.getSignedPdfConsent()!=null){
			if (consent.getSignedPdfConsent().getSignedPdfConsentContent()!=null){
				consentPdfDto=findConsentPdfDto(consent.getId());
			}
			else{
				consentPdfDto=findConsentPdfDto(consent.getId());
			}
		}
		else{
			consentPdfDto=findConsentPdfDto(consent.getId());
		}

		if (consent.getSignedPdfConsentRevoke()!=null){
				if(consent.getSignedPdfConsentRevoke().getSignedPdfConsentRevocationContent()!=null){
					consentPdfDto=findConsentRevokationPdfDto(consent.getId());
				}
				else{
					consentPdfDto=findConsentRevokationPdfDto(consent.getId());
				}

		}
		return consentPdfDto;
		
	}
	

	/**
	 * Return tagged c32. Entry tags that have been removed in segmented c32 were tagged and returned 
	 * 
	 * @param originalC32
	 * @param segmentedC32
	 * @return
	 */
	public String getTaggedC32(String originalC32,
			Long consentId) {
		
		// get segmented doc
		String segmentedC32 = tryPolicyService.tryPolicy(originalC32, consentId);
		
		List<String> originalC32Ids = new ArrayList<String>();
		List<String> segmentedC32Ids = new ArrayList<String>();
		List<String> taggedC32Ids = new ArrayList<String>();
		
		StringWriter writer = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		
		try {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		final DocumentBuilder db = dbf.newDocumentBuilder();

		Document taggedC32Doc = db.parse(new InputSource(new ByteArrayInputStream(originalC32.getBytes("utf-8"))));
		changeXslPath(taggedC32Doc);
		NodeList taggedC32List = taggedC32Doc.getElementsByTagName("entry");
		
		Document segmentedC32Doc = db.parse(new InputSource(new ByteArrayInputStream(segmentedC32.getBytes("utf-8"))));
		NodeList segmentedC32List = segmentedC32Doc.getElementsByTagName("entry");
		
		originalC32Ids = getIds(taggedC32List);
		segmentedC32Ids = getIds(segmentedC32List);
		
		taggedC32Ids = getIdsToTag(originalC32Ids, segmentedC32Ids);
		
		tagC32Document(taggedC32List, taggedC32Ids);
		
		logger.info("Tagged C32 Entry size: " + taggedC32List.getLength());
		logger.info("Segmented C32 Entry size: " + segmentedC32List.getLength());

		logger.info("Original C32: " + originalC32);
		logger.info("Segmented C32: " + segmentedC32);;
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(taggedC32Doc);
		writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
		
		transformer.transform(source, result);
		logger.info("Printing tagged file: " + writer.toString());
				
		// xslt transformation
		
		TransformerFactory tFactory = TransformerFactory.newInstance();
		InputStream is = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream("CDA_flag_redact.xsl");
		StreamSource styleSource = new StreamSource(is);
		Transformer transformer1 = tFactory.newTransformer(styleSource);
		
		byteArrayOutputStream = new ByteArrayOutputStream();
		transformer1.transform(new DOMSource(taggedC32Doc), new StreamResult(byteArrayOutputStream));
		logger.info("Printing transformed xslt: " + byteArrayOutputStream.toString());

		} catch (ParserConfigurationException e) {
			throw new DS4PException(e.toString(), e);
		} catch (UnsupportedEncodingException e) {
			throw new DS4PException(e.toString(), e);
		} catch (SAXException e) {
			throw new DS4PException(e.toString(), e);
		} catch (IOException e) {
			throw new DS4PException(e.toString(), e);
		} catch (TransformerConfigurationException e) {
			throw new DS4PException(e.toString(), e);
		} catch (TransformerException e) {
			throw new DS4PException(e.toString(), e);
		}
		
		return byteArrayOutputStream.toString();
	}
	
	
	/**
	 * Changes xsl path to local xsl
	 * 
	 * @param taggedC32Doc
	 */
	private void changeXslPath(Document taggedC32Doc) {
		
		Element root = taggedC32Doc.getDocumentElement();
		XPath xpath = XPathFactory.newInstance().newXPath();
		String expression = "/processing-instruction('xml-stylesheet')";
		ProcessingInstruction pi = null;
		try {
			pi = (ProcessingInstruction)xpath.evaluate(expression, taggedC32Doc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pi.setData("type='text/xsl' href='CDA_flag_redact.xsl'");
		
		StringWriter writer = null;
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			
			DOMSource source = new DOMSource(taggedC32Doc);
			writer = new StringWriter();
	        StreamResult result = new StreamResult(writer);
	        
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public List<String> getIds(NodeList c32List) {
		List<String> listOfIdsInC32 = new ArrayList<String>();
		
		for (int i = 0; i < c32List.getLength(); i++) {

			NodeList childNodes = c32List.item(i).getChildNodes();
			
		    for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
		        Node childNode = childNodes.item(childIndex);

		        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
		        	
		            NodeList grandChildNodeList = childNode.getChildNodes();
		            
		            for (int grandChildIndex = 0; grandChildIndex < grandChildNodeList.getLength(); grandChildIndex++) {
				        Node grandChildNode = grandChildNodeList.item(grandChildIndex);
		            	
		            	if (grandChildNode.getNodeName().equalsIgnoreCase("ID")) {

		            		listOfIdsInC32.add(grandChildNode.getAttributes().getNamedItem("root").getNodeValue());
		            	}
		            }
		        }
		    }
		}
		return listOfIdsInC32;
	}
	
	public List<String> getIdsToTag(List<String> originalC32Ids,
			List<String> segmentedC32Ids) {
		List<String> idsToTag = new ArrayList<String>();

		for (int i = 0; i < originalC32Ids.size(); i++) {
			if (!segmentedC32Ids.contains(originalC32Ids.get(i))) {
				idsToTag.add(originalC32Ids.get(i));
			}
		}

		return idsToTag;
	}
	
	public void tagC32Document(NodeList taggedC32List, List<String> taggedC32Ids) {
		
		for (int i = 0; i < taggedC32List.getLength(); i++) {
			Element elementToAddAttribute = (Element) taggedC32List.item(i);

			NodeList childNodes = taggedC32List.item(i).getChildNodes();
			
		    for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
		        Node childNode = childNodes.item(childIndex);

		        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
		            NodeList grandChildNodeList = childNode.getChildNodes();
		            
		            for (int grandChildIndex = 0; grandChildIndex < grandChildNodeList.getLength(); grandChildIndex++) {
				        Node grandChildNode = grandChildNodeList.item(grandChildIndex);
		            	
		            	if (grandChildNode.getNodeName().equalsIgnoreCase("ID")) {
	            		
		            		// tag c32 by adding redact attribute to ids that were segmented
		            		if (taggedC32Ids.contains(grandChildNode.getAttributes().getNamedItem("root").getNodeValue())) {
		            			logger.info("Match!");
		            			elementToAddAttribute.setAttribute("redact", "redact");
		            		}
		            	}
		            }
		        }
		    }
		}
	}


}
