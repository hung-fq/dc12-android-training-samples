package com.example.media_songs

import java.net.URL

class SongInfo {
    var Title: String? = null

    var AuthorName: String? = null

    var Url: String? = null

    constructor(title: String, author: String, url: String) {
        this.Title = title
        this.AuthorName = author
        this.Url = url.toString()
    }

/*    fun getTitle(): String{
        return Title
    }

    fun getAuthor(): String {
        return AuthorName
    }

    fun getUrl(): URL{
        return Url
    }

    fun setTitle(title: String){
        Title = title
    }

    fun setAuthor(author: String){
        AuthorName = author
    }

    fun setUrl(url: URL){
        Url = url
    }*/
}