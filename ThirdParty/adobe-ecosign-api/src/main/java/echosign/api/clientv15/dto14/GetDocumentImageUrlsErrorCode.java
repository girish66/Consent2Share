
package echosign.api.clientv15.dto14;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GetDocumentImageUrlsErrorCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="GetDocumentImageUrlsErrorCode">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="OK"/>
 *     &lt;enumeration value="INVALID_API_KEY"/>
 *     &lt;enumeration value="INVALID_DOCUMENT_KEY"/>
 *     &lt;enumeration value="INVALID_VERSION_KEY"/>
 *     &lt;enumeration value="INVALID_PARTICIPANT"/>
 *     &lt;enumeration value="DOCUMENT_NOT_AVAILABLE"/>
 *     &lt;enumeration value="DOCUMENT_NOT_EXPOSED"/>
 *     &lt;enumeration value="DOCUMENT_REMOVED"/>
 *     &lt;enumeration value="DOCUMENT_DELETED"/>
 *     &lt;enumeration value="IMAGES_NOT_AVAILABLE"/>
 *     &lt;enumeration value="INTERNAL_ERROR"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "GetDocumentImageUrlsErrorCode")
@XmlEnum
public enum GetDocumentImageUrlsErrorCode {

    OK,
    INVALID_API_KEY,
    INVALID_DOCUMENT_KEY,
    INVALID_VERSION_KEY,
    INVALID_PARTICIPANT,
    DOCUMENT_NOT_AVAILABLE,
    DOCUMENT_NOT_EXPOSED,
    DOCUMENT_REMOVED,
    DOCUMENT_DELETED,
    IMAGES_NOT_AVAILABLE,
    INTERNAL_ERROR;

    public String value() {
        return name();
    }

    public static GetDocumentImageUrlsErrorCode fromValue(String v) {
        return valueOf(v);
    }

}
