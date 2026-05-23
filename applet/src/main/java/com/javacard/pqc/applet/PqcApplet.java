package com.javacard.pqc.applet;

import javacard.framework.*;

public class PqcApplet extends Applet {

    public static final byte[] AID_BYTES = {(byte) 0xF0, 0x00, 0x00, 0x00, 0x01};

    private PqcApplet() {
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new PqcApplet().register();
    }

    @Override
    public void process(APDU apdu) {
        if (selectingApplet()) return;
        ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
    }
}
