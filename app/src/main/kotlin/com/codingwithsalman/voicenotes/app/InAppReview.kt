package com.codingwithsalman.voicenotes.app

import android.app.Activity
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Launch the Google Play in-app review flow. Play decides whether the review card actually appears
 * (it is quota-limited) and never surfaces the result to us — so this is fire-and-forget and records
 * nothing, in keeping with Murmur's no-analytics stance (MUR-03). It suspends only for the
 * [requestReview] handshake; the launch itself returns a Task we intentionally don't await.
 */
suspend fun Activity.launchInAppReview() {
    val manager = ReviewManagerFactory.create(this)
    val info = manager.requestReview()
    manager.launchReviewFlow(this, info)
}
