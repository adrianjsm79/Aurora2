package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import com.tecsup.aurora.databinding.DialogTermsBinding

class TermsDialogFragment(
    private val htmlContent: String,
    private val onAccepted: () -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogTermsBinding
    private var hasReachedBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("TermsDialog", "HTML recibido (Primeros 100 chars): ${htmlContent.take(100)}")

        setupWebView()
        setupButtons()
    }

    private fun setupWebView() {
        binding.webviewTerms.apply {
            settings.javaScriptEnabled = false

            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBarTerms.visibility = View.GONE
                }
            }

            setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val webView = v as WebView
                val contentHeight = (webView.contentHeight * webView.scale).toInt()
                val webViewHeight = webView.height

                val diff = contentHeight - (scrollY + webViewHeight)

                if (diff <= 20) {
                    if (!hasReachedBottom) {
                        hasReachedBottom = true
                        enableAcceptButton()
                    }
                }
            }
        }
    }

    private fun enableAcceptButton() {
        // Aseguramos que se ejecute en el hilo principal por si acaso
        binding.btnAcceptTerms.post {
            binding.btnAcceptTerms.isEnabled = true
            binding.scrollHint.text = "Gracias por leer."
            binding.scrollHint.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }
    }

    private fun setupButtons() {
        binding.btnDecline.setOnClickListener {
            dismiss()
        }
        binding.btnAcceptTerms.setOnClickListener {
            onAccepted()
            dismiss()
        }
    }
}