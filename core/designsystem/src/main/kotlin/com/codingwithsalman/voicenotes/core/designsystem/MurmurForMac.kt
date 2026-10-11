package com.codingwithsalman.voicenotes.core.designsystem

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Murmur for Mac, the desktop sibling (`projects/murmur`), downloaded and sold from the studio website.
 *
 * Nobody can install it from the phone they are reading this on, so the main action hands the link on
 * (the share sheet: email or message it to yourself, open it on the Mac) rather than opening a page whose
 * Download button does nothing useful here. `from=android` marks visits that came from this app, for
 * whenever the site keeps a visit log.
 *
 * The link goes to the page, never straight to a checkout: the Mac app is a separate product with its own
 * 14-day trial and purchase, and nothing in this app sells it.
 */
object MurmurForMac {
    const val URL = "https://tricodestudio.com/murmur?from=android"

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
