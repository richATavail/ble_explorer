package com.bitwisearts.android.explorer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

/**
 * A [ViewModel] used for stateful management of the [MainActivity]'s view. This
 * includes:
 *  * Permissions
 *
 * @author Richard Arriaga
 */
class MainViewModel: ViewModel()
{
	/**
	 * The list of [ExplorerPermission] that need to be requested of the user.
	 */
	val permissionsToRequest = mutableStateListOf<ExplorerPermission>()

	/** The [ExplorerPermission] required to be granted for this app. */
	val permissions =
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
		{
			permissions28
		}
		else
		{
			permissions31
		}

	val permissionNames get() = permissions.map { it.permission }.toTypedArray()

	/**
	 * Process the result of a request to grant an [ExplorerPermission] by the
	 * user.
	 *
	 * @param permission
	 *   The requested [ExplorerPermission] to process.
	 * @param isGranted
	 *   `true` if the [permission] is granted; `false` otherwise.
	 */
	fun processPermissionResult (
		permission: ExplorerPermission,
		isGranted: Boolean)
	{
		if (!isGranted && !permissionsToRequest.contains(permission))
			permissionsToRequest.add(permission)
	}

	companion object
	{
		/**
		 * The set of [permissions][ExplorerPermission] required for API 31
		 * and above.
		 *
		 * **NOTE** Based on the [documentation](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions#declare-android11-or-lower),
		 * [FinePermission] shouldn't be required for API 31 and above, but this
		 * seems to not work in practice. There is indication that based on
		 * internet searches, this might vary by device manufacturer/model.
		 */
		@RequiresApi(Build.VERSION_CODES.S)
		val permissions31 =
			mutableSetOf(ConnectPermission, ScanPermission, FinePermission)

		/**
		 * The set of [permissions][ExplorerPermission] required for
		 * API 28 - API 30.
		 */
		val permissions28 = mutableSetOf(FinePermission, CoarsePermission)
	}
}