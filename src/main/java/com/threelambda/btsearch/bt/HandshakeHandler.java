package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ym on 2019-04-24
 */
public class HandshakeHandler extends SimpleChannelInboundHandler<HandshakeMsg> {

    private Logger logger = LoggerFactory.getLogger(SimpleChannelInboundHandler.class);

    private final String infoHash;
    private Map<Integer, byte[]> dataMap = null;
    private int pieces = -1;
    private int metadataSize = -1;


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
        } else if (msg instanceof MetadataMsg) {
            //请求数据
            MetadataMsg metadataMsg = (MetadataMsg) msg;
            int BLOCK_SIZE = 16384;
            metadataSize = metadataMsg.getMetadataSize();
            pieces = metadataMsg.getMetadataSize() / BLOCK_SIZE;
            if ((metadataMsg.getMetadataSize() % BLOCK_SIZE) > 0) {
                pieces += 1;
            }
            ByteBuf buf = Unpooled.buffer();
            for (int i = 0; i < pieces; i++) {
                buf.writeBytes(Util.getMetadataPieceRequest(i, metadataMsg.getUtMetadata()));
            }
            context.writeAndFlush(buf);
            this.dataMap = new HashMap<>();
        } else if (msg instanceof MetadataPieceMsg) {
            MetadataPieceMsg pieceMsg = (MetadataPieceMsg) msg;
            this.dataMap.put(pieceMsg.getPiece(), pieceMsg.getPieceData());
            if (this.dataMap.size() == this.pieces) {
                //已经完成，检查infoHash
                ByteBuf data = Unpooled.buffer();
                for (int i = 0; i < pieces; i++) {
                    data.writeBytes(this.dataMap.get(i));
                }
                if (data.readableBytes() == this.metadataSize) {
                    byte[] metadata = new byte[this.metadataSize];
                    data.readBytes(metadata);
                    String infoHash = DigestUtils.sha1Hex(metadata);
                    if (this.infoHash.equals(infoHash)) {
                        logger.info("get metadata success.");
                        context.close();
                    }
                }
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }

}