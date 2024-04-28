package Project.Common;

import java.io.Serializable;

  //  New code ends here with the implementions has some of the existing code prof gave us 
    //  owe
    // 4/28/2024

public class Payload implements Serializable {

    private long clientId;          // Unique identifier of the client

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    // Read https://www.baeldung.com/java-serial-version-uid
    private static final long serialVersionUID = 1L;    // Serialization version UID

  
    private PayloadType payloadType;

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

  
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Constructor for Payload.
     * @param payloadType The type of payload
     * @param clientId The unique identifier of the client
     * @param message The message content
     */
    public Payload(PayloadType payloadType, long clientId, String message) {
        this.payloadType = payloadType;
        this.clientId = clientId;
        this.message = message;
    }

   
    public Payload() {
    }

    @Override
    public String toString() {
        return String.format("Type[%s], Message[%s], ClientId[%s]", getPayloadType().toString(),
                getMessage(), getClientId());
    }
}
