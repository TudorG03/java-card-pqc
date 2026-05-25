package com.javacard.pqc.applet;

import javacard.framework.*;

public class PqcApplet extends Applet {

    public static final byte[] AID_BYTES = { (byte) 0xF0, 0x00, 0x00, 0x00, 0x01 };

    public final static int INS_ECHO = 0x10;

    private PqcApplet() {
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
}
