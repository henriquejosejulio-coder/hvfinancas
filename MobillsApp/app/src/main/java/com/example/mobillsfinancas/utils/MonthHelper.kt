package com.example.mobillsfinancas.utils

object MonthHelper {
    val MONTH_NAMES = listOf(
        "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
        "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
    )

    fun parseMonthYear(monthYearStr: String): Pair<Int, Int>? {
        val upper = monthYearStr.trim().uppercase()
        val parts = upper.split(" ", "/", "-").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val mName = parts[0]
            val yVal = parts[1].toIntOrNull() ?: return null
            val mIdx = MONTH_NAMES.indexOfFirst { it.startsWith(mName.take(3)) }
            if (mIdx != -1) return Pair(mIdx, yVal)
            val mNum = mName.toIntOrNull()
            if (mNum != null && mNum in 1..12) return Pair(mNum - 1, yVal)
        }
        return null
    }

    fun formatMonthYear(monthIndex: Int, year: Int): String {
        val safeMonth = ((monthIndex % 12) + 12) % 12
        return "${MONTH_NAMES[safeMonth]} $year"
    }

    fun addMonths(currentMonthYear: String, offset: Int): String {
        val parsed = parseMonthYear(currentMonthYear) ?: Pair(9, 2026)
        var m = parsed.first + offset
        var y = parsed.second
        while (m >= 12) {
            m -= 12
            y += 1
        }
        while (m < 0) {
            m += 12
            y -= 1
        }
        return formatMonthYear(m, y)
    }

    fun sortMonths(months: List<String>): List<String> {
        return months.distinct().sortedWith { a, b ->
            val pA = parseMonthYear(a)
            val pB = parseMonthYear(b)
            if (pA != null && pB != null) {
                if (pA.second != pB.second) pA.second.compareTo(pB.second)
                else pA.first.compareTo(pB.first)
            } else {
                a.compareTo(b)
            }
        }
    }

    fun compareMonths(monthA: String, monthB: String): Int {
        val pA = parseMonthYear(monthA) ?: return monthA.compareTo(monthB)
        val pB = parseMonthYear(monthB) ?: return monthA.compareTo(monthB)
        if (pA.second != pB.second) return pA.second.compareTo(pB.second)
        return pA.first.compareTo(pB.first)
    }

    fun getMonthYearFromDate(dateStr: String): String? {
        val trimmed = dateStr.trim()
        val parts = trimmed.split("/", "-")
        if (parts.size == 3) {
            if (parts[0].length == 4) {
                // YYYY-MM-DD
                val year = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull() ?: return null
                if (month in 1..12 && year >= 2000) {
                    return formatMonthYear(month - 1, year)
                }
            } else {
                // DD/MM/YYYY
                val month = parts[1].toIntOrNull() ?: return null
                val year = parts[2].toIntOrNull() ?: return null
                if (month in 1..12 && year >= 2000) {
                    return formatMonthYear(month - 1, year)
                }
            }
        }
        return null
    }

    fun getTargetInvoiceMonth(dateStr: String, closingDay: Int): String? {
        val trimmed = dateStr.trim()
        val parts = trimmed.split("/", "-")
        if (parts.size == 3) {
            val day: Int
            val month: Int
            val year: Int
            if (parts[0].length == 4) {
                year = parts[0].toIntOrNull() ?: return null
                month = parts[1].toIntOrNull() ?: return null
                day = parts[2].toIntOrNull() ?: return null
            } else {
                day = parts[0].toIntOrNull() ?: return null
                month = parts[1].toIntOrNull() ?: return null
                year = parts[2].toIntOrNull() ?: return null
            }
            if (month in 1..12 && year >= 2000 && day in 1..31) {
                val currentCalMonth = formatMonthYear(month - 1, year)
                // Se o dia da compra for estritamente MAIOR que o dia de fechamento da fatura,
                // a compra cai na fatura do mês seguinte!
                return if (closingDay in 1..31 && day > closingDay) {
                    addMonths(currentCalMonth, 1)
                } else {
                    currentCalMonth
                }
            }
        }
        return null
    }

    private val BRAZILIAN_FIXED_HOLIDAYS = setOf(
        "01/01", // Confraternização Universal
        "21/04", // Tiradentes
        "01/05", // Dia do Trabalho
        "07/09", // Independência do Brasil
        "12/10", // Nossa Senhora Aparecida
        "02/11", // Finados
        "15/11", // Proclamação da República
        "20/11", // Dia da Consciência Negra
        "25/12"  // Natal
    )

    fun isBusinessDay(cal: java.util.Calendar): Boolean {
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            return false
        }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val key = String.format(java.util.Locale.US, "%02d/%02d", day, month)
        if (key in BRAZILIAN_FIXED_HOLIDAYS) return false
        return true
    }

    fun getBusinessDaysInMonth(monthYearStr: String): Int {
        val parsed = parseMonthYear(monthYearStr) ?: return 21
        val monthIdx = parsed.first
        val year = parsed.second
        val cal = java.util.Calendar.getInstance()
        cal.set(year, monthIdx, 1)
        val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        var count = 0
        for (d in 1..maxDays) {
            cal.set(java.util.Calendar.DAY_OF_MONTH, d)
            if (isBusinessDay(cal)) count++
        }
        return if (count > 0) count else 21
    }
}

