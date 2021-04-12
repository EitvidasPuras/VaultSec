package com.vaultsec.vaultsec.util.cipher

import javax.crypto.spec.SecretKeySpec

data class Wrapper(
    var key: SecretKeySpec,
    var saltBytes: ByteArray
)
