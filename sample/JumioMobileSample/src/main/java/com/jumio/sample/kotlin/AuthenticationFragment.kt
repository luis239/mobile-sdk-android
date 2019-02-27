package com.jumio.sample.kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.jumio.auth.AuthenticationCallback
import com.jumio.auth.AuthenticationResult
import com.jumio.auth.AuthenticationSDK
import com.jumio.core.enums.JumioDataCenter
import com.jumio.core.exceptions.MissingPermissionException
import com.jumio.core.exceptions.PlatformNotSupportedException
import com.jumio.sample.R


class AuthenticationFragment : Fragment(), View.OnClickListener {

    companion object {
        private val TAG = "JumioSDK_Authentication"
        private val PERMISSION_REQUEST_CODE_AUTHENTICATION = 304
        private var apiToken: String? = null
        private var apiSecret: String? = null
    }

    internal lateinit var startSDK: Button
    internal lateinit var textInputLayoutScanRef : TextInputLayout
    internal lateinit var etScanRef : EditText

    internal lateinit var authenticationSDK: AuthenticationSDK

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_main, container, false)
        rootView.findViewById<View>(R.id.tvOptions).visibility = View.GONE
        rootView.findViewById<View>(R.id.switchOptionOne).visibility = View.GONE
        rootView.findViewById<View>(R.id.switchOptionTwo).visibility = View.GONE

        val args = arguments

        apiToken = args!!.getString(MainActivity.KEY_API_TOKEN)
        apiSecret = args.getString(MainActivity.KEY_API_SECRET)

        textInputLayoutScanRef = rootView.findViewById(R.id.tilOptional) as TextInputLayout
        etScanRef = rootView.findViewById(R.id.etOptional) as EditText
        textInputLayoutScanRef.visibility = View.VISIBLE

        startSDK = rootView.findViewById<View>(R.id.btnStart) as Button
        startSDK!!.text = java.lang.String.format(resources.getString(R.string.button_start), resources.getString(R.string.section_authentication))
        startSDK!!.setOnClickListener(this)

        return rootView
    }

    override fun onClick(view: View) {
        //Since the Authentication is a singleton internally, a new instance is not
        //created here.
        startSDK!!.isEnabled = false
        initializeAuthenticationSDK()
    }

    private fun initializeAuthenticationSDK() {
        try {
            // You can get the current SDK version using the method below.
            // AuthenticationSDK.getSDKVersion();

            // Call the method isSupportedPlatform to check if the device is supported.
            if (!AuthenticationSDK.isSupportedPlatform(activity))
                Log.w(TAG, "Device not supported")

            // Applications implementing the SDK shall not run on rooted devices. Use either the below
            // method or a self-devised check to prevent usage of SDK scanning functionality on rooted
            // devices.
            if (AuthenticationSDK.isRooted(activity))
                Log.w(TAG, "Device is rooted")

            // To create an instance of the SDK, perform the following call as soon as your activity is initialized.
            // Make sure that your merchant API token and API secret are correct and specify an instance
            // of your activity. If your merchant account is created in the EU data center, use
            // JumioDataCenter.EU instead.
            authenticationSDK = AuthenticationSDK.create(activity, apiToken, apiSecret, JumioDataCenter.US)

            // Use the following method to override the SDK theme that is defined in the Manifest with a custom Theme at runtime
            // authenticationSDK.setCustomTheme(R.style.YOURCUSTOMTHEMEID);

            // You can also set a user reference (max. 100 characters).
            // Note: The user reference should not contain sensitive data like PII (Personally Identifiable Information) or account login.
            // authenticationSDK.setUserReference("USERREFERENCE");

            // Callback URL for the confirmation after the verification is completed. This setting overrides your Jumio merchant settings.
            // authenticationSDK.setCallbackUrl("YOURCALLBACKURL");

            // Use the following method to initialize the SDK. The scan reference of an eligible Netverify scan has to be used
            // as the enrollmentTransactionReference
            var enrollmentTransactionReference = ""
            if (etScanRef.text.toString().isNotEmpty()) {
                enrollmentTransactionReference = etScanRef.text.toString()
            }
            if ((activity as MainActivity).checkPermissions(PERMISSION_REQUEST_CODE_AUTHENTICATION)) {
                authenticationSDK.initiate(enrollmentTransactionReference, object : AuthenticationCallback {
                    override fun onAuthenticationInitiateSuccess() {
                        try {
                            startActivityForResult(authenticationSDK.intent, AuthenticationSDK.REQUEST_CODE)
                        } catch (e: MissingPermissionException) {
                            Toast.makeText(activity!!.applicationContext, e.message, Toast.LENGTH_LONG).show()
                        }

                        startSDK.isEnabled = true
                    }

                    override fun onAuthenticationInitiateError(errorCode: String, errorMessage: String, retryPossible: Boolean) {
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
                        startSDK.isEnabled = true
                    }
                })
            } else {
                startSDK.isEnabled = true
            }

        } catch (e: PlatformNotSupportedException) {
            Log.e(TAG, "Error in initializeAuthenticationSDK: ", e)
            Toast.makeText(activity!!.applicationContext, e.message, Toast.LENGTH_LONG).show()
            startSDK.isEnabled = true
        } catch (e: NullPointerException) {
            Log.e(TAG, "Error in initializeAuthenticationSDK: ", e)
            Toast.makeText(activity!!.applicationContext, e.message, Toast.LENGTH_LONG).show()
            startSDK.isEnabled = true
        } catch (e: MissingPermissionException) {
            Log.e(TAG, "Error in initializeAuthenticationSDK: ", e)
            Toast.makeText(activity!!.applicationContext, e.message, Toast.LENGTH_LONG).show()
            startSDK.isEnabled = true
        } catch (e : IllegalArgumentException) {
            Log.e(TAG, "Error in initializeAuthenticationSDK: ", e)
            Toast.makeText(activity!!.applicationContext, e.message, Toast.LENGTH_LONG).show()
            startSDK.isEnabled = true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AuthenticationSDK.REQUEST_CODE) {
            if (data == null)
                return
            if (resultCode == Activity.RESULT_OK) {
                val transactionReference = data.getStringExtra(AuthenticationSDK.EXTRA_TRANSACTION_REFERENCE)
                val authenticationResult = data.getSerializableExtra(AuthenticationSDK.EXTRA_SCAN_DATA) as AuthenticationResult
            } else if (resultCode == Activity.RESULT_CANCELED) {
                val errorMessage = data.getStringExtra(AuthenticationSDK.EXTRA_ERROR_MESSAGE)
                val errorCode = data.getStringExtra(AuthenticationSDK.EXTRA_ERROR_CODE)
            }

            //At this point, the SDK is not needed anymore. It is highly advisable to call destroy(), so that
            //internal resources can be freed.
            authenticationSDK!!.destroy()
        }
    }
}