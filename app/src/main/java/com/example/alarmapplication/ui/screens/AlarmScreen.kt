package com.example.alarmapplication.ui.screens

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmapplication.AlarmActivity
import com.example.alarmapplication.MainActivity
import com.example.alarmapplication.alarm_View_Models.DaysOfWeekViewModel
import com.example.alarmapplication.model.Alarm
import com.example.alarmapplication.ui.components.AlarmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale



/**
 * Основной экран приложения будильник.
 *
 * Дисплей для настройки и просмотра будильников. Позволяет пользователям выбирать время для будильника,
 * просматривать список установленных будильников и добавлять новые с помощью диалогового окна выбора времени.
 * Также поддерживает показ уведомлений с выбранным временем.
 *
 * @param daysOfWeekViewModel Модель представления, отвечающая за управление днями недели.
 */
@SuppressLint("MutableCollectionMutableState")
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AlarmScreen(
    daysOfWeekViewModal: DaysOfWeekViewModel = viewModel()
) {
    var showTimePicker by remember { mutableStateOf(false) }

    val state = rememberTimePickerState()
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()
    val context = LocalContext.current
    var cal: Calendar
    val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val alarms = remember { MutableStateFlow(listOf<Alarm>()) }
    val noteList by remember { alarms }.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.Center,
    ) {
        items(alarms.value) { alarm ->
            AlarmItem(alarm = alarm)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        Alignment.BottomCenter
    ) {
        FloatingActionButton(
            onClick = { showTimePicker = true },
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 1.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add by")
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = {
                cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, state.hour)
                cal.set(Calendar.MINUTE, state.minute)
                cal.isLenient = false
                snackScope.launch {
                    snackState.showSnackbar("Entered time: ${formatter.format(cal.time)}")
                }

                if (canScheduleExactAlarms(context)) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmClockInfo: AlarmManager.AlarmClockInfo = AlarmManager.AlarmClockInfo(
                        cal.timeInMillis,
                        getAlarmPendingIntent(context)
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, getAlarmActionPending(context))
                    makeToast(context, simpleDateFormat.format(cal.time))

                    val days = daysOfWeekViewModal.getDays()
                    var _days = ""


                    for(i in 0..<days.size) {
                        _days += days[i] + " "
                    }

                    val newList = ArrayList(noteList)
                    newList.add(
                        Alarm(
                            simpleDateFormat.format(cal.time),
                            _days,
                            true
                        )
                    )
                    alarms.value = newList

                }
                showTimePicker = false
            })
        {
            TimePicker(state = state)
        }


    }

}

/**
 * Создает PendingIntent для будильника, который запускает главную активность при срабатывании будильника.
 *
 * @param context Контекст приложения.
 * @return Возвращает PendingIntent для активации главной активности.
 */
private fun getAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * Создает PendingIntent для действия будильника.
 *
 * @param context Контекст приложения.
 * @return Возвращает PendingIntent для действия будильника.
 */
private fun getAlarmActionPending(context: Context): PendingIntent {
    val intent = Intent(context, AlarmActivity::class.java)
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}


/**
 * Проверяет, может ли приложение запланировать точные будильники на устройстве.
 *
 * Эта функция необходима для корректной работы будильников на Android версий S и выше, где
 * требуются специальные разрешения для точной установки будильников.
 *
 * @param context Контекст приложения.
 * @return Возвращает true, если установка точных будильников возможна, иначе false.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun canScheduleExactAlarms(context: Context): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    return alarmManager?.canScheduleExactAlarms() ?: false
}

/*@Composable Если будет не впадлу, то можо сделать отдельный метод добавления элемента
fun AddItem(
    alarms: MutableStateFlow<List<Alarm>> =
        remember { MutableStateFlow(listOf()) },
    noteList: List<Alarm>,
    time: String)
{

    val newList = ArrayList(noteList)
    newList.add(
        Alarm(
            time,
            "Th",
            true)
    )
    alarms.value = newList
}*/


/**
 * Показывает всплывающее уведомление (toast) с сообщением о времени установленного будильника.
 *
 * @param context Контекст приложения.
 * @param time Строковое представление времени, на которое установлен будильник.
 */
fun makeToast(context: Context, time: String) {
    Toast.makeText(
        context,
        "Будильник установлен на $time",
        Toast.LENGTH_SHORT
    ).show()
}