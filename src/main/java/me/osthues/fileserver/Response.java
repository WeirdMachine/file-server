package me.osthues.fileserver;

import java.util.*;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response response = (Response) o;
        return status == response.status && Arrays.equals(data, response.data) && Objects.equals(mimeType, response.mimeType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(status, mimeType);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
