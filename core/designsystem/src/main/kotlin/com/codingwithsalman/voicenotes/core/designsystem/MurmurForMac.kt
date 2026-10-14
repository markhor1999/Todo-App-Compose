package com.codingwithsalman.voicenotes.core.designsystem

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Murmur for Mac and Windows, the desktop sibling (`projects/murmur`). The Mac app is downloaded from the studio
 * website and the Windows app comes from the Microsoft Store ("Murmur Meeting Notes"); the page links both.
 *
 * Nobody can install it from the phone they are reading this on, so the main action hands the link on
 * (the share sheet: email or message it to yourself, open it on the computer) rather than opening a page whose
 * Download button does nothing useful here. `ref=android` marks visits that came from this app, for
 * whenever the site keeps a visit log (same convention as `ref=anoxhub`).
 *
 * The link goes to the page, never straight to a checkout: the desktop app is a separate product with its own
 * 14-day trial and purchase, and nothing in this app sells it.
 */
object MurmurForMac {
    const val URL = "https://tricodestudio.com/murmur?ref=android"

    /** Share sheet with a ready-made message carrying [URL]. */
    fun shareLink(context: Context) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.vn_mac_share_text, URL))
        }
        runCatching { context.startActivity(Intent.createChooser(send, null)) }
    }

    /** The page in the browser; a device with no browser does nothing rather than throwing. */
    fun openPage(context: Context) {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(view) }
    }
}
