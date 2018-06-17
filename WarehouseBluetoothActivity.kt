package spreetail.warehouse.tools.activity.base

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.*
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.view_manual_barcode.view.et_barcode_input
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import spreetail.warehouse.tools.R
import spreetail.warehouse.tools.R.drawable
import spreetail.warehouse.tools.R.id
import spreetail.warehouse.tools.R.string
import spreetail.warehouse.tools.WarehouseToolsApplication
import spreetail.warehouse.tools.constant.RequestCode
import spreetail.warehouse.tools.service.BluetoothService
import spreetail.warehouse.tools.service.impl.WTBluetoothService
import spreetail.warehouse.tools.util.WarehouseToolsUtil
import timber.log.Timber
import javax.inject.Inject

/**
 * This Base class is only meant to be used for Activities
 * that need to juggle bluetooth management for warehouse device(s)
 * NOTE: Tightly coupled with warehouse devices.
 * Created by elliot.mitchell on 8/2/2017.
 */
abstract class WarehouseBluetoothActivity : SpreetailActivity() {

  @Inject
  lateinit var bluetoothService: BluetoothService
  var bluetoothMenuItem: MenuItem? = null
  var retriedConnection = 0
  private var foundOurDevice: Boolean = false
  private var shownNoDevicePairedPrompt = false
  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      if (BluetoothDevice.ACTION_FOUND == action) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (bluetoothService.isExpectedDeviceName(device.name)) {
          Timber.i("BT: found our device, attempting to connect now, device: %s",
              device.toString())
          foundOurDevice = true
          bluetoothService.bluetoothAdapter.cancelDiscovery()
          runOnUiThread { hideProgressDialog() }
          //TODO: https://stackoverflow.com/a/46166223/2435402
          AsyncTask.execute {
            bluetoothService.connectToDevice(device)
          }
        } else {
          Timber.i("BT: found a device, but wasn't ours: %s", device.toString())
        }
      } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
        foundOurDevice = false
        showProgressDialog(getString(R.string.searching))
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
        Timber.i("BT: discovery finished...")
        runOnUiThread {
          hideProgressDialog()
          if (!foundOurDevice && !shownNoDevicePairedPrompt) {
            val builder = AlertDialog.Builder(this@WarehouseBluetoothActivity)
            builder.setTitle(R.string.title_no_paired_device)
            builder.setMessage(R.string.no_find_device)
            builder.setNegativeButton(
                R.string.cancel) { dialogInterface, _ ->
              dialogInterface.dismiss()
            }

            builder.setPositiveButton(R.string.retry) { dialogInterface, _ ->
              dialogInterface.dismiss()
              bluetoothService.bluetoothAdapter.startDiscovery()
            }
            builder.setCancelable(false)
            builder.create().show()
            shownNoDevicePairedPrompt = true
          }
        }
      }
    }
  }
  private var recentlyFocusedEditText: EditText? = null
  private var startingBluetooth: Boolean = false
  private var showRetryingToast: Boolean = false
  private val focusListener = View.OnFocusChangeListener { v, hasFocus ->
    if (hasFocus && v is EditText) {
      recentlyFocusedEditText = v
    }
  }
  //holding an instance of the last value scanned
  var lastScannedValue: String = ""

  /**
   * @param message the (sanitized [WTBluetoothService.sanitizeString]) string that
   * the scanner has read
   * @param alreadySetFocusedEditText if we've set an edit text in [.addScannerEditText], and
   * we've set that edittext's text, this will return true. This is so we can prioritize the focused
   * edit texts over a "smart assumption" of what we're intending to scan
   */
  abstract fun onScannerMessage(message: String, alreadySetFocusedEditText: Boolean)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WarehouseToolsApplication.getInstance().component.inject(this)

    setWakeLock()

    initBluetooth()

    setOnClickListeners()
  }

  private fun setOnClickListeners() {
    val barcodeView = window?.findViewById<View>(id.barcode_capable)
    barcodeView?.let {
      barcodeView.setOnClickListener {
        showThisFieldPopulatesWithScannerToast()
      }
    }
  }

  private fun setWakeLock() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  private fun setUpReceiver() {
    val filter = IntentFilter()
    filter.addAction(BluetoothDevice.ACTION_FOUND)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    registerReceiver(receiver, filter)
  }

  private fun initBluetooth() {
    startingBluetooth = true
    setupBluetoothStatusIcon()

    if (bluetoothService.isConnected) {
      Timber.i("BT: we're already connected, no need to reconnect")
      return
    }

    if (!bluetoothService.doesDeviceSupportBluetooth()) {
      Timber.i("BT: Device doesn't support bluetooth...")
      return
    }
    if (!bluetoothService.isBluetoothEnabled) {
      Timber.i("BT: Bluetooth isn't enabled, prompting to enable")
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      startActivityForResult(enableBtIntent, RequestCode.ENABLE_BT)
    } else {
      Timber.i("BT: Bluetooth already enabled")
      connectToBluetoothAfterBluetoothEnabled()
    }
  }

  private fun setupBluetoothStatusIcon() {
    bluetoothService.currentConnectionStatus.let {
      setBluetoothMenuItemDrawableBasedOnStatus(it)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == RequestCode.ENABLE_BT) {
      if (resultCode == Activity.RESULT_OK) {
        Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show()
        connectToBluetoothAfterBluetoothEnabled()
      } else {
        Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onStart() {
    super.onStart()
    setBluetoothMenuItemDrawableBasedOnStatus(bluetoothService.currentConnectionStatus)

    setUpReceiver()
    //todo: needs to listen to a sticky event of "just came back from background"
    //initBluetooth();
  }

  override fun onStop() {
    super.onStop()
    unregisterReceiver(receiver)
  }

  private fun clearRetriesAndAttemptConnect() {

    Timber.i("BT: resetting retry count, retrying for real now")
    retriedConnection = 0
    connectToBluetoothAfterBluetoothEnabled()
  }

  private fun retryConnection() {
    Timber.i("BT: retrying connection (%d)", retriedConnection)
    if (retriedConnection < BLUETOOTH_CONNECTION_RETRY_LIMIT) {
      if (showRetryingToast) {
        Toast.makeText(this, R.string.failed_to_connect_retrying, Toast.LENGTH_SHORT).show()
      }
      connectToBluetoothAfterBluetoothEnabled()
    } else {
      bluetoothService.closeConnection()
      hideProgressDialog()
      Timber.i("BT: Max number of retries reached, stopping for now")
    }
    retriedConnection++
  }

  private fun connectToBluetoothAfterBluetoothEnabled() {
    val device = bluetoothService.bondedDevice
    if (device != null) {
      Timber.i("BT: we found our device, attempting to connected")
      //assuming their bonded already...
      doAsync {
        bluetoothService.connectToDevice(device)
      }
    } else if (!startingBluetooth) {
      Timber.i("BT: didn't find our device, lets ask if we want to search")
      val builder = AlertDialog.Builder(this)
      builder.setTitle(R.string.title_no_paired_device)
      builder.setMessage(R.string.no_device_paired)
      builder.setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }

      builder.setPositiveButton(R.string.search_and_connect) { dialogInterface, _ ->
        dialogInterface.dismiss()
        bluetoothService.bluetoothAdapter.startDiscovery()
      }
      builder.setCancelable(false)
      builder.create().show()
    }

    startingBluetooth = false
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    bluetoothMenuItem = menu.findItem(R.id.bluetooth)
    if (WarehouseToolsUtil.isDebugEmulator) {
      addManualBarcodeEntryButton(menu)
    }
    setupBluetoothStatusIcon()
    return true
  }

  private fun addManualBarcodeEntryButton(menu: Menu) {
    val barcodeMenuItem = menu.add(BARCODE_GROUP_ID, BARCODE_MENU_ID, BARCODE_ORDER_VALUE,
        getString(string.manual_barcode))
    barcodeMenuItem.setIcon(drawable.ic_barcode)
    barcodeMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle item selection
    when (item.itemId) {
      R.id.bluetooth -> {
        onBluetoothIconClicked(item)
        return true
      }
      BARCODE_MENU_ID -> {
        val editTextView = LayoutInflater.from(this).inflate(R.layout.view_manual_barcode, null)

        AlertDialog.Builder(this)
            .setView(editTextView)
            .setPositiveButton(R.string.submit,
                { dialogInterface, _ ->
                  val submittedScanValue = editTextView.et_barcode_input.text.toString()
                  onScannerMessage(submittedScanValue,
                      setFocusedEditTextWithScannerString(submittedScanValue))
                  dialogInterface.dismiss()
                })
            .setNegativeButton(R.string.cancel,
                { dialogInterface, _ -> dialogInterface.dismiss() })
            .show()

        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onFirstActivityToForeground() {
    super.onFirstActivityToForeground()
    initBluetooth()
  }

  @Suppress("UNUSED_PARAMETER")
  fun onBluetoothIconClicked(item: MenuItem) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(R.string.bluetooth_status)
    if (bluetoothService.currentConnectionStatus == null) {
      return
    }
    when (bluetoothService.currentConnectionStatus) {
      BluetoothService.BluetoothConnectionStatus.CONNECTED -> {
        builder.setMessage(getString(R.string.currently_connected_to_device,
            if (bluetoothService.connectedDevice == null)
              ""
            else
              bluetoothService.connectedDevice.name))
        builder.setPositiveButton(R.string.ok) { dialogInterface, _ ->
          dialogInterface.dismiss()
          showRetryingToast = false
        }
      }
      BluetoothService.BluetoothConnectionStatus.DISCONNECTED -> {
        builder.setMessage(R.string.not_connected)
        builder.setPositiveButton(R.string.connect) { dialogInterface, _ ->
          dialogInterface.dismiss()
          showRetryingToast = true
          clearRetriesAndAttemptConnect()
        }
      }
      BluetoothService.BluetoothConnectionStatus.FAILED -> {
        builder.setMessage(R.string.failed_connected)
        builder.setPositiveButton(R.string.reconnect) { dialogInterface, _ ->
          dialogInterface.dismiss()
          showRetryingToast = true
          clearRetriesAndAttemptConnect()
        }
      }
    }

    builder.setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
    builder.create().show()
  }

  private fun setBluetoothMenuItemDrawableBasedOnStatus(
      status: BluetoothService.BluetoothConnectionStatus) {
    var newDrawable = R.drawable.ic_bluetooth_disabled_white_24dp
    when (status) {
      BluetoothService.BluetoothConnectionStatus.CONNECTED -> newDrawable = R.drawable.ic_bluetooth_connected_white_24dp
      BluetoothService.BluetoothConnectionStatus.DISCONNECTED -> newDrawable = R.drawable.ic_bluetooth_disabled_white_24dp
      BluetoothService.BluetoothConnectionStatus.FAILED -> newDrawable = R.drawable.ic_bluetooth_disabled_white_24dp
    }

    if (bluetoothMenuItem != null) {
      bluetoothMenuItem!!.setIcon(newDrawable)
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onEvent(event: BluetoothService.BluetoothStatusEvent) {
    when (event.status) {
      BluetoothService.BluetoothConnectionStatus.CONNECTED -> {
        hideProgressDialog()
        Timber.i("BT: Bluetooth successfully connected")
      }
      BluetoothService.BluetoothConnectionStatus.DISCONNECTED -> Timber.i(
          "BT: Bluetooth disconnected")
      null, BluetoothService.BluetoothConnectionStatus.FAILED -> {
        Timber.i("BT: Failed at connected to device with message %s, going to retry",
            event.message)
        retryConnection()
      }
    }

    setBluetoothMenuItemDrawableBasedOnStatus(event.status)
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onEvent(event: BluetoothService.BluetoothMessageReceivedEvent) {
    lastScannedValue = event.message
    log("Scanned text: ${lastScannedValue}")
    onScannerMessage(lastScannedValue, setFocusedEditTextWithScannerString(lastScannedValue))
  }

  /**
   * @param scannedString string to set the focused edit text
   * @return if we set any edit text field
   */
  private fun setFocusedEditTextWithScannerString(scannedString: String): Boolean {
    if (recentlyFocusedEditText != null) {
      recentlyFocusedEditText!!.setText(scannedString)
      return true
    } else {
      return false
    }
  }

  //TODO: fine tune this and use it prior to using bluetooth
  fun promptUserToConnectBluetoothPrior(): Boolean {
    if (bluetoothService.isConnected) {
      return false
    } else {
      val builder = AlertDialog.Builder(this)
      builder.setTitle(R.string.bluetooth_status)
      builder.setMessage(R.string.action_requires_bluetooth)

      when (bluetoothService.currentConnectionStatus) {
        BluetoothService.BluetoothConnectionStatus.DISCONNECTED -> {
          builder.setMessage(R.string.not_connected)
          builder.setPositiveButton(R.string.connect) { dialogInterface, _ ->
            dialogInterface.dismiss()
            clearRetriesAndAttemptConnect()
          }
        }
        BluetoothService.BluetoothConnectionStatus.FAILED -> {

          builder.setMessage(R.string.failed_connected)
          builder.setPositiveButton(R.string.reconnect) { dialogInterface, _ ->
            dialogInterface.dismiss()
            clearRetriesAndAttemptConnect()
          }
        }
        else -> {
        }
      }

      builder.setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
      builder.create().show()
      return true
    }
  }

  fun showThisFieldPopulatesWithScannerToast() {
    Toast.makeText(this, R.string.this_field_will_populate_with_scanner_read, Toast.LENGTH_LONG)
        .show()
  }

  /**
   * Can add edit texts that will consume scanner message if focused
   *
   * @param editText edit text that should consume scanner
   */
  fun addScannerEditText(editText: EditText) {
    editText.onFocusChangeListener = focusListener
  }

  companion object {
    const val BLUETOOTH_CONNECTION_RETRY_LIMIT = 2
    val BARCODE_MENU_ID = View.generateViewId()
    const val BARCODE_GROUP_ID = 1
    const val BARCODE_ORDER_VALUE = 0
  }
}
