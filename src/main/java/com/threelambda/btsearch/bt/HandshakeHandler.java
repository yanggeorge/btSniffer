package com.threelambda.btsearch.bt;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ym on 2019-04-24
 */
public class HandshakeHandler extends SimpleChannelInboundHandler<HandshakeMsg> {

    private Logger logger = LoggerFactory.getLogger(SimpleChannelInboundHandler.class);

    private final String infoHash;

    HandshakeHandler(String infoHash) {
        this.infoHash = infoHash;
    }


    @Override
    public void channelActive(ChannelHandlerContext context) {
        //发送握手请求
        context.writeAndFlush(Util.getHandshake(infoHash));
    }


    @Override
    protected void channelRead0(ChannelHandlerContext context, HandshakeMsg msg) {
        if (msg instanceof HandshakeOkMsg) {
            //握手成功，进行扩展协议的握手的请求。
            logger.info("ext handshake request");
            context.writeAndFlush(Util.getExtHandshake());
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }

}