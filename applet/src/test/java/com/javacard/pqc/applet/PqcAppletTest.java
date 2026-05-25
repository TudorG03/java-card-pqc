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

    public final static int CLA = 0x00;

    public final static int INS_SELECT = 0xA4;

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
}
