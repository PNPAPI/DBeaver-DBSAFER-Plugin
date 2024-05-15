package org.jkiss.pnp.dto;

/**
 * DB Access Control
 *
 * @author : yhkim0304
 * @fileName : DbAccessDto
 * @since : 2024-05-14
 */
public class DbAccess {
    private String serverIp;
    private String serverPort;
    private String id;
    private String ldapSno;
    private String database;
    private String application;
    private String cltIp;

    public DbAccess(String serverIp, String serverPort, String id, String ldapSno, String database, String application, String cltIp) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.id = id;
        this.ldapSno = ldapSno;
        this.database = database;
        this.application = application;
        this.cltIp = cltIp;
    }
}
