package me.osthues.fileserver;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
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
            this.address =  InetAddress.getByName(DEFAULT_ADDRESS);
        } else {
            this.address =  InetAddress.getByName(address);
        }

        if (port < 1) {
            this.port = DEFAULT_PORT;
        } else {
            this.port = port;
        }

        if (rootDir == null || rootDir.isEmpty()) {
            this.rootDir = DEFAULT_ROOT_DIR;
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

//        HelpFormatter formatter = new HelpFormatter();
//
//        Options options = new Options();
//        options.addOption("h", "help", false, "print help")
//               . addOption("p", "port", true, "listen port - default: 8080")
//                .addOption("a", "address", true, "listen address - default: any")
//                .addOption("dir", "directory", true, "directory to server files, required");
//
//
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse( options, args);
//
//        if (options.getOption("h")) {
//            formatter.printHelp("File Server", options);
//            return;
//        }
//
//        if (options.getOption("p").getValue() != null) {
//            port = Integer.parseInt(options.getOption("p").getValue());
//        }
//        if (options.getOption("a").getValue() != null) {
//            address = options.getOption("a").getValue();
//        }
//        if (options.getOption("dir").getValue() != null) {
//            directory = options.getOption("dir").getValue();
//        }

        FileServer fileServer = new FileServer(address, port, directory);
        fileServer.Start();

    }

    public void Start() throws IOException {

        ServerSocket serverSocket = new ServerSocket(this.port, 0, this.address);

        while (true) {
            Socket socket = serverSocket.accept();
            // create dedicated thread to manage the client connection

            Thread thread = new Thread(() -> this.handle(socket));
            System.out.println("starting thread: " + thread.getName());
            thread.start();
        }

    }

    private void handle(Socket connect) {

        if (!connect.isConnected()) {
            System.out.println("socket not connected");
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
                PrintWriter out = new PrintWriter(connect.getOutputStream());
             BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream());
        ){

            // get first line of the request from the client
            String input = in.readLine();

            if (input == null) {
                System.out.println("Socket is empty");
                return;
            } else {
                System.out.println("Receiving data");
            }

            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            String fileRequested = URLDecoder.decode(parse.nextToken(), StandardCharsets.UTF_8);

            // we support only GET and HEAD methods, we check
            if (!method.equals("GET") && !method.equals("HEAD")) {

                // we return the not supported file to the client
                String contentMimeType = "text/html";
                //read content to return to client
                byte[] data = "501 Not Implemented".getBytes();

                // we send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + data.length);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer
                // file
                dataOut.write(data, 0, data.length);
                dataOut.flush();

            }


            if (method.equals("GET")) {

                byte[] data;

                // GET or HEAD method
                if (fileRequested.endsWith("/")) {
                    String files;

                    try (Stream<Path> paths = Files.list(Paths.get(rootDir + fileRequested))) {
                        files = paths
                                .map(Path::toAbsolutePath)
                                .map(Path::toString)
                                .map(s -> s.substring(rootDir.length()))
                                .collect(Collectors.joining("\n"));
                    }

                    data = files.getBytes();

                } else {

                    File file = new File(rootDir, fileRequested);
                    int fileLength = (int) file.length();

                    data = new byte[fileLength];
                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        fileIn.read(data);
                    }
                }

                // send HTTP Headers
                out.println("HTTP/1.1 200 OK");
                out.println("Date: " + new Date());
                out.println("Content-type: text/html");
                out.println("Content-length: " + data.length);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer

                dataOut.write(data, 0, data.length);
                dataOut.flush();
            }


        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                connect.close();
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
        }

    }

}

