package com.liferlighdow.iteration.ui.search

import kotlin.math.*

/**
 * 處理搜尋中的數學運算、單位轉換與匯率轉換邏輯
 */

fun evaluateExpression(str: String): String? {
    return try {
        val cleanStr = str.replace(" ", "").lowercase()
        val result = object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < cleanStr.length) cleanStr[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double { nextChar(); val x = parseExpression(); if (pos < cleanStr.length) return Double.NaN; return x }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else if (ch == '('.code || ch == 'π'.code || (ch >= 'a'.code && ch <= 'z'.code) || (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) x *= parseFactor()
                    else return x
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) { x = parseExpression(); eat(')'.code) }
                else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = cleanStr.substring(startPos, pos).toDouble()
                } else if (ch == 'π'.code) { nextChar(); x = PI }
                else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = cleanStr.substring(startPos, pos)
                    x = when (func) {
                        "pi" -> PI
                        "e" -> E
                        else -> {
                            eat('('.code)
                            val arg = parseExpression()
                            eat(')'.code)
                            val rad = arg * PI / 180.0
                            when (func) {
                                "sqrt" -> sqrt(arg)
                                "abs" -> abs(arg)
                                "sin" -> sin(rad)
                                "cos" -> cos(rad)
                                "tan" -> tan(rad)
                                "cot" -> 1.0 / tan(rad)
                                "sec" -> 1.0 / cos(rad)
                                "csc" -> 1.0 / sin(rad)
                                "log" -> log10(arg)
                                "ln" -> ln(arg)
                                else -> throw RuntimeException("Unknown function: $func")
                            }
                        }
                    }
                } else return Double.NaN
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }.parse()
        if (result.isNaN() || result.isInfinite()) null
        else if (result == result.toLong().toDouble()) result.toLong().toString()
        else String.format("%.8f", result).trimEnd('0').trimEnd('.')
    } catch (e: Exception) { null }
}

fun performCurrencyConversion(str: String, rates: Map<String, Double>): String? {
    val regex = Regex("""^(.+?)\s*([a-zA-Z]{3})\s+(?:to|in)\s+([a-zA-Z]{3})$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]
    val amount = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null
    val fromRate = rates[from] ?: return null
    val toRate = rates[to] ?: return null
    val result = (amount / fromRate) * toRate
    return String.format("%,.2f", result) + " " + to.uppercase()
}

fun performUnitConversion(str: String): String? {
    val regex = Regex("""^(.+?)\s*([a-zA-Z]+)\s+(?:to|in)\s+([a-zA-Z]+)$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]
    val evaluatedValue = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null
    val lengthMap = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.34)
    val weightMap = mapOf("g" to 1.0, "kg" to 1000.0, "mg" to 0.001, "lb" to 453.592, "oz" to 28.3495)
    return when {
        lengthMap.containsKey(from) && lengthMap.containsKey(to) -> formatResult(evaluatedValue * lengthMap[from]!! / lengthMap[to]!!) + to
        weightMap.containsKey(from) && weightMap.containsKey(to) -> formatResult(evaluatedValue * weightMap[from]!! / weightMap[to]!!) + to
        from == "c" && to == "f" -> formatResult(evaluatedValue * 9/5 + 32) + "°F"
        from == "f" && to == "c" -> formatResult((evaluatedValue - 32) * 5/9) + "°C"
        from == "c" && to == "k" -> formatResult(evaluatedValue + 273.15) + "K"
        from == "k" && to == "c" -> formatResult(evaluatedValue - 273.15) + "°C"
        else -> null
    }
}

fun formatResult(d: Double): String {
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else String.format("%.4f", d).trimEnd('0').trimEnd('.')
}

fun calculateRelatedUnits(value: Double, fromUnit: String): List<Pair<String, String>>? {
    val lengthMap = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.34)
    val weightMap = mapOf("g" to 1.0, "kg" to 1000.0, "mg" to 0.001, "lb" to 453.592, "oz" to 28.3495)

    return when {
        lengthMap.containsKey(fromUnit) -> {
            val baseValue = value * lengthMap[fromUnit]!!
            listOf("KM", "CM", "FT", "IN").filter { it.lowercase() != fromUnit }.map { 
                it to (formatResult(baseValue / lengthMap[it.lowercase()]!!) + " " + it.lowercase())
            }
        }
        weightMap.containsKey(fromUnit) -> {
            val baseValue = value * weightMap[fromUnit]!!
            listOf("KG", "G", "LB", "OZ").filter { it.lowercase() != fromUnit }.map {
                it to (formatResult(baseValue / weightMap[it.lowercase()]!!) + " " + it.lowercase())
            }
        }
        fromUnit == "c" -> listOf("F" to (formatResult(value * 9/5 + 32) + " °F"), "K" to (formatResult(value + 273.15) + " K"))
        fromUnit == "f" -> listOf("C" to (formatResult((value - 32) * 5/9) + " °C"), "K" to (formatResult((value - 32) * 5/9 + 273.15) + " K"))
        fromUnit == "k" -> listOf("C" to (formatResult(value - 273.15) + " °C"), "F" to (formatResult((value - 273.15) * 9/5 + 32) + " °F"))
        else -> null
    }
}

fun calculateRelatedCurrencies(value: Double, from: String, rates: Map<String, Double>): List<Pair<String, String>>? {
    val targetCurrencies = listOf(
        "USD" to "美元", "EUR" to "歐元", "JPY" to "日圓", 
        "GBP" to "英鎊", "CNY" to "人民幣", "CHF" to "瑞士法郎", "TWD" to "新台幣"
    )
    
    val fromLower = from.lowercase()
    val fromUpper = from.uppercase()
    val fromRate = rates[fromLower] ?: rates[fromUpper] ?: return null
    
    return targetCurrencies.filter { it.first.lowercase() != fromLower }.mapNotNull { (code, _) ->
        val toRate = rates[code.lowercase()] ?: rates[code.uppercase()]
        toRate?.let {
            val result = (value / fromRate) * it
            val formatted = String.format("%,.2f", result)
            code to "$formatted $code"
        }
    }
}
