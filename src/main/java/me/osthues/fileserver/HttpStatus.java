package me.osthues.fileserver;

public enum HttpStatus {

    OK("200 OK"),
    INTERNAL_SERVER_ERROR("500 Internal Server Error"),
    NOT_IMPLEMENTED("501 Not Implemented"),
    NOT_FOUND("404 Not Found"),
    HTTP_VERSION_NOT_SUPPORTED("505 HTTP Version Not Supported"),
    I_AM_A_TEAPOT("418 I'm a teapot");

    private final String status;

    HttpStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.status;
    }
}
