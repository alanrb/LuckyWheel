package io.github.alanrb.luckywheel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.alanrb.luckywheel.data.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes an image the app stored privately (logo, custom background) off the main thread. */
@Composable
fun rememberStoredImage(path: String?): State<ImageBitmap?> = produceState<ImageBitmap?>(null, path) {
    value = path?.let { withContext(Dispatchers.IO) { ImageStore.decode(it)?.asImageBitmap() } }
}
