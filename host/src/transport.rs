use std::io::{Read, Write};
use std::net::TcpStream;

use anyhow::{bail, Result};

pub struct CardTransport {
    stream: TcpStream,
}

impl CardTransport {
    pub fn connect(addr: &str) -> Result<Self> {
        let stream = TcpStream::connect(addr)?;
        Ok(CardTransport { stream })
    }

    pub fn select(&mut self, aid: &[u8]) -> Result<()> {
        let mut apdu = vec![0x00, 0xA4, 0x04, 0x00, aid.len() as u8];
        apdu.extend_from_slice(aid);
        let response = self.send_apdu(&apdu)?;
        let sw = status_word(&response);
        if sw != 0x9000 {
            bail!("SELECT failed: {:04X}", sw);
        }
        Ok(())
    }

    pub fn send_apdu(&mut self, apdu: &[u8]) -> Result<Vec<u8>> {
        let len = apdu.len() as u16;
        self.stream.write_all(&len.to_be_bytes())?;
        self.stream.write_all(apdu)?;

        let mut len_buf = [0u8; 2];
        self.stream.read_exact(&mut len_buf)?;
        let resp_len = u16::from_be_bytes(len_buf) as usize;

        let mut response = vec![0u8; resp_len];
        self.stream.read_exact(&mut response)?;
        Ok(response)
    }

    pub fn collect_chunked(&mut self, first: Vec<u8>, ins_get_response: u8) -> Result<Vec<u8>> {
        let mut data = first[..first.len() - 2].to_vec();
        let mut sw = status_word(&first);

        while (sw & 0xFF00) == 0x6100 {
            let apdu = vec![0x00, ins_get_response, 0x00, 0x00, 0x00];
            let response = self.send_apdu(&apdu)?;
            sw = status_word(&response);
            data.extend_from_slice(&response[..response.len() - 2]);
        }

        if sw != 0x9000 {
            bail!("collect_chunked failed with SW {:04X}", sw);
        }
        Ok(data)
    }
}

pub fn status_word(response: &[u8]) -> u16 {
    let n = response.len();
    u16::from_be_bytes([response[n - 2], response[n - 1]])
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn status_word_extracts_9000() {
        assert_eq!(status_word(&[0xDE, 0xAD, 0x90, 0x00]), 0x9000);
    }

    #[test]
    fn status_word_detects_bytes_remaining() {
        assert_eq!(status_word(&[0x01, 0x61, 0xFF]) & 0xFF00, 0x6100);
    }

    #[test]
    fn status_word_extracts_6d00() {
        assert_eq!(status_word(&[0x6D, 0x00]), 0x6D00);
    }
}
