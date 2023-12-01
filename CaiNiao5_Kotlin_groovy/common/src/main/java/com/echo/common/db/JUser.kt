package com.echo.common.db

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "tb_juser")
class JUser {
    @PrimaryKey(autoGenerate = true)
    var jId = 0
    var age = 20
    var name = "哈哈哈哈"

    private constructor()
    constructor(age: Int) {
        this.age = age
    }

    override fun toString(): String {
        return "JUser{" +
                "jId=" + jId +
                ", age=" + age +
                ", name='" + name + '\'' +
                '}'
    }
}