package com.threelambda.btsearch.bt.udp;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;


public class IncomingPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    IncomingPacketHandler() {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {

    }

}
