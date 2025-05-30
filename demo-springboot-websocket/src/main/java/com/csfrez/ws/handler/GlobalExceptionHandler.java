package com.csfrez.ws.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author
 * @date 2025/5/30 17:51
 * @email
 */
@Slf4j
public class GlobalExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("发生异常: ", cause);
        ctx.close();
    }
}