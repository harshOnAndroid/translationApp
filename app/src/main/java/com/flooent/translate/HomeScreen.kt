package com.flooent.translate

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.*
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_home_screen.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

import kotlin.collections.ArrayList

class HomeScreen : AppCompatActivity(), SpeechManagerListener, TranslationManagerListener, TextToSpeechManagerListener {

    private lateinit var conversationAdapter: ConversationAdapter
    private var ttsManager: TextToSpeechManager? = null
    private var translationManager: TranslationManager? = null
    private var nativeToForeignTranslator: FirebaseTranslator? = null
    private var foreignToNativeTranslator: FirebaseTranslator? = null
    private var speechManager: SpeechManager? = null
    private var nativeLangCode: String = "en"
    private var foreignLangCode: String = "en"
    private var allLangs: ArrayList<Langs>? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQ_CODE_SPEECH_INPUT = 100
    private var isNativeInteraction = true
    private var isNativeLangSelection = true
    private var isTextToSpeechOn = true
    private var conversationList = ArrayList<Conversation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        speechManager = SpeechManager.getInstance(this, applicationContext)
        translationManager = TranslationManager.getInstance(this)
        ttsManager = TextToSpeechManager.getInstance(applicationContext)


        txt_speckInstruction.visibility = View.GONE
        conversationLayout.visibility= View.VISIBLE
        conversationAdapter = ConversationAdapter(this, conversationList)
        rv_conversation.adapter = conversationAdapter
        rv_conversation.layoutManager = LinearLayoutManager(this)

        rg_lang.setOnCheckedChangeListener{ radioGroup: RadioGroup, i: Int ->
            if (i == R.id.rb_flagNative)
                conversationAdapter.changeLanguage(true)
            else
                conversationAdapter.changeLanguage(false)
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

        rb_speaker.setOnClickListener {
            setTextToSpeech(rb_speaker.isChecked)
        }

        translationManager?.initTranslator(nativeLangCode, foreignLangCode)

    }

    private fun setTextToSpeech(checked: Boolean) {
        isTextToSpeechOn = checked


    }

    private fun listenAndTranslate(isNativeInteraction: Boolean) {

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
            txt_langNative.text = selectedLang?.name
            nativeLangCode = selectedLang?.code!!
            speechManager?.changeNativeLang(nativeLangCode)
            translationManager?.changeNativeLang(nativeLangCode)
            ttsManager?.setNativeLanguage(nativeLangCode)
        } else {
            txt_langForeign.text = selectedLang?.name
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
                    txt_speechOutputNative.setText(result.get(0))
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
        if (isNativeInteraction)
            txt_speechOutputNative.text = resultText
        else
            txt_speechOutputForeign.text = resultText

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
            txt_speechOutputForeign.text = translatedText
//            onNewMsgArrived(nativeInteraction, Conversation(translatedText, originalText, nativeInteraction))

            if (isTextToSpeechOn)
                ttsManager?.speakForeign(translatedText)

        } else {
            txt_speechOutputNative.text = translatedText
//            onNewMsgArrived(nativeInteraction, Conversation(originalText, translatedText, nativeInteraction))

            if (isTextToSpeechOn)
                ttsManager?.speakNative(translatedText)
        }

        onNewMsgArrived(Conversation(translatedText, originalText, nativeInteraction))

    }

    private fun onNewMsgArrived(conversation: Conversation) {
        conversationAdapter.addNewMsg(conversation)
        txt_speckInstruction.visibility = View.GONE
        conversationLayout.visibility = View.VISIBLE
        rv_conversation.scrollToPosition(conversationList.size-1)

    }

    override fun onTranslationFailure(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

}

