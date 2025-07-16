package com.habitrpg.android.habitica.models.tasks

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

/**
 * Helper functions for type-safe testing of Task monthly recurrence
 */

/**
 * Matcher to check if an integer is within a range
 */
fun beInRange(min: Int, max: Int) = object : Matcher<Int> {
    override fun test(value: Int) = MatcherResult(
        value in min..max,
        { "$value should be between $min and $max" },
        { "$value should not be between $min and $max" }
    )
}

/**
 * Matcher to check if an integer is less than or equal to a value
 */
fun beLessThanOrEqualTo(max: Int) = object : Matcher<Int> {
    override fun test(value: Int) = MatcherResult(
        value <= max,
        { "$value should be less than or equal to $max" },
        { "$value should not be less than or equal to $max" }
    )
}

/**
 * Matcher to check if an integer is greater than or equal to a value
 */
fun beGreaterThanOrEqualTo(min: Int) = object : Matcher<Int> {
    override fun test(value: Int) = MatcherResult(
        value >= min,
        { "$value should be greater than or equal to $min" },
        { "$value should not be greater than or equal to $min" }
    )
}

// Extension functions for cleaner syntax
infix fun Int.shouldBeInRange(range: IntRange) = this should beInRange(range.first, range.last)
infix fun Int.shouldBeLessThanOrEqualTo(max: Int) = this should beLessThanOrEqualTo(max)
infix fun Int.shouldBeGreaterThanOrEqualTo(min: Int) = this should beGreaterThanOrEqualTo(min)