package com.codingwithsalman.voicenotes.app

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Launch the Google Play in-app review flow. Play decides whether the review card actually appears
 * (it is quota-limited) and never surfaces the result to us — so this records nothing, in keeping
 * with Murmur's no-analytics stance (MUR-03).
 *
 * Both halves are awaited. The earlier version suspended only for [requestReview] and left the
 * launch Task unawaited, so the caller's "already asked" flag was written the moment the flow was
 * *dispatched* rather than run — meaning a throw inside the launch still burned the single
 * once-per-install attempt. Awaiting [launchReview] lets that failure propagate, so the caller
 * leaves the flag unset and retries on a later foreground.
 */
suspend fun Activity.launchInAppReview() {
    val manager = ReviewManagerFactory.create(this)
    val info = manager.requestReview()
    manager.launchReview(this, info)
}
