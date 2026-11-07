package chat.oi.core.crypto

data class RatchetState(
    var rootKey: ByteArray,
    var sendingChainKey: ByteArray? = null,
    var receivingChainKey: ByteArray? = null,
    var localDhKeyPair: KeyPair,
    var remoteDhPublicKey: ByteArray? = null,
    var sendSequenceNumber: Long = 0,
    var receiveSequenceNumber: Long = 0,
    var previousSendingChainLength: Long = 0
) {
    data class KeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    )
}
