package com.javacard.pqc.applet;

import javacard.security.MessageDigest;

import javacard.framework.*;

public class PqcApplet extends Applet {

    public static final byte[] AID_BYTES = { (byte) 0xF0, 0x00, 0x00, 0x00, 0x01 };

    private MessageDigest md;

    private byte[] buffer;

    private final static short BUFFER_SIZE = 32;

    public final static int INS_ECHO = 0x10;

    public final static int INS_HASH = 0x20;

    private PqcApplet() {
        md = MessageDigest.getInstance(MessageDigest.ALG_SHA3_256, false);
        buffer = JCSystem.makeTransientByteArray(BUFFER_SIZE, JCSystem.CLEAR_ON_DESELECT);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new PqcApplet().register();
    }

    @Override
    public void process(APDU apdu) {
        if (selectingApplet())
            return;

        switch (apdu.getBuffer()[ISO7816.OFFSET_INS]) {
            case INS_ECHO:
                echo(apdu);
                break;

            case INS_HASH:
                hash(apdu);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void echo(APDU apdu) {
        short len = apdu.setIncomingAndReceive();
        apdu.setOutgoing();
        apdu.setOutgoingLength(len);
        apdu.sendBytes(ISO7816.OFFSET_CDATA, len);
    }

    private void hash(APDU apdu) {
        short len = apdu.setIncomingAndReceive();
        md.doFinal(apdu.getBuffer(), ISO7816.OFFSET_CDATA, len, buffer, (short) 0);
        apdu.setOutgoing();
        apdu.setOutgoingLength(BUFFER_SIZE);
        apdu.sendBytesLong(buffer, (short) 0, BUFFER_SIZE);
    }
}
