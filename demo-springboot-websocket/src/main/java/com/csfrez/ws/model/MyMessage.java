package com.csfrez.ws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author
 * @date 2025/5/30 14:54
 * @email
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private String content;
}