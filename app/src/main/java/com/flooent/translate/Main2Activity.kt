package com.flooent.translate

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset

import java.util.*
import kotlin.collections.ArrayList

class Main2Activity : AppCompatActivity(), RecognitionListener {

    private var selectedLangCode: String = "us-en"
    private var allLangs: ArrayList<Langs>? = null
    private var speechListener: FlooentSpeechListener? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private var txtSpeechInput: TextView? = null
    private var btnSpeak: ImageButton? = null
    private val REQ_CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtSpeechInput = findViewById(R.id.txtSpeechInput) as TextView
        btnSpeak = findViewById(R.id.btnSpeak) as ImageButton

        // hide the action bar
//        getActionBar().hide()

        btnSpeak!!.setOnClickListener {
            //                promptSpeechInput()

            listenAndTranslate()

        }

        ll_lang.setOnClickListener {
            showLangsDialog()
        }

        initSpeechRecognition()


    }

    private fun listenAndTranslate() {

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListeningToSpeech()
        } else {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

    }

    private fun initSpeechRecognition() {

        if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)

            speechListener = FlooentSpeechListener.getInstance()
            speechRecognizer.setRecognitionListener(this)

        } else {
            Log.e("speech recognition", "not available")
        }


    }


    /**
     * Showing google speech input dialog
     */
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            getString(R.string.speech_prompt)
        )
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                getApplicationContext(),
                getString(R.string.speech_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }

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
            DialogInterface.OnClickListener { dialog, which -> setSelectedLang(which) })
        builder.show()
    }

    private fun setSelectedLang(which: Int) {
        val selectedLang = allLangs?.get(which)

        txt_lang.text = selectedLang?.name
        selectedLangCode = selectedLang?.code!!
    }

    fun getLangsArrayList(): java.util.ArrayList<Langs>? {
        val langs = java.util.ArrayList<Langs>()
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
        var json: String? = null
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
    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data!!
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    txtSpeechInput!!.setText(result.get(0))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListeningToSpeech()
        } else
            Toast.makeText(applicationContext, "Cannot proceed without audio permission", Toast.LENGTH_LONG).show()
    }

    private fun startListeningToSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLangCode)
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            getString(R.string.speech_prompt)
        )

        speechRecognizer.startListening(intent)

//        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, application.packageName)
//        speechRecognizer.startListening(intent)
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        Log.e("speech recognition", "onReadyForSpeech")
    }

    override fun onRmsChanged(p0: Float) {
        Log.e("speech recognition", "onRmsChanged")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.e("speech recognition", "onBufferReceived")
    }

    override fun onPartialResults(p0: Bundle?) {
        Log.e("speech recognition", "onPartialResults")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.e("speech recognition", "onEvent")
    }

    override fun onBeginningOfSpeech() {
        Log.e("speech recognition", "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.e("speech recognition", "onEndOfSpeech")
    }

    override fun onError(p0: Int) {
        Log.e("speech recognition", "onError= $p0")
    }

    override fun onResults(p0: Bundle?) {

        val results = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        val scores = ArrayList<String>()

        for (i in p0?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)!!) {
            scores.add(i.toString())
        }

        Log.e(
            "speech recognition", "onResults = $results " +
                    "confidence vals = $scores"
        )

        txtSpeechInput!!.setText(results?.get(0))

    }
}

