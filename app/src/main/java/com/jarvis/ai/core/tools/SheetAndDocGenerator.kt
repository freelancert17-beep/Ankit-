package com.jarvis.ai.core.tools

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SheetAndDocGenerator(private val context: Context) {

    data class GenerationResult(
        val success: Boolean,
        val file: File?,
        val textMessage: String,
        val voiceMessage: String,
        val fileType: String // "SHEET" or "DOC"
    )

    fun canHandle(query: String): Boolean {
        val q = query.lowercase()
        return q.contains("sheet") || q.contains("excel") ||
                q.contains("csv") || q.contains("document") ||
                q.contains("marksheet") || q.contains("budget") ||
                q.contains("expense") || q.contains("table banao") ||
                q.contains("file banao")
    }

    fun generateSheetOrDoc(query: String): GenerationResult {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val q = query.lowercase()

            val isExpense = q.contains("expense") || q.contains("kharcha") || q.contains("budget")
            val isStudent = q.contains("student") || q.contains("school") || q.contains("marks") || q.contains("class")
            val isAttendance = q.contains("attendance") || q.contains("hajiri")
            val isTrading = q.contains("trading") || q.contains("stock") || q.contains("portfolio")

            val fileName: String
            val content: String
            val fileType: String

            when {
                isExpense -> {
                    fileName = "Expense_Budget_Sheet_$timestamp.csv"
                    fileType = "SHEET"
                    content = """
                        Date,Category,Description,Payment Mode,Amount (INR)
                        2026-11-01,Groceries,Supermarket monthly ration,UPI,4500
                        2026-11-03,Utilities,Electricity Bill,NetBanking,1850
                        2026-11-07,Fuel,Petrol car refuel,CreditCard,2200
                        2026-11-12,Dining,Team Lunch,UPI,1400
                        2026-11-15,Subscriptions,Cloud & AI Tools,Card,999
                        2026-11-18,Medical,Health checkup & medicines,UPI,650
                        TOTAL SPENT,,, ,₹11599
                    """.trimIndent()
                }
                isStudent -> {
                    fileName = "Student_Marksheet_$timestamp.csv"
                    fileType = "SHEET"
                    content = """
                        Roll No,Student Name,Maths,Physics,Chemistry,Computer Science,Total,Percentage,Grade
                        101,Aarav Sharma,95,92,88,98,373,93.25%,A+
                        102,Diya Patel,89,84,91,95,359,89.75%,A
                        103,Rohan Verma,78,75,80,88,321,80.25%,B+
                        104,Sneha Singh,92,90,94,99,375,93.75%,A+
                        105,Vikram Malhotra,85,82,79,84,330,82.50%,A
                    """.trimIndent()
                }
                isAttendance -> {
                    fileName = "Attendance_Register_$timestamp.csv"
                    fileType = "SHEET"
                    content = """
                        Emp ID,Employee Name,Mon,Tue,Wed,Thu,Fri,Sat,Attendance %
                        JAR-01,Rajesh Kumar,P,P,P,P,P,WO,100%
                        JAR-02,Ankit Verma,P,P,P,P,P,WO,100%
                        JAR-03,Pooja Sharma,P,A,P,P,P,WO,80%
                        JAR-04,Mohit Jain,P,P,P,P,A,WO,80%
                    """.trimIndent()
                }
                isTrading -> {
                    fileName = "Trading_Portfolio_$timestamp.csv"
                    fileType = "SHEET"
                    content = """
                        Symbol,Exchange,Qty,Avg Buy Price,Current Price,P&L (INR),Return %
                        RELIANCE,NSE,25,2850.00,2980.00,+3250.00,+4.56%
                        TCS,NSE,15,3920.00,4110.00,+2850.00,+4.84%
                        HDFCBANK,NSE,40,1520.00,1630.00,+4400.00,+7.23%
                        INFY,NSE,30,1780.00,1860.00,+2400.00,+4.49%
                        PORTFOLIO TOTAL,, , , ,+₹12900.00,+5.28%
                    """.trimIndent()
                }
                else -> {
                    fileName = "JARVIS_Data_Sheet_$timestamp.csv"
                    fileType = "SHEET"
                    content = """
                        ID,Item Name,Category,Status,Remarks,Timestamp
                        01,Task Alpha,System Execution,Completed,Verified by Jarvis,2026-11-21
                        02,Task Beta,Data Processing,Active,In progress,2026-11-21
                        03,Task Gamma,Report Compilation,Completed,Exported successfully,2026-11-21
                    """.trimIndent()
                }
            }

            val targetFile = File(docsDir, fileName)
            targetFile.writeText(content)

            return GenerationResult(
                success = true,
                file = targetFile,
                textMessage = "Boss, aapki requested sheet / document taiyar kar di hai:\n\n" +
                        "📁 **File:** `${targetFile.name}`\n" +
                        "📊 **Type:** Excel/CSV Spreadsheet\n" +
                        "💾 **Size:** ${targetFile.length()} bytes\n\n" +
                        "Neeche diye gaye button par tap karke file open ya view karein.",
                voiceMessage = "Boss, file aur document ready kar diya hai, aap download kar sakte hain.",
                fileType = fileType
            )
        } catch (e: Exception) {
            return GenerationResult(
                success = false,
                file = null,
                textMessage = "Sheet create karte waqt error aaya: ${e.localizedMessage}",
                voiceMessage = "Sheet create karne mein error aaya Boss.",
                fileType = "DOC"
            )
        }
    }
}
