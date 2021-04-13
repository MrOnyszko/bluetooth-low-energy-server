package pl.gratitude.blesever

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

object BleServerProfile {

  val BLE_SERVICE: UUID = UUID.fromString("55897760-9c83-11eb-a8b3-0242ac130003")
  val PRIMARY_TEXT_VALUE: UUID = UUID.fromString("55897a08-9c83-11eb-a8b3-0242ac130003")
  val SECONDARY_TEXT_VALUE: UUID = UUID.fromString("55897b02-9c83-11eb-a8b3-0242ac130003")
  val CLIENT_CONFIG: UUID = UUID.fromString("55897c9c-9c83-11eb-a8b3-0242ac130003")

  fun createService(): BluetoothGattService = BluetoothGattService(
    BLE_SERVICE,
    BluetoothGattService.SERVICE_TYPE_PRIMARY
  ).apply {
    BluetoothGattDescriptor(
      CLIENT_CONFIG,
      BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
    ).let { configDescriptor ->
      BluetoothGattCharacteristic(
        PRIMARY_TEXT_VALUE,
        PROPERTY_WRITE_NO_RESPONSE,
        PERMISSION_WRITE
      ).apply { addDescriptor(configDescriptor) }
        .also { addCharacteristic(it) }

      BluetoothGattCharacteristic(
        SECONDARY_TEXT_VALUE,
        PROPERTY_WRITE_NO_RESPONSE,
        PERMISSION_WRITE
      ).apply { addDescriptor(configDescriptor) }
        .also { addCharacteristic(it) }
    }
  }
}