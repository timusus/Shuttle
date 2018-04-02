package com.simplecity.amp_library.playback

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.simplecity.amp_library.playback.constants.ExternalIntents
import com.simplecity.amp_library.utils.SettingsManager

class BluetoothManager(private val callbacks: MusicService.Callbacks) {

    private var bluetoothReceiver: BroadcastReceiver? = null

    private var bluetoothReceiverIsRegistered: Boolean = false

    private var a2dpReceiver: BroadcastReceiver? = null

    fun registerBluetoothReceiver(context: Context) {

        val filter = IntentFilter()
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)

        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action
                if (action != null) {
                    val extras = intent.extras
                    if (SettingsManager.getInstance().bluetoothPauseDisconnect) {
                        when (action) {
                            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> if (extras != null) {
                                val state = extras.getInt(BluetoothA2dp.EXTRA_STATE)
                                val previousState = extras.getInt(BluetoothA2dp.EXTRA_PREVIOUS_STATE)
                                if ((state == BluetoothA2dp.STATE_DISCONNECTED || state == BluetoothA2dp.STATE_DISCONNECTING) && previousState == BluetoothA2dp.STATE_CONNECTED) {
                                    callbacks.pause()
                                }
                            }
                            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> if (extras != null) {
                                val state = extras.getInt(BluetoothHeadset.EXTRA_STATE)
                                val previousState = extras.getInt(BluetoothHeadset.EXTRA_PREVIOUS_STATE)
                                if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED && previousState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                                    callbacks.pause()
                                }
                            }
                        }
                    }

                    if (SettingsManager.getInstance().bluetoothResumeConnect) {
                        when (action) {
                            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> if (extras != null) {
                                val state = extras.getInt(BluetoothA2dp.EXTRA_STATE)
                                if (state == BluetoothA2dp.STATE_CONNECTED) {
                                    callbacks.play()
                                }
                            }
                            BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> if (extras != null) {
                                val state = extras.getInt(BluetoothHeadset.EXTRA_STATE)
                                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                                    callbacks.play()
                                }
                            }
                        }
                    }
                }
            }
        }

        context.registerReceiver(bluetoothReceiver, filter)
        bluetoothReceiverIsRegistered = true
    }

    fun unregisterBluetoothReceiver(context: Context) {
        if (bluetoothReceiverIsRegistered) {
            context.unregisterReceiver(bluetoothReceiver)
            bluetoothReceiverIsRegistered = false
        }
    }

    fun registerA2dpServiceListener(context: Context) {
        a2dpReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action != null && action == ExternalIntents.PLAY_STATUS_REQUEST) {
                    callbacks.notifyChange(ExternalIntents.PLAY_STATUS_RESPONSE)
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(ExternalIntents.PLAY_STATUS_REQUEST)
        context.registerReceiver(a2dpReceiver, intentFilter)
    }

    fun unregisterA2dpServiceListener(context: Context) {
        context.unregisterReceiver(a2dpReceiver)
    }

    fun sendPlayStateChangedIntent(context: Context, extras: Bundle) {
        val intent = Intent(ExternalIntents.AVRCP_PLAY_STATE_CHANGED)
        intent.putExtras(extras)
        context.sendBroadcast(intent)
    }

    fun sendMetaChangedIntent(context: Context, extras: Bundle) {
        val intent = Intent(ExternalIntents.AVRCP_META_CHANGED)
        intent.putExtras(extras)
        context.sendBroadcast(intent)
    }
}
