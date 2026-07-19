/* Domain-layer choices controlling the Dashboard spending chart window. */
package com.teja.finflyiii.domain.model

/** Selects the nominal duration shown by the Dashboard spending chart. */
enum class DashboardChartPeriod { WEEK, MONTH }

/** Chooses calendar-aligned dates or an equivalent rolling look-back window. */
enum class DashboardRangeMode { CALENDAR, ROLLING }

/** Selects the initial visualization used for category spending. */
enum class CategoryChartStyle { BARS, PIE }
