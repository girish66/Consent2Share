
package echosign.api.clientv15;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import echosign.api.clientv15.dto14.GetDocumentsForUserResult;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="getMyDocumentsResult" type="{http://dto14.api.echosign}GetDocumentsForUserResult"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getMyDocumentsResult"
})
@XmlRootElement(name = "getMyDocumentsResponse")
public class GetMyDocumentsResponse {

    @XmlElement(required = true, nillable = true)
    protected GetDocumentsForUserResult getMyDocumentsResult;

    /**
     * Gets the value of the getMyDocumentsResult property.
     * 
     * @return
     *     possible object is
     *     {@link GetDocumentsForUserResult }
     *     
     */
    public GetDocumentsForUserResult getGetMyDocumentsResult() {
        return getMyDocumentsResult;
    }

    /**
     * Sets the value of the getMyDocumentsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link GetDocumentsForUserResult }
     *     
     */
    public void setGetMyDocumentsResult(GetDocumentsForUserResult value) {
        this.getMyDocumentsResult = value;
    }

}
