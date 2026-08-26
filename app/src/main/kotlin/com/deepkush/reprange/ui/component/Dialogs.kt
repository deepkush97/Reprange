package com.deepkush.reprange.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

data class DialogButton(
    val label: String,
    val onClick: () -> Unit = {},
    val isPrimary: Boolean = false,
    val weight: Float? = null,
)

/**
 * One dialog shell per §6.2: platform dialog -> tonal surface in AlertDialog shape ->
 * centered icon, headlineSmall title, content slot, end-aligned FlowRow of buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    buttons: List<DialogButton> = emptyList(),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(Modifier.padding(24.dp)) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                content()
                FlowRow(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    buttons.forEach { b ->
                        TextButton(
                            onClick = {
                                b.onClick()
                                onDismiss()
                            },
                            modifier = if (b.weight != null) Modifier.weight(b.weight) else Modifier,
                        ) {
                            Text(
                                b.label,
                                color = if (b.isPrimary) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** OK/Cancel(/Reset); Reset gets weight(1f) so it sits far-left per §6.2. */
@Composable
fun ActionPromptDialog(
    title: String,
    message: String? = null,
    confirmLabel: String = "OK",
    dismissLabel: String? = "Cancel",
    resetLabel: String? = null,
    icon: ImageVector? = null,
    onConfirm: () -> Unit = {},
    onReset: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = title,
        icon = icon,
        buttons = buildList {
            if (resetLabel != null && onReset != null) add(DialogButton(resetLabel, onReset, weight = 1f))
            if (!dismissLabel.isNullOrBlank()) add(DialogButton(dismissLabel))
            add(DialogButton(confirmLabel, isPrimary = true, onClick = onConfirm))
        },
    ) {
        if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

/** List picker inside the dialog shell + imePadding (§6.2). */
@Composable
fun <T> ListDialog(
    title: String,
    items: List<T>,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(onDismiss = onDismiss, title = title) {
        LazyColumn(modifier = Modifier.height(320.dp)) {
            items(items) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(item)
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Text(labelOf(item), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Enum picker: RadioButton rows; row is clickable; select closes instantly then commits (§6.2). */
@Composable
fun <T : Enum<T>> EnumDialog(
    title: String,
    selected: T,
    options: List<T>,
    labelOf: @Composable (T) -> String,
    descriptionOf: ((T) -> String)? = null,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(onDismiss = onDismiss, title = title) {
        Column {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(option)
                            onDismiss()
                        }
                        .padding(vertical = 6.dp),
                ) {
                    RadioButton(selected = option == selected, onClick = null)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(labelOf(option), style = MaterialTheme.typography.bodyLarge)
                        if (descriptionOf != null) {
                            Text(
                                descriptionOf(option),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DialogFieldSpec(
    val initial: String,
    val label: String,
    val isMultiLine: Boolean = false,
    val isValid: (String) -> Boolean = { it.isNotBlank() },
)

/** Single or multi-field input; autofocus delayed 300ms (window attach); OK gated by validity; Done commits (§6.2). */
@Composable
fun TextFieldDialog(
    title: String,
    fields: List<DialogFieldSpec>,
    confirmLabel: String = "Save",
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val values = remember(fields) {
        mutableStateListOf<String>().apply { addAll(fields.map { it.initial }) }
    }
    val allValid = fields.indices.all { i -> fields[i].isValid(values[i]) }

    val focusRequesters = remember(fields) { List(fields.size) { FocusRequester() } }
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { focusRequesters.first().requestFocus() }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = title,
        buttons = listOf(
            DialogButton("Cancel"),
            DialogButton(
                confirmLabel,
                isPrimary = true,
                onClick = { if (allValid) onConfirm(values.toList()) },
            ),
        ),
    ) {
        Column(Modifier.imePadding()) {
            fields.forEachIndexed { index, field ->
                OutlinedTextField(
                    value = values[index],
                    onValueChange = { values[index] = it },
                    label = { Text(field.label) },
                    isError = !field.isValid(values[index]),
                    minLines = if (field.isMultiLine) 3 else 1,
                    maxLines = if (field.isMultiLine) 6 else 1,
                    singleLine = !field.isMultiLine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[index]),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
