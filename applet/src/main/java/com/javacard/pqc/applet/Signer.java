package com.javacard.pqc.applet;

public interface Signer {
    short sign(byte[] message, short msgOffset, short msgLength, byte[] output, short outputOffset);
}
