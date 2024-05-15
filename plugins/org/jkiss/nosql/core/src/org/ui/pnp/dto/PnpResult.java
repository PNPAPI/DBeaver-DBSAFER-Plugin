package org.jkiss.pnp.dto;

/**
 * Pnp Result Api Information
 *
 * @author : yhkim0304
 * @fileName : PnpResult
 * @since : 2024-05-14
 */
public class PnpResult<E> {
    private String result;
    private E data;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public E getData() {
        return data;
    }

    public void setData(E data) {
        this.data = data;
    }
}
