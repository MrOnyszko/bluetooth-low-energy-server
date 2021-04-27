package pl.gratitude.blesever

import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.nio.charset.StandardCharsets
import java.util.*

class BleServerActivity : AppCompatActivity() {

  private val logTag: String = BleServerActivity::class.java.simpleName

  private var notifiableText: String = ""

  private val gattServerCallback = object : BluetoothGattServerCallback() {

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i(logTag, "BluetoothDevice CONNECTED: $device")
      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.i(logTag, "BluetoothDevice DISCONNECTED: $device")
        bleServer.registeredDevices.remove(device)
      }
    }

    override fun onCharacteristicReadRequest(
      device: BluetoothDevice, requestId: Int, offset: Int,
      characteristic: BluetoothGattCharacteristic
    ) {
      when {
        BleServerProfile.NOTIFIABLE_TEXT_VALUE == characteristic.uuid -> {
          Log.i(logTag, "Read NOTIFIABLE_TEXT_VALUE")
          bleServer.sendResponse(
            device,
            requestId,
            BluetoothGatt.GATT_SUCCESS,
            0,
            notifiableText.toByteArray(charset = Charsets.UTF_8),
          )
        }
        else -> {
          Log.w(logTag, "Invalid Characteristic Read: " + characteristic.uuid)
          bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE)
        }
      }
    }

    override fun onCharacteristicWriteRequest(
      device: BluetoothDevice,
      requestId: Int,
      characteristic: BluetoothGattCharacteristic,
      preparedWrite: Boolean,
      responseNeeded: Boolean,
      offset: Int,
      value: ByteArray?
    ) {
      when {
        BleServerProfile.PRIMARY_TEXT_VALUE == characteristic.uuid -> {
          Log.i(logTag, "Write PRIMARY_TEXT_VALUE")
          findViewById<TextView>(R.id.primaryText).text = value?.let {
            String(it, StandardCharsets.UTF_8)
          }
        }
        BleServerProfile.SECONDARY_TEXT_VALUE == characteristic.uuid -> {
          Log.i(logTag, "Write PRIMARY_TEXT_VALUE")
          findViewById<TextView>(R.id.secondaryText).text = value?.let {
            String(it, StandardCharsets.UTF_8)
          }
        }
        else -> {
          Log.w(logTag, "Invalid Characteristic Write: " + characteristic.uuid)
          bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE)
        }
      }
    }

    override fun onDescriptorReadRequest(
      device: BluetoothDevice, requestId: Int, offset: Int,
      descriptor: BluetoothGattDescriptor
    ) {
      if (BleServerProfile.CLIENT_CONFIG == descriptor.uuid) {
        Log.d(logTag, "Config descriptor read")
        val returnValue = if (bleServer.registeredDevices.contains(device)) {
          BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, returnValue)
      } else {
        Log.w(logTag, "Unknown descriptor read request")
        bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE)
      }
    }

    override fun onDescriptorWriteRequest(
      device: BluetoothDevice, requestId: Int,
      descriptor: BluetoothGattDescriptor,
      preparedWrite: Boolean, responseNeeded: Boolean,
      offset: Int, value: ByteArray
    ) {
      if (BleServerProfile.CLIENT_CONFIG == descriptor.uuid) {
        if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
          Log.d(logTag, "Subscribe device to notifications: $device")
          bleServer.registeredDevices.add(device)
        } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
          Log.d(logTag, "Unsubscribe device from notifications: $device")
          bleServer.registeredDevices.remove(device)
        }

        if (responseNeeded) {
          bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS)
        }
      } else {
        Log.w(logTag, "Unknown descriptor write request")
        if (responseNeeded) {
          bleServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE)
        }
      }
    }
  }

  private val bluetoothManager: BluetoothManager by lazy {
    application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  }

  private val bleServer: BleServer by lazy {
    BleServer(
      this,
      bluetoothManager,
      gattServerCallback
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_ble_server)
    setSupportActionBar(findViewById(R.id.toolbar))
    bleServer.start(
      BleServerProfile.BLE_SERVICE,
      BleServerProfile.createService()
    )

    listOf(
      findViewById<Button>(R.id.firstNotifyValue),
      findViewById<Button>(R.id.secondNotifyValue),
      findViewById<Button>(R.id.thirdNotifyValue),
    ).forEach { button ->
      button.setOnClickListener {
        notifiableText = button.text.toString()
        if (bleServer.isServerStarted) {
          bleServer.bluetoothGattServer
            ?.getService(BleServerProfile.NOTIFIABLE_TEXT_VALUE)
            ?.getCharacteristic(BleServerProfile.NOTIFIABLE_TEXT_VALUE)
            ?.also { characteristic ->
              characteristic.value = button.text.toString().toByteArray(charset = Charsets.UTF_8)
              bleServer.notifyRegisteredDevices(characteristic)
            }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    bleServer.stop()
  }
}