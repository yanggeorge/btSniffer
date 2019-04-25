package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by ym on 2019-04-25
 */
public class HandshakeDecoder extends ByteToMessageDecoder {

    private enum State {init, other}

    private enum ExtState {length, data}

    private State state = State.init;
    private ExtState extState = ExtState.length;
    private int extMsgLength = 0;

    private Logger logger = LoggerFactory.getLogger(HandshakeDecoder.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        if (State.init.equals(this.state)) {
            if (buf.readableBytes() < 68) {
                return;
            }
            logger.info("receive 68 bytes");
            byte b = buf.readByte();
            if ((int) b != 19) {
                logger.error("handshake error.");
                ctx.close();
            }
            ByteBuf buffer = buf.readBytes(19);
            String head = Unpooled.copiedBuffer(buffer).toString(CharsetUtil.UTF_8);
            buffer.release();
            if (!"BitTorrent protocol".equals(head)) {
                logger.error("handshake error");
                ctx.close();
            }

            buf.readBytes(8); // skip
            buffer = buf.readBytes(20);
            String infoHash = ByteBufUtil.hexDump(buffer);
            buffer.release();
            buffer = buf.readBytes(20);
            String peerId = ByteBufUtil.hexDump(buffer);
            buffer.release();
            logger.info("decode: infoHash={}, peerId={}", infoHash, peerId);

            list.add(new HandshakeOkMsg());
            this.state = State.other;
        }

        while (buf.readableBytes() > 0) {
            switch (this.extState) {
                case length:
                    if (buf.readableBytes() < 4) {
                        return;
                    }

                    extMsgLength = buf.readInt();
                    this.extState = ExtState.data;
                    break;
                case data:
                    if (buf.readableBytes() < extMsgLength) {
                        return;
                    }
                    ByteBuf tmp = buf.readBytes(extMsgLength);
                    int id = (int) tmp.readByte();
                    switch (id) {
                        case 20: // ext protocol

                            int extId = (int) tmp.readByte();
                            switch (extId) {
                                case 0: // ext handshake
                                    Map<String, Object> dic = Util.parse(tmp);
                                    logger.info(dic.toString());
                                    break;
                                case 1: //ut_metadata
                                    break;
                                default:
                                    logger.error("error");
                            }

                            break;
                        case 9: // port
                            int port = tmp.readInt();
                            logger.info("peer port = {}", port);
                            break;
                        case 1: // ignore
                            break;
                        default:
                            logger.error("error");
                    }

                    tmp.release();
                    this.extState = ExtState.length;
                    break;
                default:
                    logger.error("error");
            }

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("", cause);
        ctx.close();
    }
}
