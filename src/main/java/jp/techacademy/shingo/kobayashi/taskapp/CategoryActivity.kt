package jp.techacademy.shingo.kobayashi.taskapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_category.*
import io.realm.Realm


class CategoryActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        category_register_button.setOnClickListener {
            addCategory()
            finish()
        }
    }

    private fun addCategory(){
        mRealm = Realm.getDefaultInstance()
        mRealm.beginTransaction()
        val newCategory = Category()
        val categoryCreate = category_create_edit.text.toString()
        newCategory.name = categoryCreate
        val categoryRealmResults = mRealm.where(Category::class.java).findAll()
        val identifier:Int =
            if (categoryRealmResults.max("id") != null){
                categoryRealmResults.max("id")!!.toInt() +1
            }else{
                0
            }
        newCategory.id = identifier

        mRealm.copyToRealmOrUpdate(newCategory)
        mRealm.commitTransaction()
        mRealm.close()
    }
}

