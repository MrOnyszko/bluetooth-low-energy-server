package pl.gratitude.blesever

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class BleServer(
  private val context: Context,
  private val bluetoothManager: BluetoothManager,
  private val gattServerCallback: BluetoothGattServerCallback
) {

  var isServerStarted = false
    private set

  val tag: String = BleServer::class.java.simpleName

  val registeredDevices = mutableSetOf<BluetoothDevice>()

  val isBluetoothSupported: Boolean
    get() = bluetoothManager.adapter != null

  private val isBlueToothEnabled: Boolean
    get() = bluetoothManager.adapter != null && bluetoothManager.adapter.isEnabled

  private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

  private val bluetoothLeAdvertiser: BluetoothLeAdvertiser? by lazy {
    bluetoothManager.adapter.bluetoothLeAdvertiser
  }

  val bluetoothGattServer: BluetoothGattServer? by lazy {
    bluetoothManager.openGattServer(context, gattServerCallback)
  }

  var bluetoothReceiver: BroadcastReceiver? = null

  private val advertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
      Log.d(tag, "LE advertise started")
    }

    override fun onStartFailure(errorCode: Int) {
      Log.w(tag, "LE advertise failed: $errorCode")
    }
  }

  fun start(
    serviceUuid: UUID,
    bluetoothGattService: BluetoothGattService,
    advertiseCallback: AdvertiseCallback = this.advertiseCallback
  ) {
    if (isServerStarted) {
      Log.w(tag, "Server is already started")
      return
    }

    if (!isBlueToothEnabled) {
      Log.d(tag, "Bluetooth is currently disabled...enabling")
      bluetoothAdapter?.enable()
    } else {
      Log.d(tag, "Bluetooth enabled...starting services")
      startAdvertising(serviceUuid, advertiseCallback)
      startBluetoothGattServer(bluetoothGattService)
    }

    bluetoothReceiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
          BluetoothAdapter.STATE_ON -> {
            startAdvertising(serviceUuid, advertiseCallback)
            startBluetoothGattServer(bluetoothGattService)
          }
          BluetoothAdapter.STATE_OFF -> {
            stopBluetoothGattServer()
            stopAdvertising(advertiseCallback)
          }
        }
      }
    }
    context.registerReceiver(
      bluetoothReceiver,
      IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    )
    isServerStarted = true
    Log.d(tag, "Server started $isServerStarted")

  }

  fun stop(advertiseCallback: AdvertiseCallback = this.advertiseCallback) {
    Log.d(tag, "Server is running $isServerStarted")
    bluetoothReceiver?.let { receiver ->

      if (isBlueToothEnabled) {
        stopBluetoothGattServer()
        stopAdvertising(advertiseCallback)
      }

      context.unregisterReceiver(receiver)
      isServerStarted = false
      Log.d(tag, "Server stop [started = $isServerStarted]")
    }
  }

  fun notifyRegisteredDevices(
    bluetoothGattCharacteristic: BluetoothGattCharacteristic,
    confirm: Boolean = false
  ) {
    if (registeredDevices.isEmpty()) {
      Log.i(tag, "No subscribers registered")
      return
    }

    Log.i(tag, "Sending update to ${registeredDevices.size} subscribers")
    for (device in registeredDevices) {
      bluetoothGattServer?.notifyCharacteristicChanged(
        device,
        bluetoothGattCharacteristic,
        confirm
      )
    }
  }

  fun sendResponse(
    device: BluetoothDevice,
    requestId: Int,
    status: Int,
    offset: Int = 0,
    value: ByteArray? = null
  ): Boolean {
    return bluetoothGattServer?.sendResponse(device, requestId, status, offset, value) ?: false
  }

  fun startBluetoothGattServer(bluetoothGattService: BluetoothGattService) {
    bluetoothGattServer?.addService(bluetoothGattService)
    Log.d(tag, "Start bluetooth gatt server")
  }

  fun stopBluetoothGattServer() {
    bluetoothGattServer?.close()
    Log.d(tag, "Stop bluetooth gatt server")
  }

  fun startAdvertising(
    serviceUuid: UUID,
    advertiseCallback: AdvertiseCallback = this.advertiseCallback
  ) = bluetoothLeAdvertiser?.let { advertiser ->
    val settings = AdvertiseSettings.Builder()
      .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
      .setConnectable(true)
      .setTimeout(0)
      .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
      .build()

    val data = AdvertiseData.Builder()
      .setIncludeTxPowerLevel(false)
      .addServiceUuid(ParcelUuid(serviceUuid))
      .build()

    advertiser.startAdvertising(settings, data, advertiseCallback)
    Log.d(tag, "Start advertising with settings: $settings and data: $data")
  }

  fun stopAdvertising(advertiseCallback: AdvertiseCallback = this.advertiseCallback) {
    bluetoothLeAdvertiser?.let { advertiser ->
      advertiser.stopAdvertising(advertiseCallback)
      Log.d(tag, "Stop advertising")
    }
  }
}
