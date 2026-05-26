mod hasher;
mod signer;
mod transport;
mod verifier;

use anyhow::Result;
use clap::{Parser, Subcommand};
use std::path::PathBuf;

const SIMULATOR_ADDR: &str = "127.0.0.1:9025";
const AID: &[u8] = &[0xF0, 0x00, 0x00, 0x00, 0x01];

#[derive(Parser)]
#[command(name = "host")]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    Sign { file: PathBuf },
    Verify { file: PathBuf },
}

fn main() -> Result<()> {
    let cli = Cli::parse();

    match cli.command {
        Commands::Sign { file } => {
            let digest = hasher::hash_file(&file)?;
            println!("SHA3-256: {}", hex_encode(&digest));

            let mut transport = transport::CardTransport::connect(SIMULATOR_ADDR)?;
            transport.select(AID)?;
            println!("Applet selected");

            let signature = signer::sign_digest(&mut transport, &digest)?;

            let sig_path = file.with_extension("sig");
            std::fs::write(&sig_path, &signature)?;
            println!(
                "Signature ({} bytes) written to {}",
                signature.len(),
                sig_path.display()
            );
        }
        Commands::Verify { file } => {
            let digest = hasher::hash_file(&file)?;
            let sig_path = file.with_extension("sig");
            let signature = std::fs::read(&sig_path)?;

            let mut transport = transport::CardTransport::connect(SIMULATOR_ADDR)?;
            transport.select(AID)?;
            println!("Applet selected");

            let pub_key = verifier::get_pubkey(&mut transport)?;
            let valid = verifier::verify_signature(&pub_key, &digest, &signature)?;

            if valid {
                println!("Signature is VALID!");
            } else {
                println!("Signature is INVALID!");
                std::process::exit(1);
            }
        }
    }

    Ok(())
}

fn hex_encode(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{:02x}", b)).collect()
}
