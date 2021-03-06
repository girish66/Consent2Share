package org.hl7.v3;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="POCD_MT000040.IntendedRecipient", propOrder={"realmCode", "typeId", "templateId", "id", "addr", "telecom", "informationRecipient", "receivedOrganization"})
public class POCDMT000040IntendedRecipient
{
  protected List<CS> realmCode;
  protected POCDMT000040InfrastructureRootTypeId typeId;
  protected List<II> templateId;
  protected List<II> id;
  protected List<AD> addr;
  protected List<TEL> telecom;
  protected POCDMT000040Person informationRecipient;
  protected POCDMT000040Organization receivedOrganization;

  @XmlAttribute(name="nullFlavor")
  protected List<String> nullFlavor;

  @XmlAttribute(name="classCode")
  protected XInformationRecipientRole classCode;

  public List<CS> getRealmCode()
  {
    if (this.realmCode == null) {
      this.realmCode = new ArrayList();
    }
    return this.realmCode;
  }

  public POCDMT000040InfrastructureRootTypeId getTypeId()
  {
    return this.typeId;
  }

  public void setTypeId(POCDMT000040InfrastructureRootTypeId value)
  {
    this.typeId = value;
  }

  public List<II> getTemplateId()
  {
    if (this.templateId == null) {
      this.templateId = new ArrayList();
    }
    return this.templateId;
  }

  public List<II> getId()
  {
    if (this.id == null) {
      this.id = new ArrayList();
    }
    return this.id;
  }

  public List<AD> getAddr()
  {
    if (this.addr == null) {
      this.addr = new ArrayList();
    }
    return this.addr;
  }

  public List<TEL> getTelecom()
  {
    if (this.telecom == null) {
      this.telecom = new ArrayList();
    }
    return this.telecom;
  }

  public POCDMT000040Person getInformationRecipient()
  {
    return this.informationRecipient;
  }

  public void setInformationRecipient(POCDMT000040Person value)
  {
    this.informationRecipient = value;
  }

  public POCDMT000040Organization getReceivedOrganization()
  {
    return this.receivedOrganization;
  }

  public void setReceivedOrganization(POCDMT000040Organization value)
  {
    this.receivedOrganization = value;
  }

  public List<String> getNullFlavor()
  {
    if (this.nullFlavor == null) {
      this.nullFlavor = new ArrayList();
    }
    return this.nullFlavor;
  }

  public XInformationRecipientRole getClassCode()
  {
    if (this.classCode == null) {
      return XInformationRecipientRole.ASSIGNED;
    }
    return this.classCode;
  }

  public void setClassCode(XInformationRecipientRole value)
  {
    this.classCode = value;
  }
}

