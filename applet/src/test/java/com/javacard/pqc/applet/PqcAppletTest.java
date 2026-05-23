package com.javacard.pqc.applet;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PqcAppletTest {

    private CardSimulator buildSimulator() {
        CardSimulator simulator = new CardSimulator();
        AID aid = AIDUtil.create(PqcApplet.AID_BYTES);
        simulator.installApplet(aid, PqcApplet.class);
        return simulator;
    }

    @Test
    void selectShouldReturnSuccess() {
        CardSimulator simulator = buildSimulator();
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, PqcApplet.AID_BYTES);
        ResponseAPDU response = simulator.transmitCommand(select);
        assertEquals(0x9000, response.getSW(), "SELECT must return 9000");
    }

    @Test
    void unknownInstructionShouldReturnInsNotSupported() {
        CardSimulator simulator = buildSimulator();
        simulator.transmitCommand(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, PqcApplet.AID_BYTES));
        CommandAPDU unknown = new CommandAPDU(0x00, (byte) 0xFF, 0x00, 0x00);
        ResponseAPDU response = simulator.transmitCommand(unknown);
        assertEquals(0x6D00, response.getSW(), "Unknown INS must return 6D00");
    }
}
