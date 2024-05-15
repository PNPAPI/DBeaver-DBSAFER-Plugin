package org.jkiss.Nosql.pnp.dto;

/**
 * AssistHost Status
 *
 * @author : yhkim0304
 * @fileName : AssistStatusDto
 * @since : 2024-05-14
 */
public class AssistStatus {
    private int status;
    private String userId;
    private String message;
    private String loginId;
    private String localIpAddress;
    private String assistKey;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }

    public String getAssistKey() {
        return assistKey;
    }

    public void setAssistKey(String assistKey) {
        this.assistKey = assistKey;
    }
}
