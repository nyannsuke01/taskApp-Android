package jp.techacademy.shingo.kobayashi.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Category : RealmObject() {
    var name: String = ""
    @PrimaryKey
    var id: Int = 0
}
