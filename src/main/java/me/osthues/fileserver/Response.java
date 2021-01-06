package me.osthues.fileserver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Response
{

    private static final String HTTP_LINE_SEPARATOR = "\r\n";

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
            this.data = (status.toString() + HTTP_LINE_SEPARATOR).getBytes();
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

        headers.add("HTTP/1.1 " + status);
        headers.add("Date " + new Date());
        headers.add("Content-length: " + data.length);
        headers.add("Connection: close");
        headers.add("Content-type: " + mimeType);

        headers = headers.stream().map(h -> h + HTTP_LINE_SEPARATOR).collect(Collectors.toList());
        headers.add(HTTP_LINE_SEPARATOR);

        return headers;
    }

}
