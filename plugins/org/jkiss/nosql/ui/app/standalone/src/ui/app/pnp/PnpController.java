package org.jkiss.ui.app.standalone.pnp;

import org.jkiss.ui.app.standalone.pnp.dto.LoginInfoDto;
import org.jkiss.utils.rest.RequestMapping;

/**
 * Pnp Nosql AssistHost API Interpace
 *
 * @author : yhkim0304
 * @fileName : PnpController
 * @since : 2024-05-07
 */
public interface PnpController {
    @RequestMapping("svc/loginInfo")
    LoginInfoDto getLoginInfo();
}
