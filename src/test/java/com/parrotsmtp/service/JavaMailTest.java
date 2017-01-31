package com.parrotsmtp.service;

import com.parrotsmtp.util.ParrotUtils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author vgorin (Administrator)
 *         file created: 20.05.12 16:08
 */
public class JavaMailTest {
    public static void main(String[] args) throws MessagingException, IOException {
        final InputStream input = JavaMailTest.class.getResourceAsStream("/1337520730992.eml");
        final MimeMessage message = new MimeMessage(null, input);
        System.out.println(message.getSubject());
        System.out.println(message.getFrom()[0]);
        System.out.println(message.getRecipients(Message.RecipientType.TO)[0]);
        System.out.println(message.getSentDate());
        System.out.println(message.getSize());
        System.out.println();
        System.out.println(ParrotUtils.mimeMessageToHtml(message));
    }
}
