package com.bitwisearts.android.explorer.ui.peripheral

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The Composable that displays the user interface for the Central role.
 *
 * @param viewModel
 *   The [CentralViewModel] that provides the data for this Composable.
 * @param modifier
 *   The [Modifier] to be applied to this Composable.
 *
 * @author Richard Arriaga
 */
@Composable
fun CentralView(
	modifier: Modifier = Modifier,
	viewModel: CentralViewModel = viewModel()
) {
	CentralViewContent(
		modifier
	)
}

@Composable
fun CentralViewContent(
	modifier: Modifier = Modifier
) {

}

/**
 * The [ViewModel]
 *
 * @author Richard Arriaga
 */
class CentralViewModel: ViewModel()
{
}