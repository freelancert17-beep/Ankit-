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
                q.contains("file banao") || q.contains("data table")
    }

    fun generateSheetOrDoc(query: String): GenerationResult {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val q = query.lowercase()

            val fileName: String
            val content: String
            val topicLabel: String

            when {
                // 1. Gym / Fitness / Diet
                q.contains("gym") || q.contains("fitness") || q.contains("workout") || q.contains("diet") -> {
                    topicLabel = "Gym & Workout Tracker"
                    fileName = "Gym_Workout_Schedule_$timestamp.csv"
                    content = """
                        Day,Exercise Category,Target Muscle,Sets,Reps,Weight (KG),Status
                        Monday,Push,Chest & Triceps (Bench Press & Dips),4,10-12,70,Completed
                        Tuesday,Pull,Back & Biceps (Lat Pulldowns & Curls),4,10-12,65,Completed
                        Wednesday,Legs,Quads & Calves (Squats & Leg Press),4,12-15,90,Completed
                        Thursday,Push,Shoulders & Abs (Overhead Press),4,10-12,50,Scheduled
                        Friday,Pull,Deadlifts & Pull-ups,4,8-10,100,Scheduled
                        Saturday,Cardio & HIIT,Treadmill & Core,1,30 Mins,Bodyweight,Scheduled
                        Sunday,Rest & Recovery,Full Body Mobility,-,-,-,Rest Day
                    """.trimIndent()
                }

                // 2. Sales Leads & CRM
                q.contains("sales") || q.contains("lead") || q.contains("client") || q.contains("customer") || q.contains("crm") -> {
                    topicLabel = "Sales & Client Leads"
                    fileName = "Sales_Leads_CRM_$timestamp.csv"
                    content = """
                        Lead ID,Client Name,Company,Contact,Deal Value (INR),Lead Stage,Next Action
                        LD-101,Vikram Sharma,Apex Global,+91-9876543210,₹450000,Proposal Sent,Follow up on contract
                        LD-102,Priya Nair,Quantum Tech,+91-9811223344,₹820000,Negotiation,Schedule demo call
                        LD-103,Amit Verma,Starlight Logistics,+91-9988776655,₹290000,Discovery Call,Send pricing sheet
                        LD-104,Sneha Rao,Urban Retail,+91-9765432109,₹650000,Deal Won,Onboarding kickoff
                        LD-105,Karan Patel,Falcon Motors,+91-9654321876,₹1200000,Proposal Review,Legal team signoff
                    """.trimIndent()
                }

                // 3. Inventory / Shop Stock
                q.contains("inventory") || q.contains("stock") || q.contains("godown") || q.contains("warehouse") || q.contains("item") -> {
                    topicLabel = "Inventory Stock Register"
                    fileName = "Inventory_Stock_Register_$timestamp.csv"
                    content = """
                        SKU Code,Product Name,Category,Unit Price (INR),In Stock Qty,Reorder Level,Supplier,Stock Status
                        SKU-001,Gaming Mouse RGB,Electronics,1250.00,140,25,LogiTech Corp,In Stock
                        SKU-002,Mechanical Keyboard,Electronics,3499.00,45,15,Redragon India,Optimal
                        SKU-003,Type-C Fast Cable,Accessories,299.00,320,50,Anker Direct,High
                        SKU-004,27-inch 144Hz Monitor,Displays,18500.00,12,10,ViewSonic India,Reorder Soon
                        SKU-005,Bluetooth Noise Cancelling Buds,Audio,2499.00,8,20,Boat Lifestyle,CRITICAL LOW
                    """.trimIndent()
                }

                // 4. Employee Salary / Payroll / Attendance
                q.contains("salary") || q.contains("payroll") || q.contains("attendance") || q.contains("employee") || q.contains("staff") -> {
                    topicLabel = "Employee Payroll & Attendance"
                    fileName = "Employee_Payroll_$timestamp.csv"
                    content = """
                        Emp ID,Employee Name,Designation,Department,Present Days,Basic Pay (INR),HRA (INR),Deductions (INR),Net Salary (INR)
                        EMP-01,Rajesh Sharma,Senior Android Dev,Engineering,26,75000,25000,3500,96500
                        EMP-02,Ananya Gupta,AI Research Lead,Data Science,25,90000,30000,4200,115800
                        EMP-03,Mohit Verma,UI/UX Designer,Product,24,55000,18000,2800,70200
                        EMP-04,Rohan Das,DevOps Specialist,Infrastructure,26,70000,22000,3100,88900
                        EMP-05,Pooja Mehta,HR Manager,Human Resources,25,48000,15000,2100,60900
                    """.trimIndent()
                }

                // 5. Stock & Trading Watchlist
                q.contains("trading") || q.contains("stock") || q.contains("portfolio") || q.contains("nifty") || q.contains("crypto") -> {
                    topicLabel = "Stock Market Watchlist"
                    fileName = "Trading_Watchlist_$timestamp.csv"
                    content = """
                        Symbol,Exchange,Sector,LTP (INR),Target Price (INR),Stop Loss (INR),RSI (14),Signal,Setup
                        RELIANCE,NSE,Oil & Retail,2980.00,3150.00,2890.00,58.4,BULLISH,Breakout above 20 EMA
                        TCS,NSE,IT Services,4120.00,4300.00,4020.00,62.1,ACCUMULATE,Trendline Bounce
                        HDFCBANK,NSE,Banking,1645.00,1750.00,1590.00,54.8,HOLD,Consolidation Near Support
                        INFY,NSE,IT Services,1865.00,1980.00,1810.00,65.3,BUY,Bullish Flag Pattern
                        ZOMATO,NSE,Consumer Tech,268.00,310.00,248.00,71.2,MOMENTUM,Fresh 52-Week High
                    """.trimIndent()
                }

                // 6. Expense & Monthly Budget
                q.contains("expense") || q.contains("kharcha") || q.contains("budget") || q.contains("paisa") -> {
                    topicLabel = "Monthly Expense Budget"
                    fileName = "Monthly_Expense_Budget_$timestamp.csv"
                    content = """
                        Date,Category,Description,Payment Mode,Budget Allocated (INR),Actual Spent (INR),Variance
                        2026-11-01,Groceries,Monthly Supermarket Supplies,UPI,10000,9450,+550 (Under)
                        2026-11-04,Utilities,Electricity & Fiber Broadband,NetBanking,4000,3850,+150 (Under)
                        2026-11-08,Fuel & Transport,Car Petrol & Metro Card,CreditCard,6000,6200,-200 (Over)
                        2026-11-12,Dining & Outings,Weekend Dinner with Friends,UPI,5000,4800,+200 (Under)
                        2026-11-16,Cloud & Subscriptions,Server hosting & Gemini AI,Card,3000,2890,+110 (Under)
                        TOTALS,,, ,₹28000,₹27190,+₹810 SAVINGS
                    """.trimIndent()
                }

                // 7. Student Marksheet & Academic Grading
                q.contains("student") || q.contains("marksheet") || q.contains("result") || q.contains("school") || q.contains("college") -> {
                    topicLabel = "Student Marksheet"
                    fileName = "Student_Marksheet_$timestamp.csv"
                    content = """
                        Roll No,Student Name,Maths,Physics,Chemistry,Computer Science,English,Total (500),Percentage,Grade,Status
                        201,Aarav Sharma,96,92,89,99,94,470,94.0%,A+,PASSED
                        202,Diya Nair,88,85,91,96,90,450,90.0%,A+,PASSED
                        203,Rohan Verma,74,78,72,85,81,390,78.0%,B+,PASSED
                        204,Sneha Patel,95,94,96,100,95,480,96.0%,A+ (Topper),PASSED
                        205,Vikram Malhotra,82,79,84,89,86,420,84.0%,A,PASSED
                    """.trimIndent()
                }

                // 8. Custom Dynamic Data (Extracts terms from Boss's query)
                else -> {
                    val customName = q.replace(Regex("""(?:sheet|excel|csv|file|banao|bana|do|karo|please|ki|ka)"""), "").trim()
                    val cleanTopic = if (customName.isNotBlank() && customName.length < 25) {
                        customName.replaceFirstChar { it.uppercase() }
                    } else {
                        "Project Master Data"
                    }

                    topicLabel = cleanTopic
                    fileName = "${cleanTopic.replace(" ", "_")}_$timestamp.csv"
                    content = """
                        Entry ID,Record Name,Category,Quantity / Metric,Assigned Owner,Status,Target Date,Remarks
                        01,$cleanTopic Entry Alpha,Primary Unit,100,System Core,Verified,2026-11-25,Priority High
                        02,$cleanTopic Entry Beta,Secondary Unit,250,Operations,In Progress,2026-11-28,On Schedule
                        03,$cleanTopic Entry Gamma,Review Tier,15,Quality Assurance,Completed,2026-11-21,Validated by Jarvis
                        04,$cleanTopic Entry Delta,Expansion,50,Strategy Team,Scheduled,2026-12-05,Pending Resource
                        05,$cleanTopic Entry Epsilon,Optimization,80,Autonomous AI,Active,2026-11-30,Running Smoothly
                    """.trimIndent()
                }
            }

            val targetFile = File(docsDir, fileName)
            targetFile.writeText(content)

            return GenerationResult(
                success = true,
                file = targetFile,
                textMessage = "Boss, aapki requested **$topicLabel** Excel / CSV file ready kar di hai:\n\n" +
                        "📁 **File:** `${targetFile.name}`\n" +
                        "📊 **Type:** Excel / CSV Spreadsheet\n" +
                        "💾 **Size:** ${targetFile.length()} bytes\n\n" +
                        "Aap ise neeche diye gaye button par tap karke Google Sheets ya Excel me open kar sakte hain.\n\n" +
                        "👉 *Boss, kya is sheet me koi aur column, calculation ya custom data add karna hai?*",
                voiceMessage = "Boss, $topicLabel Excel sheet bana kar ready kar di hai. Kya isme kuch aur add karna hai?",
                fileType = "SHEET"
            )
        } catch (e: Exception) {
            return GenerationResult(
                success = false,
                file = null,
                textMessage = "Sheet create karte waqt error aaya: ${e.localizedMessage}",
                voiceMessage = "Sheet create karne mein samasya aayi Boss.",
                fileType = "SHEET"
            )
        }
    }
}
