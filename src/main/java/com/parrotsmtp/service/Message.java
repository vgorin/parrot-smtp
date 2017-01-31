package com.parrotsmtp.service;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.parrotsmtp.model.KnownSMTPCommands;
import com.parrotsmtp.model.SMTPRequest;
import com.parrotsmtp.util.ParrotUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static javax.mail.Message.RecipientType;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 11.05.12 16:15
 */
public class Message {
    public static enum ParseError {
        HOST_EMPTY("'host' field is not filled"),
        FROM_EMPTY("'from' field is not filled"),
        TO_EMPTY("'to' field is not filled"),
        MSG_FINISHED("message built, no more lines accepted");

        private String errorMessage;

        private ParseError(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return errorMessage;
        }
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SocketAddress remoteAddress;

    private long built;
    private String host;
    private String from;
    private String to;
    private boolean dataOpened = false;
    private List<String> data = new ArrayList<>();

    private boolean dataClosed = false;

    private ParseError parseError;

    public Message(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Message(SocketAddress remoteAddress, String host) {
        this.remoteAddress = remoteAddress;
        this.host = host;
    }

    public boolean parseRequest(SMTPRequest request) {
        log.trace("parsing SMTP command {}", request.getCommand());
        if(request.isCommand(KnownSMTPCommands.HELO, KnownSMTPCommands.EHLO)) {
            host = request.extractFirstArgument();
            log.trace("'host' set to {}", host);
        }
        else if(request.isCommand(KnownSMTPCommands.MAIL)) {
            from = request.extractFirstArgument();
            log.trace("'from' set to {}", from);
        }
        else if(request.isCommand(KnownSMTPCommands.RCPT)) {
            to = request.extractFirstArgument();
            log.trace("'to' set to {}", to);
        }
        else if(request.isCommand(KnownSMTPCommands.DATA)) {
            if(host == null || from == null || to == null) {
                if(host == null) {
                    parseError = ParseError.HOST_EMPTY;
                }
                else if(from == null) {
                    parseError = ParseError.FROM_EMPTY;
                }
                else {
                    parseError = ParseError.TO_EMPTY;
                }
                log.debug("message is malformed: {}", parseError);
                return false;
            }
            else {
                dataOpened = true;
                log.trace("waiting for data");
            }
        }
        else {
            log.trace("not a message building command, ignoring");
        }
        return true;
    }

    public boolean parseLine(String line) {
        log.trace("received line: {}", line);
        if(dataClosed) {
            parseError = ParseError.MSG_FINISHED;
            log.debug(ParseError.MSG_FINISHED.toString());
            return false;
        }
        else {
            if(dataOpened) {
                if(".".equals(line)) {
                    log.trace("end of message received");
                    dataClosed = true;
                    dataOpened = false;
                }
                else {
                    log.trace("feeding data");
                    data.add(line);
                    built = System.currentTimeMillis();
                }
            }
            else {
                return parseRequest(SMTPRequest.parse(line));
            }
        }
        return true;
    }

    public long getBuilt() {
        return built;
    }

    public String getHost() {
        return host;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public ParseError getParseError() {
        return parseError;
    }

    public String getErrorMessage() {
        return parseError.toString();
    }

    public boolean isDataOpened() {
        return dataOpened;
    }

    public boolean isDataClosed() {
        return dataClosed;
    }

    public boolean isValid() {
        return parseError == null && from != null && to != null && host != null;
    }

    @Override
    public String toString() {
        if(parseError != null) {
            return parseError.toString();
        }
        else {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("CONNECTED FROM: %s%n%nRECEIVED FROM: %s%nFROM: %s%nTO: %s%nMESSAGE:%n", remoteAddress, host, from, to));
            for(String line: data) {
                sb.append(String.format("%s%n", line));
            }
            return sb.toString();
        }
    }

    public String toEmlString() {
        final StringBuilder sb = new StringBuilder();
        for(String line: data) {
            sb.append(String.format("%s%n", line));
        }
        return sb.toString();
    }

    public String toShtmlString() {
        try (
                final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
                final PrintWriter writer = new PrintWriter(outputBuffer)
        ) {
            writer.println(toEmlString());
            writer.flush();
            try (
                    final ByteArrayInputStream inputBuffer = new ByteArrayInputStream(outputBuffer.toByteArray())
            ) {
                final MimeMessage message = new MimeMessage(null, inputBuffer);
                final String pattern = IOUtils.toString(getClass().getResourceAsStream("/message_template.html"));
                return MessageFormat.format(pattern,
                        message.getSubject(),
                        ParrotUtils.addressesToString(message.getFrom()),
                        ParrotUtils.addressesToString(message.getRecipients(RecipientType.TO)),
                        message.getSize(),
                        message.getSentDate(),
                        ParrotUtils.mimeMessageToHtml(message)
                );
            }
            catch(MessagingException e) {
                log.debug("messaging exception caught while printing message as HTML", e);
                return toEmlString();
            }
            catch(IOException e) {
                log.debug("i/o exception caught while writing message to buffer", e);
            }
        }
        catch(IOException io) {
            log.warn("unexpected exception caught while creating message from template", io);
        }
        log.trace("method will return message as plain text, #toEmlString()");
        return toEmlString();
    }
}
