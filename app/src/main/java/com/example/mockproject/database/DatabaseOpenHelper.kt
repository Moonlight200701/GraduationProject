package com.example.mockproject.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.mockproject.model.Movie

class DatabaseOpenHelper(
    context: Context?, factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(context, DB_NAME, factory, DB_VERSION) {

    companion object {
        const val DB_VERSION = 1
        const val DB_NAME = "movie_db"

        private var MOVIE_TABLE = "movie_table"
        private var REMINDER_TABLE = "reminder_table"

        private var MOVIE_ID = "movie_id"
        private var MOVIE_TITLE = "movie_name"
        private var MOVIE_RATING = "movie_rating"
        private var MOVIE_OVERVIEW = "movie_overview"
        private var MOVIE_DATE = "movie_date"
        private var MOVIE_IMAGE_POSTER = "movie_image"
        private var MOVIE_ADULT = "movie_adult"
        private var MOVIE_FAVORITE = "movie_favorite"
        private var REMINDER_TIME = "movie_reminder_time"
        private var REMINDER_TIME_DISPLAY = "movie_reminder_time_display"
        private var USER_ID = "user_id"

        private var GENRE_TABLE = "genre_table"
        private var GENRE_ID = "genre_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // SQL statement to create movie table
        
        val createTableMovie = "CREATE TABLE $MOVIE_TABLE ( " +
                "$MOVIE_ID INTEGER PRIMARY KEY," +
                "$MOVIE_TITLE TEXT," +
                "$MOVIE_OVERVIEW TEXT, " +
                "$MOVIE_RATING REAL, " +
                "$MOVIE_DATE TEXT, " +
                "$MOVIE_IMAGE_POSTER TEXT, " +
                "$MOVIE_ADULT INTEGER, " +
                "$MOVIE_FAVORITE INTEGER," +
                "$USER_ID TEXT)"
        // SQL statement to create reminder table
        val createTableReminder = "CREATE TABLE $REMINDER_TABLE ( " +
                "$MOVIE_ID INTEGER PRIMARY KEY," +
                "$MOVIE_TITLE TEXT," +
                "$MOVIE_OVERVIEW TEXT, " +
                "$MOVIE_RATING REAL, " +
                "$MOVIE_DATE TEXT, " +
                "$MOVIE_IMAGE_POSTER TEXT, " +
                "$MOVIE_ADULT INTEGER, " +
                "$MOVIE_FAVORITE INTEGER," +
                "$REMINDER_TIME TEXT," +
                "$REMINDER_TIME_DISPLAY TEXT," +
                "$USER_ID TEXT)"
        //SQL statement to create a genres table
        val createTableMovieGenres = "CREATE TABLE $GENRE_TABLE ( " +
                "$MOVIE_ID INTEGER," +
                "$GENRE_ID TEXT," +
                "PRIMARY KEY($MOVIE_ID, $GENRE_ID))"

        // Execute SQL statements to create tables
        db.execSQL(createTableMovie)
        db.execSQL(createTableReminder)
        db.execSQL(createTableMovieGenres)
    }

    // Called when the database needs to be upgraded
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableMovie = "DROP TABLE $MOVIE_TABLE"
        val dropTableReminder = "DROP TABLE $REMINDER_TABLE"
        val dropTableGenre = "DROP TABLE $GENRE_TABLE"
        db!!.execSQL(dropTableMovie)
        db.execSQL(dropTableReminder)
        db.execSQL(dropTableGenre)
        onCreate(db)
    }

    // Method to add a movie to the database
    fun addMovie(movie: Movie, userId: String) : Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(MOVIE_ID, movie.id)
        contentValues.put(MOVIE_TITLE, movie.title)
        contentValues.put(MOVIE_OVERVIEW, movie.overview)
        contentValues.put(MOVIE_RATING, movie.voteAverage)
        contentValues.put(MOVIE_DATE, movie.releaseDate)
        contentValues.put(MOVIE_IMAGE_POSTER, movie.posterPath)
        contentValues.put(USER_ID, userId)
//        Log.d("Content Value", contentValues.toString())
        if (movie.adult) {
            contentValues.put(MOVIE_ADULT, 0)
        } else {
            contentValues.put(MOVIE_ADULT, 1)
        }
        contentValues.put(MOVIE_FAVORITE, 0)
        val recordCount = db.insert(MOVIE_TABLE, null, contentValues)
        db.close()
        return recordCount.toInt()
    }

    //Function to add the genres taken from tmdb to the database
    fun addMovieGenres(movieId: Int, genreIds: List<String>) {
        val db = this.writableDatabase
        genreIds.forEach { genreId ->
            val contentValues = ContentValues()
            contentValues.put(MOVIE_ID, movieId)
            contentValues.put(GENRE_ID, genreId)
            db.insert(GENRE_TABLE, null, contentValues)
        }
        db.close()
    }


    // Method to get list of movies from the database
    fun getListMovie(userId: String): ArrayList<Movie> {
        val listMovie: ArrayList<Movie> = ArrayList()
        val selectQuery = "SELECT * FROM $MOVIE_TABLE where $USER_ID = ?"
        val db = this.readableDatabase
        val cursor: Cursor
        var movie: Movie
        try {
            cursor = db.rawQuery(selectQuery, arrayOf(userId))
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        // Iterate through cursor and add movies to list
        if (cursor.moveToFirst()) {
            do {
                val movieId = cursor.getInt(0)
                val genreIds = getGenreIdsForMovie(movieId)
                movie = Movie(
                    movieId,
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getInt(6) == 0,
                    genreIds,
                    cursor.getInt(7) == 0,
                    userId = cursor.getString(8)
                )
                listMovie.add(movie)
            } while (cursor.moveToNext())
        }
        return listMovie
    }

    private fun getGenreIdsForMovie(movieId: Int): List<String> {
        val selectQuery = "SELECT $GENRE_ID FROM $GENRE_TABLE WHERE $MOVIE_ID = ?"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(movieId.toString()))
        val genreIds = ArrayList<String>()
        if (cursor.moveToFirst()) {
            do {
                genreIds.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        return genreIds
    }


    fun addReminder(movie: Movie, userId: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(MOVIE_ID, movie.id)
        contentValues.put(MOVIE_TITLE, movie.title)
        contentValues.put(MOVIE_OVERVIEW, movie.overview)
        contentValues.put(MOVIE_RATING, movie.voteAverage)
        contentValues.put(MOVIE_DATE, movie.releaseDate)
        contentValues.put(MOVIE_IMAGE_POSTER, movie.posterPath)
        contentValues.put(USER_ID, userId)
        if (movie.adult) {
            contentValues.put(MOVIE_ADULT, 0)
        } else {
            contentValues.put(MOVIE_ADULT, 1)
        }
        if (movie.isFavorite)
            contentValues.put(MOVIE_FAVORITE, 0)
        else
            contentValues.put(MOVIE_FAVORITE, 1)
        contentValues.put(REMINDER_TIME, movie.reminderTime)
        contentValues.put(REMINDER_TIME_DISPLAY, movie.reminderTimeDisplay)
        val recordCount = db.insert(REMINDER_TABLE, null, contentValues)
        db.close()
        return recordCount.toInt()
    }

    fun deleteReminderByMovieId(movieId: Int, userId: String): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(MOVIE_ID, movieId)
        contentValues.put(USER_ID, userId)
        val recordCount =
            db.delete(REMINDER_TABLE, "$MOVIE_ID = ? AND $USER_ID = ?", arrayOf(movieId.toString(), userId))
        db.close()
        return recordCount
    }

    fun updateReminder(movie: Movie): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(REMINDER_TIME, movie.reminderTime)
        contentValues.put(REMINDER_TIME_DISPLAY, movie.reminderTimeDisplay)
        val recordCount =
            db.update(REMINDER_TABLE, contentValues, "$MOVIE_ID = ?", arrayOf(movie.id.toString()))
        db.close()
        return recordCount
    }

    fun deleteMovie(id: Int): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(MOVIE_ID, id)

        //Delete from the Movie Table
        val recordCount = db.delete(MOVIE_TABLE, "$MOVIE_ID = $id", null)

        //Delete the movie from the genre table
        db.delete(GENRE_TABLE, "$MOVIE_ID = $id", null)
        db.close()
        return recordCount
    }

    fun getListReminder(userId: String): ArrayList<Movie> {
        val listMovie: ArrayList<Movie> = ArrayList()
        val selectQuery =
            "SELECT * FROM $REMINDER_TABLE where $USER_ID = ?"
        val db = this.readableDatabase
        var movie: Movie

        val cursor: Cursor

        try {
            cursor = db.rawQuery(selectQuery, arrayOf(userId))
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        if (cursor.moveToFirst()) {
            do {
                val movieId = cursor.getInt(0)
                val genreIds = getGenreIdsForMovie(movieId)
                movie = Movie(
                    movieId,
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getInt(6) == 0,
                    genreIds = genreIds,
                    isFavorite = cursor.getInt(7) == 0,
                    reminderTime = cursor.getString(8),
                    reminderTimeDisplay = cursor.getString(9),
                    userId = cursor.getString(10)
                )
                listMovie.add(movie)
            } while (cursor.moveToNext())
        }
        return listMovie
    }

    fun getReminderByMovieId(movieId: Int, userId: String): ArrayList<Movie> {
        val movieReminderList: ArrayList<Movie> = ArrayList()
        val selectQuery =
            "SELECT * FROM $REMINDER_TABLE WHERE $MOVIE_ID = $movieId AND $USER_ID = ?"
        val db = this.readableDatabase
        val cursor: Cursor
        var movie: Movie
        try {
            cursor = db.rawQuery(selectQuery, arrayOf(userId))
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return movieReminderList
        }
        if (cursor.moveToFirst()) {
            do {
                val genreIds = getGenreIdsForMovie(cursor.getInt(0))
                movie = Movie(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getInt(6) == 0,
                    genreIds,
                    cursor.getInt(7) == 0,
                    cursor.getString(8),
                    cursor.getString(9),
                    userId = cursor.getString(10)
                )
                movieReminderList.add(movie)
            } while (cursor.moveToNext())
        }
        return movieReminderList
    }
}