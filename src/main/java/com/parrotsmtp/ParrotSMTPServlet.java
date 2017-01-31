package com.parrotsmtp;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.parrotsmtp.service.Message;
import com.parrotsmtp.service.MessagePool;
import com.parrotsmtp.service.SMTPService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 11.05.12 9:21
 */
public class ParrotSMTPServlet extends HttpServlet {
    private final ResourceBundle app = ResourceBundle.getBundle("app");
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SMTPService service = new SMTPService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.trace("received incoming request: {}", req.getQueryString());
        final String command = req.getParameter("cmd");

        log.trace("creating writer to build response");
        resp.setHeader("Content-Type", "text/plain; charset=utf-8");
        final PrintWriter printer = new PrintWriter(resp.getOutputStream(), true);

        // commands
        if("service_start".equalsIgnoreCase(command)) {
            serviceStart(req, printer);
        }
        else if("service_stop".equalsIgnoreCase(command)) {
            serviceStop(req, printer);
        }
        else if("service_stats".equalsIgnoreCase(command)) {
            serviceStats(req, printer);
        }
        else if("clear_pool".equalsIgnoreCase(command)) {
            clearPool(req, printer);
        }
        else if("fetch_all".equalsIgnoreCase(command)) {
            final String recipient = req.getParameter("recipient");
            if(recipient != null) {
                log.trace("fetching all recipient's {} messages", recipient);
                final Message[] messages = service.getMessagePool().fetchMessages(recipient);
                if(messages != null && messages.length > 0) {
                    log.trace("{} new message(s) for recipient {}", messages.length, recipient);
                    for(Message message: messages) {
                        printer.println(message);
                        printer.println();
                    }
                }
                else {
                    log.trace("no new messages for recipient {}", recipient);
                    printer.println("no new messages for " + recipient);
                }
            }
            else {
                log.trace("recipient is not specified, command 'fetch_all' ignored");
                printer.println("error: recipient is not specified");
            }
        }
        else if("fetch_email".equalsIgnoreCase(command)) {
            final String recipient = req.getParameter("recipient");
            if(recipient != null) {
                log.trace("fetching one email for recipient {}", recipient);
                final MessagePool pool = service.getMessagePool();
                if(pool.hasMessages(recipient)) {
                    final Message message = pool.nextMessage(recipient);
                    if(message != null) {
                        log.trace("got new message for recipient {}", recipient);

                        final String asEml = req.getParameter("as_eml");
                        if(asEml != null && asEml.equalsIgnoreCase("true")) {
                            log.trace("message will be delivered as message/rfc822 (*.eml file)");
                            resp.setHeader("Content-Type", "message/rfc822; charset=utf-8");
                            resp.setHeader("Content-Disposition", String.format("attachment; filename=%d-%d.eml", message.getBuilt(), System.currentTimeMillis()));
                            printer.println(message.toEmlString());
                        }
                        else {
                            log.trace("message will be delivered as text/html");
                            resp.setHeader("Content-Type", "text/html; charset=Windows-1251");  // TODO: Why "Windows-1251"?
                            final String messageAsHtml = message.toShtmlString();
                            log.trace("printing text/html:\n{}", messageAsHtml);
                            printer.println(messageAsHtml);
                        }
                    }
                    else {
                        log.warn("pool.hasMessages() returned true but no new message really exists");
                    }
                }
                else {
                    log.trace("no new messages for recipient {}", recipient);
                    printer.println("no new email for " + recipient);
                }
            }
            else {
                log.trace("recipient is not specified, command 'fetch_email' ignored");
                printer.println("error: recipient is not specified");
            }
        }
        else {
            log.trace("unknown command {}", command);
            printer.println("error: unknown command " + command);
        }
    }

    private void serviceStart(HttpServletRequest req, PrintWriter printer) {
        if(checkAdminPassword(req, printer)) {
            changeServicePortIfNeeded(req);
            printer.println(service.start()? "service started": "service not started");
        }
    }

    private void serviceStop(HttpServletRequest req, PrintWriter printer) {
        if(checkAdminPassword(req, printer)) {
            service.stop();
            printer.println("service stopped");
        }
    }

    private void serviceStats(HttpServletRequest req, PrintWriter printer) {
        if(checkAdminPassword(req, printer)) {
            final MessagePool messagePool = service.getMessagePool();
            printer.println(String.format("SMTP service is%s running", service.isRunning()? "": " NOT"));
            printer.println(String.format("users (undelivered):\t%d", messagePool.getUsersCurrent()));
            printer.println(String.format("undelivered messages:\t%d", messagePool.getMessagesCurrent()));
            printer.println(String.format("messages delivered:\t%d", messagePool.getMessagesDelivered()));
            printer.println(String.format("messages total:\t\t%d", messagePool.getMessagesTotal()));
        }
    }

    private void clearPool(HttpServletRequest req, PrintWriter printer) {
        if(checkAdminPassword(req, printer)) {
            service.getMessagePool().clear();
            printer.println("message pool cleared");
        }
    }

    private boolean changeServicePortIfNeeded(HttpServletRequest req) {
        final String port = req.getParameter("port");
        if(port != null) {
            try {
                log.trace("changing service port to {}", port);
                service.setPort(Integer.parseInt(port));
                return true;
            }
            catch (NumberFormatException numberFormat) {
                log.trace("port cant be changed - {} is not a number", port);
            }
        }
        return false;
    }

    private boolean checkAdminPassword(HttpServletRequest req, PrintWriter printer) {
        final String adminPassword = req.getParameter("admin_pwd");
        if(adminPassword != null) {
            log.trace("verifying admin password");
            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-512");
                final boolean passwordOk = app.getString("admin_pwd").equalsIgnoreCase(Hex.encodeHexString(digest.digest(adminPassword.getBytes())));
                log.trace(passwordOk? "password is ok": "password is wrong");
                return passwordOk;
            }
            catch(NoSuchAlgorithmException e) {
                log.error("can't verify admin password: {}", e.getMessage());
            }
        }
        else {
            log.trace("admin password is not specified");
        }
        printer.println("error: admin auth failed");
        return false;
    }

    @Override
    public void init() throws ServletException {
        service.start();
    }

    @Override
    public void destroy() {
        service.stop();
        super.destroy();
    }
}
