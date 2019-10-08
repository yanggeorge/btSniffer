package com.threelambda.btsearch.bt;

/**
 * Created by ym on 2019-04-25
 */
public class Msg {

    public static class HandshakeOkMsg extends Msg {
    }

    public static class MetadataMsg extends Msg {

        private final int metadataSize;
        private final int utMetadata;

        public MetadataMsg(int metadataSize, int utMetadata) {
            this.metadataSize = metadataSize;
            this.utMetadata = utMetadata;
        }

        public int getMetadataSize() {
            return metadataSize;
        }

        public int getUtMetadata() {
            return utMetadata;
        }
    }

    public static class MetadataPieceMsg extends Msg {

        private final int piece;
        private final byte[] pieceData;

        public MetadataPieceMsg(int piece, byte[] pieceData) {
            this.piece = piece;
            this.pieceData = pieceData;
        }

        public int getPiece() {
            return piece;
        }

        public byte[] getPieceData() {
            return pieceData;
        }
    }
}
