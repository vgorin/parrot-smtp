package com.parrotsmtp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.parrotsmtp.model.DefaultSMTPResponses;
import com.parrotsmtp.model.KnownSMTPCommands;
import com.parrotsmtp.model.SMTPRequest;
import com.parrotsmtp.model.SMTPResponse;
import com.parrotsmtp.util.LimitedBufferedInputStream;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 11.05.12 10:08
 */
public class SMTPService implements Runnable {
    private static final ResourceBundle APP = ResourceBundle.getBundle("app");

    private static final int SO_TIMEOUT_MS = Integer.parseInt(APP.getString("so_timeout_ms"));
    private static final int MAX_MSG_LENGTH = Integer.parseInt(APP.getString("max_msg_length"));
    private static final int POOL_AWAIT_MS = Integer.parseInt(APP.getString("smtp_service_executor_service_await_termination_ms"));
    private static final int POOL_DEFAULT_SIZE = Integer.parseInt(APP.getString("smtp_service_executor_service_default_pool_size"));
    private static final int SMTP_DEFAULT_PORT = Integer.parseInt(APP.getString("smtp_service_default_port"));

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ExecutorService pool;
    private final MessagePool messagePool = new MessagePool();

    private AtomicInteger port = new AtomicInteger();
    private AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;

    public SMTPService() {
        this(SMTP_DEFAULT_PORT);
    }

    public SMTPService(int port) {
        this(port, POOL_DEFAULT_SIZE);
    }

    public SMTPService(int port, int poolSize) {
        setPort(port);
        pool = Executors.newFixedThreadPool(poolSize);
    }

    public int getPort() {
        return port.get();
    }

    public void setPort(int port) {
        if(port > 0 && port < 1 << 16) {
            log.trace("SMTP service is setting port to {}", port);
            if(this.port.get() != port) {
                stop();
                this.port.set(port);
            }
        }
        else {
            log.trace("specified port is out of range 1 - 65535: {}, setting ignored", port);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public boolean start()  {
        if(isRunning()) {
            log.trace("SMTP service is already running");
            return true;
        }
        else {
            log.debug("SMTP service is starting");
            if(bindServerSocket()) {
                setRunning(true);
                log.trace("Server socket bound, executing the pool");
                pool.execute(this);
                return true;
            }
            else {
                log.info("SMTP service couldn't start: unable to bind server socket");
                return false;
            }
        }
    }

    public void stop() {
        if(isRunning()) {
            log.info("SMTP service is stopping");
            setRunning(false); // Disable new tasks from being submitted
            log.trace("shutting down the pool");
            pool.shutdown();
            try{
                // Wait a while for existing tasks to terminate
                log.trace("waiting for pool to terminate");
                if(!pool.awaitTermination(POOL_AWAIT_MS, TimeUnit.MILLISECONDS)) {
                    log.trace("forcibly terminating the pool");
                    pool.shutdownNow(); // Cancel currently executing tasks

                    // Wait a while for tasks to respond to being cancelled
                    log.trace("waiting for pool to terminate");
                    if(!pool.awaitTermination(POOL_AWAIT_MS, TimeUnit.MILLISECONDS)) {
                        log.trace("pool did not terminate");
                    }
                    else {
                        log.trace("pool forcibly terminated");
                    }
                }
                else {
                    log.trace("pool terminated successfully");
                }
            }
            catch (InterruptedException interrupted) {
                // (Re-)Cancel if current thread also interrupted
                log.trace("InterruptedException caught: {}; terminating the pool", interrupted.getMessage());
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            closeServerSocket();
            log.info("SMTP service stopped");
        }
        else {
            log.trace("SMTP service is already stopped");
        }
    }

    private boolean bindServerSocket() {
        log.debug("trying to bind on port {}", getPort());
        try {
            serverSocket = new ServerSocket(getPort()); // critical: bind exception
            log.debug("bound successfully on port {}", getPort());
            return true;
        }
        catch(IOException onBind) {
            log.warn("could not bind on port {}: {}", getPort(), onBind.getMessage());
            return false;
        }
    }

    private boolean closeServerSocket() {
        if(serverSocket != null && !serverSocket.isClosed()) {
            log.trace("closing server socket");
            try {
                serverSocket.close();
                log.trace("server socket closed");
                return true;
            }
            catch(IOException onClose) {
                log.debug("could not close server socket");
                return false;
            }
        }
        else {
            log.trace("server socket is already closed");
            return true;
        }
    }

    private boolean closeSocket(Socket socket) {
        if(socket != null && !socket.isClosed()) {
            log.trace("closing socket");
            try {
                socket.close();
                log.trace("socket closed");
                return true;
            }
            catch(IOException onClose) {
                log.debug("could not close socket");
                return false;
            }
        }
        else {
            log.trace("socket is already closed");
            return true;
        }
    }

    @Override
    public void run() {
        log.info("SMTP service started");
        int counter = 0;
        while(isRunning()) {
            log.trace("accepting connection {}", counter);
            try {
                // blocking method
                final Socket socket = serverSocket.accept(); // dangerous: can be retried several times
                if(isRunning()) {
                    log.trace("incoming connection accepted, handling it");
                    pool.execute(new Handler(socket));
                    counter++;
                }
                else {
                    log.trace("incoming connection accepted, but service was scheduled for shutdown, connection won't be served");
                    closeSocket(socket);
                }
            }
            catch (IOException onAccept) {
                log.warn("IOException caught while trying to accept connection: {}", onAccept.getMessage());
            }
        }
        log.trace("service was scheduled for shutdown, connections are no longer accepted; total number of connections accepted: {}", counter);
        closeServerSocket();
    }

    public MessagePool getMessagePool() {
        return messagePool;
    }

    class Handler implements Runnable {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final Socket socket;

        Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                log.trace("setting socket SO_TIMEOUT to {}ms", SO_TIMEOUT_MS);
                socket.setSoTimeout(SO_TIMEOUT_MS);
                log.trace("trying to open i/o streams for reading and writing");
                try(
                        final InputStream inputStream = socket.getInputStream(); // normal: bad socket, should be closed
                        final OutputStream outputStream = socket.getOutputStream(); // normal: bad socket, should be closed
                        final LimitedBufferedInputStream limitedInput = new LimitedBufferedInputStream(inputStream) {{
                            setLimit(MAX_MSG_LENGTH);
                        }};
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(limitedInput));
                        final PrintWriter printer = new PrintWriter(outputStream, true)
                ) {
                    Message message;    // message is reusable, not final
                    String inputLine;

                    log.debug("i/o streams opened successfully, communicating");
                    printLine(printer, DefaultSMTPResponses.OK_CONNECTED);

                    message = createNewMessage(socket, null);

                    try {
                        while((inputLine = reader.readLine()) != null) {
                            log.trace("received message from client: {}", inputLine);
                            if(!message.parseLine(inputLine)) {
                                final Message.ParseError parseError = message.getParseError();
                                if(parseError == Message.ParseError.HOST_EMPTY) {
                                    printLine(printer, new SMTPResponse(503, "Send hello first"));
                                }
                                else if(parseError == Message.ParseError.FROM_EMPTY) {
                                    printLine(printer, new SMTPResponse(503, "Send MAIL command first"));
                                }
                                else if(parseError == Message.ParseError.TO_EMPTY) {
                                    printLine(printer, new SMTPResponse(503, "Send RCPT command first"));
                                }
                                else {
                                    printLine(printer, DefaultSMTPResponses.INTERNAL_ERROR);
                                    log.debug("an error occurred while building a message: {}; session will be closed", message.getErrorMessage());
                                    break;
                                }
                            }
                            else {
                                final SMTPRequest request = SMTPRequest.parse(inputLine);
                                if(request.isCommand(KnownSMTPCommands.DATA)) {
                                    printLine(printer, DefaultSMTPResponses.OK_DATA);
                                }
                                else if(request.isCommand(KnownSMTPCommands.HELP)) {
                                    printLine(printer, DefaultSMTPResponses.OK_HELP);
                                }
                                else if(request.isCommand(KnownSMTPCommands.QUIT)) {
                                    log.trace("client asked to close session, it will be closed");
                                    break;
                                }
                                // Do not send 250 OK while receiving data
                                else if(!message.isDataOpened()) {
                                    printLine(printer, DefaultSMTPResponses.OK);
                                }
                            }
                            if(message.isDataClosed()) {
                                // do not close connection on data close, but start new message
                                log.trace("message finished, saving message");
                                saveMessage(message);
                                message = createNewMessage(socket, message.getHost());
                            }
                        }
                    }
                    catch(SocketTimeoutException timeout) {
                        log.info("connection timed out (possibly service abuse) - {}ms", SO_TIMEOUT_MS);
                        printLine(printer, new SMTPResponse(451, "Timeout waiting for client input. Max idle time is " + SO_TIMEOUT_MS + "ms"));
                    }
                    saveMessage(message); // save last message
                    if(limitedInput.limitOverflow()) {
                        log.info("too long message received (possibly service abuse), the message was truncated");
                        printLine(printer, new SMTPResponse(552, "Requested action aborted: too long message - max message length is " + MAX_MSG_LENGTH));
                    }
                    printLine(printer, DefaultSMTPResponses.OK_QUIT);
                    log.trace("communication finished successfully");
                }
                catch(IOException onReadWrite) {
                    log.debug("IOException caught while working with socket: {}", onReadWrite.getMessage());
                }
            }
            catch(SocketException so) {
                log.error("setting socket SO_TIMEOUT failed! communication aborted");
            }

            log.trace("handler is closing client socket");
            closeSocket(socket);
        }

        private void saveMessage(Message message) {
            if(message.isValid()) {
                log.debug(String.format("message is valid%s, saving\n{}", message.isDataClosed()?"": " (but truncated)"), message);
                messagePool.storeMessage(message);
            }
            else {
                log.debug("message is not valid, ignoring\n{}", message);
            }
        }

        private Message createNewMessage(Socket socket, String host) {
            return new Message(socket.getRemoteSocketAddress(), host);
        }

        private void printLine(PrintWriter printer, Object obj) {
            log.trace("sending message back to client: {}", String.valueOf(obj));
            printer.println(obj);
        }

    }

    public static void main(String[] args) {
        new SMTPService().start();
    }

}
