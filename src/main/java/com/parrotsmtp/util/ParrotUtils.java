package com.parrotsmtp.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import java.io.IOException;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 20.05.12 17:40
 */
public class ParrotUtils {
    private static final Logger log = LoggerFactory.getLogger(ParrotUtils.class);

    public static String addressesToString(Address[] addresses) throws MessagingException {
        log.trace("building addresses line");
        if(addresses != null && addresses.length > 0) {
            final StringBuilder sb = new StringBuilder();
            for(int i = 0; i < addresses.length; i++) {
                sb.append(StringEscapeUtils.escapeHtml(String.valueOf(addresses[i])));
                if(i != addresses.length - 1) {
                    sb.append(", ");
                }
            }
            log.trace("{} addresses concatenated", addresses.length);
            return sb.toString();
        }
        else {
            log.trace("addresses array is null or has zero length");
            return "";
        }
    }

    // TODO: this method requires serious refactoring
    public static String mimeMessageToHtml(Part message) throws MessagingException, IOException {
        final StringBuilder sb = new StringBuilder();
        final Object content = message.getContent();
        final String contentType = message.getContentType();
        log.trace("building {} message/part", contentType);
        if(content instanceof Multipart) {
            final Multipart multipart = (Multipart) content;
            final int count = multipart.getCount();
            log.trace("message/part contains {} parts", count);
            for(int i = 0; i < count; i++) {
                log.trace("processing part {}", i);
                sb.append(String.format("%s%n", mimeMessageToHtml(multipart.getBodyPart(i))));
            }
        }
        else {
            if(message.isMimeType("text/html")) {
                final String template = IOUtils.toString(ParrotUtils.class.getResourceAsStream("/text_html_template.html"));
                sb.append(MessageFormatter.arrayFormat(template, new Object[]{
                        contentType,
                        message.getSize(),
                        content
                }).getMessage());
            }
            // format text/plain, text/xml and all other text/* as text/plain
            else if(message.isMimeType("text/*")) {
                final String template = IOUtils.toString(ParrotUtils.class.getResourceAsStream("/text_plain_template.html"));
                sb.append(MessageFormatter.arrayFormat(template, new Object[]{
                        contentType,
                        message.getSize(),
                        StringEscapeUtils.escapeHtml(String.valueOf(content))
                }).getMessage());
            }
            else if(message.isMimeType("image/*") && message instanceof MimeBodyPart) {
                final String template = IOUtils.toString(ParrotUtils.class.getResourceAsStream("/image_template.html"));
                final int indexOfSemicolon = contentType.indexOf(";");
                final String mimeType = indexOfSemicolon == -1? contentType: contentType.substring(0, indexOfSemicolon);
                sb.append(MessageFormatter.arrayFormat(template,
                        new Object[]{
                                message.getFileName(),
                                message.getSize(),
                                String.format("data:%s;base64,%s", mimeType, IOUtils.toString(((MimeBodyPart) message).getRawInputStream())),
                        }
                ).getMessage());
            }
            else if (message instanceof MimeBodyPart) {
                final String template = IOUtils.toString(ParrotUtils.class.getResourceAsStream("/attachment_template.html"));
                final int indexOfSemicolon = contentType.indexOf(";");
                final String mimeType = indexOfSemicolon == -1? contentType: contentType.substring(0, indexOfSemicolon);
                sb.append(MessageFormatter.arrayFormat(template,
                        new Object[]{
                                mimeType,
                                String.format("data:%s;base64,%s", mimeType, IOUtils.toString(((MimeBodyPart) message).getRawInputStream())),
                                message.getFileName(),
                                message.getSize(),
                        }
                ).getMessage());
            }
            else {
                log.debug("message/part has unsupported content type {} ({}), ignoring", contentType, content.getClass().getName());
            }
        }
        return sb.toString();
    }

}
