package kr.puze.weddingphotobook.Data

import android.os.Parcel
import android.os.Parcelable
import nl.siegmann.epublib.domain.Book

data class BookData(var path: String?, var book: Book?) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString(),
        source.readSerializable() as Book
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(path)
        writeSerializable(book)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BookData> = object : Parcelable.Creator<BookData> {
            override fun createFromParcel(source: Parcel): BookData = BookData(source)
            override fun newArray(size: Int): Array<BookData?> = arrayOfNulls(size)
        }
    }
}