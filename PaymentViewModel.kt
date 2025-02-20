package com.deep.infotech.atm_card_wallet.ui.ScanCard

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import com.deep.infotech.atm_card_wallet.R
import com.deep.infotech.atm_card_wallet.databinding.ScanDataBottomDialogBinding
import com.deep.infotech.atm_card_wallet.maniya.model.DatabaseHelperManiya
import com.deep.infotech.atm_card_wallet.maniya.test.dataModel.PaymentCardScanDataManiya
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentViewModel : ViewModel() {

    var cardNO: String? = ""
    var cardExpiredDate: String? = ""
    var cardType: String? = ""
    var cardCVV: String? = ""
    var cardHolder: String? = ""
    var cardTier: String? = ""
    var cardBankNM: String? = ""
    var isValidCard: String? = ""
    var drivingLicenceNo: String? = ""


    val parsedDates = mutableListOf<Date>()

    val detectedBlocks: MutableList<String> = ArrayList()

    val EXPIRY_DATE_REGEX: String = """^(0?[1-9]|1[0-2])([/-])(\d{2}|\d{4})$|^(0?[1-9]|[12][0-9]|3[01])([/-])(0?[1-9]|1[0-2])([/-])(\d{2}|\d{4})$"""

    // Regex patterns for various card information
    val cardNumberPattern = Regex("\\b(?:\\d{4}[-\\s]?){3}\\d{4}|\\d{13,19}\\b")
    val cardholderNamePattern = Regex("\\b[A-Za-z][a-zA-Z]+(?: [A-Za-z][a-zA-Z]+)*\\b")
    val expiryDatePattern = Regex("\\b(0[1-9]|1[0-2])/\\d{2,4}\\b")
    val cardBankPattern = Regex("^[A-Za-z\\s]+\$")
    val cardTypePattern = Regex("\\b(?:VISA|MasterCard|AMEX|Discover|JCB|Diners Club)\\b")
    val bankNamePattern = Regex("\\b(?:Bank|Financial|Credit|Union|Institute)\\s?[A-Za-z]+(?: [A-Za-z]+)*\\b")
    val cardTierPattern = Regex("\\b(?:Platinum|Gold|Black|Classic|Silver|Titanium)\\b")

    //BACK side
    val cvvPattern = Regex("\\b\\d{3,4}\\b")
    val signaturePattern = Regex("\\bSIGNATURE\\b")

    // Regex patterns for various flight ticket information
    val flightNumberPattern = Regex("\\b[A-Z]{2}\\d{1,4}\\b")  //  flight number (e.g., UA303)
    val passengerNamePattern = Regex("\\b[A-Z][a-z]+(?: [A-Z][a-z]+)+\\b")  //  names
    val datePattern =
        Regex("\\b(?:\\d{1,2}[-/.\\s]?\\d{1,2}[-/.\\s]?\\d{4}|\\w{3,9} \\d{1,2}, \\d{4})\\b")  //  dates
    val ticketTypePattern =
        Regex("\\b(Economy|Business|First|Premium|Class|Comfort|Economy\\+|Standard|Saver|Flex)\\b")  //  ticket type
    val seatNumberPattern = Regex("\\b[1-9][0-9]{0,2}[A-Z]\\b")  //  seat numbers (e.g., 12A)
    val ticketNumberPattern = Regex("\\b\\d{13}\\b")  // 13-digit ticket numbers
    val pricePattern = Regex("\\b(?:\\$\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?|€\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)\\b")  //  price
    val frequentFlyerPattern = Regex("\\b[A-Za-z0-9]{6,12}\\b")  // flyer numbers

    val drivinceLicencePattern= Regex("^(([A-Z]{2}[0-9]{2})( )|([A-Z]{2}-[0-9]{2}))((19|20)[0-9][0-9])[0-9]{7}$")

    // List of restricted keywords that should not appear in the cardholder name
    val restrictedKeywords = listOf(
        "platinu", "platinum", "gold", "black", "classic", "silver", "titanium",
        "bank", "credit", "debit", "atm", "transaction",
        "international", "card", "paypal", "money", "e-commerce", "charge",
        "visa", "mastercard", "american express", "discover", "jcb", "diners club",
        "unionpay", "maestro", "rupay", "laser", "china unionpay,payment",
        "valid", "from", "thru", "validity", "birthdate", "dob","authorized", "signature"
    )

    fun invalidateData() {
        cardNO = null
        cardExpiredDate = null
        cardType = null
        cardCVV = null
        cardHolder = null
        cardTier = null
        cardBankNM = null
        isValidCard = null

    }

    fun findMaxDate() {
        // SimpleDateFormat for parsing the dates
        val dateFormats = listOf(
            SimpleDateFormat("MM-yy", Locale.getDefault()), // MM-YY format (05-23)
            SimpleDateFormat("MM/yyyy", Locale.getDefault()), // MM/YYYY format (05/2023)
            SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()), // MM-DD-YYYY format (05-04-2040)
            SimpleDateFormat("M-dd-yyyy", Locale.getDefault()), // M-DD-YYYY format (5-04-2040)
            SimpleDateFormat("M-dd-yy", Locale.getDefault()), // M-DD-YY format (5-04-20)
            SimpleDateFormat("MM/dd/yy", Locale.getDefault()), // MM/DD/YY format (05/23)
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()), // MM/DD/YYYY format (05/04/2023)
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // dd/MM/YYYY format (05/10/2023)
        )

        // Iterate through the detectedBlocks and try parsing each one with different date formats
        detectedBlocks.forEach { blockText ->
            dateFormats.forEach { format ->
                try {
                    // Try parsing each block with each date format
                    val date = format.parse(blockText)
                    if (date != null) {
                        parsedDates.add(date)
                    }
                } catch (e: Exception) {
                    // If parsing fails, we ignore the block
                }
            }
        }

        // Now we need to find the latest (most recent) date from the parsedDates list
        val latestDate = parsedDates.maxOrNull()

        // Check if the latest date is expired
        val isExpired =
            latestDate?.let { isExpired(it) } ?: true // Default to expired if no valid date found

        if (!isExpired) {
            cardExpiredDate = "${latestDate}"
            Log.d("DATES+++", "${latestDate}")
        }
    }

    fun checkValidCardNumber(cardNo: String): Boolean {
            val digits = cardNo.replace(" ", "").reversed().map { it.toString().toInt() }
            val sum = digits.mapIndexed { index, digit ->
                if (index % 2 == 1) {
                    val doubled = digit * 2
                    if (doubled > 9) doubled - 9 else doubled
                } else {
                    digit
                }
            }.sum()
            return sum % 10 == 0
    }

    fun isAlphabeticWithSpaces(str: String): Boolean {
        // Regular expression to check if the string contains only alphabetic characters and spaces
        val regex = Regex("^[a-zA-Z ]+$")
        return regex.matches(str)
    }
    fun isValidDrivingLicenceNumber(str: String): Boolean {
        return drivinceLicencePattern.matches(str)
    }

    fun getCardType(cardNumber: String): String {
        val cleanCardNumber = cardNumber.replace(" ", "") // Remove spaces from the card number

        return when {


            // Visa: Starts with 4 and length 13 or 16 digits
            cleanCardNumber.matches("^4[0-9]{12}(?:[0-9]{3})?$".toRegex()) -> "Visa"

            // MasterCard: Starts with 51-55 and length 16 digits
            cleanCardNumber.matches("^5[1-5][0-9]{14}$".toRegex()) -> "MasterCard"

            // American Express: Starts with 34 or 37 and length 15 digits
            cleanCardNumber.matches("^3[47][0-9]{13}$".toRegex()) -> "American Express"

            // Discover: Starts with 6011, 622126-622925, 644-649, or 65 and length 16 digits
            cleanCardNumber.matches("^(6011|622[126-925]|64[4-9]|65)[0-9]{12}$".toRegex()) -> "Discover"

            // JCB: Starts with 3528-3589 and length 16 digits
            cleanCardNumber.matches("^35[2-8][0-9]{13}$".toRegex()) -> "JCB"

            // Diners Club: Starts with 36, 38, 39 and length 14 digits
            cleanCardNumber.matches("^(36|38|39)[0-9]{12}$".toRegex()) -> "Diners Club"

            // UnionPay: Starts with 62 and length 16-19 digits
            cleanCardNumber.matches("^62[0-9]{14,17}$".toRegex()) -> "UnionPay"

            // Maestro: Starts with 50, 5x, 6x, 63, 64, or 67 and length 12-19 digits
            cleanCardNumber.matches("^(50|5[1-5]|6[1-9]|63|64|67)[0-9]{11,18}$".toRegex()) -> "Maestro"

            // RuPay: Starts with 60, 65, 81, or 82 and length 16 digits
            cleanCardNumber.matches("^(60|65|81|82)[0-9]{12}$".toRegex()) -> "RuPay"

            // Laser: Starts with 6304, 6706, 6771, 6709 and length 16 digits
            cleanCardNumber.matches("^(6304|6706|6771|6709)[0-9]{12}$".toRegex()) -> "Laser"

            // Carte Blanche: Starts with 300-305 and length 14 digits
            cleanCardNumber.matches("^3[0-5][0-9]{13}$".toRegex()) -> "Carte Blanche"

            // InstaPayment: Starts with 637 and length 16 digits
            cleanCardNumber.matches("^637[0-9]{12}$".toRegex()) -> "InstaPayment"

            // Elo (Brazilian card network): Starts with 4011, 4312, 4389, 4514, 4576, or 5041
            cleanCardNumber.matches("^(4011|4312|4389|4514|4576|5041)[0-9]{12}$".toRegex()) -> "Elo"

            // Interpayment: Starts with 636 and length 16 digits
            cleanCardNumber.matches("^636[0-9]{12}$".toRegex()) -> "Interpayment"

            // Switch (UK): Starts with 4903, 4905, 4911, or 4936 and length 16 digits
            cleanCardNumber.matches("^(4903|4905|4911|4936)[0-9]{12}$".toRegex()) -> "Switch"

            // Solo: Starts with 6334 and length 16 digits
            cleanCardNumber.matches("^6334[0-9]{12}$".toRegex()) -> "Solo"

            // Maestro UK: Starts with 6759 and length 16 digits
            cleanCardNumber.matches("^6759[0-9]{12}$".toRegex()) -> "Maestro UK"

            // China UnionPay: Starts with 62 and length 16-19 digits
            cleanCardNumber.matches("^62[0-9]{14,17}$".toRegex()) -> "China UnionPay"

            // Unknown type if none match
            else -> "Unknown"
        }
    }



    fun isValidExpiryDateFormat(block: String): Boolean {
        /* val pattern = Pattern.compile(EXPIRY_DATE_REGEX)
         val matcher = pattern.matcher(block.trim { it <= ' ' })
         return matcher.matches()*/
        return Regex(EXPIRY_DATE_REGEX).matches(block)
    }

    // Method to check if a date has expired (compared to the current date)
    fun isExpired(date: Date?): Boolean {
        val currentDate = Date()
        return date!!.before(currentDate)
    }

    fun fixOCRArtifacts(rawText: String): String {
        val corrections = mapOf(

            "D" to "0",  // 'D' can be misread as '0' in certain fonts
            "Q" to "0",  // 'Q' can be misread as '0'
            "o" to "0",  // 'o' is often misread as '0'
            "c" to "0",  // 'c' is often misread as '0'
            "d" to "0",  // 'd' can be misread as '0'
            "C" to "0",  // 'C' can be misread as '0' in some fonts
            "O" to "0",  // 'O' is often misread as '0'

            "I" to "1",  // 'I' is often misread as '1'
            "i" to "1",  // 'i' is often misread as '1'
            "l" to "1",// 'l' can be misread as '1'
            "L" to "1",// 'l' can be misread as '1'

            "Z" to "2",  // 'Z' can be misread as '2'

            "e" to "3",  // 'e' can be misread as '3'
            "E" to "3",  // 'E' can be misread as '3'

            "A" to "4", // 'A' can sometimes be misread as '4'
            "a" to "4",  // 'a' can be misread as '4'

            "S" to "5",  // 'S' can be misread as '5'

            "G" to "6",  // 'G' can be misread as '6'
            "b" to "6",  // 'b' can be misread as '6'
            "g" to "6",   // 'g' can be misread as '6'

            "T" to "7",  // 'T' can be misread as '7'
            "⅂" to "7",// '⅃' can be misread as '1'

            "F" to "8",  // 'F' can be misread as '8'
            "B" to "8",  // 'B' is sometimes misread as '8'

            "P" to "9",  // 'P' can be misread as '9'
            "q" to "9",  // 'q' can be misread as '9'
            "ᑫ" to "9"

        )
        var fixedText = rawText
        corrections.forEach { (misread, correct) ->
            fixedText = fixedText.replace(Regex("$misread"), correct)
        }
        return fixedText
    }

    @SuppressLint("SuspiciousIndentation")
    fun isValidCardholderName(cardholderName: String): Boolean {

        if (restrictedKeywords.any { cardholderName.contains(it, ignoreCase = true) }) {
            return false
        }

        val cleanHolderName = cardholderName.replace(" ", "")
            if (cleanHolderName.isNotEmpty() && cleanHolderName.length > 1) {
                if (cardholderNamePattern.matches(cleanHolderName)) {
                    cardHolder = cardholderName
                    Log.e("cardholderName+++", "cardholderName: ${cardHolder}")
                    return true
                } else {
                    return false
                }
            } else {
                return false
            }

    }

    fun getBankCardName(boxtext: String) {
        if (boxtext.contains("bank", ignoreCase = true))
        {
            if (cardBankPattern.matches(boxtext))
            {
                Log.e("formattedBankText+++", "formattedBankText: ${boxtext}")
                cardBankNM = boxtext.trim()
            }
        }
    }

    fun getValidCVV(cardType: String?, sortedBlocks: Array<String>?): Any? {
        for (block in sortedBlocks!!)
        {
            val blockText = block

            if(cardType.equals("American Express",ignoreCase = true))
            {
                    //----getCVVFromFRONTSide---------
                    if((blockText.trim().length == 3) ||
                        (blockText.trim().length == 4))
                    {
                        cardCVV = cvvPattern.find("${blockText}")?.value
                    Log.e("cvvPattern+++FRONT", "cvvPattern: ${cardCVV}")}

            }
            else
            {
                   //------------- getCVVFromBACKSide--------------
                    if(blockText.trim().length == 3){
                    cardCVV = cvvPattern.find("${blockText}")?.value
                    Log.e("cvvPattern+++BACK", "cvvPattern: ${cardCVV}")}
            }
        }
        return  cardCVV
    }


    fun showRecognizationBottomSheetDialog(context: Context,categoryID:Long,ocrText:String,stringFront:String,stringBack:String) {

       val formattedText= StringBuilder().append(
            "CardHolderName-->${cardHolder}\n" +
                    "CardNumber--->${cardNO}\n" +
                    "CardType--->${cardType}\n" +
                    "CardDATE--->${cardExpiredDate}\n" +
                    "CardCVV--->${cardCVV}\n" +
                    "isValidCardNumber--->${isValidCard}\n" +
                    "Bank--->${cardBankNM}\n" +
                    "CardTier--->${cardTier}\n"
        )
        Log.e("FINAL FRONT+BACK ---formattedText+++", "${formattedText}")

        val  bottomSheetDialog = BottomSheetDialog(context)
        val binding = ScanDataBottomDialogBinding.inflate(bottomSheetDialog.layoutInflater)
        bottomSheetDialog.setContentView(binding.root)

        binding.root.setBackgroundResource(R.drawable.bottom_sheet_background)
        binding.tvOCRData.text =ocrText

        binding.tvContinue.setOnClickListener {

            val scanCard = PaymentCardScanDataManiya(
                holderName = cardHolder.toString(),
                cardNumber = cardNO.toString(),
                cvv = cardCVV.toString(),
                expDate = cardExpiredDate.toString(),
                cardType = cardType.toString(),
                bankName = cardBankNM.toString(),
                cardPIN = cardTier.toString(),
                category = DatabaseHelperManiya(context).getCategoryDao().queryForId(5),
                frontCardImage = stringFront,
                backCardImage = stringBack,
                isLock = false,
                isFav = false,
                isDelete = false,
                isArchive = false
            )

            DatabaseHelperManiya(context).getPaymentDao().createOrUpdate(scanCard)

            bottomSheetDialog.dismiss()

        }

        binding.tvRetake.setOnClickListener {

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    fun showAlertDialog(context: Context, datatext: String/*, bitmap: Bitmap*/) {

        Log.d("TEXT+++", "DataText-->: ${datatext}")

        val builder = AlertDialog.Builder(context)
            .setTitle("Info")
            .setMessage(datatext)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                invalidateData()
            }
            .setNegativeButton("Dismiss") { dialog, _ ->
                dialog.dismiss()
                invalidateData()
            }

        val alertDialog = builder.create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
    }

    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()

        // Create a color matrix for grayscale conversion
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        // Apply the color matrix as a color filter
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter

        // Draw the original bitmap on the new canvas with the paint (grayscale effect)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayscaleBitmap
    }

    // parse the expiry date from a string block (MM/YY, MM/YYYY, etc.)
    fun parseExpiryDate(expiryDate: String): Date? {
        var dateFormat: SimpleDateFormat? = null

        try {
            // different formats: MM/YY, M/YY, MM/YYYY, M/YYYY, MM-DD-YYYY, etc.
            if (expiryDate.contains("/")) {
                dateFormat = if (expiryDate.length == 5) { // MM/YY format
                    SimpleDateFormat("MM/yy")
                } else if (expiryDate.length == 7){ // MM/YYYY or M/YYYY format
                    SimpleDateFormat("MM/yyyy")
                }else{
                    SimpleDateFormat("dd/MM/yyyy")

                }
            } else if (expiryDate.contains("-")) {
                // Handle MM-DD-YYYY or M-DD-YYYY format
                dateFormat = SimpleDateFormat("MM-dd-yyyy")
            }

            if (dateFormat != null) {
                return dateFormat.parse(expiryDate)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return null
    }
}

