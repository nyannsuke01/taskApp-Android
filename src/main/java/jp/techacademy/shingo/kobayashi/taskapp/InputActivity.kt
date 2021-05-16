package jp.techacademy.shingo.kobayashi.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

class InputActivity : AppCompatActivity() {

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null

    private lateinit var mRealm: Realm

    private val mOnDateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                val dateString =
                    mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
                date_button.text = dateString

            }, mYear, mMonth, mDay
        )
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(
            this,
            TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                mHour = hour
                mMinute = minute
                val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)
                times_button.text = timeString
            }, mHour, mMinute, false
        )
        timePickerDialog.show()
    }

    private val mOnDoneClickListener = View.OnClickListener {
        addTask()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as android.support.v7.widget.Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // UI部品の設定
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)

        // EXTRA_TASK から Task の id を取得して、 id から Task のインスタンスを取得する
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        mRealm = Realm.getDefaultInstance()
        mTask = mRealm.where(Task::class.java).equalTo("id", taskId).findFirst()
        mRealm.close()

        if (mTask == null) {
            // 新規作成の場合
            val calender = Calendar.getInstance()
            mYear = calender.get(Calendar.YEAR)
            mMonth = calender.get(Calendar.MONTH)
            mDay = calender.get(Calendar.DAY_OF_MONTH)
            mHour = calender.get(Calendar.HOUR_OF_DAY)
            mMinute = calender.get(Calendar.MINUTE)
        } else {
            // 更新の場合
            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)

            val calender = Calendar.getInstance()
            calender.time = mTask!!.date
            mYear = calender.get(Calendar.YEAR)
            mMonth = calender.get(Calendar.MONTH)
            mDay = calender.get(Calendar.DAY_OF_MONTH)
            mHour = calender.get(Calendar.HOUR_OF_DAY)
            mMinute = calender.get(Calendar.MINUTE)

            val dateString =
                mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

            date_button.text = dateString
            times_button.text = timeString
        }

        category_add_button.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        showSpinner()
    }

    private fun addTask() {
        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

        if (mTask == null) {
            // 新規作成の場合
            mTask = Task()

            val taskRealmResults = realm.where(Task::class.java).findAll()
            val identifier: Int =
                if (taskRealmResults.max("id") != null) {
                    taskRealmResults.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mTask!!.id = identifier
        }

        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()
        val result = realm.where(Category::class.java).findAll()
        val categoryList = realm.copyFromRealm(result)

        mTask!!.title = title
        mTask!!.contents = content
        //カテゴリー選択された項目のindexをとってきている
        if (mTask!!.category == null){
            Toast.makeText(applicationContext, "カテゴリを追加でカテゴリの新規作成ができます。", Toast.LENGTH_SHORT).show()
        }else{
            mTask!!.category = categoryList[category_spinner.selectedItemPosition]
        }
        val calender = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calender.time
        mTask!!.date = date

        realm.copyToRealmOrUpdate(mTask!!)
        realm.commitTransaction()

        realm.close()

        val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK, mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calender.timeInMillis, resultPendingIntent)
    }

    private fun showSpinner(){
        val result = mRealm.where(Category::class.java).findAll()
        val categoryList = mRealm.copyFromRealm(result)

        val adapter = ArrayAdapter(
            applicationContext, android.R.layout.simple_spinner_item, categoryList.map { it.name })
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item)

        category_spinner.adapter = adapter

        category_spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val spinnerSelectItem = spinnerParent.selectedItem as String

                category_edit_text.text = spinnerSelectItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }
}
