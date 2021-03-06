package org.hl7.v3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="HXIT_PQ", propOrder={"validTime"})
public class HXITPQ extends PQ
{
  protected IVLTS validTime;

  public IVLTS getValidTime()
  {
    return this.validTime;
  }

  public void setValidTime(IVLTS value)
  {
    this.validTime = value;
  }
}

