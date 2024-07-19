package com.bitwisearts.android.explorer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.advertisement.AdvertisementData
import com.bitwisearts.android.ble.advertisement.FlagsData
import com.bitwisearts.android.ble.advertisement.ManufacturerData
import com.bitwisearts.android.ble.connection.BleDeviceManager
import com.bitwisearts.android.ble.utility.asCompactHex
import com.bitwisearts.android.explorer.DeviceRoute
import com.bitwisearts.android.explorer.R
import com.bitwisearts.android.explorer.ui.theme.Ble_explorerTheme
import java.util.UUID

/**
 * A [Composable] view of an [Advertisement]
 *
 * @param navController
 *   The [NavController] used for app navigation.
 * @param advertisement
 *   The [Advertisement] to display.
 */
@Composable
fun AdvertisementView (
	navController: NavController,
	advertisement: Advertisement)
{
	var isExpanded by remember { mutableStateOf(false) }
	Column(
		modifier = Modifier
			.padding(16.dp)
			.fillMaxSize()
//			.background(Color(0xFFE6E1E6))
			.clickable { isExpanded = !isExpanded }
			.border(2.dp, MaterialTheme.colorScheme.primary)
			.padding(10.dp))
	{
		if (isExpanded)
		{
			AdvertisementExpandedWithExploreButton(
				address = advertisement.address,
				deviceName = advertisement.deviceName,
				rssi = advertisement.rssi,
				txPower = advertisement.txPower,
				serviceUUIDs = advertisement.serviceUUIDs,
				scanRecordBytes = advertisement.populatedAdvertisementBytes,
				advertisementData = advertisement.advertisementData)
			{
				BleDeviceManager.advertisements[advertisement.address] =
					advertisement
				BleDeviceManager.selectedAddress.value = advertisement.address
				DeviceRoute.navigate(navController, advertisement.address)
			}
		}
		else
		{
			AdvertisementCollapsed(
				address = advertisement.address,
				deviceName = advertisement.deviceName)
		}
	}
}

/**
 * The [Composable] view of an [Advertisement] that is collapsed; showing only
 * the provided [Advertisement.address] and [Advertisement.deviceName].
 */
@Composable
fun ColumnScope.AdvertisementCollapsed(
	address: String,
	deviceName: String
) {
	Row {
		Text(
			text = stringResource(id = R.string.mac_address),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		Text(text = address)
	}

	Row {
		Text(
			text = stringResource(id = R.string.device_name),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		Text(text = deviceName)
	}
}

/**
 * The [Composable] view of an [Advertisement] that is expanded; showing
 * the provided:
 * * [Advertisement.address]
 * * [Advertisement.deviceName]
 * * [Advertisement.rssi]
 * * [Advertisement.txPower]
 * * [Advertisement.serviceUUIDs]
 * * [Advertisement.scanRecordBytes]
 * * [Advertisement.advertisementData]
 */
@Composable
fun ColumnScope.AdvertisementExpandedWithExploreButton(
	address: String,
	deviceName: String,
	rssi: Int,
	txPower: Int,
	serviceUUIDs: Set<UUID>,
	scanRecordBytes: ByteArray,
	advertisementData: List<AdvertisementData>,
	onButtonClick: () -> Unit = {}
) {
	AdvertisementExpanded(
		address,
		deviceName,
		rssi,
		txPower,
		serviceUUIDs,
		scanRecordBytes,
		advertisementData
	)
	Button(onClick = onButtonClick) {
		Text(text = stringResource(id = R.string.explore))
	}
}

/**
 * The [Composable] view of an [Advertisement] that is expanded; showing
 * the provided:
 * * [Advertisement.address]
 * * [Advertisement.deviceName]
 * * [Advertisement.rssi]
 * * [Advertisement.txPower]
 * * [Advertisement.serviceUUIDs]
 * * [Advertisement.scanRecordBytes]
 * * [Advertisement.advertisementData]
 */
@Composable
fun ColumnScope.AdvertisementExpanded(
	address: String,
	deviceName: String,
	rssi: Int,
	txPower: Int,
	serviceUUIDs: Set<UUID>,
	scanRecordBytes: ByteArray,
	advertisementData: List<AdvertisementData>
) {
	Row {
		Text(
			text = stringResource(id = R.string.mac_address),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		SelectionContainer {
			Text(text = address)
		}
	}

	Row {
		Text(
			text = stringResource(id = R.string.device_name),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		Text(text = deviceName)
	}
	Row {
		Text(
			text = stringResource(id = R.string.rssi),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		Text(text = rssi.toString())
	}
	Row {
		Text(
			text = stringResource(id = R.string.tx_power),
			fontWeight = FontWeight.Bold)
		Spacer(modifier = Modifier.padding(horizontal = 4.dp))
		Text(text = txPower.toString())
	}
	if (serviceUUIDs.isNotEmpty())
	{
		Text(
			text = stringResource(id = R.string.service_uuid),
			fontWeight = FontWeight.Bold)
		serviceUUIDs.forEach {
			Row {
				SelectionContainer {
					Text(text = it.toString())
				}
			}
		}
	}

	Column(modifier = Modifier.padding(vertical = 5.dp))
	{
		advertisementData.forEach { AdvertisementDataView(it) }
	}
	Text(
		text = stringResource(id = R.string.scan_record_bytes),
		fontWeight = FontWeight.Bold)
	SelectionContainer {
		Text(text = "0x${scanRecordBytes.asCompactHex}")
	}
}

/**
 * The [Composable] view for showing the provided [AdvertisementData].
 */
@Composable
fun ColumnScope.AdvertisementDataView (data: AdvertisementData)
{
	Row {
		Text(
			modifier = Modifier.padding(end = 3.dp),
			text = data.type.adName,
			fontWeight = FontWeight.Bold)
		Text(text = "(0x${Integer.toHexString(data.type.adByte)})")
	}
	Row {
		SelectionContainer {
			Text(
				text = stringResource(id = R.string.adv_data_bytes),
				fontWeight = FontWeight.Bold)
		}
	}
	Row {
		SelectionContainer {
			Text(text = "0x${data.data.asCompactHex}")
		}
	}
}

@Preview(showBackground = true)
@Composable
fun AdvertisementCollapsedPreview()
{
	Ble_explorerTheme {
		Column {
			AdvertisementCollapsed(
				"00:11:22:AA:BB:CC",
				"My Name")
		}
	}
}

@Preview(showBackground = true)
@Composable
fun AdvertisementExpandedPreview()
{
	Ble_explorerTheme {
		Column {
			AdvertisementExpandedWithExploreButton(
				"00:11:22:AA:BB:CC",
				"My Name",
				-5,
				10,
				setOf(UUID.randomUUID(), UUID.randomUUID()),
				byteArrayOf(0x01, 0x6A, 0xFF.toByte(), 0x44, 0x22, 0x1E),
				listOf(
					ManufacturerData(byteArrayOf(3, 4, 22, -44)),
					FlagsData(byteArrayOf(1, -1, 22, -85)))
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun AdvertisementDataPreview()
{
	Ble_explorerTheme {
		Column {
			AdvertisementDataView(
				ManufacturerData(byteArrayOf(3, 4, 22, -44)))
		}
	}
}