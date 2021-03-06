package com.zeapo.pwdstore

import android.net.Uri

import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException

/**
 * A single entry in password store.
 */
class PasswordEntry(private val content: String) {

    var extraContent: String? = null
        private set
    val password: String
    val username: String
    val totpSecret: String?
    val hotpSecret: String?
    val hotpCounter: Long?
    private var isIncremented = false

    @Throws(UnsupportedEncodingException::class)
    constructor(os: ByteArrayOutputStream) : this(os.toString("UTF-8")) {
    }

    init {
        val passContent = content.split("\n".toRegex(), 2).toTypedArray()
        password = passContent[0]
        totpSecret = findTotpSecret(content)
        hotpSecret = findHotpSecret(content)
        hotpCounter = findHotpCounter(content)
        extraContent = findExtraContent(passContent)
        username = findUsername()
    }

    fun hasExtraContent(): Boolean {
        return extraContent!!.length != 0
    }

    fun hasUsername(): Boolean {
        return username != null
    }

    fun hasTotp(): Boolean {
        return totpSecret != null
    }

    fun hasHotp(): Boolean {
        return hotpSecret != null && hotpCounter != null
    }

    fun hotpIsIncremented(): Boolean {
        return isIncremented
    }

    fun incrementHotp() {
        for (line in content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (line.startsWith("otpauth://hotp/")) {
                extraContent = extraContent!!.replaceFirst("counter=[0-9]+".toRegex(), "counter=" + java.lang.Long.toString(hotpCounter!! + 1))
                isIncremented = true
            }
        }
    }

    private fun findUsername(): String {
        val extraLines = extraContent!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in extraLines) {
            for (field in USERNAME_FIELDS) {
                if (line.toLowerCase().startsWith("$field:")) {
                    return line.split(": *".toRegex(), 2).toTypedArray()[1]
                }
            }
        }
        return ""
    }

    private fun findTotpSecret(decryptedContent: String): String? {
        for (line in decryptedContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (line.startsWith("otpauth://totp/")) {
                return Uri.parse(line).getQueryParameter("secret")
            }
        }
        return null
    }

    private fun findHotpSecret(decryptedContent: String): String? {
        for (line in decryptedContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (line.startsWith("otpauth://hotp/")) {
                return Uri.parse(line).getQueryParameter("secret")
            }
        }
        return null
    }

    private fun findHotpCounter(decryptedContent: String): Long? {
        for (line in decryptedContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (line.startsWith("otpauth://hotp/")) {
                return java.lang.Long.parseLong(Uri.parse(line).getQueryParameter("counter")!!)
            }
        }
        return null
    }

    private fun findExtraContent(passContent: Array<String>): String {
        val extraContent = if (passContent.size > 1) passContent[1] else ""
        // if there is a HOTP URI, we must return the extra content with the counter incremented
        return if (hasHotp()) {
            extraContent.replaceFirst("counter=[0-9]+".toRegex(), "counter=" + java.lang.Long.toString(hotpCounter!!))
        } else extraContent
    }

    companion object {

        private val USERNAME_FIELDS = arrayOf("login", "username")
    }
}
