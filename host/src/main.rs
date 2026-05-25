use std::io::{Read, Write};
use std::net::TcpStream;

const SIMULATOR_ADDR: &str = "127.0.0.1:9025";
const AID: &[u8] = &[0xF0, 0x00, 0x00, 0x00, 0x01];

const CLA: u8 = 0x00;
const INS_SELECT: u8 = 0xA4;
const INS_ECHO: u8 = 0x10;

fn main() {
    let mut stream = TcpStream::connect(SIMULATOR_ADDR)
        .expect("Could not connect to simulator — is it running?");
    println!("Connected to simulator");

    let sw = send_select(&mut stream, AID);
    println!("SELECT response: {:04X}", sw);
    assert_eq!(sw, 0x9000, "SELECT failed");
    println!("Applet selected");

    let payload: &[u8] = &[0xDE, 0xAD, 0xBE, 0xEF];
    let response = send_echo(&mut stream, payload);
    let sw = status_word(&response);
    let data = &response[..response.len() - 2];
    println!("ECHO response: {:04X}", sw);
    println!("ECHO data: {:02X?}", data);
    assert_eq!(sw, 0x9000, "ECHO failed");
    assert_eq!(data, payload, "ECHO returned wrong bytes");
    println!("ECHO ok");
}

fn send_apdu(stream: &mut TcpStream, apdu: &[u8]) -> Vec<u8> {
    let len = apdu.len() as u16;
    stream.write_all(&len.to_be_bytes()).unwrap();
    stream.write_all(apdu).unwrap();

    let mut len_buf = [0u8; 2];
    stream.read_exact(&mut len_buf).unwrap();
    let resp_len = u16::from_be_bytes(len_buf) as usize;

    let mut response = vec![0u8; resp_len];
    stream.read_exact(&mut response).unwrap();
    response
}

fn send_select(stream: &mut TcpStream, aid: &[u8]) -> u16 {
    let mut apdu = vec![CLA, INS_SELECT, 0x04, 0x00, aid.len() as u8];
    apdu.extend_from_slice(aid);
    let response = send_apdu(stream, &apdu);
    status_word(&response)
}

fn send_echo(stream: &mut TcpStream, data: &[u8]) -> Vec<u8> {
    let mut apdu = vec![CLA, INS_ECHO, 0x00, 0x00, data.len() as u8];
    apdu.extend_from_slice(data);
    send_apdu(stream, &apdu)
}

fn status_word(response: &[u8]) -> u16 {
    let n = response.len();
    u16::from_be_bytes([response[n - 2], response[n - 1]])
}
