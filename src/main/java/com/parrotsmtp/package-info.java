/**
 * TODO in "Beta" release:
 * <ol>
 *     <li>Remove somehow message_template.html. Use message_template.shtml instead.</li>
 *     <li>Process SSI in Tomcat filter or in Apache (not as Tomcat servlet)</li>
 *     <li>Implement fetching all messages in a *.zip archive</li>
 *     <li>ParrotUtils: optimize and refactor building HTML algorithm</li>
 *     <li>ParrotSMTPServlet: remove response charset hard code "Windows-1251"</li>
 * </ol>
 *
 * TODO in "RC" releases:
 * <ol>
 *     <li>MessagePool: try to avoid Collections.synchronizedMap() usage</li>
 *     <li>Implement detection and blocking of users which are abusing service</li>
 * </ol>
 *
 * FIXED in "Beta" release:
 * <ol>
 *     <li>admin page: clear message pool option added</li>
 *     <li>ParrotSMTPService: serious copy-paste bug in fetch_email fixed</li>
 *     <li>ParrotUtils: Implement fetching a message in html format (in browser window)</li>
 *     <li>ParrotUtils: Implement image display in multipart messages</li>
 *     <li>ParrotUtils: filter special html characters and tags for text/plain mime parts</li>
 *     <li>ParrotUtils: implement arbitrary mime typed attachment download</li>
 * </ol>
 *
 * FIXED in "Alpha" release:
 * <ol>
 *     <li>SMTPService.Handler: do not send 250 OK while receiving data</li>
 *     <li>SMTPService.Handler: do not close connection on "."</li>
 *     <li>SMTPService.Handler: do not close connection on SMTP commands errors</li>
 *     <li>SMTPService.Handler: limit BufferedReader.readLine(); limit maximum message size</li>
 *     <li>SMTPService.Handler: implement connection timeout</li>
 *     <li>MessagePool: implemented pool size limit: max users, max messages per user</li>
 *     <li>ParrotSMTPServlet: correct response messaging when service did not start</li>
 *     <li>SMTPService: bind mechanism fixed; port change mechanism fixed</li>
 *     <li>KnownSMTPCommands: from and to regexps fixed</li>
 *     <li>ParrotSMTPServlet: shutdown SMTP service when servlet is destroyed</li>
 *     <li>Refactor logging to log more relevant events when switching to log level higher then "trace"</li>
 *     <li>SMTPService.Handler: save truncated messages too</li>
 *     <li><strike>SMTPService.Handler: don't create new Message object when no new message will be built</strike> - not necessary</li>
 *     <li>SMTPService: Realize and rethink possible service load, thread pool sizes</li>
 *     <li>ParrotSMTPServlet, MessagePool: Add service monitoring features for admin to see basic server statistic</li>
 *     <li>Message, ParrotSMTPServlet: add actual message creation time to the *.eml attachment name</li>
 *     <li>MessagePool: deliver more statistical data</li>
 * </ol>
 */

package com.parrotsmtp;
