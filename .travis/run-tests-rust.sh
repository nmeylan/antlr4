#!/bin/bash

set -euo pipefail

export PATH=$HOME/.cargo/bin:$PATH
mvn test -Dtest=rust.*Left* -q
