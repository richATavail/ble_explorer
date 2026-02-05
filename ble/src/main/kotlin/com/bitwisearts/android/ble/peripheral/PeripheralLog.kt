package com.bitwisearts.android.ble.peripheral

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The type of log message.
  */
enum class LogType {
	/** Represents an event related to the BLE peripheral connection. */
	CONNECTION,

	/** A read request related event. */
	READ,

	/** A write request related event. */
	WRITE,

	/** A notification request related event. */
	NOTIFICATION,

	/**
	 * Any kind of logging event that deals with none of the other [LogType]s
	 * but still provides information about something pertinent to the
	 * peripheral.
	 */
	OPERATIONAL
}

/**
 * A log entry for the BLE peripheral.
 *
 * @property message
 *   The message to log
 * @property type
 *   The type of log message
 * @property timestamp
 *   The timestamp of the log message in milliseconds since the Unix Epoch UTC.
 *   Defaults to the [current time][System.currentTimeMillis].
 */
data class LogEntry(
	val message: String,
	val type: LogType,
	val timestamp: Long = System.currentTimeMillis()
) {
	/** This [LogEntry] as an escaped CSV string. */
	val asCsv: String
		get() = "$timestamp,$type,${escapeCsv(message)}"

	companion object {
		/**
		 * Escapes a string for proper CSV representation.
		 * - Wraps in quotes if it contains comma, quote, or newline
		 * - Doubles any quotes inside the string
		 *
		 * @param value
		 *   The string to escape
		 * @return
		 *   The escaped string
		 */
		fun escapeCsv(value: String): String {
			val needsEscaping = value.contains(',')
				|| value.contains('"')
				|| value.contains('\n')
				|| value.contains('\r')
			return if (needsEscaping) {
				"\"${value.replace("\"", "\"\"")}\""
			} else {
				value
			}
		}
	}
}

class PeripheralLog
{
	/**
	 * The activity log of the peripheral, tracking connection, reads and
	 * writes.
	 */
	private val _activity: MutableStateFlow<List<LogEntry>> =
		MutableStateFlow(listOf())

	/**
	 * The activity log of the peripheral, tracking connection, reads and
	 * writes.
	 */
	val activity: StateFlow<List<LogEntry>> = _activity.asStateFlow()

	/** Exportable CSV representation of the activity log. */
	val asCsv: String
		get() {
			val header = "timestamp,type,message"
			val entries = _activity.value.joinToString("\n") { it.asCsv }
			return "$header\n$entries"
		}

	/**
	 * Logs an activity entry to the activity log.
	 *
	 * @param entry
	 *   The string entry to log.
	 * @param logType
	 *   The [LogType] indicating what the logged entry is related to.
	 */
	fun logActivity(
		entry: String,
		logType: LogType
	) {
		val currentList = _activity.value
		val newList = currentList + LogEntry(entry, logType)
		_activity.value = newList
	}
}

