package com.parrotsmtp.model;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 31.03.12 15:15
 */
public class SMTPRequest {
    private final String command; // TODO: should we make this a enum?
    private final String argumentString;

    public SMTPRequest(String command, String argumentString) {
        this.command = command;
        this.argumentString = argumentString;
    }

    public static SMTPRequest parse(String commandLine) {
        final String command;
        final String arguments;
        commandLine = StringUtils.trimToEmpty(commandLine);

        final int whitespace = commandLine.indexOf(' ');
        if(whitespace == -1) {
            command = commandLine;
            arguments = null;
        }
        else {
            command = commandLine.substring(0, whitespace);
            arguments = StringUtils.trimToEmpty(commandLine.substring(whitespace + 1));
        }
        return new SMTPRequest(command, arguments);
    }

    @Override
    public String toString() {
        return command + (argumentString != null? ' ' + argumentString : "");
    }

    public boolean isCommand(String command) {
        return this.command.equalsIgnoreCase(command);
    }

    public boolean isCommand(KnownSMTPCommands command) {
        return this.command.equalsIgnoreCase(command.toString());
    }
    public boolean isCommand(String... commands) {
        for(String command: commands) {
            if(isCommand(command)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCommand(KnownSMTPCommands... commands) {
        for(KnownSMTPCommands command: commands) {
            if(isCommand(command)) {
                return true;
            }
        }
        return false;
    }

    private KnownSMTPCommands getKnownSMTPCommand() {
        for(KnownSMTPCommands command: KnownSMTPCommands.values()) {
            if(isCommand(command)) {
                return command;
            }
        }
        return null;
    }

    public String getCommand() {
        return command;
    }

    public String getArgumentString() {
        return argumentString;
    }

    public String[] extractArguments() {
        if(argumentString != null) {
            final KnownSMTPCommands command = getKnownSMTPCommand();
            if(command != null) {
                final Matcher m = command.getExtractPattern().matcher(argumentString.toLowerCase());
                if(m.matches()) {
                    final String[] result = new String[m.groupCount()];
                    for(int i = 0; i < result.length; i++) {
                        result[i] = m.group(i + 1);
                    }
                    return result;
                }
            }
            return new String[] {argumentString};
        }
        else {
            return new String[]{};
        }
    }

    public String extractFirstArgument() {
        if(argumentString != null) {
            return extractArguments()[0];
        }
        else {
            return null;
        }
    }
}
