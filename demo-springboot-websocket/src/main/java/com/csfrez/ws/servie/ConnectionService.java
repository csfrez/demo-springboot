package com.csfrez.ws.servie;

import io.netty.channel.Channel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author
 * @date 2025/5/30 14:51
 * @email
 */
@Service
public class ConnectionService {

    private final Map<String, Channel> connections = new ConcurrentHashMap<>();

    public void addConnection(String token, Channel channel) {
        connections.put(token, channel);
    }

    public void removeConnection(String token, Channel channel) {
        connections.remove(token, channel);
    }

    public Channel getChannel(String token) {
        return connections.get(token);
    }
}