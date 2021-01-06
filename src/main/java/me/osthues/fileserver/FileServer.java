package me.osthues.fileserver;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileServer {

    public static final String DEFAULT_ADDRESS = "0.0.0.0";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_ROOT_DIR = "/var/www/html";

    private final InetAddress address;
    private final int port;
    private final String rootDir;

    public FileServer(String address, int port, String rootDir) throws UnknownHostException {

        if (address == null || address.isEmpty()) {
            this.address = InetAddress.getByName(DEFAULT_ADDRESS);
        } else {
            this.address = InetAddress.getByName(address);
        }

        if (port < 1) {
            this.port = DEFAULT_PORT;
        } else {
            this.port = port;
        }

        if (rootDir == null || rootDir.isEmpty()) {
            this.rootDir = DEFAULT_ROOT_DIR;
        } else if (rootDir.endsWith("/")) {
                this.rootDir = rootDir.substring(rootDir.length()-1);
        } else {
            this.rootDir = rootDir;
        }

        System.out.println("Serving: " + this.address.getHostAddress() + " at port: " + this.port + ", root dir: " + this.rootDir);

    }

    public static void main(String[] args) throws IOException, ParseException {

        int port = 0;
        String address;
        String directory;

        Map<String, String> env = System.getenv();

        if (env.get("PORT") != null) {
            port = Integer.parseInt(env.get("PORT"));
        }

        address = env.get("ADDRESS");
        directory = env.get("DIRECTORY");

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        options.addOption("h", "help", false, "print help")
               . addOption("p", "port", true, "listen port - default: "+ DEFAULT_PORT)
                .addOption("a", "address", true, "listen address - default: " + DEFAULT_ADDRESS)
                .addOption("dir", "directory", true, "directory to server files - default: " + DEFAULT_ROOT_DIR);


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            formatter.printHelp("File Server", options);
            return;
        }

        if (cmd.hasOption("p")) {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        }
        if (cmd.hasOption("a")) {
            address = cmd.getOptionValue("a");
        }
        if (cmd.hasOption("dir")) {
            directory = cmd.getOptionValue("dir");
        }

        FileServer fileServer = new FileServer(address, port, directory);
        fileServer.Start();

    }

    public void Start() throws IOException {

        ServerSocket serverSocket = new ServerSocket(this.port, 0, this.address);

        while (true) {
            Socket socket = serverSocket.accept();

            Thread thread = new Thread(() -> this.handleRequest(socket));
            System.out.println("starting thread: " + thread.getName());
            thread.start();
        }

    }


    public Response httpHandler(String method, String path) {
        Response response = new Response();

        switch (method) {
            case "HEAD":
                response = buildHeadResponse(path);
                break;
            case "GET":
                try {
                    response = buildGetResponse(path);
                } catch (IOException e) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                break;

            default:
                response.setStatus(HttpStatus.NOT_IMPLEMENTED);
        }

        return response;

    }



    public void handleRequest(Socket socket) {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream());
             BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream())
        ) {

            // get first line of the request from the client
            String input = in.readLine();
            if (input == null) {
                System.out.println("Socket is empty");
                return;
            }

            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            String path = URLDecoder.decode(parse.nextToken(), StandardCharsets.UTF_8);


            //Prevent directory traversal outside rootDir
            String canonicalPath = new File(this.rootDir + path).getCanonicalPath();
            if (!canonicalPath.startsWith(rootDir)) {
                path = "/";
            }

            Response response;

            //check for supported http version
            if (!parse.nextToken().equals("HTTP/1.1")) {
                response = new Response(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
            }
            else {
                response = httpHandler(method, path);
            }

            //write headers
            response.buildHeaders().forEach(out::print);
            out.flush();

            //write data
            dataOut.write(response.getData(), 0, response.getData().length);
            dataOut.flush();


        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
        }

        System.out.println("Finished: " + Thread.currentThread());

    }

    public Response buildGetResponse(String path) throws IOException {

        Response response = new Response();

        Path filePath = Paths.get(this.rootDir + path);

        if (Files.exists(filePath)) {

            if (Files.isRegularFile(filePath)) {
                response.setMimeType(URLConnection.guessContentTypeFromName(path));
                response.setData(this.getFile(this.rootDir + path));
            }

            if (Files.isDirectory(filePath)) {

                String htmlResponse = "<html>\r\n" +
                        "<head>\r\n" +
                        "<title>\r\n" +
                        "Index of " + path + "\r\n" +
                        "</title>\r\n" +
                        "</head>\r\n" +
                        "<body bgcolor=\"white\">\r\n" +
                        "<h1>\r\n" +
                        "Index of " + path + "\r\n" +
                        "</h1>\r\n" +
                        "<hr>\r\n" +
                        "<pre>\r\n" +
                        "<a href=\"../\">../</a>\r\n" +
                        this.getDirectory(filePath) +
                        "</pre>\r\n" +
                        "<hr>\r\n" +
                        "</body>\r\n" +
                        "</html>\r\n";

                response.setData(htmlResponse.getBytes());
            }

            response.setStatus(HttpStatus.OK);

        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    public Response buildHeadResponse(String path) {

        Response response = new Response();

        File file = new File(this.rootDir + path);

        if (file.exists()) {
            response.setContentLength(file.length());
            response.setStatus(HttpStatus.OK);

        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }

        return response;

    }


    public String getDirectory(Path filePath) throws IOException {
        final String hrefTemplate = "<a href=\"%s\">%s</a>";
        String files;

        try (Stream<Path> paths = Files.list(filePath)) {
            files = paths
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .map(s -> s.substring(filePath.toString().length()+1))
                    .map(s -> String.format(hrefTemplate, s, s))
                    .collect(Collectors.joining("\r\n"));
        }

        return files;

    }

    public byte[] getFile(String path) throws IOException {
        File file = new File(path);
        int fileLength = (int) file.length();

        byte[] data = new byte[fileLength];
        try (FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(data);
        }

        return data;
    }


    public String getRootDir() {
        return this.rootDir;
    }
}

