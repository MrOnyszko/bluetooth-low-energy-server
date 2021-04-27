package pl.gratitude.blesever

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

object BleServerProfile {

  val BLE_SERVICE: UUID = UUID.fromString("55897760-9c83-11eb-a8b3-0242ac130003")
  val PRIMARY_TEXT_VALUE: UUID = UUID.fromString("c9c94f04-a74d-11eb-bcbc-0242ac130002")
  val SECONDARY_TEXT_VALUE: UUID = UUID.fromString("c9c95102-a74d-11eb-bcbc-0242ac130002")
  val NOTIFIABLE_TEXT_VALUE: UUID = UUID.fromString("c9c951f2-a74d-11eb-bcbc-0242ac130002")
  val CLIENT_CONFIG: UUID = UUID.fromString("394b1b77-c05b-4663-b20d-db4db09eb765")

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

      BluetoothGattCharacteristic(
        NOTIFIABLE_TEXT_VALUE,
        PROPERTY_READ or PROPERTY_NOTIFY,
        PERMISSION_READ
      ).apply { addDescriptor(configDescriptor) }
        .also { addCharacteristic(it) }
    }
  }
}