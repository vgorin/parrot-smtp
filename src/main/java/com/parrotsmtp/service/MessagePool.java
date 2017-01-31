package com.parrotsmtp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.parrotsmtp.util.LRUMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 11.05.12 16:34
 */
public class MessagePool {
    private static final ResourceBundle APP = ResourceBundle.getBundle("app");

    private static final int MAX_MSGS_PER_USER = Math.max(1, Integer.parseInt(APP.getString("max_msgs_per_user")));
    private static final int POOL_SIZE = Math.max(8, Integer.parseInt(APP.getString("pool_size")));

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, List<Message>> messages = Collections.synchronizedMap(new LRUMap<String, List<Message>>(POOL_SIZE, POOL_SIZE));

    // statistic counters
    private final AtomicInteger deliveredMessagesCounter = new AtomicInteger();
    private final AtomicInteger totalMessagesCounter = new AtomicInteger();

    int storeMessage(Message message) {
        final String to = message.getTo();
        log.trace("storing message to the pool; recipient: {}", to);
        List<Message> userMessages = messages.get(to);
        if(userMessages == null) {
            log.trace("user {} doesn't exist in the pool, creating", to);
            userMessages = Collections.synchronizedList(new ArrayList<Message>());
            messages.put(to, userMessages);
            log.trace("user {} (number {}) created", to, messages.size());
        }
        else  if(userMessages.size() >= MAX_MSGS_PER_USER) {
            log.trace("user {} has reached maximum messages per user limit, old message will be removed", to);
            userMessages.remove(0);
        }
        log.trace("storing message {}", message);
        userMessages.add(message);
        log.trace("message number {} for user {} stored", userMessages.size(), to);
        return totalMessagesCounter.incrementAndGet();
    }

    public Message[] fetchMessages(String userId) {
        log.trace("fetching messages for user {}", userId);
        final List<Message> userMessages = messages.remove(userId);
        final int size = userMessages != null? userMessages.size(): -1;
        if(size > 0) {
            log.trace("{} new messages for user {}", size, userId);
            final Message[] result = userMessages.toArray(new Message[size]);
            deliveredMessagesCounter.addAndGet(result.length);
            return result;
        }
        else {
            log.trace("no new messages for user {}", userId);
            return new Message[]{};
        }
    }

    public boolean hasMessages(String userId) {
        final boolean result = messages.containsKey(userId);
        log.trace("user {} has messages: {}", userId, result);
        return result;
    }

    public int newMessages(String userId) {
        final List<Message> userMessages = messages.get(userId);
        final int size = userMessages != null? userMessages.size(): 0;
        log.trace("user {} has {} new messages", userId, size);
        return size;
    }

    public Message nextMessage(String userId) {
        log.trace("next message requested for user {}", userId);
        final List<Message> userMessages = messages.get(userId);
        if(userMessages != null && userMessages.size() > 0) {
            final Message message = userMessages.remove(0);
            if(userMessages.size() == 0) {
                messages.remove(userId);
            }
            log.trace("message {} for user {} found", message, userId);
            deliveredMessagesCounter.incrementAndGet();
            return message;
        }
        else {
            log.trace("no new messages for user {}", userId);
            return null;
        }
    }

    public int getUsersCurrent() {
        log.trace("total number of users requested");
        final int size = messages.size();
        log.trace("total number of users: {}", size);
        return size;
    }

    public int getMessagesCurrent() {
        log.trace("total number of messages requested");
        int counter = 0;
        final Set<Map.Entry<String, List<Message>>> entries = messages.entrySet();
        synchronized(messages) {
            for(Map.Entry<String, List<Message>> entry : entries) {
                counter += entry.getValue().size();
            }
        }
        log.trace("total number of messages: {}", counter);
        return counter;
    }

    public int getMessagesDelivered() {
        return deliveredMessagesCounter.get();
    }

    public int getMessagesTotal() {
        return totalMessagesCounter.get();
    }

    public void clear() {
        log.trace("clearing message pool");
        messages.clear();
    }
}
