package com.userndot.sdk.android

import android.content.Context
import androidx.room.*
import androidx.room.Database

@Dao
public interface PersistData {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public fun save(data:Data)

    @Insert
    public fun saveAll(data:List<Data>)

    @Query("delete from dataTable where id= :id")
    public fun delete(id:Long)

    @Query("SELECT * FROM dataTable")
    public fun get():List<Data>

    @Query ("SELECT * FROM dataTable ORDER BY time ASC LIMIT 1")
    fun getFirst(): Data

    @Query("DELETE FROM dataTable")
    fun deleteAll()

    @Query("SELECT *FROM dataTable ORDER BY time DESC LIMIT 1 ")
    fun getLast():Data
}


@Database(entities = arrayOf(Data::class),version = 1)
abstract class MyDatabase :RoomDatabase(){

    abstract fun persistData():PersistData
}

class UserNDotDatabase {
    private val DATABASE_NAME:String="userndot"

    companion object {

        @Volatile private var database: MyDatabase? = null

        fun getDatabase(context:Context):MyDatabase{
            if(database==null){
                synchronized(this) {
                    if(database == null) {
                        this.database = Room.databaseBuilder(context, MyDatabase::class.java, "userndot").build()
                    }
                    return this.database!!
                }
            }else{
                return database as MyDatabase
            }
        }
    }


}