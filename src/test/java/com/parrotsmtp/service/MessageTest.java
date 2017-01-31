package com.parrotsmtp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

/**
 * @author vgorin (Administrator)
 *         file created: 27.05.12 19:58
 */
public class MessageTest {
    public static void main(String[] args) {
        final Message message = new Message(new InetSocketAddress(555));
        message.parseLine("HELO localhost");
        message.parseLine("MAIL from Sender");
        message.parseLine("RCPT to Receiver");
        message.parseLine("DATA");

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(MessageTest.class.getResourceAsStream("/1337520730992.eml")))) {
            while(reader.ready()) {
                message.parseLine(reader.readLine());
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        message.parseLine(".");
        System.out.println(message.toShtmlString());
    }
}
