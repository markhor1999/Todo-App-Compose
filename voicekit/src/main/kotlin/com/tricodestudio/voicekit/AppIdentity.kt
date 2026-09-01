package com.tricodestudio.voicekit

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * Who the host app is, as the platform reports it — the pair a live licence key is bound to.
 *
 * Read from `PackageManager` rather than from `BuildConfig`, deliberately: a constant a developer
 * can edit binds a key to whatever they typed, which binds nothing at all.
 */
internal data class AppIdentity(
    val applicationId: String,
    /** SHA-256 of the signing certificate, uppercase hex, no separators. Empty if unreadable. */
    val certificateSha256: String,
) {
    companion object {

        fun of(context: Context): AppIdentity = AppIdentity(
            applicationId = context.packageName,
            certificateSha256 = signingCertificateSha256(context),
        )

        /**
         * The app's signing certificate digest.
         *
         * Uses the v2+ `signingInfo` API on P and above and the deprecated `GET_SIGNATURES` below
         * it. Both are read-only calls on the app's *own* package, which is why this needs no
         * permission and cannot be used to inspect anything else on the device — worth stating
         * because a security reviewer at a customer will ask what an SDK is doing with
         * PackageManager.
         *
         * Returns empty rather than throwing when the platform will not answer. An unreadable
         * certificate must not stop an app from starting; it degrades binding to applicationId only.
         */
        private fun signingCertificateSha256(context: Context): String = try {
            val pm = context.packageManager
            val pkg = context.packageName
            val signatures: Array<Signature>? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                    val signing = info.signingInfo
                    when {
                        signing == null -> null
                        signing.hasMultipleSigners() -> signing.apkContentsSigners
                        // The rotation-aware list; the current signer is the last entry.
                        else -> signing.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
                }

            val current = signatures?.lastOrNull() ?: return ""
            MessageDigest.getInstance("SHA-256")
                .digest(current.toByteArray())
                .joinToString("") { "%02X".format(it) }
        } catch (t: Throwable) {
            ""
        }
    }
}
