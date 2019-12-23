#!/bin/bash

set -euo pipefail

curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- --default-toolchain nightly-2020-12-23 -y
export PATH=$HOME/.cargo/bin:$PATH
( rustc --version ; cargo --version ) || true