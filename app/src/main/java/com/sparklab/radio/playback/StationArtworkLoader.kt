package com.sparklab.radio.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sparklab.radio.BuildConfig
import com.sparklab.radio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/** Local branding is available without network access, including cold car reconnections. */
object SparkRadioArtwork {
    val uri: String = "android.resource://${BuildConfig.APPLICATION_ID}/${R.drawable.station_artwork}"
    fun isFallback(uri: String?): Boolean = uri == this.uri
}

/** Legacy Auto and notification artwork use the same failed-image fallback as the phone. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class StationArtworkLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    private val fallback: Bitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.drawable.station_artwork)
    }

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        Futures.immediateFuture(BitmapFactory.decodeByteArray(data, 0, data.size) ?: fallback)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future {
        if (SparkRadioArtwork.isFallback(uri.toString())) fallback
        else {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(512)
                .allowHardware(false)
                .error(R.drawable.station_artwork)
                .build()
            context.imageLoader.execute(request).drawable?.toBitmap() ?: fallback
        }
    }
}
