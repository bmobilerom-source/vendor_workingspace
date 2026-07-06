package com.bmobile.workingspace.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Thin wrapper around [TopAppBar] shared by Working Space screens.
 *
 * Supports either a simple [onBack] callback (renders the default back arrow)
 * or a fully custom [navigationIcon] slot for screens that need a different
 * icon or extra actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingSpaceTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    scrolledContainerColor: Color = containerColor,
    navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = title,
                )
            }
        }
    },
) {
    val resolvedContainerColor =
        if (containerColor == Color.Unspecified) TopAppBarDefaults.topAppBarColors().containerColor
        else containerColor
    val resolvedScrolledColor =
        if (scrolledContainerColor == Color.Unspecified) resolvedContainerColor
        else scrolledContainerColor

    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        navigationIcon = navigationIcon,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = resolvedContainerColor,
            scrolledContainerColor = resolvedScrolledColor,
        ),
    )
}
