package fi.helsinki.lib.simplestatsreporter;

public class UnknownHandleException extends Exception {

    private String handle;

    public UnknownHandleException(String handle) {
	this.handle = handle;
    }

    public String toString() {
	return "Unknown handle: " + this.handle;
    }
}
