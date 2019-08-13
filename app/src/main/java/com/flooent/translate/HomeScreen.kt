package com.flooent.translate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.RecognizerIntent
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

import kotlin.collections.ArrayList

class HomeScreen : AppCompatActivity(), SpeechManagerListener, TranslationManagerListener, TextToSpeechManagerListener,
    SensorEventListener {

    private var selectedLangNative: String? = "English"
    private var selectedLangForeign: String? = "English"
    private lateinit var rv_foreignConversation: RecyclerView
    private lateinit var rv_nativeConversation: RecyclerView
    private lateinit var rg_lang: RadioGroup
    private lateinit var img_flagForeign: ImageView
    private lateinit var img_flagNative: ImageView
    private lateinit var img_speakForeign: ImageView
    private lateinit var img_speakNative: ImageView
    private lateinit var btn_cancelDownload: Button
    private lateinit var swt_speakerForeign: Switch
    private lateinit var swt_speakerBothLang: Switch
    private lateinit var swt_speakerNative: Switch
    private lateinit var txt_langForeign: TextView
    private lateinit var txt_langNative: TextView
    private lateinit var layout_downloadModule: View
    private lateinit var layout_foreignSpeakProgress: View
    private lateinit var layout_nativeSpeakProgress: View
    private lateinit var layout_progressForeign: View
    private lateinit var layout_progressNative: View
    private lateinit var layout_speckInstruction: View
    private lateinit var conversationLayout: View
    private val REQ_CODE_SPEECH_INPUT = 100


    private lateinit var senAccelerometer: Sensor
    private var isTextToSpeechOnForeign: Boolean = true
    private lateinit var conversationAdapterForeign: ConversationAdapter
    private lateinit var conversationAdapterNative: ConversationAdapter
    private var ttsManager: TextToSpeechManager? = null
    private var translationManager: TranslationManager? = null
    private var speechManager: SpeechManager? = null
    private var nativeLangCode: String = "en"
    private var foreignLangCode: String = "en"
    private var allLangs: ArrayList<Langs>? = null
    private var isNativeInteraction = true
    private var isNativeLangSelection = true
    private var isTextToSpeechOnNative = true
    private var conversationList = ArrayList<Conversation>()
    private lateinit var senSensorManager: SensorManager
    private lateinit var mSensorThread: HandlerThread
    private lateinit var mSensorHandler: Handler
    private lateinit var setFlatViewRunnable: Runnable
    private lateinit var setVerticalViewRunnable: Runnable
    private lateinit var viewModel: HomeScreenViewModel

    private var isFlat = false
    private lateinit var detectTiltRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        Log.println(Log.ASSERT, "onCreate", "called")
        speechManager = SpeechManager.getInstance(this, applicationContext)
        translationManager = TranslationManager.getInstance(this)
        ttsManager = TextToSpeechManager.getInstance(applicationContext)

        viewModel = ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)
        viewModel.getUsers().observe(this, object : Observer<List<Conversation>>,
            Observable() {
            override fun onChanged(t: List<Conversation>?) {
//                conversationAdapterNative.addNewMsg(conversation)
//                conversationAdapterForeign.addNewMsg(conversation)
                conversationAdapterNative.updateList(t as ArrayList<Conversation>)
                conversationAdapterForeign.updateList(t as ArrayList<Conversation>)
                rv_nativeConversation.scrollToPosition(conversationList.size - 1)
                rv_foreignConversation.scrollToPosition(conversationList.size - 1)
                layout_speckInstruction.visibility = View.GONE
                conversationLayout.visibility = View.VISIBLE
            }

        })

        initView()

        setVerticalViewRunnable = Runnable {
            setContentView(R.layout.activity_home_screen)
            initView()
        }
        setFlatViewRunnable = Runnable {
            setContentView(R.layout.activity_home_screen_flat)
            initView()
        }



        initListeners()
        translationManager?.initTranslator(nativeLangCode, foreignLangCode)

    }

    private fun initListeners() {

        rg_lang.setOnCheckedChangeListener { radioGroup: RadioGroup, i: Int ->
            if (i == R.id.rb_flagNative) {
//                conversationAdapterNative.changeLanguage(true)
                rv_nativeConversation.visibility = View.VISIBLE
                rv_foreignConversation.visibility = View.GONE
            } else {
                rv_nativeConversation.visibility = View.GONE
                rv_foreignConversation.visibility = View.VISIBLE
//                conversationAdapterNative.changeLanguage(false)
            }
        }

        img_speakNative.setOnClickListener {
            //promptSpeechInput()
            isNativeInteraction = true
            listenAndTranslate(true)
        }

        img_flagNative.setOnClickListener {
            isNativeLangSelection = true
            showLangsDialog()
        }

        img_speakForeign.setOnClickListener {
            //promptSpeechInput()
            isNativeInteraction = false
            listenAndTranslate(false)

        }

        img_flagForeign.setOnClickListener {
            isNativeLangSelection = false
            showLangsDialog()
        }

        btn_cancelDownload.setOnClickListener {
            onLanguageDownloadFailure()
        }

        swt_speakerBothLang.setOnClickListener {
            setTextToSpeech(swt_speakerBothLang.isChecked)
        }

        swt_speakerForeign.setOnClickListener {
            isTextToSpeechOnForeign = swt_speakerForeign.isChecked
        }

        swt_speakerNative.setOnClickListener {
            isTextToSpeechOnNative = swt_speakerNative.isChecked
        }

    }

    override fun onResume() {
        super.onResume()

        senSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorThread = HandlerThread("Sensor thread", Thread.MAX_PRIORITY)
        mSensorThread.start()
        mSensorHandler = Handler(mSensorThread.getLooper())

        senSensorManager.registerListener(this, senAccelerometer, 2 * 1000000, 1 * 1000000, mSensorHandler)
    }

    override fun onPause() {
        super.onPause()

        senSensorManager.unregisterListener(this)
        mSensorThread.quitSafely()

    }

    private fun initView() {
        rv_foreignConversation = findViewById(R.id.rv_foreignConversation)
        rv_nativeConversation = findViewById(R.id.rv_nativeConversation)
        rg_lang = findViewById(R.id.rg_lang)
        img_flagForeign = findViewById(R.id.img_flagForeign)
        img_flagNative = findViewById(R.id.img_flagNative)
        img_speakForeign = findViewById(R.id.img_speakForeign)
        img_speakNative = findViewById(R.id.img_speakNative)
        btn_cancelDownload = findViewById(R.id.btn_cancelDownload)
        swt_speakerForeign = findViewById(R.id.swt_speakerForeign)
        swt_speakerBothLang = findViewById(R.id.swt_speakerBothLang)
        swt_speakerNative = findViewById(R.id.swt_speakerNative)
        txt_langForeign = findViewById(R.id.txt_langForeign)
        txt_langNative = findViewById(R.id.txt_langNative)
        layout_downloadModule = findViewById(R.id.layout_downloadModule)
        layout_foreignSpeakProgress = findViewById(R.id.layout_foreignSpeakProgress)
        layout_nativeSpeakProgress = findViewById(R.id.layout_nativeSpeakProgress)
        layout_progressForeign = findViewById(R.id.layout_progressForeign)
        layout_progressNative = findViewById(R.id.layout_progressNative)
        layout_speckInstruction = findViewById(R.id.layout_speckInstruction)
        conversationLayout = findViewById(R.id.conversationLayout)


        conversationAdapterNative = ConversationAdapter(this, conversationList, true)
        conversationAdapterForeign = ConversationAdapter(this, conversationList, false)
        rv_nativeConversation.adapter = conversationAdapterNative
        rv_nativeConversation.layoutManager = LinearLayoutManager(this)
        rv_foreignConversation.adapter = conversationAdapterForeign
        if (isFlat)
            rv_foreignConversation.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
        else
            rv_foreignConversation.layoutManager = LinearLayoutManager(this)

        initListeners()

        swt_speakerForeign.isChecked = isTextToSpeechOnForeign
        swt_speakerNative.isChecked = isTextToSpeechOnNative


        if (conversationList.size > 0) {
            conversationLayout.visibility = View.VISIBLE
            layout_speckInstruction.visibility = View.GONE
        } else {
            conversationLayout.visibility = View.GONE
            layout_speckInstruction.visibility = View.VISIBLE
        }

    }

    private fun setTextToSpeech(checked: Boolean) {
        isTextToSpeechOnNative = checked
        isTextToSpeechOnForeign = checked
    }

    private fun listenAndTranslate(isNativeInteraction: Boolean) {

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("inside ", "listenAndTranslate ")
            speechManager?.startListeningToSpeech(isNativeInteraction)
            showSpeakNowToast()
        } else {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

    }

    private fun showSpeakNowToast() {
        Toast.makeText(this, "Speak Now", Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()

        speechManager?.removeView()
        translationManager?.removeView()
    }


    fun showLangsDialog() {
        val langsStr = java.util.ArrayList<String>()

        val langs = getLangsArrayList()
        for (i in langs?.indices!!) {
            val lan1 = langs.get(i)
            langsStr.add(lan1.name)
        }
        val langStrArr = langsStr.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Language")
        builder.setItems(langStrArr,
            DialogInterface.OnClickListener { dialog, which ->
                setSelectedLang(which)
                onLanguageDownloadStarted()
            })
        builder.show()
    }

    private fun setSelectedLang(which: Int) {
        val selectedLang = allLangs?.get(which)

        if (isNativeLangSelection) {
            selectedLangNative = selectedLang?.name
            txt_langNative.text = selectedLangNative
            nativeLangCode = selectedLang?.code!!
            speechManager?.changeNativeLang(nativeLangCode)
            translationManager?.changeNativeLang(nativeLangCode)
            ttsManager?.setNativeLanguage(nativeLangCode)
        } else {
            selectedLangForeign = selectedLang?.name
            txt_langForeign.text = selectedLangForeign
            foreignLangCode = selectedLang?.code!!
            speechManager?.changeForeignLang(foreignLangCode)
            translationManager?.changeForeignLang(foreignLangCode)
            ttsManager?.setForeignLanguage(foreignLangCode)
        }
    }

    fun getLangsArrayList(): java.util.ArrayList<Langs>? {
        try {
            val jsonObject = JSONObject(loadLanguageJSONFromAsset())
            allLangs = Gson().fromJson<ArrayList<Langs>>(
                jsonObject.getString("langs"),
                object : TypeToken<ArrayList<Langs>>() {

                }.type
            )

            return allLangs
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    fun loadLanguageJSONFromAsset(): String? {
        var json: String?
        try {
            val asset = applicationContext.assets.open("supported_langs2.json")
            val size = asset.available()
            val buffer = ByteArray(size)
            asset.read(buffer)
            asset.close()
            json = buffer.toString(Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        Log.e("lang json ", "\n $json")
        return json
    }


    /**
     * Receiving speech input
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speechManager?.startListeningToSpeech(isNativeInteraction)
        } else
            Toast.makeText(applicationContext, "Cannot proceed without audio permission", Toast.LENGTH_LONG).show()
    }

    fun lockSpeakViews() {

        if (isNativeInteraction) {
            layout_nativeSpeakProgress.visibility = View.VISIBLE
            layout_progressForeign.visibility = View.VISIBLE

            layout_foreignSpeakProgress.visibility = View.GONE
            layout_progressNative.visibility = View.GONE

        } else {
            layout_nativeSpeakProgress.visibility = View.GONE
            layout_progressForeign.visibility = View.GONE

            layout_foreignSpeakProgress.visibility = View.VISIBLE
            layout_progressNative.visibility = View.VISIBLE
        }

    }

    fun unlockSpeakViews() {

        layout_nativeSpeakProgress.visibility = View.GONE
        layout_progressForeign.visibility = View.GONE

        layout_foreignSpeakProgress.visibility = View.GONE
        layout_progressNative.visibility = View.GONE

    }

    override fun onReadyForSpeech() {
        lockSpeakViews()
    }

    override fun onError() {
        unlockSpeakViews()
    }


    override fun onResults(isNativeInteraction: Boolean, resultText: String?) {

        unlockSpeakViews()

        translationManager?.translateText(isNativeInteraction, resultText)
    }

    fun onLanguageDownloadStarted() {
        layout_downloadModule.visibility = View.VISIBLE
    }

    override fun onLanguageDownloadSuccessful() {
        layout_downloadModule.visibility = View.GONE
    }

    override fun onLanguageDownloadFailure() {
        layout_downloadModule.visibility = View.GONE
        Toast.makeText(
            this,
            "The requested Language could not be downloaded now. Please try again later.",
            Toast.LENGTH_LONG
        ).show()
        setSelectedLang(12) // 12 is index for English in the arraylist
    }

    override fun onTranslationSuccessful(
        nativeInteraction: Boolean,
        translatedText: String,
        originalText: String
    ) {
        if (nativeInteraction) {
            Log.e("in trans succesful", "")
//            onNewMsgArrived(nativeInteraction, Conversation(translatedText, originalText, nativeInteraction))

            if (isTextToSpeechOnNative)
                ttsManager?.speakForeign(translatedText)

        } else {
//            onNewMsgArrived(nativeInteraction, Conversation(originalText, translatedText, nativeInteraction))

            if (isTextToSpeechOnForeign)
                ttsManager?.speakNative(translatedText)
        }

        onNewMsgArrived(Conversation(translatedText, originalText, nativeInteraction))

    }

    private fun onNewMsgArrived(conversation: Conversation) {


        conversationList.add(conversation)


        viewModel.setConversationList(conversationList)

    }

    override fun onTranslationFailure(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
//        Log.e("sensor", "accuracy")
    }

    override fun onSensorChanged(event: SensorEvent?) {
//        Log.e("sensor", "changed")

        detectTiltRunnable = Runnable { detectDeviceOrientation(event) }

        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR)
            AsyncTask.execute(detectTiltRunnable)

    }

    fun detectDeviceOrientation(event: SensorEvent?) {
//        Log.e("sensor", "rotation vector")

        val g = event?.values?.clone()!!

        val norm_Of_g = Math.sqrt((g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).toDouble())

        // Normalize the accelerometer vector
        g[0] = (g[0] / norm_Of_g).toFloat()
        g[1] = (g[1] / norm_Of_g).toFloat()
        g[2] = (g[2] / norm_Of_g).toFloat()

        val inclination = Math.round(Math.toDegrees(Math.acos(g[2].toDouble()))).toInt()

        if (inclination < 25 || inclination > 155) {
//            Log.e("sensor", "flat")
            if (!isFlat) {
                runOnUiThread(setFlatViewRunnable)
                isFlat = !isFlat
            }
        } else {
//            Log.e("sensor", "not flat")

            if (isFlat) {
                runOnUiThread(setVerticalViewRunnable)
                isFlat = !isFlat
            }
        }


    }

}

