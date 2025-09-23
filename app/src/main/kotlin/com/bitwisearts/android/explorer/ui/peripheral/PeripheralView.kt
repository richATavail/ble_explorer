package com.bitwisearts.android.explorer.ui.peripheral

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitwisearts.android.explorer.ble.peripheral.SampleBlePeripheral
import com.bitwisearts.android.explorer.ui.theme.BleExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The Composable that displays the user interface for a [SampleBlePeripheral].
 *
 * @param viewModel
 *   The [PeripheralViewModel] that provides the data for this Composable.
 * @param modifier
 *   The [Modifier] to be applied to this Composable.
 *
 * @author Richard Arriaga
 */
@Composable
fun PeripheralView(
    modifier: Modifier = Modifier,
    viewModel: PeripheralViewModel = viewModel()
) {
    var isPeripheralActive by remember { mutableStateOf(false) }

    PeripheralViewContent(
        isActive = isPeripheralActive,
        writeValueFlow = viewModel.peripheral.writeValueFlow,
        onReadTextChanged = viewModel::setReadCharacteristicValue,
        onTogglePeripheral = {
            if (!isPeripheralActive) {
                val success = viewModel.startPeripheral()
                isPeripheralActive = success
            } else {
                viewModel.stopPeripheral()
                isPeripheralActive = false
            }
        },
        onSendNotification = viewModel::sendNotification,
        modifier = modifier
    )
}

/**
 * Content of the peripheral view that doesn't depend on the ViewModel.
 * This allows for easier testing and previewing.
 *
 * @param isActive
 *   Whether the peripheral is currently active
 * @param onTogglePeripheral
 *   Callback when peripheral is toggled on/off
 * @param onSendNotification
 *   Callback when a notification is sent
 * @param modifier
 *   The modifier to apply to this composable
 */
@Composable
private fun PeripheralViewContent(
    isActive: Boolean,
    writeValueFlow: StateFlow<ByteArray>,
    onTogglePeripheral: () -> Unit,
    onReadTextChanged: (String) -> Unit,
    onSendNotification: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BLE Peripheral",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Peripheral Status",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = if (isActive) "Active" else "Inactive",
                    color = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTogglePeripheral,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = if (!isActive) "Start Peripheral" else "Stop Peripheral"
                        )
                    }
                }
            }
        }

        if (isActive)
        {
            var notificationText by remember {
                mutableStateOf("Hello from BLE!")
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Send Notification",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = notificationText,
                        onValueChange = {
                            notificationText = it
                        },
                        label = { Text("Notification message") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSendNotification(notificationText)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Send")
                    }
                }
            }
            var readText by remember {
                mutableStateOf("0")
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Update Read Characteristic",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = readText,
                        onValueChange = { newValue ->
                            // Only accept digits or empty string
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                readText = newValue
                                onReadTextChanged(newValue)
                            }
                        },
                        label = { Text("Read value (numbers only)") },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val writeValue by writeValueFlow.collectAsStateWithLifecycle()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Last Write Value",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = writeValue.decodeToString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Preview of the inactive peripheral state
 */
@Preview(showBackground = true, name = "Peripheral Inactive")
@Composable
private fun PeripheralViewInactivePreview()
{
    BleExplorerTheme {
        PeripheralViewContent(
            isActive = false,
            writeValueFlow = MutableStateFlow(ByteArray(0)),
            onReadTextChanged = {},
            onTogglePeripheral = {},
            onSendNotification = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Preview of the active peripheral state
 */
@Preview(showBackground = true, name = "Peripheral Active")
@Composable
private fun PeripheralViewActivePreview()
{
    BleExplorerTheme {
        PeripheralViewContent(
            isActive = true,
            writeValueFlow = MutableStateFlow(ByteArray(0)),
            onTogglePeripheral = {},
            onReadTextChanged = {},
            onSendNotification = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The [ViewModel] for the [PeripheralView].
 *
 * @author Richard Arriaga
 */
class PeripheralViewModel: ViewModel()
{
    /**
     * The [SampleBlePeripheral] being controlled by this [ViewModel].
     */
    val peripheral: SampleBlePeripheral = SampleBlePeripheral()

    /**
     * [Starts][SampleBlePeripheral.startPeripheral] the [peripheral] making it
     * available for discovery.
     *
     * @return
     *   `true` if the peripheral started successfully, `false` otherwise
     */
    fun startPeripheral(): Boolean {
        return peripheral.startPeripheral()
    }

    /**
     * [Stops][SampleBlePeripheral.stopPeripheral] the [peripheral].
     */
    fun stopPeripheral() {
        peripheral.stopPeripheral()
    }

    /**
     * Sends a notification to connected devices.
     *
     * @param message The message to send as a notification
     */
    fun sendNotification(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            peripheral.sendNotification(message)
        }
    }

    /**
     * Sets the value of the read characteristic.
     *
     * @param value
     *   The value to set for the read characteristic
     */
    fun setReadCharacteristicValue(value: String) {
        try
        {
            peripheral.setReadCharacteristicValue(value.toIntOrNull() ?: 0)
        }
        catch (e: IllegalArgumentException)
        {
            Log.e(
                "PeripheralViewModel",
                "Error setting read characteristic value: $value", e)
            // If the value is out of range, set it to 0
            0
        }
    }

    override fun onCleared()
    {
        stopPeripheral()
    }
}