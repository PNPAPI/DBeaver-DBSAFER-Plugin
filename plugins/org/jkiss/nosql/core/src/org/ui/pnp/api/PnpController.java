package org.jkiss.pnp.api;

import org.jkiss.pnp.dto.*;
import org.jkiss.utils.rest.RequestMapping;

/**
 * Pnp Nosql AssistHost API Interpace
 *
 * @author : yhkim0304
 * @fileName : PnpController
 * @since : 2024-05-07
 */
public interface PnpController {
    @RequestMapping("extapi/getassiststatus")
    PnpResult<AssistStatusDto> getAssistStatus();

    @RequestMapping("svc/loginInfo")
    LoginInfoDto getLoginInfo();

    @RequestMapping("Nosql/connectControl")
    PnpResult<DbAccessResult> checkDbConnect(DbAccessDto dbAccessDto);
}
