package org.jkiss.ui.app.standalone.pnp.dto;


/**
 * 접속 사용자 로그인 정보
 *
 * @author : yhkim0304
 * @fileName : VersionDto
 * @since : 2024-04-30
 */
public class LoginInfoDto {
    private int assistState;
    private String loginId;
    private String securityServer;
    private String localHostName;
    private String localIpAddress;
    private String localMacAddress;
    private String assistKey;

    public int getAssistState() {
        return assistState;
    }

    public void setAssistState(int assistState) {
        this.assistState = assistState;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getSecurityServer() {
        return securityServer;
    }

    public void setSecurityServer(String securityServer) {
        this.securityServer = securityServer;
    }

    public String getLocalHostName() {
        return localHostName;
    }

    public void setLocalHostName(String localHostName) {
        this.localHostName = localHostName;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }

    public String getLocalMacAddress() {
        return localMacAddress;
    }

    public void setLocalMacAddress(String localMacAddress) {
        this.localMacAddress = localMacAddress;
    }

    public String getAssistKey() {
        return assistKey;
    }

    public void setAssistKey(String assistKey) {
        this.assistKey = assistKey;
    }
}
