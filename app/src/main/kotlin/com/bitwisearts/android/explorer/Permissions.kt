package com.bitwisearts.android.explorer

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * An abstract representation of a [permission][Manifest.permission] required
 * for this app to run.
 */
sealed interface ExplorerPermission
{
	/** The target [Manifest.permission]. */
	val permission: String

	/**
	 * The resource id of the description of the [permission] and why it is
	 * needed.
	 */
	val description: Int

	/**
	 * The resource id of the description of how to enable the permission.
	 */
	val declinedText: Int

	@Composable
	fun PermissionDialog(
		isPermanentlyDeclined: Boolean,
		onDismiss: () -> Unit,
		requestPermission: () -> Unit,
		gotoSettings: () -> Unit,
		modifier: Modifier
	) {
		AlertDialog(
			modifier = modifier,
			onDismissRequest = onDismiss,
			text = {
				Text(
					text = stringResource(id =
					if (isPermanentlyDeclined)
					{
						declinedText
					}
					else
					{
						description
					}),
					modifier = Modifier.clickable {
						if (isPermanentlyDeclined)
						{
							gotoSettings()
						}
						else
						{
							requestPermission()
						}
					})
			},
			confirmButton = {
				Text(
					text = stringResource(id =
						if (isPermanentlyDeclined)
						{
							R.string.go_to_settings
						}
						else
						{
							R.string.ok
						}),
					modifier = Modifier.clickable {
						if (isPermanentlyDeclined)
						{
							gotoSettings()
						}
						else
						{
							requestPermission()
						}
					})
			})
	}

	companion object
	{
		/**
		 * Answer the appropriate [ExplorerPermission] for the given
		 * [Manifest.permission] String name or `null` if not found.
		 */
		operator fun get (permission: String): ExplorerPermission? =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			{
				when (permission)
				{
					ScanPermission.permission -> ScanPermission
					ConnectPermission.permission -> ConnectPermission
					else -> null
				}
			}
			else
			{
				when (permission)
				{
					CoarsePermission.permission -> CoarsePermission
					FinePermission.permission -> FinePermission
					else -> null
				}
			}
	}
}

/**
 * The [ExplorerPermission] for the permission to
 * [scan][Manifest.permission.BLUETOOTH_SCAN] for BLE devices as of Android API
 * 31.
 *
 * @author Richard Arriaga
 */
object ScanPermission: ExplorerPermission
{
	@RequiresApi(Build.VERSION_CODES.S)
	override val permission: String = Manifest.permission.BLUETOOTH_SCAN

	override val description: Int = R.string.scan_permission_reason

	override val declinedText: Int = R.string.scan_permission_declined
}

/**
 * The [ExplorerPermission] for the permission to
 * [connecting][Manifest.permission.BLUETOOTH_CONNECT] to BLE devices as of
 * Android API 31.
 *
 * @author Richard Arriaga
 */
object ConnectPermission: ExplorerPermission
{
	@RequiresApi(Build.VERSION_CODES.S)
	override val permission: String = Manifest.permission.BLUETOOTH_CONNECT

	override val description: Int = R.string.connect_permission_reason

	override val declinedText: Int = R.string.connect_permission_declined
}

/**
 * The [Manifest.permission.ACCESS_COARSE_LOCATION] [ExplorerPermission] for
 * the permission to scan for and connect to BLE devices before Android API 31.
 *
 * @author Richard Arriaga
 */
object CoarsePermission: ExplorerPermission
{
	override val permission: String = Manifest.permission.ACCESS_COARSE_LOCATION

	override val description: Int = R.string.coarse_location_permission_reason

	override val declinedText: Int = R.string.coarse_permission_declined
}

/**
 * The [Manifest.permission.ACCESS_FINE_LOCATION] [ExplorerPermission] for
 * the permission to scan for and connect to BLE devices before Android API 31.
 *
 * @author Richard Arriaga
 */
object FinePermission: ExplorerPermission
{
	override val permission: String = Manifest.permission.ACCESS_FINE_LOCATION

	override val description: Int = R.string.fine_location_permission_reason

	override val declinedText: Int = R.string.fine_permission_declined
}