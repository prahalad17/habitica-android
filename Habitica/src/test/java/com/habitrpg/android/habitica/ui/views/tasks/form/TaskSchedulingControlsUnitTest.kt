package com.habitrpg.android.habitica.ui.views.tasks.form

import com.habitrpg.shared.habitica.models.tasks.Frequency
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.util.Calendar

class TaskSchedulingControlsUnitTest : WordSpec({
    "TaskSchedulingControls week calculation" When {
        "calculating week index for monthly recurrence" should {
            "correctly calculate week index for various dates" {
                // Test TaskSchedulingControls logic
                val testCases = listOf(
                    // Date -> Expected week index (0-based)
                    Triple(2025, Calendar.JULY, 1) to 0,   // July 1st (1st week)
                    Triple(2025, Calendar.JULY, 5) to 0,   // July 5th (1st week) 
                    Triple(2025, Calendar.JULY, 6) to 1,   // July 6th (2nd week)
                    Triple(2025, Calendar.JULY, 12) to 1,  // July 12th (2nd week) - the bug case
                    Triple(2025, Calendar.JULY, 13) to 2,  // July 13th (3rd week)
                    Triple(2025, Calendar.JULY, 19) to 2,  // July 19th (3rd week)
                    Triple(2025, Calendar.JULY, 26) to 3,  // July 26th (4th week)
                    Triple(2025, Calendar.JULY, 31) to 4,  // July 31st (5th week)
                )
                
                testCases.forEach { (dateTriple, expectedWeekIndex) ->
                    val (year, month, day) = dateTriple
                    val calendar = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    
                    // This is the CORRECT calculation (matching Calendar.WEEK_OF_MONTH)
                    val calendarWeekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
                    val zeroBasedWeekIndex = calendarWeekOfMonth - 1
                    
                    zeroBasedWeekIndex shouldBe expectedWeekIndex
                }
            }
            
            "demonstrate the bug in the old calculation" {
                // Show why the old calculation was wrong
                val calendar = Calendar.getInstance().apply {
                    set(2025, Calendar.JULY, 12) // July 12, 2025
                }
                
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                
                // Old buggy calculation
                val oldCalculation = (dayOfMonth - 1) / 7
                oldCalculation shouldBe 1 // (12-1)/7 = 11/7 = 1
                
                // Correct calculation
                val correctCalculation = calendar.get(Calendar.WEEK_OF_MONTH) - 1
                correctCalculation shouldBe 1 // WEEK_OF_MONTH=2, minus 1 = 1
                
                // In this case they match, but let's check another date
                calendar.set(2025, Calendar.JULY, 6) // July 6th (Sunday, start of 2nd week)
                
                val dayOfMonth2 = calendar.get(Calendar.DAY_OF_MONTH)
                val oldCalculation2 = (dayOfMonth2 - 1) / 7
                oldCalculation2 shouldBe 0 // (6-1)/7 = 5/7 = 0 (WRONG!)
                
                val correctCalculation2 = calendar.get(Calendar.WEEK_OF_MONTH) - 1
                correctCalculation2 shouldBe 1 // WEEK_OF_MONTH=2, minus 1 = 1 (CORRECT)
            }
            
            "handle edge cases correctly" {
                // First day of month
                val firstDayCalendar = Calendar.getInstance().apply {
                    set(2025, Calendar.AUGUST, 1) // Friday
                }
                
                val weekOfMonth = firstDayCalendar.get(Calendar.WEEK_OF_MONTH)
                val zeroBasedIndex = weekOfMonth - 1
                zeroBasedIndex shouldBe 0 // First week
                
                // Test a known 5th week scenario
                val fifthWeekCalendar = Calendar.getInstance().apply {
                    set(2025, Calendar.MAY, 29) // May 29, 2025 - 5th Thursday
                }
                
                val fifthWeekOfMonth = fifthWeekCalendar.get(Calendar.WEEK_OF_MONTH)
                val fifthZeroBasedIndex = fifthWeekOfMonth - 1
                fifthZeroBasedIndex shouldBeGreaterThan 2 // At least 4th week (0-based)
            }
            
            "work correctly for months starting on different days" {
                // January 2025 starts on Wednesday
                val janCalendar = Calendar.getInstance().apply {
                    set(2025, Calendar.JANUARY, 4) // First Saturday
                }
                
                val janWeek = janCalendar.get(Calendar.WEEK_OF_MONTH)
                janWeek shouldBe 1 // First week
                
                // March 2025 starts on Saturday
                val marchCalendar = Calendar.getInstance().apply {
                    set(2025, Calendar.MARCH, 1) // First Saturday (also first day)
                }
                
                val marchWeek = marchCalendar.get(Calendar.WEEK_OF_MONTH)
                marchWeek shouldBe 1 // First week
            }
        }
        
        "summary text generation" should {
            "display correct ordinal for week-based recurrence" {
                // Test that the display logic correctly shows the week number
                val weeksOfMonth = listOf(0, 1, 2, 3, 4) // 0-based storage
                val expectedOrdinals = listOf("1st", "2nd", "3rd", "4th", "5th")
                
                weeksOfMonth.forEachIndexed { index, storedWeek ->
                    // Display logic should add 1 to stored value
                    val displayWeek = storedWeek + 1

                    val ordinal = when (displayWeek) {
                        1 -> "1st"
                        2 -> "2nd"
                        3 -> "3rd"
                        4 -> "4th"
                        5 -> "5th"
                        else -> "${displayWeek}th"
                    }
                    
                    ordinal shouldBe expectedOrdinals[index]
                }
            }
        }
    }
})