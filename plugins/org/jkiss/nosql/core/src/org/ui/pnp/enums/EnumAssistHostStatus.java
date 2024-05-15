package org.jkiss.pnp.enums;

/**
 * AssistHost Status Enum
 *
 * @author : yhkim0304
 * @fileName : AssistHostStatus
 * @since : 2024-05-14
 */
public enum EnumAssistHostStatus {
    NORMAL(0), // normal ( Run + login)
    NOT_EXECUTE(1), // Not running ASIST
    NOT_LOGIN(2), // Failed to login
    CHECK_ERR(9); // Status check error

    private int status;

    EnumAssistHostStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static AssistHostStatus getAssisHostStatus(int status) {
        for (AssistHostStatus enumAssistHostStatus : AssistHostStatus.values()) {
            if (enumAssistHostStatus.getStatus() == status) {
                return enumAssistHostStatus;
            }
        }
        return CHECK_ERR;
    }
}
