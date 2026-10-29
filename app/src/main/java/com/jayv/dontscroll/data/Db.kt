package com.jayv.dontscroll.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local SQLite database. All data stays on the phone. */
class Db(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "dontscroll.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE reels (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER NOT NULL, " +
                "day TEXT NOT NULL, " +
                "hour INTEGER NOT NULL, " +
                "app TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_reels_day ON reels(day)")
        db.execSQL(
            "CREATE TABLE usage (" +
                "day TEXT NOT NULL, " +
                "app TEXT NOT NULL, " +
                "reel_ms INTEGER NOT NULL DEFAULT 0, " +
                "app_ms INTEGER NOT NULL DEFAULT 0, " +
                "sessions INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (day, app))"
        )
        db.execSQL(
            "CREATE TABLE days (" +
                "day TEXT PRIMARY KEY, " +
                "budget INTEGER NOT NULL, " +
                "time_limit INTEGER NOT NULL, " +
                "challenge TEXT, " +
                "tracked INTEGER NOT NULL DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema.
    }
}
