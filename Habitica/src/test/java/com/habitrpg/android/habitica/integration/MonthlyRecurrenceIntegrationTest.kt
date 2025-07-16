package com.habitrpg.android.habitica.integration

import com.habitrpg.android.habitica.models.tasks.Days
import com.habitrpg.android.habitica.models.tasks.RemindersItem
import com.habitrpg.android.habitica.models.tasks.Task
import com.habitrpg.android.habitica.models.tasks.shouldBeLessThanOrEqualTo
import com.habitrpg.shared.habitica.models.tasks.Frequency
import com.habitrpg.shared.habitica.models.tasks.TaskType
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class MonthlyRecurrenceIntegrationTest : WordSpec({
    "Monthly recurrence integration" When {
        "user creates a task with 'Day of Week' monthly recurrence" should {
            "maintain consistency between UI selection and task behavior" {
                // Scenario: User creates a task on July 12, 2025 (2nd Saturday)
                // and selects "Monthly" -> "Day of Week"
                
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    // User sets start date
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JULY, 12, 9, 0, 0)
                    }
                    startDate = startCal.time

                    // Calendar.WEEK_OF_MONTH = 2 (1-based)
                    // Stored as 1 (0-based)
                    setWeeksOfMonth(listOf(1))
                }
                
                // Add a reminder
                val reminder = RemindersItem().apply {
                    time = "2025-07-12T09:00:00"
                }
                task.reminders?.add(reminder)
                
                // Get next occurrences
                val occurrences = task.getNextReminderOccurrences(reminder, 12)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(12)
                    
                    // All should be 2nd Saturdays
                    forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.SATURDAY
                    
                    // Verify it's the 2nd Saturday
                    val firstOfMonth = occurrence.withDayOfMonth(1)
                    var firstSaturday = firstOfMonth
                    while (firstSaturday.dayOfWeek != DayOfWeek.SATURDAY) {
                        firstSaturday = firstSaturday.plusDays(1)
                    }
                    val secondSaturday = firstSaturday.plusWeeks(1)
                    
                    occurrence.dayOfMonth shouldBe secondSaturday.dayOfMonth
                    }
                    
                    // Specific dates check
                    val expectedDates = listOf(
                        Triple(2025, 7, 12),   // 2nd Saturday of July
                        Triple(2025, 8, 9),    // 2nd Saturday of August
                        Triple(2025, 9, 13),   // 2nd Saturday of September
                        Triple(2025, 10, 11),  // 2nd Saturday of October
                        Triple(2025, 11, 8),   // 2nd Saturday of November
                        Triple(2025, 12, 13)   // 2nd Saturday of December
                    )
                    
                    take(6).forEachIndexed { index, occurrence ->
                    val (year, month, day) = expectedDates[index]
                    occurrence.year shouldBe year
                    occurrence.monthValue shouldBe month
                    occurrence.dayOfMonth shouldBe day
                    }
                }
            }
            
            "handle edge case: 5th occurrence of a weekday" {
                // Some months have 5 occurrences of certain weekdays
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    // May 29, 2025 is the 5th Thursday
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.MAY, 29, 14, 0, 0)
                    }
                    startDate = startCal.time
                    
                    // With our fix: Calendar.WEEK_OF_MONTH = 5, stored as 4 (0-based)
                    setWeeksOfMonth(listOf(4))
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-05-29T14:00:00"
                }
                
                // Get occurrences for the whole year
                val occurrences = task.getNextReminderOccurrences(reminder, 12)
                
                occurrences shouldNotBeNull {
                    // Not all months have a 5th Thursday
                    val monthsWithFifthThursday = listOf(5, 7, 10, 12) // May, July, October, December in 2025
                    
                    filter { it.year == 2025 }.forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.THURSDAY
                    
                    // It should be in the 5th week
                    val weekOfMonth = (occurrence.dayOfMonth - 1) / 7 + 1
                    weekOfMonth shouldBe 5
                    }
                }
            }
            
            "work correctly when transitioning between months with different week patterns" {
                // Test a complex case: 4th Monday
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    // January 27, 2025 is the 4th Monday
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 27, 8, 0, 0)
                    }
                    startDate = startCal.time
                    
                    // Calendar.WEEK_OF_MONTH = 4, stored as 3 (0-based)
                    setWeeksOfMonth(listOf(3))
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-27T08:00:00"
                }
                
                val occurrences = task.getNextReminderOccurrences(reminder, 6)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(6)
                    
                    // All should be 4th Mondays
                    val expectedDates = listOf(
                        Triple(2025, 1, 27),  // 4th Monday of January
                        Triple(2025, 2, 24),  // 4th Monday of February (non-leap year)
                        Triple(2025, 3, 24),  // 4th Monday of March
                        Triple(2025, 4, 28),  // 4th Monday of April
                        Triple(2025, 5, 26),  // 4th Monday of May
                        Triple(2025, 6, 23)   // 4th Monday of June
                    )
                    
                    forEachIndexed { index, occurrence ->
                    val (year, month, day) = expectedDates[index]
                    occurrence.year shouldBe year
                    occurrence.monthValue shouldBe month
                    occurrence.dayOfMonth shouldBe day
                    occurrence.dayOfWeek shouldBe DayOfWeek.MONDAY
                    }
                }
            }
            
            "correctly handle first week of month edge cases" {
                // When the 1st of the month is the target weekday
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    // March 1, 2025 is a Saturday (1st Saturday)
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.MARCH, 1, 10, 0, 0)
                    }
                    startDate = startCal.time
                    
                    // Calendar.WEEK_OF_MONTH = 1, stored as 0 (0-based)
                    setWeeksOfMonth(listOf(0))
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-03-01T10:00:00"
                }
                
                val occurrences = task.getNextReminderOccurrences(reminder, 4)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(4)
                    
                    // All should be 1st Saturdays
                    forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.SATURDAY
                    occurrence.dayOfMonth shouldBeLessThanOrEqualTo 7 // First week
                    }
                }
            }
            
            "preserve behavior when combined with 'every X months'" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 3 // Every 3 months
                    
                    // Start with 3rd Tuesday of January 2025
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 21, 15, 30, 0)
                    }
                    startDate = startCal.time
                    
                    // Calendar.WEEK_OF_MONTH = 3, stored as 2 (0-based)
                    setWeeksOfMonth(listOf(2))
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-21T15:30:00"
                }
                
                val occurrences = task.getNextReminderOccurrences(reminder, 4)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(4)
                    
                    // Should be every 3 months: Jan, Apr, Jul, Oct
                    val expectedDates = listOf(
                        Triple(2025, 1, 21),   // 3rd Tuesday of January
                        Triple(2025, 4, 15),   // 3rd Tuesday of April
                        Triple(2025, 7, 15),   // 3rd Tuesday of July
                        Triple(2025, 10, 21)   // 3rd Tuesday of October
                    )
                    
                    forEachIndexed { index, occurrence ->
                    val (year, month, day) = expectedDates[index]
                    occurrence.year shouldBe year
                    occurrence.monthValue shouldBe month
                    occurrence.dayOfMonth shouldBe day
                    occurrence.dayOfWeek shouldBe DayOfWeek.TUESDAY
                    }
                }
            }
        }
        
        "comparing with day-based monthly recurrence" should {
            "show different behavior patterns" {
                // Compare two tasks: one with week-based, one with day-based
                val weekBasedTask = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 8) // 2nd Wednesday
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(1)) // 2nd week
                }
                
                val dayBasedTask = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 8) // 8th of month
                    }
                    startDate = startCal.time
                    setDaysOfMonth(listOf(8)) // 8th day
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-08T09:00:00"
                }
                
                val weekOccurrences = weekBasedTask.getNextReminderOccurrences(reminder, 12)
                val dayOccurrences = dayBasedTask.getNextReminderOccurrences(reminder, 12)
                
                // Week-based: always 2nd Wednesday
                weekOccurrences shouldNotBeNull {
                    forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.WEDNESDAY
                    }
                }
                
                // Day-based: always 8th, but day of week varies
                dayOccurrences shouldNotBeNull {
                    forEach { occurrence ->
                    occurrence.dayOfMonth shouldBe 8
                    }
                    
                    // They diverge after the first occurrence
                    get(1).dayOfMonth shouldBe 12 // Feb 12 (2nd Wed)
                }
                
                dayOccurrences?.get(1)?.dayOfMonth shouldBe 8    // Feb 8 (Sat)
            }
        }
    }
})
