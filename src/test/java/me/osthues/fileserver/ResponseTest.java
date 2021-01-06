package me.osthues.fileserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    private final Response response = new Response();

    @Test
    void buildHeaders() {

        List<String> headers = response.buildHeaders();

        headers.forEach(h -> assertTrue(h.endsWith("\r\n")));

        assertEquals("\r\n", headers.get(headers.size()-1));

    }

    @Test
    void setStatus() {

        response.setStatus(HttpStatus.I_AM_A_TEAPOT);

        List<String> headers = response.buildHeaders();
        assertTrue(headers.get(0).contains(HttpStatus.I_AM_A_TEAPOT.toString()));

        String expectedData = HttpStatus.I_AM_A_TEAPOT.toString() + "\r\n";
        assertTrue(Arrays.equals(expectedData.getBytes(), response.getData()));


    }
}