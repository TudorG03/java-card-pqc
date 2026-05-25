package com.javacard.pqc.applet;

import javacard.security.MessageDigest;

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;

import javacard.framework.*;

public class PqcApplet extends Applet {

    public static final byte[] AID_BYTES = { (byte) 0xF0, 0x00, 0x00, 0x00, 0x01 };

    private MessageDigest md;

    private byte[] hashBuffer;

    private byte[] outBuffer;
    private short outOffset;
    private short outLength;

    private final byte[] publicKey;
    private final Signer signer;

    private static final short HASH_BUFFER_SIZE = 32;
    private static final short OUT_BUFFER_SIZE = 2420;

    public static final byte INS_ECHO = 0x10;
    public static final byte INS_HASH = 0x20;
    public static final byte INS_GET_PUBKEY = 0x30;
    public static final byte INS_SIGN = 0x40;
    public static final byte INS_GET_RESPONSE = (byte) 0xC0;

    private PqcApplet() {
        md = MessageDigest.getInstance(MessageDigest.ALG_SHA3_256, false);
        hashBuffer = JCSystem.makeTransientByteArray(HASH_BUFFER_SIZE, JCSystem.CLEAR_ON_DESELECT);
        outBuffer = JCSystem.makeTransientByteArray(OUT_BUFFER_SIZE, JCSystem.CLEAR_ON_DESELECT);

        MLDSAKeyPairGenerator generator = new MLDSAKeyPairGenerator();
        generator.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_44));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();

        publicKey = ((MLDSAPublicKeyParameters) pair.getPublic()).getEncoded();
        signer = new MlDsaSigner((MLDSAPrivateKeyParameters) pair.getPrivate());
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
            case INS_GET_PUBKEY:
                getPubKey(apdu);
                break;
            case INS_SIGN:
                sign(apdu);
                break;
            case INS_GET_RESPONSE:
                getResponse(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
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
        md.doFinal(apdu.getBuffer(), ISO7816.OFFSET_CDATA, len, hashBuffer, (short) 0);
        apdu.setOutgoing();
        apdu.setOutgoingLength(HASH_BUFFER_SIZE);
        apdu.sendBytesLong(hashBuffer, (short) 0, HASH_BUFFER_SIZE);
    }

    private void getPubKey(APDU apdu) {
        Util.arrayCopyNonAtomic(publicKey, (short) 0, outBuffer, (short) 0, (short) publicKey.length);
        outOffset = 0;
        outLength = (short) publicKey.length;
        sendChunk(apdu);
    }

    private void sign(APDU apdu) {
        short len = apdu.setIncomingAndReceive();
        if (len != HASH_BUFFER_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        outLength = signer.sign(apdu.getBuffer(), ISO7816.OFFSET_CDATA, len, outBuffer, (short) 0);
        outOffset = 0;
        sendChunk(apdu);
    }

    private void getResponse(APDU apdu) {
        if (outOffset >= outLength) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        sendChunk(apdu);
    }

    private void sendChunk(APDU apdu) {
        short remaining = (short) (outLength - outOffset);
        short sendNow = remaining > 255 ? (short) 255 : remaining;

        apdu.setOutgoing();
        apdu.setOutgoingLength(sendNow);
        apdu.sendBytesLong(outBuffer, outOffset, sendNow);
        outOffset += sendNow;

        short stillRemaining = (short) (outLength - outOffset);
        if (stillRemaining > 0) {
            short sw = (short) (ISO7816.SW_BYTES_REMAINING_00
                    | (stillRemaining > 255 ? 0xFF : stillRemaining));
            ISOException.throwIt(sw);
        }
    }
}
