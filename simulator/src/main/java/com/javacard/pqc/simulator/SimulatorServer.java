package com.javacard.pqc.simulator;

import com.javacard.pqc.applet.PqcApplet;
import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.AID;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.*;
import java.net.*;

public class SimulatorServer {

    private static final int PORT = 9025;

    public static void main(String[] args) throws IOException {
        CardSimulator simulator = new CardSimulator();
        AID aid = AIDUtil.create(PqcApplet.AID_BYTES);
        simulator.installApplet(aid, PqcApplet.class);

        System.out.println("Simulator ready on port " + PORT);

        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                System.out.println("Client connected: " + client.getRemoteSocketAddress());
                handleClient(client, simulator);
            }
        }
    }

    private static void handleClient(Socket socket, CardSimulator simulator) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                int length = in.readUnsignedShort();
                byte[] apduBytes = in.readNBytes(length);

                ResponseAPDU response = simulator.transmitCommand(new CommandAPDU(apduBytes));
                byte[] responseBytes = response.getBytes();

                out.writeShort(responseBytes.length);
                out.write(responseBytes);
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
