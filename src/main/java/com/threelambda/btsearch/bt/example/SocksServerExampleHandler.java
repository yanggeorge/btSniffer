package com.threelambda.btsearch.bt.example;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Created by ym on 2019-04-24
 */
@Sharable
public class SocksServerExampleHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.writeAndFlush(Unpooled.copiedBuffer("abc".getBytes()));
        Thread.sleep(5000);
        ctx.writeAndFlush(Unpooled.copiedBuffer("defg".getBytes()));
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }
}
