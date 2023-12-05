package company.tap.tapcardformkit.open.web_wrapper

import TapTheme
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import cards.pay.paycardsrecognizer.sdk.Card
import com.google.gson.Gson
import company.tap.nfcreader.open.utils.TapNfcUtils
import company.tap.tapcardformkit.*
import company.tap.tapcardformkit.open.DataConfiguration
import company.tap.tapcardformkit.open.web_wrapper.enums.CardFormWebStatus
import company.tap.tapcardformkit.open.web_wrapper.model.ThreeDsResponse
import company.tap.tapcardformkit.open.web_wrapper.nfc_activity.nfcbottomsheet.NFCBottomSheetActivity
import company.tap.tapcardformkit.open.web_wrapper.pref.Pref
import company.tap.tapcardformkit.open.web_wrapper.scanner_activity.ScannerActivity
import company.tap.tapcardformkit.open.web_wrapper.threeDsWebView.ThreeDsBottomSheetFragment
import company.tap.tapcardformkit.open.web_wrapper.threeDsWebView.ThreeDsWebViewActivity
import company.tap.tapuilibrary.themekit.ThemeManager
import company.tap.tapuilibrary.uikit.atoms.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.*


@RequiresApi(Build.VERSION_CODES.N)
@SuppressLint("ViewConstructor")
class TapCardKit : LinearLayout {
    lateinit var hideableWebView: WebView
    private var isRedirected = false
    lateinit var constraintLayout: ConstraintLayout
    lateinit var webViewFrame: FrameLayout
    lateinit var webFrame3ds: FrameLayout
    private var cardPrefillPair:Pair<String,String> = Pair("","")
    private var userIpAddress =""
    private val retrofit = RetrofitClient.getClient()
    private val retrofit2 = RetrofitClient.getClient2()

    private val cardConfiguration = retrofit.create(UserApi::class.java)
    private val ipAddressConfiguration = retrofit2.create(IPaddressApi::class.java)

    private lateinit var cardUrlPrefix:String



    companion object{
         var alreadyEvaluated = false
        var NFCopened:Boolean = false
        lateinit var threeDsResponse: ThreeDsResponse
        lateinit var cardWebview: WebView
        lateinit var cardConfiguraton: CardConfiguraton
         var languageThemePair:Pair<String?,String?> = Pair("","")


        var card:Card?=null
        fun fillCardNumber(cardNumber:String,expiryDate:String,cvv:String,cardHolderName:String){
            Log.e("fillcardNumber","card in fillCardNumber ${cardNumber} + ${expiryDate} +${cvv} + ${cardHolderName}")
            cardWebview.loadUrl("javascript:window.fillCardInputs({cardNumber:'$cardNumber',expiryDate:'$expiryDate',cvv:'$cvv',cardHolderName:'$cardHolderName'})")
        }
        fun setIpAddress(ipAddress:String){
            cardWebview.loadUrl("javascript:window.setIP('$ipAddress')")
        }


        fun generateTapAuthenticate(authIdPayer: String) {
            cardWebview.loadUrl("javascript:window.loadAuthentication('$authIdPayer')")
        }



    }

    /**
     * Simple constructor to use when creating a TapPayCardSwitch from code.
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     **/
    constructor(context: Context) : super(context)

    /**
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     *  @param attrs The attributes of the XML Button tag being used to inflate the view.
     *
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)


    init {
     LayoutInflater.from(context).inflate(R.layout.activity_card_web_wrapper, this)
        initWebView()
    }

     private fun initWebView() {
        cardWebview = findViewById(R.id.webview)

         hideableWebView = findViewById(R.id.hideableWebView)
        webViewFrame = findViewById(R.id.webViewFrame)
        webFrame3ds = findViewById(R.id.webViewFrame3ds)
        constraintLayout = findViewById(R.id.constraint)

         cardWebview.settings.javaScriptEnabled = true
         cardWebview.settings.useWideViewPort = true
         hideableWebView.settings.javaScriptEnabled = true
        cardWebview.settings.domStorageEnabled = true
         cardWebview.settings.loadsImagesAutomatically = true
         cardWebview.settings.databaseEnabled = false
         cardWebview.settings.javaScriptCanOpenWindowsAutomatically = true
         cardWebview.settings.builtInZoomControls = true;

         cardWebview.setBackgroundColor(Color.TRANSPARENT)
        cardWebview.setLayerType(LAYER_TYPE_SOFTWARE, null)

         cardWebview.webViewClient = MyWebViewClient()
         cardWebview.webChromeClient = object : WebChromeClient() {
             override fun onJsPrompt(
                 view: WebView?, url: String?,
                 message: String?, defaultValue: String?, result: JsPromptResult?
             ): Boolean {
                 return true
             }

             override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                 Log.d("console", consoleMessage.message())

                 return true
             }
         }
         hideableWebView.webViewClient = HideableWebViewClient()

     }


    fun init(
        configuraton: CardConfiguraton,
        cardNumber: String = "",
        cardExpiry: String = "",
        context: Context ?=null
    ) {

        GlobalScope.launch {

             try {
                 val usersResponse = cardConfiguration.getCardConfiguration()
                 if (usersResponse.android.toString().contains(BuildConfig.VERSION_CODE.toString())){
                     cardUrlPrefix = usersResponse.android.`50`
                 }

             }catch (e:Exception){
              //   Log.e("error",e.message.toString())
                 cardUrlPrefix = urlWebStarter
             }
             try {

                 val geoLocationResponse = ipAddressConfiguration.getGeoLocation()
                 userIpAddress = geoLocationResponse.IPv4

             }catch (e:Exception){
                 Log.e("error",e.message.toString())

             }



             cardConfiguraton = configuraton
             cardPrefillPair = Pair(cardNumber, cardExpiry)
             applyThemeForShimmer()
             when (configuraton) {
                 CardConfiguraton.MapConfigruation -> {
                     val url = "${cardUrlPrefix}${encodeConfigurationMapToUrl(DataConfiguration.configurationsAsHashMap)}"
                     Log.e("url", url.toString())
                     cardWebview.post {
                         cardWebview.loadUrl(
                             "${cardUrlPrefix}${
                                 encodeConfigurationMapToUrl(
                                     DataConfiguration.configurationsAsHashMap
                                 )
                             }"
                         )

                     }


                 }
                 else -> {}
             }
         }




    }

    private fun applyThemeForShimmer() {
        /**
         * need to be refactored : mulitple copies of same code
         */
        when(cardConfiguraton){
            CardConfiguraton.MapConfigruation ->{
                val tapInterface = DataConfiguration.configurationsAsHashMap?.get("interface") as? Map<*, *>
                var lanugage = tapInterface?.get("locale")?.toString() ?: getDeviceLocale()?.language
                if (lanugage == "dynamic"){
                    lanugage =  getDeviceLocale()?.language
                }
                var theme = tapInterface?.get("theme")?.toString() ?: context.getDeviceTheme()
                if (theme == "dynamic"){
                    theme =  context.getDeviceTheme()
                }
                languageThemePair = Pair(lanugage,theme)
              setTapThemeAndLanguage(
                    this.context,
                    language = lanugage,
                  themeMode =theme)
            }
            else -> {}
        }


    }

    private fun setTapThemeAndLanguage(context: Context, language: String?, themeMode: String?) {
        when (themeMode) {
            TapTheme.light.name -> {
                DataConfiguration.setTheme(
                    context, context.resources, null,
                    R.raw.defaultlighttheme, TapTheme.light.name
                )
                ThemeManager.currentThemeName = TapTheme.light.name
            }
            TapTheme.dark.name -> {
                DataConfiguration.setTheme(
                    context, context.resources, null,
                    R.raw.defaultdarktheme, TapTheme.dark.name
                )
                ThemeManager.currentThemeName = TapTheme.dark.name
            }
            else -> {}
        }
        DataConfiguration.setLocale(this.context, language ?:"en", null, this@TapCardKit.context.resources, R.raw.lang)

    }


    private fun encodeConfigurationMapToUrl(configuraton: HashMap<String,Any>?): String? {
        val gson = Gson()
        val json: String = gson.toJson(configuraton)

        val encodedUrl = URLEncoder.encode(json, "UTF-8")
        return encodedUrl

    }


    inner class MyWebViewClient : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {

            /**
             * main checker if url start with "tapCardWebSDK://"
             */
            if (request?.url.toString().startsWith(CardWebUrlPrefix, ignoreCase = true)) {
                /**
                 * listen for states of cardWebStatus of onReady , onValidInput .. etc
                 */
                if (request?.url.toString().contains(CardFormWebStatus.onReady.name)) {
                    /**
                     * this scenario only for the first launch of the app , due to issue navigation
                     * of webview after shimmering , if issue appears [in first install only] init function isCalled again .
                     */
                   val isFirstTime =  Pref.getValue(context!!,"firstRun","true").toString()
                    if (isFirstTime == "true"){
                        init(TapCardKit.cardConfiguraton, context = context)
                        Pref.setValue(context,"firstRun","false")
                    }else{
                        DataConfiguration.getTapCardStatusListener()?.onReady()
                        Log.e("ipAddress after",userIpAddress.toString())
                        if (userIpAddress.isNotEmpty()){
                            setIpAddress(userIpAddress)
                        }
                        if (cardPrefillPair.first.length>=7){
                            fillCardNumber(cardNumber = cardPrefillPair.first, expiryDate = cardPrefillPair.second,"","")
                        }
                    }

                }
                if (request?.url.toString().contains(CardFormWebStatus.onValidInput.name)) {
                    val validInputValue =
                        request?.url?.getQueryParameterFromUri(keyValueName).toString()
                    when (validInputValue.toBoolean()) {
                        true -> {
                            DataConfiguration.getTapCardStatusListener()?.onValidInput(
                                request?.url?.getQueryParameterFromUri(keyValueName).toString()
                            )

                        }
                        false -> {}
                    }

                }
                if (request?.url.toString().contains(CardFormWebStatus.onError.name)) {
                    DataConfiguration.getTapCardStatusListener()
                        ?.onError(request?.url?.getQueryParameterFromUri(keyValueName).toString())
                }
                if (request?.url.toString().contains(CardFormWebStatus.onFocus.name)) {
                    DataConfiguration.getTapCardStatusListener()?.onFocus()

                }
                if (request?.url.toString().contains(CardFormWebStatus.onSuccess.name)) {
                    DataConfiguration.getTapCardStatusListener()?.onSuccess(request?.url?.getQueryParameterFromUri(keyValueName).toString())
                }
                if (request?.url.toString().contains(CardFormWebStatus.onHeightChange.name)) {
                    val newHeight = request?.url?.getQueryParameter(keyValueName)
                    val params: ViewGroup.LayoutParams? = webViewFrame.layoutParams
                    params?.height = webViewFrame.context.getDimensionsInDp(newHeight?.toInt() ?: 95)
                    webViewFrame.layoutParams = params

                    DataConfiguration.getTapCardStatusListener()?.onHeightChange(newHeight.toString())



                }
                if (request?.url.toString().contains(CardFormWebStatus.onBinIdentification.name)) {
                    DataConfiguration.getTapCardStatusListener()
                        ?.onBindIdentification(
                            request?.url?.getQueryParameterFromUri(keyValueName).toString()
                        )
                }

                if (request?.url.toString().contains(CardFormWebStatus.on3dsRedirect.name)) {
                    /**
                     * navigate to 3ds Activity
                     */
                    val queryParams = request?.url?.getQueryParameterFromUri(keyValueName).toString()
                    Log.e("data", queryParams.toString())

                    threeDsResponse = queryParams.getModelFromJson()
                    Log.e("data", threeDsResponse.toString())
                    hideableWebView.loadUrl(threeDsResponse.threeDsUrl)


                }
                if (request?.url.toString().contains(CardFormWebStatus.onScannerClick.name)) {
                    /**
                     * navigate to Scanner Activity
                     */
                    val intent = Intent(context,ScannerActivity::class.java)
                    (context).startActivity(intent)

                }
                if (request?.url.toString().contains(CardFormWebStatus.onNfcClick.name)) {
                    /**
                     * navigate to NFC Activity
                     */
                    if (TapNfcUtils.isNfcAvailable(context)) {
                        NFCopened = true
//                        val intent = Intent(context,NFCLaunchActivity::class.java)
//                        (context).startActivity(intent)

                        val intent = Intent(context,NFCBottomSheetActivity::class.java)
                        (context).startActivity(intent)
                    }else
                    {
                        //TODO:check if u need any other call back here if device doesn't support NFC
                        DataConfiguration.getTapCardStatusListener()?.onError("NFC is not supported on this device")
                    }



                }
                return true

            }
        else {
             //   webView?.loadUrl(request?.url.toString())
                return false}
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

        }



        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            Log.e("error",error.toString())
            Log.e("error",request.toString())

            super.onReceivedError(view, request, error)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            Log.e("error",error.toString())
            Log.e("error",handler.toString())
            handler?.proceed()
        }
    }


    inner class HideableWebViewClient : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            isRedirected = true;
            return false
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (!alreadyEvaluated) {
                alreadyEvaluated = true;
                Handler().postDelayed({
                    navigateTo3dsActivity()
                }, waitDelaytime)
            } else {
                alreadyEvaluated = false;
            }


        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            isRedirected = false;

        }

        fun navigateTo3dsActivity() {
            // on below line we are creating a new bottom sheet dialog.
            /**
             * put buttomsheet in separate class
             */


            val intent = Intent(context, ThreeDsWebViewActivity::class.java)
            (context).startActivity(intent)
            ThreeDsBottomSheetFragment.tapCardKit = this@TapCardKit

        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
        }
    }

    fun generateTapToken() {
        cardWebview.loadUrl("javascript:window.generateTapToken()")
    }
}



enum class CardConfiguraton() {
    MapConfigruation, ModelConfiguration
}




