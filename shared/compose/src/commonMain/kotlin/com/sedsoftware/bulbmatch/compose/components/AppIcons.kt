package com.sedsoftware.bulbmatch.compose.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bulbmatch.shared.compose.generated.resources.Res
import bulbmatch.shared.compose.generated.resources.ic_arrow_back
import bulbmatch.shared.compose.generated.resources.ic_camera_off
import bulbmatch.shared.compose.generated.resources.ic_cancel
import bulbmatch.shared.compose.generated.resources.ic_check_circle
import bulbmatch.shared.compose.generated.resources.ic_chevron_right
import bulbmatch.shared.compose.generated.resources.ic_clear
import bulbmatch.shared.compose.generated.resources.ic_close
import bulbmatch.shared.compose.generated.resources.ic_delete
import bulbmatch.shared.compose.generated.resources.ic_edit
import bulbmatch.shared.compose.generated.resources.ic_error
import bulbmatch.shared.compose.generated.resources.ic_history
import bulbmatch.shared.compose.generated.resources.ic_info
import bulbmatch.shared.compose.generated.resources.ic_language
import bulbmatch.shared.compose.generated.resources.ic_lightbulb
import bulbmatch.shared.compose.generated.resources.ic_mail
import bulbmatch.shared.compose.generated.resources.ic_menu_book
import bulbmatch.shared.compose.generated.resources.ic_open_in_new
import bulbmatch.shared.compose.generated.resources.ic_overflow
import bulbmatch.shared.compose.generated.resources.ic_palette
import bulbmatch.shared.compose.generated.resources.ic_photo_camera
import bulbmatch.shared.compose.generated.resources.ic_photo_library
import bulbmatch.shared.compose.generated.resources.ic_restart_alt
import bulbmatch.shared.compose.generated.resources.ic_save
import bulbmatch.shared.compose.generated.resources.ic_search
import bulbmatch.shared.compose.generated.resources.ic_settings
import bulbmatch.shared.compose.generated.resources.ic_shield
import bulbmatch.shared.compose.generated.resources.ic_source
import bulbmatch.shared.compose.generated.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal object AppIcons {
    val ArrowBack = Res.drawable.ic_arrow_back
    val CameraOff = Res.drawable.ic_camera_off
    val Cancel = Res.drawable.ic_cancel
    val CheckCircle = Res.drawable.ic_check_circle
    val ChevronRight = Res.drawable.ic_chevron_right
    val Clear = Res.drawable.ic_clear
    val Close = Res.drawable.ic_close
    val Delete = Res.drawable.ic_delete
    val Edit = Res.drawable.ic_edit
    val Error = Res.drawable.ic_error
    val History = Res.drawable.ic_history
    val Info = Res.drawable.ic_info
    val Language = Res.drawable.ic_language
    val Lightbulb = Res.drawable.ic_lightbulb
    val Mail = Res.drawable.ic_mail
    val MenuBook = Res.drawable.ic_menu_book
    val MoreVert = Res.drawable.ic_overflow
    val OpenInNew = Res.drawable.ic_open_in_new
    val Palette = Res.drawable.ic_palette
    val PhotoCamera = Res.drawable.ic_photo_camera
    val PhotoLibrary = Res.drawable.ic_photo_library
    val RestartAlt = Res.drawable.ic_restart_alt
    val Save = Res.drawable.ic_save
    val Search = Res.drawable.ic_search
    val Settings = Res.drawable.ic_settings
    val Shield = Res.drawable.ic_shield
    val Source = Res.drawable.ic_source
    val Warning = Res.drawable.ic_warning
}

@Composable
internal fun AppIcon(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
