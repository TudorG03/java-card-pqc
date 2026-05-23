use std::io::{Read, Write};
use std::net::TcpStream;

const SIMULATOR_ADDR: &str = "127.0.0.1:9025";
const AID: &[u8] = &[0xF0, 0x00, 0x00, 0x00, 0x01];

fn main() {
    let mut stream = TcpStream::connect(SIMULATOR_ADDR)
        .expect("Could not connect to simulator — is it running?");
    println!("Connected to simulator");

    let sw = send_select(&mut stream, AID);
    println!("SELECT response: {:04X}", sw);

    if sw == 0x9000 {
        println!("Applet selected successfully");
    } else {
        eprintln!("SELECT failed");
        std::process::exit(1);
    }
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
    let mut apdu = vec![0x00, 0xA4, 0x04, 0x00, aid.len() as u8];
    apdu.extend_from_slice(aid);
    let response = send_apdu(stream, &apdu);
    let n = response.len();
    u16::from_be_bytes([response[n - 2], response[n - 1]])
}
