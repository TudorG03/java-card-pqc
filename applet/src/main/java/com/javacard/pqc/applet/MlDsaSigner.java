package com.javacard.pqc.applet;

import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;

public class MlDsaSigner implements Signer {

    private final MLDSAPrivateKeyParameters privateKey;

    public MlDsaSigner(MLDSAPrivateKeyParameters privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public short sign(byte[] message, short msgOffset, short msgLength, byte[] output, short outputOffset) {
        org.bouncycastle.crypto.Signer bcSigner = new MLDSASigner();
        bcSigner.init(true, privateKey);
        bcSigner.update(message, msgOffset, msgLength);
        byte[] sig;
        try {
            sig = bcSigner.generateSignature();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.arraycopy(sig, 0, output, outputOffset, sig.length);
        return (short) sig.length;
    }
}
