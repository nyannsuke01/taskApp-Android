package jp.techacademy.shingo.kobayashi.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.realm.*

import kotlinx.android.synthetic.main.activity_main.*
//import kotlin.cEXTRA_TASKollections.ArrayList

const val EXTRA_TASK = "jp.techacademy.shingo.kobayashi.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mTaskAdapter: TaskAdapter

    private lateinit var mRealm: Realm

    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(t: Realm) {
            reloadListView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            AlertDialog.Builder(this).apply {
                setTitle("削除")
                setMessage(task.title + "を削除しますか")
                setPositiveButton("Ok") { _, _ ->
                    val result = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                    mRealm.beginTransaction()
                    result.deleteAllFromRealm()
                    mRealm.commitTransaction()

                    val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                    val resultPendingIntent = PendingIntent.getBroadcast(
                        this@MainActivity,
                        task.id,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(resultPendingIntent)

                    reloadListView()
                }
                setNegativeButton("CANCEL", null)

                val dialog = create()
                dialog.show()
            }
            true
        }

        category_search_image.setOnClickListener {
            categorySearch()
        }

        reloadListView()

        category_show_all_button.setOnClickListener {
            reloadListView()
        }

    }

    override fun onResume() {
        super.onResume()
        showSpinner()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    private fun categorySearch() {
        val category = category_search_edit.text.toString()
        //Taskクラスからcategory_search_editに記載したのと一致するものを探す
        val query = mRealm.where(Task::class.java).equalTo("category.name",category).findAll()
        mTaskAdapter.taskList = mRealm.copyFromRealm(query)
        listView1.adapter = mTaskAdapter
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun showSpinner(){
        //spinner
        //realmに保存されているカテゴリーを抽出する。
        val result = mRealm.where(Category::class.java).findAll()
        val categoryList = mRealm.copyFromRealm(result)

        val adapter = ArrayAdapter(
            //抽出してきたCategoryのデータをセット
            applicationContext, android.R.layout.simple_spinner_item, categoryList.map { it.name }
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        main_category_spinner.adapter = adapter

        main_category_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val spinnerSelectItem = spinnerParent.selectedItem as String

                category_search_edit.text = spinnerSelectItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }
}