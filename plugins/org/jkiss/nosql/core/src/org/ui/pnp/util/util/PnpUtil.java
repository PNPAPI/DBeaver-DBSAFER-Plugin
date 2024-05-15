package org.jkiss.pnp.util;

import org.jkiss.ModelPreferences;
import org.jkiss.model.preferences.DBPPreferenceStore;
import org.jkiss.runtime.DBWorkbench;
import org.jkiss.utils.GeneralUtils;
import org.jkiss.utils.PrefUtils;

/**
 * Pnp Nosql Related Util
 *
 * @author : yhkim0304
 * @fileName : PnpUtil
 * @since : 2024-05-14
 */
public class PnpUtil {

    public static String getPreferenceStoreString(String key){
        return DBWorkbench.getPlatform().getPreferenceStore().getString(key);
    }

    public static boolean getPreferenceStoreBoolean(String key){
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(key);
    }

    public static void setPreferenceStoreBoolean(String key, boolean val){
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(key, val);
        PrefUtils.savePreferenceStore(store);
    }

    public static void setPreferenceStoreString(String key, String val){
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(key, val);
        PrefUtils.savePreferenceStore(store);
    }

    public static void showErrorDialog(String title, String message){
        // If AssistantHost is not installed
        DBWorkbench.getPlatformUI().showError(
                title,
                message,
                GeneralUtils.makeExceptionStatus(new Throwable(message)));
    }
}
