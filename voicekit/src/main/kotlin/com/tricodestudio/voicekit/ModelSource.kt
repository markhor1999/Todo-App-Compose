package com.tricodestudio.voicekit

/**
 * Where model bytes come from, and what has to be sent to get them.
 *
 * This is the seam the build plan calls the highest-leverage control: **gate the model download, not
 * the code.** A licence check inside an AAR can be stripped in an afternoon. Re-hosting 274 MB of
 * weights, keeping them online, and shipping that to your own users is a different kind of effort —
 * and it is the one that makes casual piracy not worth doing.
 *
 * It does not make piracy impossible. The models themselves (Whisper, sherpa-onnx) are open source
 * and freely downloadable; what is sold is the productized integration, not the weights. See
 * `LICENSING.md`.
 */
@VoiceKitInternalApi
public interface ModelSource {

    /** Resolves [file] to a URL and the headers needed to fetch it. */
    public fun resolve(spec: AsrModelSpec, file: ModelFileSpec): ResolvedDownload
}

/** A URL plus whatever headers the request needs. */
@VoiceKitInternalApi
public data class ResolvedDownload(
    public val url: String,
    public val headers: Map<String, String> = emptyMap(),
)

/**
 * Fetches straight from the public mirrors named in [ModelCatalog].
 *
 * This is what ships pre-revenue and it gates nothing — the URLs are public and always were. Kept as
 * a real implementation rather than a stub because it is also the correct fallback if the licence
 * endpoint is ever down: a customer's users must not lose the ability to install a model because our
 * infrastructure had a bad afternoon.
 */
@VoiceKitInternalApi
public object DirectModelSource : ModelSource {
    override fun resolve(spec: AsrModelSpec, file: ModelFileSpec): ResolvedDownload =
        ResolvedDownload(file.url)
}

/**
 * Fetches through the VoiceKit distribution endpoint, authenticated with the licence key.
 *
 * The endpoint **302-redirects** to a short-lived signed URL on object storage rather than returning
 * JSON to parse or proxying the bytes itself. Three things fall out of that:
 *
 * - the SDK parses nothing, so there is no JSON dependency in the download path;
 * - our server never sits in the path of a 274 MB transfer, so it stays cheap and cannot be the
 *   bottleneck;
 * - the signed URL carries its own authorization in the query string, so the `Authorization` header
 *   being dropped on the cross-host redirect (which is correct behaviour) does not matter.
 *
 * @param licenseKey sent as a bearer token; the endpoint decides what it is entitled to.
 * @param endpoint base URL, no trailing slash.
 */
@VoiceKitInternalApi
public class GatedModelSource(
    private val licenseKey: String,
    private val endpoint: String,
) : ModelSource {

    override fun resolve(spec: AsrModelSpec, file: ModelFileSpec): ResolvedDownload =
        ResolvedDownload(
            url = "$endpoint/v1/models/${spec.id}/${file.fileName}",
            headers = mapOf(
                "Authorization" to "Bearer $licenseKey",
                // Lets the endpoint refuse a build whose SDK is too old to understand a future
                // response, rather than failing in a way the developer cannot diagnose.
                "X-VoiceKit-Version" to VoiceKitBuild.VERSION,
            ),
        )
}

/** Build identity, sent on distribution requests. */
@VoiceKitInternalApi
public object VoiceKitBuild {
    public const val VERSION: String = "0.1.0"
}
