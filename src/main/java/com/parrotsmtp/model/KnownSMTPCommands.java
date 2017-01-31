package com.parrotsmtp.model;

import java.util.regex.Pattern;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 02.04.12 17:04
 */
public enum KnownSMTPCommands {
    HELO("HELO", "(.+)"),
    EHLO("EHLO", "(.+)"),
    MAIL("MAIL", "from\\s*:?\\s*<?([@a-z0-9._%+-]+)>?\\s*"),
    RCPT("RCPT", "to\\s*:?\\s*<?([@a-z0-9._%+-]+)>?\\s*"),
    DATA("DATA", ""),
    HELP("HELP", ""),
    QUIT("QUIT", "");

    private String commandName;
    private Pattern extractPattern;

    private KnownSMTPCommands(String commandName, String regexp) {
        this.commandName = commandName;
        extractPattern = Pattern.compile(regexp);
    }

    @Override
    public String toString() {
        return commandName;
    }

    public Pattern getExtractPattern() {
        return extractPattern;
    }
}
