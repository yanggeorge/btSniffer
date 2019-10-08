package com.threelambda.btsearch.bt.metadata;

import com.threelambda.btsearch.bt.Msg;
import com.threelambda.btsearch.bt.Util;
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
public class MetadataDecoder extends ByteToMessageDecoder {

    private enum State {INIT, OTHER}

    private enum ExtState {READ_MSG_LENGTH, READ_DATA}

    private State state = State.INIT;
    private ExtState readState = ExtState.READ_MSG_LENGTH;
    private int msgLength = 0;

    private Logger logger = LoggerFactory.getLogger(MetadataDecoder.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        if (State.INIT.equals(this.state)) {
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

            list.add(new Msg.HandshakeOkMsg());
            this.state = State.OTHER;
        }

        while (buf.readableBytes() > 0) {
            switch (this.readState) {
                case READ_MSG_LENGTH:
                    if (buf.readableBytes() < 4) {
                        return;
                    }
                    msgLength = buf.readInt();
                    if (msgLength == 0) {
                        continue;
                    }
                    this.readState = ExtState.READ_DATA;
                    break;
                case READ_DATA:
                    if (buf.readableBytes() < msgLength) {
                        return;
                    }
                    ByteBuf tmp = buf.readBytes(msgLength);
                    int id = (int) tmp.readByte();
                    switch (id) {
                        case 20: // ext protocol
                            int extId = (int) tmp.readByte();
                            ByteBuf bufData = tmp.readBytes(msgLength - 2);
                            Map<String, Object> dic = Util.decode(bufData);
                            switch (extId) {
                                case 0: // ext handshake
                                    long metadataSize = (long) dic.get("metadata_size");
                                    logger.info(dic.toString());
                                    Map<String, Object> metadataDic = (Map<String, Object>) dic.get("m");
                                    long utMetadata = (long) metadataDic.get("ut_metadata");
                                    list.add(new Msg.MetadataMsg((int) metadataSize, (int) utMetadata));
                                    break;
                                case 1: //ut_metadata
                                    long piece = (long) dic.get("piece");
                                    byte[] data = new byte[bufData.readableBytes()];
                                    bufData.readBytes(data);
                                    logger.info("piece={}, length={}", piece, data.length);
                                    list.add(new Msg.MetadataPieceMsg((int) piece, data));
                                    break;
                                default:
                                    logger.error("error");
                            }

                            break;
                        case 9: // port
                            int port = tmp.readUnsignedShort();
                            logger.info("peer port = {}", port);
                            break;
                        default:
                            //ignore
                    }

                    tmp.release();
                    this.readState = ExtState.READ_MSG_LENGTH;
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
