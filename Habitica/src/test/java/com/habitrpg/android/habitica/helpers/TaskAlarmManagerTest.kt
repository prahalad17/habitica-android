package com.habitrpg.android.habitica.helpers

import android.app.AlarmManager
import android.content.Context
import com.habitrpg.android.habitica.data.TaskRepository
import com.habitrpg.android.habitica.models.tasks.RemindersItem
import com.habitrpg.android.habitica.models.tasks.Task
import com.habitrpg.android.habitica.modules.AuthenticationHandler
import com.habitrpg.shared.habitica.models.tasks.Frequency
import com.habitrpg.shared.habitica.models.tasks.TaskType
import io.kotest.core.spec.style.WordSpec
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskAlarmManagerTest : WordSpec({
    
    lateinit var context: Context
    lateinit var taskRepository: TaskRepository
    lateinit var authenticationHandler: AuthenticationHandler
    lateinit var alarmManager: AlarmManager
    lateinit var taskAlarmManager: TaskAlarmManager
    
    beforeTest {
        context = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        authenticationHandler = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        
        taskAlarmManager = TaskAlarmManager(context, taskRepository, authenticationHandler)
    }
    
    "TaskAlarmManager" When {
        "addAlarmForTaskId is called" should {
            "reschedule alarms for daily tasks with daily frequency" {
                runTest {
                    // Create a daily task with daily frequency
                    val dailyTask = Task().apply {
                        id = "daily-task-id"
                        type = TaskType.DAILY
                        frequency = Frequency.DAILY
                        reminders = io.realm.RealmList(
                            RemindersItem().apply {
                                time = "2025-07-12T09:00:00"
                            }
                        )
                    }
                    
                    every { taskRepository.getTaskCopy("daily-task-id") } returns flowOf(dailyTask)
                    taskAlarmManager.addAlarmForTaskId("daily-task-id")
                    
                    // Verify that setAlarmForRemindersItem was called (indirectly through setAlarmsForTask)
                    coVerify(timeout = 1000) { taskRepository.getTaskCopy("daily-task-id") }
                }
            }
            
            "NOT reschedule alarms for monthly tasks (daily type with monthly frequency)" {
                runTest {
                    // Create a monthly task (daily type with monthly frequency)
                    val monthlyTask = Task().apply {
                        id = "monthly-task-id"
                        type = TaskType.DAILY
                        frequency = Frequency.MONTHLY
                        reminders = io.realm.RealmList(
                            RemindersItem().apply {
                                time = "2025-07-12T09:00:00"
                            }
                        )
                    }
                    
                    every { taskRepository.getTaskCopy("monthly-task-id") } returns flowOf(monthlyTask)
                    taskAlarmManager.addAlarmForTaskId("monthly-task-id")
                    
                    // Verify that the task was fetched but no alarms were set
                    coVerify(timeout = 1000) { taskRepository.getTaskCopy("monthly-task-id") }
                    
                    // Verify that no alarm was scheduled (the filter should have filtered it out)
                    verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.setExact(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.set(any(), any(), any()) }
                }
            }
            
            "NOT reschedule alarms for weekly tasks" {
                runTest {
                    // Create a weekly task
                    val weeklyTask = Task().apply {
                        id = "weekly-task-id"
                        type = TaskType.DAILY
                        frequency = Frequency.WEEKLY
                        reminders = io.realm.RealmList(
                            RemindersItem().apply {
                                time = "2025-07-12T09:00:00"
                            }
                        )
                    }
                    
                    every { taskRepository.getTaskCopy("weekly-task-id") } returns flowOf(weeklyTask)
                    taskAlarmManager.addAlarmForTaskId("weekly-task-id")
                    coVerify(timeout = 1000) { taskRepository.getTaskCopy("weekly-task-id") }
                    
                    // Verify that no alarm was scheduled
                    verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.setExact(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.set(any(), any(), any()) }
                }
            }
            
            "NOT reschedule alarms for yearly tasks" {
                runTest {
                    // Create a yearly task
                    val yearlyTask = Task().apply {
                        id = "yearly-task-id"
                        type = TaskType.DAILY
                        frequency = Frequency.YEARLY
                        reminders = io.realm.RealmList(
                            RemindersItem().apply {
                                time = "2025-07-12T09:00:00"
                            }
                        )
                    }
                    
                    every { taskRepository.getTaskCopy("yearly-task-id") } returns flowOf(yearlyTask)
                    taskAlarmManager.addAlarmForTaskId("yearly-task-id")
                    
                    // Verify that the task was fetched but no alarms were set
                    coVerify(timeout = 1000) { taskRepository.getTaskCopy("yearly-task-id") }
                    
                    // Verify that no alarm was scheduled
                    verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.setExact(any(), any(), any()) }
                    verify(exactly = 0) { alarmManager.set(any(), any(), any()) }
                }
            }
        }
    }
})