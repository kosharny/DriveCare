package com.example.drivecare.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.example.drivecare.R
import java.io.File

@Composable
fun VehiclePhoto(photo: String, vehicleName: String, modifier: Modifier = Modifier) {
    val file = File(LocalContext.current.filesDir, "attachments/$photo")
    var expanded by rememberSaveable(photo) { mutableStateOf(false) }
    val windowHeight =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val description = stringResource(R.string.view_vehicle_photo, vehicleName)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val photoHeight = minOf(maxWidth * .75f, 360.dp, windowHeight * .55f)
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier =
                Modifier.fillMaxWidth().height(photoHeight).semantics {
                    contentDescription = description
                }
        ) {
            Box {
                PhotoImage(file, ContentScale.Crop, Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = .6f),
                    contentColor = Color.White
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_expand_photo),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            stringResource(R.string.open_vehicle_photo),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        vehicleName,
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.close_vehicle_photo), color = Color.White)
                    }
                }
                PhotoImage(file, ContentScale.Fit, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun PhotoImage(file: File, scale: ContentScale, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = file,
        contentDescription = null,
        contentScale = scale,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
        },
        error = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_car),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.vehicle_photo_error))
                }
            }
        }
    )
}
