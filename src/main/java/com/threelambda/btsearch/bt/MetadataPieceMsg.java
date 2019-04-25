package com.threelambda.btsearch.bt;

/**
 * Created by ym on 2019-04-25
 */
public class MetadataPieceMsg extends HandshakeMsg{

    private final int piece;
    private final byte[] pieceData ;

    MetadataPieceMsg(int piece, byte[] pieceData) {
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
