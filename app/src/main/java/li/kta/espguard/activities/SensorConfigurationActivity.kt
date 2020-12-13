package li.kta.espguard.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_configure_sensor.*
import li.kta.espguard.MqttService
import li.kta.espguard.R
import li.kta.espguard.room.LocalSensorDb
import li.kta.espguard.room.SensorEntity

class SensorConfigurationActivity : AppCompatActivity() {

    companion object {
        val TAG: String = SensorConfigurationActivity::class.java.name
        const val EXTRA_SENSOR_ID = "sensorId"

        const val RESULT_DELETE_SENSOR = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_sensor)
        setSupportActionBar(findViewById(R.id.toolbar_support_configure))

        val sensor = LocalSensorDb.getSensorDao(this)
                .findSensorById(intent.getIntExtra(EXTRA_SENSOR_ID, -1))

        setupButtons(sensor)
    }

    private fun setupButtons(sensor: SensorEntity) {
        button_health_check.setOnClickListener { MqttService.getInstance()?.healthCheck(sensor) }

        switch_sensor_on.isChecked = sensor.turnedOn
        switch_sensor_on.setOnCheckedChangeListener { _, _ -> toggleOnSwitch(sensor) }

        button_delete_events.setOnClickListener { deleteEvents(sensor) }
        button_delete_device.setOnClickListener { deleteSensor(sensor) }

        button_sensor_name_save.setOnClickListener { saveNewName(sensor) }
    }

    private fun toggleOnSwitch(sensor: SensorEntity) {
        sensor.turnedOn != sensor.turnedOn
        LocalSensorDb.getSensorDao(this).updateSensor(sensor)
        MqttService.getInstance()?.turnOnOff(sensor)
    }

    private fun saveNewName(sensor: SensorEntity) {
        val newName = et_sensor_name.text.toString()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Cannot rename to \"\"", Toast.LENGTH_SHORT).show()
            return
        }

        Log.i(TAG, "Changing name of $sensor to $newName")  // TODO: NOT WORKING
        sensor.name = newName
        LocalSensorDb.getSensorDao(this).updateSensor(sensor)

        Toast.makeText(this, "Renamed sensor to $newName", Toast.LENGTH_SHORT).show()
    }

    private fun deleteEvents(sensor: SensorEntity) {
        Log.i(TAG, "Deleting events of $sensor")
        removeEventsFromDatabase(sensor)

        Toast.makeText(this, "Deleted ${sensor.name} sensor events",
                       Toast.LENGTH_SHORT).show()
    }

    private fun removeEventsFromDatabase(sensor: SensorEntity) {
        sensor.deviceId?.let {
            LocalSensorDb.getEventDao(applicationContext)
                    .let { dao -> dao.deleteEvents(*dao.findEventsByDeviceId(it)) }
        }
    }

    private fun deleteSensor(sensor: SensorEntity) {
        Log.i(TAG, "Deleting sensor $sensor")

        removeEventsFromDatabase(sensor)
        removeSensorFromDatabase(sensor)

        Toast.makeText(this, "Deleted device", Toast.LENGTH_SHORT).show()

        setResult(RESULT_DELETE_SENSOR)
        finish()
    }

    private fun removeSensorFromDatabase(sensor: SensorEntity) {
        LocalSensorDb.getSensorDao(applicationContext).deleteSensor(sensor)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.open_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}