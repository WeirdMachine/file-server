package me.osthues.fileserver;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileServerTest {

    private final FileServer fileServer;

    FileServerTest() throws UnknownHostException {
        this.fileServer = new FileServer("127.0.0.1", 8080, System.getProperty("user.dir") + "/src/test/resources");
    }

    @Test
    void httpHandlerNotImplementedMethod() {

        Response expected = new Response(HttpStatus.NOT_IMPLEMENTED);
        Response actual = fileServer.httpHandler("POST", "/");

        assertEquals(expected, actual);

    }
    @Test
    void httpHandler() throws IOException {

        Response expected = fileServer.buildFileResponse("/test.txt");
        Response actual = fileServer.httpHandler("GET", "/test.txt");

        assertEquals(expected, actual);

    }

    @Test
    void buildFileResponse() throws IOException {

        Response actual = fileServer.buildFileResponse("/test.txt");

        byte[] data = fileServer.getFile(fileServer.getRootDir()+ "/test.txt");

        List<String> headers = actual.buildHeaders();

        assertTrue(Arrays.equals(data, actual.getData()));

        assertTrue(headers.get(0).contains(HttpStatus.OK.toString()));


        String length = headers.stream().filter(h -> !h.contains("Content-length")).findFirst().get();

        assertEquals(Integer.parseInt(length.substring("Content-length: ".length())), data.length);
        assertTrue(headers.get(0).contains(HttpStatus.OK.toString()));

    }

    @Test
    void buildDirectoryResponse() throws IOException {

        Response response = fileServer.buildFileResponse("/");

        List<String> headers = response.buildHeaders();
        assertTrue(headers.get(0).contains(HttpStatus.OK.toString()));

        String actual = new String(response.getData());

        assertTrue(actual.contains("<a href=\"test.txt\">test.txt</a"));

    }

    @Test
    void getDirectory() throws IOException {

        Path filePath = Paths.get(fileServer.getRootDir() + "/");
        String actual = fileServer.getDirectory(filePath);

        assertEquals(actual, "<a href=\"test.txt\">test.txt</a>");

    }

    @Test
    void getFile() throws IOException {

        byte[] data = fileServer.getFile(fileServer.getRootDir()+ "/test.txt");
        byte[] expected = "This is a file for testing purpose".getBytes();

        assertTrue(Arrays.equals(data, expected));

    }
}