use anyhow::Result;

use crate::transport::CardTransport;

const CLA: u8 = 0x00;
const INS_SIGN: u8 = 0x40;
const INS_GET_RESPONSE: u8 = 0xC0;

pub fn sign_digest(transport: &mut CardTransport, digest: &[u8; 32]) -> Result<Vec<u8>> {
    let mut apdu = vec![CLA, INS_SIGN, 0x00, 0x00, 0x20];
    apdu.extend_from_slice(digest);
    apdu.push(0x00);
    let first = transport.send_apdu(&apdu)?;
    transport.collect_chunked(first, INS_GET_RESPONSE)
}
