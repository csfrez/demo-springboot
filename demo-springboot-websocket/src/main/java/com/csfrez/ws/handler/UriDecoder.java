package com.csfrez.ws.handler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author
 * @date 2025/6/3 12:42
 * @email
 */
public class UriDecoder {
    private final Map<String, String> parameters = new HashMap<>();
    private final String path;

    public UriDecoder(String uri) {
        int queryStart = uri.indexOf('?');
        this.path = (queryStart == -1) ? uri : uri.substring(0, queryStart);

        if (queryStart != -1 && uri.length() > queryStart + 1) {
            String query = uri.substring(queryStart + 1);
            parseQuery(query);
        }
    }

    private void parseQuery(String query) {
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                parameters.put(pair[0], pair[1]);
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }
}
