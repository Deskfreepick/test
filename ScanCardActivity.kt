package com.deep.infotech.atm_card_wallet.ui.ScanCard

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.deep.infotech.atm_card_wallet.DBHelper.ScanDatabase
import com.deep.infotech.atm_card_wallet.R
import com.deep.infotech.atm_card_wallet.adapter.ImageRecyclerAdapter
import com.deep.infotech.atm_card_wallet.databinding.ActivityScanCardBinding
import com.deep.infotech.atm_card_wallet.databinding.DialogHistoryLockBinding
import com.deep.infotech.atm_card_wallet.databinding.ScanDataBottomDialogBinding
import com.deep.infotech.atm_card_wallet.maniya.model.DatabaseHelperManiya
import com.deep.infotech.atm_card_wallet.maniya.test.TESTMainActivityManiya
import com.deep.infotech.atm_card_wallet.maniya.test.dataModel.LicenseScanDataManiya
import com.deep.infotech.atm_card_wallet.model.CardType
import com.deep.infotech.atm_card_wallet.model.ScanScanModel
import com.deep.infotech.atm_card_wallet.ui.BaseActivity
import com.deep.infotech.atm_card_wallet.ui.CadDetails.CardSaveHistoryActivity
import com.geniusscansdk.ocr.OcrConfiguration
import com.geniusscansdk.ocr.OcrProcessor
import com.geniusscansdk.ocr.OcrProcessor.ProgressListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScanCardActivity : BaseActivity() {

    private lateinit var binding: ActivityScanCardBinding

    val formattedText = StringBuilder()
    val entityText = StringBuilder()

    val entityValueList = ArrayList<Pair<String, String>>()

    private val imagesList = mutableListOf<Bitmap>()

    private lateinit var progressDialog: ProgressDialog
    private var frontImage: Bitmap? = null
    private var backImage: Bitmap? = null

    private var bitmapToStringFront: ByteArray? = null
    private var stringFront: String = ""
    private var stringBack: String = ""
    private var bitmapToStringBack: ByteArray? = null

    var wordsFront: Array<String>? = null
    var wordsBack: Array<String>? = null

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editer: SharedPreferences.Editor

    private lateinit var cardTypeList: List<CardType>

    private var categoryName: String = "Custom"
    private var categoryId: Long = 15

    private val bankList = listOf(
        "STATE BANK OF INDIA",
        "BANK OF BARODA",
        "PUNJAB NATIONAL BANK",
        "UNION BANK OF INDIA",
        "CANARA BANK",
        "INDIAN BANK",
        "BANK OF INDIA",
        "CENTRAL BANK OF INDIA",
        "INDIAN OVERSEAS BANK",
        "UCO BANK",
        "BANK OF MAHARASHTRA",
        "PUNJAB & SIND BANK",
        "HDFC BANK",
        "ICICI BANK",
        "AXIS BANK",
        "KOTAK MAHINDRA BANK",
        "INDUSIND BANK",
        "YES BANK",
        "FEDERAL BANK",
        "SOUTH INDIAN BANK",
        "CITY UNION BANK",
        "TAMILNAD MERCANTILE BANK",
        "DHANLAXMI BANK",
        "IDFC FIRST BANK",
        "RBL BANK",
        "STANDARD CHARTERED BANK",
        "HSBC BANK",
        "CITIBANK",
        "DEUTSCHE BANK",
        "BARCLAYS BANK",
        "BANK OF AMERICA",
        "BNP PARIBAS",
        "ANDHRA PRADESH GRAMEENA VIKAS BANK",
        "ARYAVART GRAMIN BANK",
        "BIHAR GRAMIN BANK",
        "ELLAQUAI DEHATI BANK",
        "KERALA GRAMIN BANK",
        "KARNATAKA VIKAS GRAMEENA BANK",
        "NARMADA JHABUA GRAMIN BANK",
        "PUNJAB GRAMIN BANK",
        "UTTARBANGA KSHETRIYA GRAMIN BANK",
        "THE COSMOS COOPERATIVE BANK",
        "THE GUJARAT COOPERATIVE BANK",
        "THE MAHARASHTRA STATE COOPERATIVE BANK",
        "THE SARASWAT BANK",
        "NATIONAL BANK FOR AGRICULTURE AND RURAL DEVELOPMENT (NABARD)",
        "SMALL INDUSTRIES DEVELOPMENT BANK OF INDIA (SIDBI)",
        "INDUSTRIAL DEVELOPMENT BANK OF INDIA (IDBI)",
        "POST OFFICE SAVINGS BANK",
        "PAYTM PAYMENTS BANK"
    )

    private var paymentViewModel: PaymentViewModel = PaymentViewModel()
    private var licenceViewModel: LicenseViewModel = LicenseViewModel()

    private lateinit var entityExtractor: EntityExtractor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCardBinding.inflate(layoutInflater)
        Handler().postDelayed({
            setContentView(binding.root)
        }, 2000)

        entityExtractor = EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        )
        lifecycle.addObserver(entityExtractor)


        categoryName = intent.getStringExtra("category_name").toString()
        categoryId = intent.getLongExtra("category_id", 15)

        Toast.makeText(this@ScanCardActivity, categoryId.toString(), Toast.LENGTH_SHORT).show()

        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.processing))
            setCancelable(false)
        }

        cardTypeList = listOf(
            CardType(getString(R.string.credit_card)),
            CardType(getString(R.string.debit_card)),
            CardType(getString(R.string.business_card)),
            CardType(getString(R.string.gift_card)),
            CardType(getString(R.string.travel_card)),
            CardType(getString(R.string.rewards_card)),
            CardType(getString(R.string.virtual_card)),
            CardType(getString(R.string.charge_card)),
            CardType(getString(R.string.prepaid_card)),
            CardType(getString(R.string.other_card))
        )

        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item,
            cardTypeList.map { it.typeName })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCardTypes.adapter = adapter
        binding.spinnerCardTypes.setSelection(0)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        editer = sharedPreferences.edit()

     initCamera()
        setupListeners()
        setupViewPager()

    }

    private fun initCamera() {
        val options = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(false).setPageLimit(2)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL).build()

        val scanner = GmsDocumentScanning.getClient(options)

        val scannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    handleScanningResult(result.data)
                } else {
                    Log.e(
                        "ScanCardActivity++++++",
                        "Scanning failed with resultCode: ${result.resultCode}"
                    )
                    val intent = Intent(this@ScanCardActivity, TESTMainActivityManiya::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finishAffinity()
                }
            }

        startScanning(scanner, scannerLauncher)
    }

    private fun getEntityExtractionParams(input: String): EntityExtractionParams {
        return EntityExtractionParams.Builder(input).build()
    }

    private fun startScanning(
        scanner: GmsDocumentScanner, scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {

        scanner.getStartScanIntent(this).addOnSuccessListener { intentSender ->
            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }.addOnFailureListener { e ->
            Log.e("ScanCardActivity", "Failed to start scanning: ${e.message}")
            Toast.makeText(
                this, getString(R.string.failed_to_start_scanning_try_again), Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun bitmapToByteArray(bitmap: Bitmap?): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun handleScanningResult(data: Intent?) {
        val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
        gmsResult?.pages?.let { pages ->
            try {
                Log.d("ScanCardActivit+++y", "Number of pages: ${pages.size}")

                if (pages.isNotEmpty()) {
                    // Process front image
                    val frontUri = pages[0].imageUri
                    stringFront = frontUri.toString()

                    //  bitmapToStringFront = frontUri.toString()
                    val frontFile = File(URI(frontUri.toString()))
                    Log.d("ScanCardActivit+++y", "frontUri: ${frontUri}")

                    if (frontFile.exists()) {
                        frontImage = BitmapFactory.decodeFile(frontFile.absolutePath)
                        bitmapToStringFront = bitmapToByteArray(frontImage)
                        imagesList.add(frontImage!!)
                    }

                    // Process back image if available
                    if (pages.size > 1) {
                        val backUri = pages[1].imageUri
                        stringBack = backUri.toString()
                        // bitmapToStringBack = backUri.toString()

                        val backFile = File(URI(backUri.toString()))

                        if (backFile.exists()) {
                            backImage = BitmapFactory.decodeFile(backFile.absolutePath)
                            bitmapToStringBack = bitmapToByteArray(backImage)
                            backImage?.let { imagesList.add(it) }
                        }
                    } else {
                        Log.d("ScanCardActivity", "No back image found.")
                    }
                    // Update UI with the images and start text recognition process
                    setupViewPager()
                    // processImagesForText()
                    /* GENIUSSSSSSSS   processImagesForTextAnkita()*/

                    /*-----------------------Ankita---------------------------*/
                    val ocrConfiguration = OcrConfiguration(mutableListOf("en-US"))
                    val ocrProcessor = OcrProcessor(
                        this@ScanCardActivity,
                        ocrConfiguration,
                        object : ProgressListener {
                            override fun onProgressUpdate(progress: Int) {

                            }
                        })
                    try {
                        val result =
                            ocrProcessor.processImage(File(URI(pages[0].imageUri.toString())))

                        Log.w("SCANACT++++OCR++++++FRONT", result.text.trim { it <= ' ' })
                        // Split the string by space
                        wordsFront =
                            result.text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        Log.e("WORDS++++FRONT", wordsFront.contentToString())
                    } catch (e: Exception) {
                    }

                    if (pages.size > 1) {
                        val ocrConfiguration = OcrConfiguration(mutableListOf("en-US"))
                        val ocrProcessor = OcrProcessor(
                            this@ScanCardActivity,
                            ocrConfiguration,
                            object : ProgressListener {
                                override fun onProgressUpdate(progress: Int) {

                                }
                            })
                        try {
                            val result =
                                ocrProcessor.processImage(File(URI(pages[1].imageUri.toString())))

                            Log.w("SCANACT++++OCR++++++BACK", result.text.trim { it <= ' ' })
                            // Split the string by space
                            wordsBack =
                                result.text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            Log.e("WORDS++++BACK", wordsFront.contentToString())
                        } catch (e: Exception) {
                        }
                    }
                    entityText.clear()
                    getEntityInfo(false)

                    /*----------------------------------------------------------------------------------*/
                    /*               getCardInfoAnkitaGINUS()*/
                    /*----------------------------------------------------------------------------------*/


                } else {
                    Log.e("ScanCardActivity", "No pages returned from scanning.")
                }

            } catch (e: Exception) {
                Log.e("ScanCardActivity", "Error processing images: ${e.message}")
            }
        }
    }

    private fun setupListeners() {

        binding.llBack.setOnClickListener { onBackPressed() }

        binding.llEditCard.setOnClickListener {

            if (binding.edHolderName.text.isNotEmpty() || binding.edCardNumber.text.isNotEmpty() || binding.edCvv.text.isNotEmpty() || binding.edExpDate.text.isNotEmpty()) {

                if (sharedPreferences.getBoolean("historyDialogShow", true)) {
                    val binding = DialogHistoryLockBinding.inflate(LayoutInflater.from(this))
                    val dialog = Dialog(this)
                    dialog.setContentView(binding.root)
                    dialog.window!!.setBackgroundDrawable(ColorDrawable(0))

                    binding.llTurnOff.setOnClickListener {
                        editer.putBoolean("lockIsCheck", false)
                        editer.putBoolean("historyDialogShow", false)
                        editer.apply()
                        dialog.dismiss()
                    }

                    binding.llTurnOn.setOnClickListener {
                        editer.putBoolean("lockIsCheck", true)
                        editer.putBoolean("historyDialogShow", false)
                        editer.apply()
                        dialog.dismiss()
                    }
                    dialog.show()
                } else {
                    progressDialog.show()
                    val scanScanModel = ScanScanModel(
                        0,
                        binding.edHolderName.text.toString(),
                        maskCardNumber(binding.edCardNumber.text.toString()),
                        binding.edCvv.text.toString(),
                        binding.edExpDate.text.toString(),
                        "",
                        "",
                        cardTypeList[binding.spinnerCardTypes.selectedItemPosition].typeName,
                        binding.edBankName.text.toString()
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        ScanDatabase.getDatabase(this@ScanCardActivity).cardDao()
                            .insertCard(scanScanModel)

                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()

                            Toast.makeText(
                                this@ScanCardActivity,
                                getString(R.string.saved_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }

                    startActivity(
                        Intent(this, CardSaveHistoryActivity::class.java).putExtra(
                            "pos", 1
                        )
                    )
                }

            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_fill_all_the_required_fields),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length >= 16) {
            "**** **** **** " + cardNumber.takeLast(4)
        } else {
            cardNumber
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = ImageRecyclerAdapter(imagesList)
        binding.dotsIndicator.setViewPager2(binding.viewPager)
    }

    private fun processImagesForText() {
        lifecycleScope.launch(Dispatchers.Main) {
            val frontTextDeferred = async { extractText(frontImage!!) }
            val frontText = frontTextDeferred.await()

            val backText = backImage?.let { async { extractText(it) } }?.await()

            if (frontText.isEmpty() && backText.isNullOrEmpty()) {
                Log.e("fatal", "Failed to extract text from images")
                progressDialog.dismiss()
                return@launch
            }

            val combinedText = "$frontText ${backText.orEmpty()}"
            Log.e("fatal", "Extracted text: $combinedText")
            extractCardDetails(combinedText)
            progressDialog.dismiss()
        }
    }

    private fun processImagesForTextAnkita() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (frontImage != null) {
                extractTextManiya(frontImage!!, isBackImage = false)
            }

            //  extractTextManiya(frontImage!!, isBackImage)
            /* val frontTextDeferred = async { extractTextManiya(frontImage!!) }
             val frontText = frontTextDeferred.await()*/

            /* val backText = backImage?.let { async { extractTextManiya(it) } }?.await()*/

            /* if (frontText?.textBlocks!!.isEmpty() && backText?.textBlocks!!.isEmpty()) {
                 Log.e("ANKITA++++++", "Failed to extract text from images")
                 progressDialog.dismiss()
                 return@launch
             }*/


            /*  val combinedSortedBlocks = if (backText?.textBlocks.isNullOrEmpty()) {
                  frontText.textBlocks  // If backText's textBlocks is empty or null, just use frontText's blocks
              } else {
                  mergeSortedBlocks(frontText.textBlocks, backText?.textBlocks)  // Otherwise, merge both lists
              }

              Log.e("ANKITA++++++", "Extracted text: ${combinedSortedBlocks}")

              runBlocking {
                  withContext(Dispatchers.Default) {
                      val detectedValues =  viewModel.processText(this@ScanCardActivity,combinedSortedBlocks)

        *//*--------------------------------------
                    cardHolder//0
                    cardNO//1
                    cardType//2
                    cardCVV//3
                    cardExpiredDate//4
                    isValidCard//5
                    cardBankNM//6
                    cardTier//7

      -----------------------------------------*//*
                    binding.edHolderName.setText(detectedValues.get(0))
                    binding.edCardNumber.setText(detectedValues.get(1))
                    binding.edExpDate.setText(detectedValues.get(4))
                    binding.edCvv.setText(detectedValues.get(3))
                    binding.edBankName.setText(detectedValues.get(6))
                }
            }*/

            // progressDialog.dismiss()
        }
    }

    private fun getEntityInfo(isWordsBack: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!isWordsBack) {
                if (frontImage != null) {
                    processEntity(this@ScanCardActivity, wordsFront, false, true)
                }
            } else {
                if (backImage != null) {
                    processEntity(this@ScanCardActivity, wordsBack, false, false)
                }
            }
        }
    }

    private fun getCardInfoAnkitaGINUS() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (frontImage != null) {
                processPaymentCardTextTESTGENIUS(this@ScanCardActivity, wordsFront, false)
            }
        }
    }

    private fun getDriverLicenceInfoAnkitaGINUS() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (frontImage != null) {
                processDriverLicenceTextTESTGENIUS(this@ScanCardActivity, wordsFront, false)
            }
        }
    }

    private suspend fun extractText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText -> continuation.resume(visionText.text, null) }
                .addOnFailureListener { e ->
                    Log.e("extractTextFromImage", "OCR failed: ${e.message}")
                    continuation.resume("", null)
                }
        }

    private fun extractTextManiya(bitmap: Bitmap, isBackImage: Boolean) {

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var text: Text? = null

        val latinrecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        /*         val chineserecognizer =
                     TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

                 val devanagarirecognizer =
                     TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

                 val japaneserecognizer =
                     TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

                 val koreanrecognizer =
                     TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())*/

        latinrecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                if (visionText?.textBlocks!!.isNotEmpty()) {
                    Log.e("ANKITA++++++", "Processing  image.......")
                    /*progressDialog.dismiss()*/
                    processTextTEST(this@ScanCardActivity, visionText, isBackImage)
                }
                /*extractTextManiya(backImage!!)*/
                // continuation.resume(visionText, null)

            }
            .addOnFailureListener {
                Toast.makeText(
                    this@ScanCardActivity,
                    "Text could not be read!",
                    Toast.LENGTH_SHORT
                ).show()
                // continuation.resume(text, null)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun processTextTEST(
        context: Context,
        visionText: Text,
        isBackImage: Boolean
    ) {
        val blocks = visionText.text
        Log.e("+++++", blocks.toString());


        if (blocks.isEmpty()) {
            Toast.makeText(context, "no detected OCR", Toast.LENGTH_SHORT).show()
            if (!isBackImage && backImage != null) {
                extractTextManiya(backImage!!, isBackImage = true)
            } else {
                progressDialog.dismiss()
                return
            }
        }
        //  sort the blocks based on the top coordinate (from top to bottom)
        val sortedBlocks =
            visionText.textBlocks.sortedBy { block -> block.boundingBox?.top ?: Int.MAX_VALUE }
        if (!isBackImage)//isBackImage=FALSE
        {
            var t = StringBuilder()
            for (block in sortedBlocks) {

                for (line in block.lines) {
                    for (element in line.elements) {
                        t.append(element.text + "\n")
                    }
                }

                val blockText = block.text.trim()
                val g = block.recognizedLanguage
                val blockBounds = block.boundingBox
                Log.d("blockText++++", block.text)
                // Check if the block text contains a possible card number based on length (typically between 13 to 19 digits)
                if (blockText.length in 13..19) {
                    if (!paymentViewModel.isAlphabeticWithSpaces(blockText)) {
                        val cardNumber = paymentViewModel.fixOCRArtifacts(blockText)
                        val cleanCardNumber = cardNumber.replace(" ", "")
                        val regex = "^\\d+$".toRegex()
                        if (regex.matches(cleanCardNumber)) {
                            paymentViewModel.cardNO = cardNumber
                            Log.e("Cleaned Card Number++++", cardNumber)
                            paymentViewModel.cardType = paymentViewModel.getCardType(cardNumber)
                            Log.e("Card Type++++", paymentViewModel.getCardType(cardNumber))
                            val isvalid = paymentViewModel.checkValidCardNumber(cardNumber)
                            paymentViewModel.isValidCard = "${isvalid}"
                            Log.e("IsCardNumberValid?++++", "${isvalid}")
                        }
                    }

                } else {

                    if (paymentViewModel.isValidExpiryDateFormat("${blockText}")) {
                        var xDate = paymentViewModel.parseExpiryDate("${blockText}")
                        if (!paymentViewModel.isExpired(xDate)) {
                            paymentViewModel.detectedBlocks.add("${blockText}")
                            if (paymentViewModel.detectedBlocks.size > 1) {
                                paymentViewModel.findMaxDate()
                            } else {
                                paymentViewModel.cardExpiredDate = "${blockText}"
                                Log.e("DATES+++", "${blockText}")
                            }
                        }
                    }

                    val cardType1 = paymentViewModel.cardTypePattern.find("${blockText}")?.value
                    Log.e("cardType1+++", "cardType: ${cardType1}")

                    paymentViewModel.cardTier =
                        paymentViewModel.cardTierPattern.find("${blockText}")?.value
                    Log.e("cardTier+++", "cardTier: ${paymentViewModel.cardTier}")

                }

                paymentViewModel.getBankCardName("${blockText}")
                Log.e("bankName+++", "${paymentViewModel.cardBankNM}")

                if (paymentViewModel.isAlphabeticWithSpaces(blockText)) {

                    paymentViewModel.isValidCardholderName("${blockText}")
                }
            }
            /*   if (!viewModel.cardType.isNullOrEmpty()) {
                   viewModel.cardCVV = viewModel.getValidCVV(viewModel.cardType, sortedBlocks)?.toString()
               }*/
            Log.d("TT++++", t.toString())

        } else {
            if (isBackImage && backImage != null) { //isBackImage=TRUE
                /* if (!viewModel.cardType.isNullOrEmpty()) {
                     viewModel.cardCVV = viewModel.getValidCVV(viewModel.cardType, sortedBlocks)?.toString()
                 }*/
            }
        }

        if (!isBackImage && backImage != null) {
            extractTextManiya(backImage!!, isBackImage = true)
        } else {
            progressDialog.dismiss()
        }

        binding.edHolderName.setText(paymentViewModel.cardHolder)
        binding.edCardNumber.setText(
            "Card NO:- " + paymentViewModel.cardNO
                    + "\nCard TYPE:- " + paymentViewModel.cardType
        )
        binding.edExpDate.setText(
            "Exp:- " + paymentViewModel.cardExpiredDate
                    + "\nis Valid?:- " + paymentViewModel.isValidCard
        )
        binding.edCvv.setText(paymentViewModel.cardCVV)
        binding.edBankName.setText(paymentViewModel.cardBankNM + "\nCardTier:- " + paymentViewModel.cardTier)

        //append data in collectedData text
        formattedText.append(
            "CardHolderName-->${paymentViewModel.cardHolder}\n" +
                    "CardNumber--->${paymentViewModel.cardNO}\n" +
                    "CardType--->${paymentViewModel.cardType}\n" +
                    "CardCVV--->${paymentViewModel.cardCVV}\n" +
                    "CardDATE--->${paymentViewModel.cardExpiredDate}\n" +
                    "isValidCardNumber--->${paymentViewModel.isValidCard}\n" +
                    "Bank--->${paymentViewModel.cardBankNM}\n" +
                    "CardTier--->${paymentViewModel.cardTier}\n"
        )
        Log.e("FINAL formattedText+++", "${formattedText}")

    }

    suspend fun extractEntities(input: String) {
        Log.e("+++processEntity+++", "++++++++++++$input++++++++++++++")

        try {
            awaitDownloadModel()

            val result = awaitAnnotation(input)

            if (result.isEmpty()) {
                Log.e("+++processEntity+++", "+++++++++++++resultEmpty++++++++++++++\n")
                return
            }

            for (entityAnnotation in result) {
                val entities = entityAnnotation.entities
                val annotatedText = entityAnnotation.annotatedText
                for (entity in entities) {
                    displayEntityInfo(annotatedText, entity)
                    entityText.append("\n")
                }
            }


        } catch (e: Exception) {
            Log.e("+++processEntity+++", "extractEntities failed\n", e)
        }
    }

    // Suspend function to download the model
    private suspend fun awaitDownloadModel(): Unit {
        suspendCancellableCoroutine { continuation ->
            entityExtractor.downloadModelIfNeeded()
                .addOnSuccessListener {
                    continuation.resume(Unit)
                    /*Log.e("+++processEntity+++", "awaitDownloadModel Success")*/

                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                    Log.e("+++processEntity+++", "awaitDownloadModel failed\n")
                }
        }
    }

    // Suspend function to annotate the input
    private suspend fun awaitAnnotation(input: String): List<EntityAnnotation> {
        return suspendCancellableCoroutine { continuation ->
            entityExtractor.annotate(getEntityExtractionParams(input))
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                    /*  Log.e("+++extractEntities++", "Annotation Success")*/

                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                    Log.e("+++processEntity+++", "Annotation failed\n")

                }
        }
    }

    private fun displayEntityInfo(annotatedText: String, entity: Entity) {
        when (entity.type) {
            Entity.TYPE_ADDRESS -> displayAddressInfo(annotatedText)
            Entity.TYPE_DATE_TIME -> displayDateTimeInfo(entity, annotatedText)
            Entity.TYPE_EMAIL -> displayEmailInfo(annotatedText)
            Entity.TYPE_FLIGHT_NUMBER -> displayFlightNoInfo(entity, annotatedText)
            Entity.TYPE_IBAN -> displayIbanInfo(entity, annotatedText)
            Entity.TYPE_ISBN -> displayIsbnInfo(entity, annotatedText)
            Entity.TYPE_MONEY -> displayMoneyEntityInfo(entity, annotatedText)
            Entity.TYPE_PAYMENT_CARD -> displayPaymentCardInfo(entity, annotatedText)
            Entity.TYPE_PHONE -> displayPhoneInfo(annotatedText)
            Entity.TYPE_TRACKING_NUMBER -> displayTrackingNoInfo(entity, annotatedText)
            Entity.TYPE_URL -> displayUrlInfo(annotatedText)
            else -> ""
        }
    }

    private fun displayAddressInfo(annotatedText: String) {
        entityValueList.add(Pair("Address", annotatedText))
        entityText.append("Address:-->" + annotatedText + "\n")
    }

    private fun displayEmailInfo(annotatedText: String) {
        entityValueList.add(Pair("Email", annotatedText))
        entityText.append("Email-->" + annotatedText + "\n")
    }

    private fun displayPhoneInfo(annotatedText: String) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(annotatedText)) {
            entityValueList.add(Pair("Phone", annotatedText))

            entityText.append(
                "Phone-->" +
                        annotatedText + "----" +
                        PhoneNumberUtils.formatNumber(annotatedText) + "\n"

            )
        }
    }

    private fun displayUrlInfo(annotatedText: String) {
        entityValueList.add(Pair("URL", annotatedText))

        entityText.append("URL-->" + annotatedText).toString() + "\n"
    }

    private fun displayDateTimeInfo(entity: Entity, annotatedText: String) {
        entityValueList.add(Pair("Date", annotatedText))
        licenceViewModel.detectedBlocks.add(annotatedText)
        licenceViewModel.checkDateFormat(annotatedText)

        entityText.append(
            "Date:-" +
                    annotatedText + "\n"
        )
    }


    private fun displayTrackingNoInfo(entity: Entity, annotatedText: String) {
        val trackingNumberEntity = entity.asTrackingNumberEntity()
        entityValueList.add(
            Pair(
                "TrakingNumber",
                annotatedText + "Carrier" + trackingNumberEntity!!.parcelCarrier + "TrackingNumber" + trackingNumberEntity.parcelTrackingNumber
            )
        )
        entityText.append(
            "TrakingNumber" +
                    annotatedText + "----" +
                    trackingNumberEntity!!.parcelCarrier + "----" +
                    trackingNumberEntity.parcelTrackingNumber + "\n"

        )
    }

    private fun displayPaymentCardInfo(entity: Entity, annotatedText: String) {
        val paymentCardEntity = entity.asPaymentCardEntity()
        entityValueList.add(Pair("PaymentCard", paymentCardEntity!!.paymentCardNumber))

        entityText.append(
            "PaymentCard-->" +
                    annotatedText + "----" +
                    paymentCardEntity!!.paymentCardNetwork + "----" +
                    paymentCardEntity.paymentCardNumber + "\n"
        )
    }

    private fun displayIsbnInfo(entity: Entity, annotatedText: String) {
        entityValueList.add(Pair("ISBN", annotatedText + "ISBN" + entity.asIsbnEntity()!!.isbn))

        entityText.append(
            "ISBN-->" + annotatedText + "----" + entity.asIsbnEntity()!!.isbn
        ).toString() + "\n"

    }

    private fun displayIbanInfo(entity: Entity, annotatedText: String) {

        val ibanEntity = entity.asIbanEntity()
        entityValueList.add(
            Pair(
                "IBAN", annotatedText
                        + "IBAN" + ibanEntity!!.iban.toString()
                        + "COUNTRY" + ibanEntity!!.ibanCountryCode
            )
        )

        entityText.append(
            "IBAN" +
                    annotatedText + "----" +
                    ibanEntity!!.iban + "----" +
                    ibanEntity.ibanCountryCode + "\n"

        )
    }

    private fun displayFlightNoInfo(entity: Entity, annotatedText: String) {
        val flightNumberEntity = entity.asFlightNumberEntity()
        entityValueList.add(
            Pair(
                "FlightNo",
                annotatedText + "Number" + flightNumberEntity!!.flightNumber + "Code" + flightNumberEntity!!.airlineCode
            )
        )

        entityText.append(
            "FlightNo-->" +
                    annotatedText + "----" +
                    flightNumberEntity!!.airlineCode + "----" +
                    flightNumberEntity.flightNumber + "\n"

        )
    }

    private fun displayMoneyEntityInfo(entity: Entity, annotatedText: String) {
        val moneyEntity = entity.asMoneyEntity()
        entityValueList.add(
            Pair(
                "Money",
                annotatedText + "Number" + moneyEntity!!.unnormalizedCurrency
            )
        )
        entityText.append(
            "Money-->" +
                    annotatedText + "----" +
                    moneyEntity!!.unnormalizedCurrency + "----" +
                    moneyEntity.integerPart + "----" +
                    moneyEntity.fractionalPart + "\n"

        )
    }


    @SuppressLint("SetTextI18n")
    private fun processEntity(
        context: Context,
        visionText: Array<String>?,
        isBackImage: Boolean,
        isWordsBack: Boolean
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            for (block in visionText!!) {
                extractEntities(block.trim().replace(" ", ""))
            }
            Log.e("+++processEntity+++", "entityAnnotation---" + entityText)
            if (isWordsBack && backImage != null) {
                getEntityInfo(true)
            } else {
                when (categoryId) {
                    5L -> getCardInfoAnkitaGINUS()
                    else -> getDriverLicenceInfoAnkitaGINUS()
                }
            }

        }

    }

    @SuppressLint("SetTextI18n")
    private fun processPaymentCardTextTESTGENIUS(
        context: Context,
        visionText: Array<String>?,
        isBackImage: Boolean
    ) {
        if (visionText?.size == 0) {
            Toast.makeText(context, "No Text Detected In Image!", Toast.LENGTH_SHORT).show()
            if (!isBackImage && backImage != null) {
                processPaymentCardTextTESTGENIUS(
                    this@ScanCardActivity,
                    wordsBack,
                    isBackImage = true
                )
            } else {
                progressDialog.dismiss()
                return
            }
        }
        if (!isBackImage)//isBackImage=FALSE
        {

            for (block in visionText!!) {
                val blockText = block.trim()



                Log.d("blockText++++", block)
                // Check if the block text contains a possible card number based on length (typically between 13 to 19 digits)
                if (blockText.length in 13..19) {
                    if (!paymentViewModel.isAlphabeticWithSpaces(blockText)) {
                        val cardNumber = paymentViewModel.fixOCRArtifacts(blockText)
                        val cleanCardNumber = cardNumber.replace(" ", "")
                        val regex = "^\\d+$".toRegex()
                        if (regex.matches(cleanCardNumber)) {
                            paymentViewModel.cardNO = cardNumber
                            Log.e("Cleaned Card Number++++", cardNumber)
                            paymentViewModel.cardType = paymentViewModel.getCardType(cardNumber)
                            Log.e("Card Type++++", paymentViewModel.getCardType(cardNumber))
                            val isvalid = paymentViewModel.checkValidCardNumber(cardNumber)
                            paymentViewModel.isValidCard = "${isvalid}"
                            Log.e("IsCardNumberValid?++++", "${isvalid}")
                        }
                    }

                } else {

                    if (paymentViewModel.isValidExpiryDateFormat("${blockText}")) {
                        var xDate = paymentViewModel.parseExpiryDate("${blockText}")
                        if (!paymentViewModel.isExpired(xDate)) {
                            paymentViewModel.detectedBlocks.add("${blockText}")
                            if (paymentViewModel.detectedBlocks.size > 1) {
                                paymentViewModel.findMaxDate()
                            } else {
                                paymentViewModel.cardExpiredDate = "${blockText}"
                                Log.e("DATES+++", "${blockText}")
                            }
                        }
                    }

                    val cardType1 = paymentViewModel.cardTypePattern.find("${blockText}")?.value
                    Log.e("cardType1+++", "cardType: ${cardType1}")

                    paymentViewModel.cardTier =
                        paymentViewModel.cardTierPattern.find("${blockText}")?.value
                    Log.e("cardTier+++", "cardTier: ${paymentViewModel.cardTier}")

                }

                paymentViewModel.getBankCardName("${blockText}")
                Log.e("bankName+++", "${paymentViewModel.cardBankNM}")

                if (paymentViewModel.isAlphabeticWithSpaces(blockText)) {

                    paymentViewModel.isValidCardholderName("${blockText}")
                }
            }

            if (!paymentViewModel.cardType.isNullOrEmpty()) {
                paymentViewModel.cardCVV =
                    paymentViewModel.getValidCVV(paymentViewModel.cardType, visionText)?.toString()
            }

        } else {
            if (isBackImage && backImage != null) { //isBackImage=TRUE
                if (!paymentViewModel.cardType.isNullOrEmpty()) {
                    paymentViewModel.cardCVV =
                        paymentViewModel.getValidCVV(paymentViewModel.cardType, visionText)
                            ?.toString()
                }
            }
        }

        if (!isBackImage && backImage != null) {
            processPaymentCardTextTESTGENIUS(this@ScanCardActivity, wordsBack, isBackImage = true)
        } else {
            progressDialog.dismiss()
        }

        binding.edHolderName.setText(paymentViewModel.cardHolder)
        binding.edCardNumber.setText(
            "Card NO:- " + paymentViewModel.cardNO
                    + "\nCard TYPE:- " + paymentViewModel.cardType
        )
        binding.edExpDate.setText(
            "Exp:- " + paymentViewModel.cardExpiredDate
                    + "\nis Valid?:- " + paymentViewModel.isValidCard
        )
        binding.edCvv.setText(paymentViewModel.cardCVV)
        binding.edBankName.setText(paymentViewModel.cardBankNM + "\nCardTier:- " + paymentViewModel.cardTier)
        if (backImage == null) {
            showRecognizationBottomSheetDialog(
                this@ScanCardActivity,
                categoryId,
                paymentViewModel.cardHolder.toString() + "\n" +
                        paymentViewModel.cardNO.toString() + "\n"
                        + paymentViewModel.cardExpiredDate.toString()
                        + "  " + paymentViewModel.cardType.toString()
                        + "  " + paymentViewModel.cardCVV.toString(),
                stringFront, stringBack
            )



            progressDialog.dismiss()
        } else {
            if (isBackImage) {
                paymentViewModel.showRecognizationBottomSheetDialog(
                    this@ScanCardActivity,
                    categoryId,
                    paymentViewModel.cardHolder.toString() + "\n" +
                            paymentViewModel.cardNO.toString() + "\n"
                            + paymentViewModel.cardExpiredDate.toString()
                            + "  " + paymentViewModel.cardType.toString()
                            + "  " + paymentViewModel.cardCVV.toString(),
                    stringFront, stringBack
                )

                progressDialog.dismiss()
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun processDriverLicenceTextTESTGENIUS(
        context: Context,
        visionText: Array<String>?,
        isBackImage: Boolean
    ) {

        if (visionText?.size == 0) {
            Toast.makeText(context, "No Text Detected In Image!", Toast.LENGTH_SHORT).show()
            if (!isBackImage && backImage != null) {
                processDriverLicenceTextTESTGENIUS(
                    this@ScanCardActivity,
                    wordsBack,
                    isBackImage = true
                )
            } else {
                progressDialog.dismiss()
                return
            }
        }

        for (i in visionText!!.indices) {
            val combinedText = visionText[i].replace(" ", "")
            val blockText = combinedText.trim()
            Log.d("blockText++++", blockText)


            if (blockText.contains("son/", ignoreCase = true) ||
                blockText.contains("wife/", ignoreCase = true) ||
                blockText.contains("Daughter/", ignoreCase = true) ||
                blockText.contains("DaughterOf", ignoreCase = true) ||
                blockText.contains("sonOf", ignoreCase = true) ||
                blockText.contains("wifeOf", ignoreCase = true)
            ) {
                if (i + 1 < visionText.size) {
                    licenceViewModel.checkParentsName(visionText[i + 1].trim())
                }
            }

            if (licenceViewModel.d_o_b.equals("")) {
                val blockTextLowerCase = blockText.toLowerCase()

                if (blockText.contains("DateOfIssue", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("BirthDate", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("BirthDete", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                   if (blockText.contains("DeteofBirth", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("DOB", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("D.O.B", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("D-O-B", ignoreCase = true)) {
                    checkDOBTrimDate(blockTextLowerCase)
                }

            if (blockText.contains("DateOfBirth", ignoreCase = true) ||
                blockText.contains("BirthDate", ignoreCase = true) ||
                blockText.contains("BirthDate", ignoreCase = true) ||
                blockText.contains("DeteofBirth", ignoreCase = true) ||
                blockText.contains("DOB", ignoreCase = true)
            ) {
                if (i + 1 < visionText.size) {
                    licenceViewModel.checkBirthDateFormat(visionText[i + 1].trim())
                }
            }
            }

            if (licenceViewModel.issueDate.equals("")) {

                val blockTextLowerCase = blockText.toLowerCase()

                if (blockText.contains("Dateofssue", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("DateOfIssue", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("Date of issue", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("issue", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("ssue", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("IssueDate", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("ISS", ignoreCase = true)) {
                    checkIssueTrimDate(blockTextLowerCase)
                }

                if (licenceViewModel.issueDate.equals("")) {
                if (blockText.contains("DateOfIssue", ignoreCase = true) ||
                    blockText.contains("Dateofssue", ignoreCase = true) ||
                    blockText.contains("Date of issue", ignoreCase = true) ||
                    blockText.contains("issue", ignoreCase = true) ||
                    blockText.contains("ssue", ignoreCase = true) ||
                    blockText.contains("IssueDate", ignoreCase = true) ||
                    blockText.contains("ISS", ignoreCase = true)
                ) {
                    if (i + 1 < visionText.size) {
                        licenceViewModel.checkIssueDateFormat(visionText[i + 1].trim())
                    }
                }}

            }

            if (licenceViewModel.expDate.equals("")) {

                val blockTextLowerCase = blockText.toLowerCase()

                if (blockText.contains("validity", ignoreCase = true)) {
                    checkExpiryTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("ExpiryDate", ignoreCase = true)) {
                    checkExpiryTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("ExpiryDete", ignoreCase = true)) {
                    checkExpiryTrimDate(blockTextLowerCase)
                }
                if (blockText.contains("EXP", ignoreCase = true)) {
                    checkExpiryTrimDate(blockTextLowerCase)
                }

                if (blockText.contains("validity", ignoreCase = true) ||
                    blockText.contains("ExpiryDate", ignoreCase = true) ||
                    blockText.contains("ExpiryDete", ignoreCase = true) ||
                    blockText.contains("EXP", ignoreCase = true)
                ) {
                    if (i + 1 < visionText.size) {
                        licenceViewModel.checkExpiryDateFormat(visionText[i + 1].trim())
                    }
                }
            }


            if (blockText.contains("class", ignoreCase = true)) {
                if (i + 1 < visionText.size) {
                    licenceViewModel.checkClassFormate(visionText[i + 1].trim())
                }
            }
            if (blockText.contains("name", ignoreCase = true)) {
                if (i + 1 < visionText.size) {
                    licenceViewModel.checkFullName(visionText[i + 1].trim())
                }
            }
            if (licenceViewModel.fullName.equals("")) {

                licenceViewModel.checkFullName(blockText)

            }
            licenceViewModel.checkDrivingLicenceNumber(blockText)
            licenceViewModel.checkGender(blockText)
            licenceViewModel.checkBloodGroup(blockText)
          //  licenceViewModel.checkDateFormat(blockText)
        }


            licenceViewModel.setExpiryDate()

        Log.e("Licence+++-IssueDate---+++", licenceViewModel.issueDate.toString())
        Log.e("Licence+++-IssueDate---+++", licenceViewModel.d_o_b.toString())
        Log.e("Licence+++-IssueDate---+++", licenceViewModel.expDate.toString())

        showRecognizationBottomSheetDialog(
            this@ScanCardActivity,
            categoryId,
            licenceViewModel.licenceNo.toString() + "\n"
                    + licenceViewModel.fullName.toString()
                    + licenceViewModel.parentsName.toString() + "\n"
                    + licenceViewModel.expDate.toString() + "\n"
                    + licenceViewModel.d_o_b.toString()
                    + licenceViewModel.issueDate.toString()
                    + licenceViewModel.bloodGroup.toString() + "\n"
                    + licenceViewModel.gender.toString() + "\n"
                    + DatabaseHelperManiya(this).getCategoryDao().queryForId(1).name,
            stringFront,
            stringBack
        )
        progressDialog.dismiss()
    }

    private fun checkIssueTrimDate(blockTextLowerCase: String) {
        val substring = blockTextLowerCase.substringAfter(blockTextLowerCase, "").trim()
        licenceViewModel.issueDate = Regex(paymentViewModel.EXPIRY_DATE_REGEX).find(substring)?.value

    }
    private fun checkDOBTrimDate(blockTextLowerCase: String) {
        val substring = blockTextLowerCase.substringAfter(blockTextLowerCase, "").trim()
        licenceViewModel.d_o_b = Regex(paymentViewModel.EXPIRY_DATE_REGEX).find(substring)?.value

    }    private fun checkExpiryTrimDate(blockTextLowerCase: String) {
        val substring = blockTextLowerCase.substringAfter(blockTextLowerCase, "").trim()
        licenceViewModel.expDate = Regex(paymentViewModel.EXPIRY_DATE_REGEX).find(substring)?.value

    }

    private fun extractCardDetails(extractedText: String) {

        val lines = extractedText.split("\n").map { it.trim() }

        Log.d("fatal", "extractCardDetails: $lines")

        val cardNumber = extractCardNumber(lines) ?: getString(R.string.not_found)

        val (expiryMonth, expiryYear) = extractExpiration(lines)
        val expiryDate =
            if (expiryMonth != null && expiryYear != null) "$expiryMonth/$expiryYear" else getString(
                R.string.not_found
            )
        val cvv = extractCvv(lines) ?: getString(R.string.not_found)
        val cardHolderName = extractOwner(lines) ?: getString(R.string.not_found)

        matchBankName(extractedText)

        val input = cardNumber.replace(" ", "")
        val format = input.chunked(4).joinToString(" ")

        binding.edHolderName.setText(cardHolderName)
        binding.edExpDate.setText(expiryDate)
        binding.edCvv.setText(cvv)
        binding.edCardNumber.setText(format)
    }

    private fun extractCardNumber(lines: List<String>): String? {
        val cleanedLines =
            lines.joinToString(" ").replace("|", "").replace("\n", " ").replace("1800 1080", "")
                .replace(",", "").replace(".", "")

        val regex = Regex("\\b(\\d\\s*?){16}\\b")
        return regex.find(cleanedLines)?.value?.replace(" ", "")
    }

    private fun extractCvv(lines: List<String>): String? {

        val regex = Regex("\\b\\d{3}\\b")
        return lines.find { regex.matches(it) }?.trim()
    }

    private fun extractOwner(lines: List<String>): String? {
        val ignoredTerms =
            listOf("AUTHORIZED SIGNATURE", "Customer Care No.", "1800", "SBFINKAO14641123")

        return lines.asSequence().filter { it.trim().isNotEmpty() }
            .filter { line -> line.contains(" ") }.filter { line ->
                line.all { char -> char.isUpperCase() || char.isWhitespace() }
            }.filter { line ->
                ignoredTerms.none { term -> line.contains(term, ignoreCase = true) }
            }.filter { line ->
                val wordCount = line.split(" ").filter { it.isNotEmpty() }.size
                wordCount == 2 || wordCount == 3
            }.filter { line ->
                !line.any { it.isDigit() }
            }.maxByOrNull { it.length }?.trim()
    }


    private fun extractExpiration(lines: List<String>): Pair<String?, String?> {
        val expirationLine = extractExpirationLine(lines)
        val month = expirationLine?.substring(startIndex = 0, endIndex = 2)
        val year = expirationLine?.substring(startIndex = 3)
        return Pair(month, year)
    }

    private fun extractExpirationLine(lines: List<String>): String? {
        return lines.flatMap { it.split(" ") }
            .firstOrNull { (it.length == 5 || it.length == 7) && it[2] == '/' }
    }

    private fun matchBankName(extractedText: String) {
        val matchedBanks = mutableListOf<String>()

        for (bank in bankList) {
            if (extractedText.contains(bank, ignoreCase = true)) {
                matchedBanks.add(bank)
            }
        }

        displayMatchedBanks(matchedBanks)
    }

    private fun displayMatchedBanks(matchedBanks: List<String>) {
        if (matchedBanks.isNotEmpty()) {
            binding.edBankName.setText(matchedBanks.joinToString(", "))
            Log.d("Matched Banks", "Matched Banks: ${matchedBanks.joinToString(", ")}")
        } else {
            binding.edBankName.setText(getString(R.string.no_matching_bank_found))
        }
    }

    override fun onBackPressed() {
        finish()

    }

    override fun onResume() {
        super.onResume()
//        showBannerAdsSecond()
    }

    fun showRecognizationBottomSheetDialog(context: Context, categoryID: Long, ocrText: String, stringFront: String, stringBack: String) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val binding = ScanDataBottomDialogBinding.inflate(bottomSheetDialog.layoutInflater)
        bottomSheetDialog.setContentView(binding.root)

        binding.root.setBackgroundResource(R.drawable.bottom_sheet_background)
        binding.tvOCRData.text = ocrText

        binding.tvContinue.setOnClickListener {
            licenceViewModel.addLicenceData(context,categoryID,ocrText,stringFront, stringBack)
            //openHereViewActivty
            bottomSheetDialog.dismiss()
        }

        binding.tvRetake.setOnClickListener {
            initCamera()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }


}
