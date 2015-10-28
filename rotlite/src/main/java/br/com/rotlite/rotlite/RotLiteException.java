package br.com.rotlite.rotlite;

/**
 * Created by claudio on 07/09/15.
 */
public class RotLiteException extends Exception {

    public String message = "";
    public int errorCode = 0;

    public RotLiteException(String message) {
        super(message);
    }

    public RotLiteException(String message, int code) {
        super(message);
        this.errorCode = code;
    }

    public int hashCode() {
        return this.errorCode;
    }

}
