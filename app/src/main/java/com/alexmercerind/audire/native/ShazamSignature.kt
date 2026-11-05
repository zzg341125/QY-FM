package com.alexmercerind.audire.native

/**
 * JNI binding to Shazam's acoustic fingerprinting signature algorithm.
 * Extracted and compiled from SongRec / shazam-signature-jni.
 *
 * ShazamSignature.create takes audio samples as ShortArray.
 * Format: PCM 16-Bit Little-Endian
 * Sample Rate: 16000 Hz, Mono
 * Returns: Base64 data URI string (data:audio/vnd.shazam.sig;base64,...)
 */
class ShazamSignature {
    init {
        try {
            System.loadLibrary("shazam_signature_jni")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun create(input: ShortArray): String
}
