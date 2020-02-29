package com.threelambda.btsniffer.bt.metadata;

import com.threelambda.btsniffer.bt.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2019-04-24
 */
public class MetadataHandler extends SimpleChannelInboundHandler<Msg> {
    private Logger logger = LoggerFactory.getLogger(SimpleChannelInboundHandler.class);

    private final String infoHashHex;
    private final String ip;
    private final int port;

    private BlockingQueue<Metadata> queue;

    private Map<Integer, byte[]> dataMap = null;
    private int pieces = -1;
    private int metadataSize = -1;
    private byte[] metadata = null;

    public MetadataHandler(String infoHashHex, String ip, int port, BlockingQueue<Metadata> queue) {
        this.infoHashHex = infoHashHex;
        this.ip = ip;
        this.port = port;
        this.queue = queue;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) {
        //发送握手请求
        context.writeAndFlush(Util.getHandshake(infoHashHex));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Msg msg) {
        if (msg instanceof Msg.HandshakeOkMsg) {
            //握手成功，进行扩展协议的握手的请求。
            context.writeAndFlush(Util.getExtHandshake());
        } else if (msg instanceof Msg.MetadataMsg) {
            //请求数据
            Msg.MetadataMsg metadataMsg = (Msg.MetadataMsg) msg;
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
        } else if (msg instanceof Msg.MetadataPieceMsg) {
            Msg.MetadataPieceMsg pieceMsg = (Msg.MetadataPieceMsg) msg;
            this.dataMap.put(pieceMsg.getPiece(), pieceMsg.getPieceData());
            if (this.dataMap.size() == this.pieces) {
                //已经完成，检查infoHash
                if (checkInfoHash()) {
                    try {
                        queue.offer(new Metadata(infoHashHex, metadataSize, metadata, ip, port), 1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                    logger.debug("get metadata success|infoHashHex={}", infoHashHex);
                    context.close();
                }else{
                    logger.info("get metadata failure|infoHashHex={}|ip={}|port={}", infoHashHex, ip, port);
                }
            }
        }
    }

    private boolean checkInfoHash() {
        ByteBuf data = Unpooled.buffer();
        for (int i = 0; i < pieces; i++) {
            data.writeBytes(this.dataMap.get(i));
        }
        if (data.readableBytes() == this.metadataSize) {
            metadata = new byte[this.metadataSize];
            data.readBytes(metadata);
            String currInfoHashHex = DigestUtils.sha1Hex(metadata);
            if (this.infoHashHex.equals(currInfoHashHex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }

}