package com.example.recaptchapoc

import android.os.Build.VERSION_CODES.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

  private val siteKey = "<SITE_KEY>"
  private val secretKey = "<SECRET_KEY>"

  private lateinit var button1: Button
  private lateinit var button2: Button
  private lateinit var text: TextView
  private lateinit var queue: RequestQueue

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    queue = Volley.newRequestQueue(applicationContext)
    button1 = findViewById(R.id.button1)
    button1.setOnClickListener { launchRecaptcha() }
    button2 = findViewById(R.id.button2)
    button2.setOnClickListener { updateUIForVerification(false) }
    text = findViewById(R.id.text)
  }

  private fun launchRecaptcha() {
    SafetyNet.getClient(this).verifyWithRecaptcha(siteKey)
      .addOnSuccessListener {
        handleSuccess(it)
      }
      .addOnFailureListener {
        handleFailure(it)
      }
  }

  private fun handleSuccess(recaptchaTokenResponse: SafetyNetApi.RecaptchaTokenResponse) {
    if (recaptchaTokenResponse.tokenResult?.isNotEmpty() == true) {
      Log.d("SafetyNet", "handleSuccess: ${recaptchaTokenResponse.tokenResult}")
      recaptchaTokenResponse.tokenResult?.let { handleSiteVerification(it) }
    }
  }

  private fun handleFailure(exception: Exception) {
    if (exception is ApiException) {
      Log.d(
        "SafetyNet",
        "Error message: " + CommonStatusCodes.getStatusCodeString(exception.statusCode) + " " + exception.message
      )
    } else {
      Log.d("SafetyNet", "Unknown type of error: " + exception.message)
    }
  }

  private fun handleSiteVerification(tokenResult: String) {
    val url = "https://www.google.com/recaptcha/api/siteverify"
    val request: StringRequest = object : StringRequest(
      Method.POST, url,
      Response.Listener { response -> handleResponse(response) },
      Response.ErrorListener { error -> Log.d("SafetyNet", error.message.toString()) }) {
      override fun getParams(): Map<String, String> {
        val params: MutableMap<String, String> = HashMap()
        params["secret"] = secretKey
        params["response"] = tokenResult
        return params
      }
    }

    request.retryPolicy = DefaultRetryPolicy(50000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
    queue.add(request)
  }

  private fun handleResponse(response: String) {
    try {
      Log.d("SafetyNet", "JSON response: " + response)
      val jsonObject = JSONObject(response)
      if (jsonObject.getBoolean("success")) {
        updateUIForVerification(true)
      } else {
        Toast.makeText(
          applicationContext,
          jsonObject.getString("error-codes").toString(),
          Toast.LENGTH_LONG
        ).show()
      }
    } catch (ex: Exception) {
      Log.d("SafetyNet", "JSON exception: " + ex.message)
    }
  }

  private fun updateUIForVerification(verified: Boolean) {
    if (verified) {
      button1.visibility = View.GONE
      button2.visibility = View.VISIBLE
      text.text = "User Verified!"
    } else {
      button1.visibility = View.VISIBLE
      button2.visibility = View.GONE
      text.text = "Jumping Minds"
    }
  }
}