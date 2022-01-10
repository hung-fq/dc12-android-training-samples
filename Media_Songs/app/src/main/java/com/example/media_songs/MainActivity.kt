package com.example.media_songs

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    var listSongs = ArrayList<SongInfo>()
    var adapter: MySongAdapter? = null
    var mp: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(
            this,
            "Danh sách nhạc đang khởi động! Vui lòng chờ trong giây lát",
            Toast.LENGTH_LONG
        ).show()
        //load list songs
        LoadURLOnline()

        //check users permission
        //CheckUserPermision()

        //load list in View
        adapter = MySongAdapter(listSongs)
        findViewById<ListView>(R.id.lsListSong).adapter = adapter

        var myTracking = MySongTrack()
        myTracking.start()
    }

    fun LoadURLOnline() {
        listSongs.add(
            SongInfo(
                "001",
                "Amed1",
                "https://drive.google.com/file/d/1AMXDOMTOv3ULn8Egym85jb3O00n_vzgP/view"
            )
        )
        listSongs.add(
            SongInfo(
                "002",
                "Amed2",
                "http://server6.mp3quran.net/thubti/002.mp3"
            ),
        )
        listSongs.add(
            SongInfo(
                "003",
                "Amed3",
                "http://server6.mp3quran.net/thubti/003.mp3"
            )
        )
        listSongs.add(
            SongInfo(
                "004",
                "Amed4",
                "http://server6.mp3quran.net/thubti/004.mp3"
            )
        )
    }

    inner class MySongAdapter : BaseAdapter {
        var myListSong = ArrayList<SongInfo>()

        constructor(myListSong: ArrayList<SongInfo>) : super() {
            this.myListSong = myListSong
        }

        override fun getCount(): Int {
            return this.myListSong.size
        }

        override fun getItem(p0: Int): Any {
            return this.myListSong[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val Song = this.myListSong[p0]

            val myView = layoutInflater.inflate(R.layout.song_ticket, null)
            myView.findViewById<TextView>(R.id.tvSongName).text = Song.AuthorName
            myView.findViewById<TextView>(R.id.tvAuthor).text = Song.Title
//            Log.d("AAA", Song.Title.toString())

            myView.findViewById<Button>(R.id.btnPlay).setOnClickListener(
                View.OnClickListener {
                    //Stop play song
                    if (myView.findViewById<Button>(R.id.btnPlay).text.equals("Stop")) {
//                        Log.d("AAA", "Stop")
                        mp!!.stop()
                        myView.findViewById<Button>(R.id.btnPlay).text = "Start"
                    } else {
                        //ready play song
                        mp = MediaPlayer()
                        Toast.makeText(this@MainActivity, "Chuan bi phat", Toast.LENGTH_LONG)
//                        Log.d("AAA", "Khoi động")
                        try {
                            mp!!.setDataSource(Song.Url)
                            mp!!.prepare()
                            mp!!.start()
                            myView.findViewById<Button>(R.id.btnPlay).text = "Stop"
                            myView.findViewById<SeekBar>(R.id.sbProgress).max = mp!!.duration
                        } catch (e: Exception) {
                            Log.d("AAA", e.toString())
                        }
                    }
                }
            )
            return myView
        }
    }

    inner class MySongTrack() : Thread() {
        override fun run() {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    Log.d("ABC", e.toString())
                }
                runOnUiThread {
                    if (mp != null) {
                        findViewById<SeekBar>(R.id.sbProgress).progress = mp!!.currentPosition
                    }
                }
            }
        }
    }

    //get access to location permission
    private val REQUEST_CODE_ASK_PERMISSIONS = 123
    fun CheckUserPermision() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_ASK_PERMISSIONS
                )
                return
            }
        }
        LoadSong()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LoadSong()
            } else {
                Toast.makeText(this, "deny", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @SuppressLint("Range")
    fun LoadSong() {
        val allSongURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!=0"
        val cursor = contentResolver.query(allSongURI, null, selection, null, null)
        if (cursor != null) {
            Log.d("AZ", cursor.toString())
            if (cursor!!.moveToFirst()) {
                do {
                    val songURL =
                        cursor!!.getString(cursor!!.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val songAuthor =
                        cursor!!.getString(cursor!!.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    val songName =
                        cursor!!.getString(cursor!!.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                    System.out.println(songURL)
                    listSongs.add(SongInfo(songName, songAuthor, songURL))

                } while (cursor!!.moveToFirst())
            }
            cursor!!.close()
            adapter = MySongAdapter(listSongs)
            findViewById<ListView>(R.id.lsListSong).adapter = adapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Cua so da duoc dong lai!", Toast.LENGTH_LONG).show()
    }
}