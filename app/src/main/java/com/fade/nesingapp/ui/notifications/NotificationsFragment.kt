package com.fade.nesingapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fade.nesingapp.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val tracerouteGoogleButton: Button = binding.buttonTracerouteGoogle
        val tracerouteAmazonButton: Button = binding.buttonTracerouteAmazon
        val tracerouteMicrosoftButton: Button = binding.buttonTracerouteMicrosoft
        val tracerouteFacebookButton: Button = binding.buttonTracerouteFacebook
        val tracerouteAllegroButton: Button = binding.buttonTracerouteAllegro
        val tracerouteWpButton: Button = binding.buttonTracerouteWp
        val tracerouteCustomButton: Button = binding.buttonTracerouteCustom
        val customUrlEditText: EditText = binding.edittextTracerouteCustomUrl
        val tracerouteResultTextView: TextView = binding.textTracerouteResult

        fun setupTracerouteButton(button: Button, url: String) {
            button.setOnClickListener {
                tracerouteResultTextView.text = ""
                lifecycleScope.launch {
                    val hops = runTracerouteKotlin(url)
                    android.util.Log.d("Traceroute", "Hops for $url: $hops")
                    tracerouteResultTextView.text = hops.mapIndexed { index, hop -> "${index + 1}. $hop" }.joinToString("\n")
                }
            }
        }

        setupTracerouteButton(tracerouteGoogleButton, "google.com")
        setupTracerouteButton(tracerouteAmazonButton, "amazon.com")
        setupTracerouteButton(tracerouteMicrosoftButton, "microsoft.com")
        setupTracerouteButton(tracerouteFacebookButton, "facebook.com")
        setupTracerouteButton(tracerouteAllegroButton, "allegro.pl")
        setupTracerouteButton(tracerouteWpButton, "wp.pl")

        tracerouteCustomButton.setOnClickListener {
            val url = customUrlEditText.text.toString()
            if (url.isBlank()) {
                tracerouteResultTextView.text = "Please enter a URL."
                return@setOnClickListener
            }
            tracerouteResultTextView.text = ""
            lifecycleScope.launch {
                val hops = runTracerouteKotlin(url)
                tracerouteResultTextView.text = hops.mapIndexed { index, hop -> "${index + 1}. $hop" }.joinToString("\n")
            }
        }

        return root
    }

    private suspend fun runTracerouteKotlin(host: String): List<String> {
        return withContext(Dispatchers.IO) {
            val hops = mutableListOf<String>()
            try {
                val maxHops = 30
                val timeout = 3000
                val port = 33434
                val address = InetAddress.getByName(host)
                val socket = DatagramSocket()
                socket.soTimeout = timeout
                for (ttl in 1..maxHops) {
                socket.soTimeout = timeout
                socket.setSoTimeout(timeout)
                val buf = ByteArray(32)
                val packet = DatagramPacket(buf, buf.size, address, port)
                val sendTime = System.currentTimeMillis()
                socket.send(packet)
                    try {
                        val recvPacket = DatagramPacket(ByteArray(512), 512)
                        socket.receive(recvPacket)
                        val recvTime = System.currentTimeMillis()
                        val hopAddress = recvPacket.address.hostAddress ?: "Unknown"
                        val timeTaken = recvTime - sendTime
                        hops.add("$hopAddress - ${timeTaken}ms")
                        if (recvPacket.address == address) {
                            break
                        }
                    } catch (e: SocketTimeoutException) {
                        hops.add("* Request timed out")
                    }
                }
                socket.close()
            } catch (e: Exception) {
                hops.add("Traceroute failed: ${e.localizedMessage}")
            }
            hops
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
