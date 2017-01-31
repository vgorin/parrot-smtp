package com.parrotsmtp.model;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 31.03.12 15:15
 */
public class SMTPResponse {
    private static final Pattern VALID_CODE = Pattern.compile("\\d{3}");
    private static final int INTERNAL_ERROR_CODE = 451; // Requested action aborted: local error in processing

    private final int code; // values from 200 to 554
    private final String message;

    public SMTPResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static SMTPResponse parse(String messageLine) {
        final int code;
        final String message;
        messageLine = StringUtils.trimToEmpty(messageLine);

        final int whitespace = messageLine.indexOf(' ');
        if(whitespace == -1) {
            code = parseStatusCode(messageLine);
            message = parseMessage(code, messageLine);
        }
        else {
            code = parseStatusCode(messageLine.substring(0, whitespace));
            message = parseMessage(code, messageLine.substring(whitespace + 1));
        }

        return new SMTPResponse(code, message);
    }

    private static int parseStatusCode(String codeString) {
        if(VALID_CODE.matcher(codeString).matches()) {
            return Integer.parseInt(codeString);
        }
        else {
            return INTERNAL_ERROR_CODE;
        }
    }

    private static String parseMessage(int code, String message) {
        if(code == INTERNAL_ERROR_CODE) {
            return "Requested action aborted: local error in processing";
        }
        else {
            return StringUtils.trimToEmpty(message);
        }
    }

    @Override
    public String toString() {
        return code + (message != null? ' ' + message: "");
    }

    public String toCommandString() {
        return String.format("%s%n", toString());
    }

    public static SMTPResponse create503Response(KnownSMTPCommands command) {
        return new SMTPResponse(503, command + " command expected");
    }

}
