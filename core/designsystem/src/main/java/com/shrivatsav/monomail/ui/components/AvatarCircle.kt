package com.shrivatsav.monomail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import java.security.MessageDigest

data class AvatarUrls(
    val domainLogo: String?,
    val gravatar: String?
)

fun avatarUrlsForEmail(email: String): AvatarUrls {
    val normalized = email.trim().lowercase()
    val domain = normalized.substringAfterLast('@', "")
        .takeIf { it.isNotBlank() && it.contains('.') }
    if (domain == null) return AvatarUrls(null, null)
    val digest = MessageDigest.getInstance("MD5")
        .digest(normalized.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    return AvatarUrls(
        domainLogo = "https://www.google.com/s2/favicons?domain=$domain&sz=128",
        gravatar = "https://www.gravatar.com/avatar/$digest?d=404&s=128"
    )
}

@Composable
fun AvatarCircle(
    photoUrl: String?,
    displayName: String,
    size: Dp,
    textStyle: TextStyle,
    fallbackPhotoUrl: String? = null,
) {
    if (!photoUrl.isNullOrEmpty()) {
        val imageState = androidx.compose.runtime.remember(photoUrl, fallbackPhotoUrl) {
            androidx.compose.runtime.mutableStateOf<Any?>(photoUrl)
        }
        SubcomposeAsyncImage(
            model = imageState.value,
            contentDescription = displayName,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            loading = { InitialsAvatar(displayName, size, textStyle) },
            error = {
                if (imageState.value != fallbackPhotoUrl && !fallbackPhotoUrl.isNullOrEmpty()) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        imageState.value = fallbackPhotoUrl
                    }
                    InitialsAvatar(displayName, size, textStyle)
                } else {
                    InitialsAvatar(displayName, size, textStyle)
                }
            }
        )
    } else {
        InitialsAvatar(displayName, size, textStyle)
    }
}

@Composable
private fun InitialsAvatar(displayName: String, size: Dp, textStyle: TextStyle) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.firstOrNull()?.uppercase() ?: "?",
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.background
        )
    }
}
