package com.javacard.pqc.applet;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PqcAppletTest {

    private CardSimulator simulator;

    private final static int CLA = 0x00;

    private final static int INS_SELECT = 0xA4;

    private final static String EMPTY_SHA3_256_OUTPUT = "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a";

    private final static String ABC_SHA3_256_OUTPUT = "3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532";

    private CardSimulator buildSimulator() {
        CardSimulator simulator = new CardSimulator();
        AID aid = AIDUtil.create(PqcApplet.AID_BYTES);
        simulator.installApplet(aid, PqcApplet.class);
        return simulator;
    }

    @BeforeEach
    void setupSimulator() {
        simulator = buildSimulator();
        simulator
                .transmitCommand(new CommandAPDU(CLA, INS_SELECT, 0x04, 0x00, PqcApplet.AID_BYTES));
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }

        return bytes;
    }

    @Test
    void selectShouldReturnSuccess() {
        CardSimulator simulator = buildSimulator();
        CommandAPDU select = new CommandAPDU(CLA, INS_SELECT, 0x04, 0x00, PqcApplet.AID_BYTES);
        ResponseAPDU response = simulator.transmitCommand(select);
        assertEquals(0x9000, response.getSW(), "SELECT must return 9000");
    }

    @Test
    void unknownInstructionShouldReturnInsNotSupported() {
        CommandAPDU unknown = new CommandAPDU(CLA, (byte) 0xFF, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(unknown);
        assertEquals(0x6D00, response.getSW(), "Unknown INS must return 6D00");
    }

    @Test
    void echoShouldReturnSuccessAndSameBytes() {
        byte[] payload = { 0x01, 0x02, 0x03 };
        CommandAPDU echo = new CommandAPDU(CLA, PqcApplet.INS_ECHO, 0x00, 0x00, payload);
        ResponseAPDU response = simulator.transmitCommand(echo);
        assertEquals(0x9000, response.getSW());
        assertArrayEquals(payload, response.getData());
    }

    @Test
    void hashOfEmptyInputReturnSuccessAndMatchesNistVector() {
        CommandAPDU emptyHash = new CommandAPDU(CLA, PqcApplet.INS_HASH, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(emptyHash);
        assertEquals(0x9000, response.getSW());
        assertArrayEquals(hexToBytes(EMPTY_SHA3_256_OUTPUT), response.getData());
    }

    @Test
    void hashOfAbcReturnSuccessAndMatchesNistVector() {
        byte[] payload = { 0x61, 0x62, 0x63 };
        CommandAPDU abcHash = new CommandAPDU(CLA, PqcApplet.INS_HASH, 0x00, 0x00, payload);
        ResponseAPDU response = simulator.transmitCommand(abcHash);
        assertEquals(0x9000, response.getSW());
        assertArrayEquals(hexToBytes(ABC_SHA3_256_OUTPUT), response.getData());
    }

    @Test
    void getPubKeyShouldReturnSuccessAndCorrectSize() {
        byte[] accumulated = new byte[1312];
        int total = 0;

        ResponseAPDU response = simulator.transmitCommand(
                new CommandAPDU(CLA, PqcApplet.INS_GET_PUBKEY, 0x00, 0x00, 256));
        byte[] chunk = response.getData();
        System.arraycopy(chunk, 0, accumulated, total, chunk.length);
        total += chunk.length;

        while ((response.getSW() & 0xFF00) == 0x6100) {
            response = simulator.transmitCommand(
                    new CommandAPDU(CLA, PqcApplet.INS_GET_RESPONSE, 0x00, 0x00, 256));
            chunk = response.getData();
            System.arraycopy(chunk, 0, accumulated, total, chunk.length);
            total += chunk.length;
        }

        assertEquals(0x9000, response.getSW());
        assertEquals(1312, total);
    }
}
