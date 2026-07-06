package com.liferlighdow.iteration.ui.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.getWidgetPhoto

@Composable
fun PhotoWidget(widget: WidgetModel, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val widgetUpdateSignal by viewModel.widgetUpdateSignal.collectAsState()
    val photo by remember(widget.id, widgetUpdateSignal) { mutableStateOf<Bitmap?>(viewModel.getWidgetPhoto(widget.id)) }
    val isWide = (widget.type as? WidgetType.Photo)?.isWide ?: false
    val aspectRatio = if (isWide) 2.1f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(
                    bitmap = photo!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.widget_photo), 
                        color = Color.White.copy(alpha = 0.6f), 
                        style = MaterialTheme.typography.labelSmall.withGlassShadow()
                    )
                }
            }
        }
    }
}
