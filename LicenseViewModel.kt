package com.deep.infotech.atm_card_wallet.ui.ScanCard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.deep.infotech.atm_card_wallet.R
import com.deep.infotech.atm_card_wallet.databinding.ScanDataBottomDialogBinding
import com.deep.infotech.atm_card_wallet.maniya.model.DatabaseHelperManiya
import com.deep.infotech.atm_card_wallet.maniya.test.dataModel.LicenseScanDataManiya
import com.deep.infotech.atm_card_wallet.maniya.test.dataModel.PaymentCardScanDataManiya
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LicenseViewModel : ViewModel() {
    val parsedDates = mutableListOf<Date>()
    val detectedBlocks: MutableList<String> = ArrayList()
    val expiryBlocks: MutableList<String> = ArrayList()
    val futureBlocks: MutableList<String> = ArrayList()

    var expDate: String? = ""
    var issueDate: String? = ""
    var d_o_b: String? = ""

    var licenceNo: String? = ""
    var fullName: String? = ""
    var contactNumber: String? = ""
    var parentsName: String? = ""
    var gender: String? = ""
    var bloodGroup: String? = ""
    var issueCountry: String? = ""
    var issueAuthority: String? = ""
    var regAddress: String? = ""
    var classs: String? = ""

    val DATE_REGEX: String =
        """^(0?[1-9]|1[0-2])([/-])(\d{2}|\d{4})$|^(0?[1-9]|[12][0-9]|3[01])([/-])(0?[1-9]|1[0-2])([/-])(\d{2}|\d{4})$"""

    val restrictedKeywords = listOf(
        "platinu", "platinum", "gold", "black", "classic", "silver", "titanium",
        "bank", "credit", "debit", "atm", "transaction", "international", "card",
        "paypal", "money", "e-commerce", "charge", "visa", "mastercard", "american express",
        "discover", "jcb", "diners club", "unionpay", "maestro", "rupay", "laser",
        "china unionpay,payment", "valid", "from", "thru", "validity", "birthdate",
        "dob", "authorized", "signature", "licence", "idcard", "vehical",
        "name", "son","of","wife","daughter",
        "blood","group",
        "licence","card","vehicales","state","drive","no","yes","n/a","ID",
        "health","hospital",
        "date","issuedate","birthdate","expiryDate",
    )

    fun checkContactNumber(str: String) {
        val drivinceLicencePattern =
            Regex("")
        if (drivinceLicencePattern.matches(str))
            contactNumber = str
        Log.d("Licence+++contactNumber++++", str)
    }
    fun isExpired(dateString: String): Boolean {
        val dateFormats = listOf(
            "MM-yy",
            "MM/yyyy",
            "MM-dd-yyyy",
            "M-dd-yyyy",
            "M-dd-yy",
            "MM/dd/yy",
            "dd/MM/yyyy"
        )

        val currentDate = Date()

        for (format in dateFormats) {
            try {
                val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                dateFormat.isLenient = false
                val parsedDate = dateFormat.parse(dateString)

                if (parsedDate != null) {
                    return parsedDate.before(currentDate)
                }
            } catch (e: Exception) {
                continue
            }
        }
        throw IllegalArgumentException("Invalid date format")
    }

    fun isFutureDate(dateString: String) {
         if( !isExpired(dateString)){
             futureBlocks.add(dateString)
         }
    }

    fun findMaxDate(){
        val dateFormats = listOf(
            "MM-yy",
            "MM/yyyy",
            "MM-dd-yyyy",
            "M-dd-yyyy",
            "M-dd-yy",
            "MM/dd/yy",
            "dd/MM/yyyy"
        )

        val parsedDates = mutableListOf<Pair<Date, String>>()

        detectedBlocks.forEach { blockText ->
            dateFormats.forEach { format ->
                try {
                    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                    dateFormat.isLenient = false
                    val date = dateFormat.parse(blockText)
                    date?.let { parsedDates.add(Pair(it, format)) }
                } catch (e: Exception) {

                }
            }
        }

        val latestDatePair = parsedDates.maxByOrNull { it.first }
        val latestDate = latestDatePair?.first
        val matchedFormat = latestDatePair?.second

        val isExpired = latestDate?.let { isExpired(it) } ?: true

        if (!isExpired ) {
            matchedFormat?.let {
                val dateFormat = SimpleDateFormat(it, Locale.getDefault())
                val formattedDate = dateFormat.format(latestDate)
                if(expDate.equals(""))
                {
                Log.e("Licence+++Formatted--ExpDate---+++", formattedDate)
                expDate = formattedDate.toString()
                detectedBlocks.remove(expDate)
               }
                if(detectedBlocks.size>0){
                    findMaxDate()?.toString()
                }

            }
        }
        else{
                matchedFormat?.let {
                    val dateFormat = SimpleDateFormat(it, Locale.getDefault())
                    val formattedDate = dateFormat.format(latestDate)
                    if(issueDate.equals(""))
                    {
                    Log.e("Licence+++Formatted--IssueDate---+++", formattedDate)
                    issueDate = formattedDate.toString()
                    detectedBlocks.remove(issueDate)
                    }
                    else
                    {
                        if(d_o_b.equals("")){
                            Log.e("Licence+++Formatted--DOBDate---+++", formattedDate)
                            d_o_b = formattedDate.toString()
                            detectedBlocks.remove(d_o_b)
                        }
                    }
                    if(detectedBlocks.size>0){
                        findMaxDate()?.toString()
                    }
                }

        }

    }


    fun checkFullName(cardholderName: String) {
        if(cardholderName.length>2)
        {
        if (restrictedKeywords.any { cardholderName.contains(it, ignoreCase = true) }) return
        val cleanHolderName = cardholderName.trim()
        if (cleanHolderName.isNotEmpty() && cleanHolderName.length > 1) {
            val fullNamePattern = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")
            if (fullNamePattern.matches(cleanHolderName)) {
                fullName = cardholderName
                Log.e("Licence+++FullName---+++", "cardholderName: $fullName")
            }
        }
        }
    }
    fun checkParentsName(cardholderName: String) {
        if (restrictedKeywords.any { cardholderName.contains(it, ignoreCase = true) }) return
        val cleanHolderName = cardholderName.trim()
        if (cleanHolderName.isNotEmpty() && cleanHolderName.length > 1) {
            val fullNamePattern = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")
            if (fullNamePattern.matches(cleanHolderName)) {
                parentsName = cardholderName
                Log.e("Licence+++parentsName---+++", "parentsName: $parentsName")
            }
        }
    }

    fun checkGender(text: String) {
        val genderPattern = Regex("^(?:m|M|male|Male|f|F|female|Female|FEMALE|MALE|Not prefer to say)$")
        if (genderPattern.matches(text)) {
            gender = text
        }
    }
    fun checkBloodGroup(str: String) {
        val bloodGroupPattern = Regex("^(A|B|AB|O)[+-]$")
        if (bloodGroupPattern.matches(str)) {
            bloodGroup=str
            Log.d("Licence+++BloodGroup++++", str)

        }
    }

    fun checkDrivingLicenceNumber(str: String) {
        val drivinceLicencePattern = Regex("^[A-Z]{2}[0-9]{2}( |-|)[0-9]{4}((19|20)[0-9]{2})[0-9]{7}$")
        if (drivinceLicencePattern.matches(str)) {
            licenceNo = str
            Log.d("Licence+++drivingLicenceNo++++", str)
        }
    }

    fun isExpired(date: Date?): Boolean {
        val currentDate = Date()
        return date?.before(currentDate) ?: false
    }
    fun checkFutureDate(date: Date?) {
        val currentDate = Date()
        if( date!!.after(currentDate)){
           futureBlocks.add(date.toString())
        }
    }

    fun checkDateFormat(block: String) {
        if (Regex(DATE_REGEX).matches(block)) {
            detectedBlocks.add(block)
            isFutureDate(block)
           if( isExpired(block)){
               expiryBlocks.add(block)
           }
        }
    }
    fun checkBirthDateFormat(block: String) {
        if (Regex(DATE_REGEX).matches(block)) {
            d_o_b= block
            Log.d("Licence+++DOBDate++++", d_o_b.toString())
        }
    }
    fun checkClassFormate(block: String) {
        if (Regex("^[A-Za-z]$").matches(block)) {
            classs= block
        }
    }
    fun checkExpiryDateFormat(block: String) {
        if (Regex(DATE_REGEX).matches(block)) {
            expDate= block
            Log.d("Licence+++ExpiryDate++++", expDate.toString())
        }
    }
    fun checkIssueDateFormat(block: String) {
        if (Regex(DATE_REGEX).matches(block)) {
            issueDate= block
            Log.d("Licence+++IssueDate++++", issueDate.toString())
        }
    }

    fun setExpiryDate() {
        if(detectedBlocks.size>0){
         findMaxDate()?.toString() }
    }


    fun addLicenceData(context: Context, categoryID: Long, ocrText: String, stringFront: String, stringBack: String) {
        val licenseCard = LicenseScanDataManiya(
            documentNumber = licenceNo.toString(),
            issueDate = issueDate.toString(),
            expiryDate = expDate.toString(),
            dob = d_o_b.toString(),
            fullName = fullName.toString(),
            personamNumber = contactNumber.toString(),
            issueCountry = issueCountry.toString(),
            issueAuthority = issueAuthority.toString(),
            regAddress = regAddress.toString(),
            category = DatabaseHelperManiya(context).getCategoryDao().queryForId(1),
            frontCardImage = stringFront,
            backCardImage = stringBack,
            isLock = false,
            isFav = false,
            isDelete = false,
            isArchive = false
        )

        DatabaseHelperManiya(context).getLicenceCardDao().createOrUpdate(licenseCard)
    }
}


