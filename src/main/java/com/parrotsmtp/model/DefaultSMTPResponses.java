package com.parrotsmtp.model;

/**
 * @author vgorin (Administrator)
 *         file created: 31.03.12 15:30
 */
public enum DefaultSMTPResponses {
    OK_HELP(new SMTPResponse(211, "ParrotSMTP help is not implemented yet. Sorry for inconvenience")), // TODO: implement
    OK_CONNECTED(new SMTPResponse(220, "ParrotSMTP at your service")),
    OK_QUIT(new SMTPResponse(221, "Service closing transmission channel")),
    OK(new SMTPResponse(250, "OK")),
    OK_DATA(new SMTPResponse(354, "Start mail input; end with <CRLF>.<CRLF>")),

    INTERNAL_ERROR(new SMTPResponse(451, "Requested action aborted: local error in processing"));

    private final SMTPResponse response;

    private DefaultSMTPResponses(SMTPResponse response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return response.toString();
    }

    public SMTPResponse toResponse() {
        return response;
    }
}
