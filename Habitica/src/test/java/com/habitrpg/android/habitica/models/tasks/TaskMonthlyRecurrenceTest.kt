package com.habitrpg.android.habitica.models.tasks

import com.habitrpg.android.habitica.extensions.toZonedDateTime
import com.habitrpg.shared.habitica.models.tasks.Frequency
import com.habitrpg.shared.habitica.models.tasks.TaskType
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class TaskMonthlyRecurrenceTest : WordSpec({
    "Task monthly recurrence with week-based scheduling" When {
        "calculating next reminder occurrences" should {
            "correctly handle 2nd Saturday of the month" {
                // Setup task for July 12, 2025 (2nd Saturday)
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JULY, 12) // July 12, 2025
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(1)) // 0-based: 2nd week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-07-12T09:00:00"
                }
                
                // Get next 6 occurrences
                // Pass a specific date to make test deterministic - use July 10 so July 12 is in the future
                val testToday = ZonedDateTime.of(2025, 7, 10, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 6, testToday)
                
                // Assert not null and work with non-null value in the block
                occurrences shouldNotBeNull {
                    shouldHaveSize(6)
                    
                    // Expected dates (all 2nd Saturdays):
                    val expectedDates = listOf(
                        Triple(2025, 7, 12),
                        Triple(2025, 8, 9),
                        Triple(2025, 9, 13),
                        Triple(2025, 10, 11),
                        Triple(2025, 11, 8),
                        Triple(2025, 12, 13)
                    )
                    
                    forEachIndexed { index, occurrence ->
                    val (year, month, day) = expectedDates[index]
                    occurrence.year shouldBe year
                    occurrence.monthValue shouldBe month
                    occurrence.dayOfMonth shouldBe day
                    occurrence.dayOfWeek shouldBe DayOfWeek.SATURDAY
                    occurrence.hour shouldBe 9
                    occurrence.minute shouldBe 0
                    }
                }
            }
            
            "correctly handle 1st Tuesday of the month" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JULY, 1) // July 1, 2025 (1st Tuesday)
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(0)) // 0-based: 1st week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-07-01T14:30:00"
                }
                
                // Pass a specific date to make test deterministic
                val testToday = ZonedDateTime.of(2025, 6, 15, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 4, testToday)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(4)
                    
                    // All should be first Tuesdays
                    forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.TUESDAY
                    // Verify it's in the first week by checking day is <= 7
                    occurrence.dayOfMonth shouldBeLessThanOrEqualTo 7
                    }
                }
            }
            
            "correctly handle 4th Friday of the month" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JULY, 25) // July 25, 2025 (4th Friday)
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(3)) // 0-based: 4th week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-07-25T18:00:00"
                }
                
                // Pass a specific date to make test deterministic
                val testToday = ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 3, testToday)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(3)
                    
                    val expectedDates = listOf(
                        Triple(2025, 7, 25),  // 4th Friday of July
                        Triple(2025, 8, 22),  // 4th Friday of August
                        Triple(2025, 9, 26)   // 4th Friday of September
                    )
                    
                    forEachIndexed { index, occurrence ->
                    val (year, month, day) = expectedDates[index]
                    occurrence.year shouldBe year
                    occurrence.monthValue shouldBe month
                    occurrence.dayOfMonth shouldBe day
                    occurrence.dayOfWeek shouldBe DayOfWeek.FRIDAY
                    }
                }
            }
            
            "handle months with 5 weeks correctly" {
                // May 2025 has 5 Thursdays
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.MAY, 29) // May 29, 2025 (5th Thursday)
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(4)) // 0-based: 5th week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-05-29T10:00:00"
                }
                
                // Pass a specific date to make test deterministic
                val testToday = ZonedDateTime.of(2025, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 12, testToday)
                
                occurrences shouldNotBeNull {
                    // Not all months have a 5th Thursday, so we should skip those months
                    val monthsWithFifthThursday = filter { occurrence ->
                    // Calculate if this is actually the 5th occurrence of the weekday
                    val firstOfMonth = occurrence.withDayOfMonth(1)
                    var count = 0
                    var current = firstOfMonth
                    while (current.month == occurrence.month) {
                        if (current.dayOfWeek == occurrence.dayOfWeek) {
                            count++
                            if (current == occurrence) break
                        }
                        current = current.plusDays(1)
                    }
                    count == 5
                }
                
                // All occurrences should be 5th Thursdays
                monthsWithFifthThursday.forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.THURSDAY
                    occurrence.dayOfMonth shouldBeGreaterThanOrEqualTo 29 // 5th week is always late in month
                    }
                }
            }
            
            "handle every 2 months correctly" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 2 // Every 2 months
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 8) // January 8, 2025 (2nd Wednesday)
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(1)) // 0-based: 2nd week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-08T07:00:00"
                }
                
                // Pass a specific date to make test deterministic
                val testToday = ZonedDateTime.of(2024, 12, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 6, testToday)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(6)
                    
                    // Should be every 2 months: Jan, Mar, May, Jul, Sep, Nov
                    val expectedMonths = listOf(1, 3, 5, 7, 9, 11)
                    
                    forEachIndexed { index, occurrence ->
                    occurrence.monthValue shouldBe expectedMonths[index]
                    occurrence.dayOfWeek shouldBe DayOfWeek.WEDNESDAY
                    occurrence.year shouldBe 2025
                    }
                }
            }
            
            "handle edge case when start date is in the past" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2024, Calendar.JANUARY, 13) // Past date: January 13, 2024 (2nd Saturday)
                    }
                    startDate = startCal.time
                    setWeeksOfMonth(listOf(1)) // 0-based: 2nd week
                }
                
                val reminder = RemindersItem().apply {
                    time = "2024-01-13T10:00:00"
                }
                
                // Simulate current date being in 2025
                val fakeNow = ZonedDateTime.of(2025, 7, 15, 8, 0, 0, 0, ZoneId.systemDefault())
                
                val occurrences = task.getNextReminderOccurrences(reminder, 3, fakeNow)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(3)
                    
                    // Should return future occurrences only
                    forEach { occurrence ->
                    occurrence.isAfter(fakeNow) shouldBe true
                    occurrence.dayOfWeek shouldBe DayOfWeek.SATURDAY
                    }
                }
            }
            
            "maintain consistency between UI selection and calculated dates" {
                // This tests the specific bug scenario
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    // July 12, 2025 - User selects this as "2nd Saturday"
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JULY, 12)
                    }
                    startDate = startCal.time
                    
                    // With our fix, this should be stored as 1 (0-based for 2nd week)
                    setWeeksOfMonth(listOf(1))
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-07-12T09:00:00"
                }
                
                val testToday = ZonedDateTime.of(2025, 7, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 12, testToday)
                
                occurrences shouldNotBeNull {
                    // All occurrences should be 2nd Saturdays
                    forEach { occurrence ->
                    occurrence.dayOfWeek shouldBe DayOfWeek.SATURDAY
                    
                    // Calculate which week of month this is
                    val firstOfMonth = occurrence.withDayOfMonth(1)
                    val firstSaturday = if (firstOfMonth.dayOfWeek == DayOfWeek.SATURDAY) {
                        firstOfMonth
                    } else {
                        var day = firstOfMonth
                        while (day.dayOfWeek != DayOfWeek.SATURDAY) {
                            day = day.plusDays(1)
                        }
                        day
                    }
                    
                    // Should be exactly 1 week after first Saturday (2nd Saturday)
                    val weeksSinceFirst = (occurrence.dayOfMonth - firstSaturday.dayOfMonth) / 7
                    weeksSinceFirst shouldBe 1
                    }
                }
            }
        }
        
        "handling day-based monthly recurrence" should {
            "work correctly for specific days of month" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 15)
                    }
                    startDate = startCal.time
                    setDaysOfMonth(listOf(15)) // 15th of each month
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-15T12:00:00"
                }
                
                val testToday = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 6, testToday)
                
                occurrences shouldNotBeNull {
                    shouldHaveSize(6)
                    
                    forEach { occurrence ->
                    occurrence.dayOfMonth shouldBe 15
                    }
                }
            }
            
            "handle end-of-month edge cases" {
                val task = Task().apply {
                    type = TaskType.DAILY
                    frequency = Frequency.MONTHLY
                    everyX = 1
                    
                    val startCal = Calendar.getInstance().apply {
                        set(2025, Calendar.JANUARY, 31)
                    }
                    startDate = startCal.time
                    setDaysOfMonth(listOf(31)) // 31st of each month
                }
                
                val reminder = RemindersItem().apply {
                    time = "2025-01-31T15:00:00"
                }
                
                val testToday = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                val occurrences = task.getNextReminderOccurrences(reminder, 12, testToday)
                
                occurrences shouldNotBeNull {
                    // Not all months have 31 days
                    val expected31stDays = listOf(1, 3, 5, 7, 8, 10, 12) // Months with 31 days
                    
                    filter { it.year == 2025 }.forEach { occurrence ->
                    if (expected31stDays.contains(occurrence.monthValue)) {
                        occurrence.dayOfMonth shouldBe 31
                    } else if (occurrence.monthValue == 2) {
                        // February has 28 days in 2025 (non-leap year)
                        occurrence.dayOfMonth shouldBe 28
                    } else {
                        // April, June, September, November have 30 days
                        occurrence.dayOfMonth shouldBe 30
                    }
                    }
                }
            }
        }
    }
})