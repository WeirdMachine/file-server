package me.osthues.fileserver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Response
{

    private HttpStatus status;
    private byte[] data = new byte[0];
    private String mimeType = "text/html";

    public Response(HttpStatus status) {
        this.status = status;
    }

    public Response() {
    }

    public void setStatus(HttpStatus status) {
        this.status = status;

        if (status != HttpStatus.OK) {
            this.data = (status.toString() + "\r\n").getBytes();
        }
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public List<String> buildHeaders() {
        List<String> headers = new ArrayList<>();

        headers.add("HTTP/1.1 " + status + "\r\n");
        headers.add("Date " + new Date() + "\r\n");
        headers.add("Content-length: " + data.length + "\r\n");
        headers.add("Connection: close" + "\r\n");
        headers.add("Content-type: " + mimeType + "\r\n");
        headers.add("\r\n");

        return headers;
    }

}
