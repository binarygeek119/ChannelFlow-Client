package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.channelflow.ChannelFlowConnectionStore
import org.jellyfin.androidtv.channelflow.ChannelFlowGuideRepository
import org.jellyfin.androidtv.channelflow.ChannelFlowPairClient
import org.jellyfin.androidtv.channelflow.ChannelFlowPairException
import org.jellyfin.androidtv.channelflow.ChannelFlowPinCrypto
import org.jellyfin.androidtv.databinding.FragmentConnectPinBinding
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.koin.android.ext.android.inject

class ConnectPinFragment : Fragment() {
	private val pairClient by inject<ChannelFlowPairClient>()
	private val connectionStore by inject<ChannelFlowConnectionStore>()
	private val catalog by inject<ChannelFlowGuideRepository>()
	private var _binding: FragmentConnectPinBinding? = null
	private val binding get() = _binding!!
	private var waitJob: Job? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		_binding = FragmentConnectPinBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.retry.setOnClickListener { startWait() }
		startWait()
	}

	private fun startWait() {
		waitJob?.cancel()
		binding.pin.text = getString(R.string.lbl_quick_pin_waiting)
		binding.status.setText(R.string.lbl_quick_pin_help)
		binding.error.text = ""
		binding.retry.isVisible = false
		waitJob = viewLifecycleOwner.lifecycleScope.launch {
			val result = pairClient.waitForConnection { pin ->
				binding.root.post {
					val views = _binding ?: return@post
					if (!isAdded) return@post
					views.pin.text = ChannelFlowPinCrypto.format(pin)
					views.status.setText(R.string.lbl_quick_pin_waiting_server)
				}
			}
			if (!isAdded) return@launch
			result.fold(
				onSuccess = { connection ->
					connectionStore.save(connection)
					catalog.clear()
					(activity as? StartupActivity)?.openMain()
				},
				onFailure = { error ->
					binding.retry.isVisible = true
					binding.retry.requestFocus()
					binding.error.setText(
						when ((error as? ChannelFlowPairException)?.kind) {
							ChannelFlowPairException.Kind.EXPIRED -> R.string.msg_quick_pin_expired
							else -> R.string.msg_pin_server_unreachable
						}
					)
				},
			)
		}
	}

	override fun onDestroyView() {
		waitJob?.cancel()
		_binding = null
		super.onDestroyView()
	}
}
