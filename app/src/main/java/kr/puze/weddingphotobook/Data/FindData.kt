package kr.puze.weddingphotobook.Data

import android.os.Parcel
import android.os.Parcelable

data class FindData(var path: String?, var isEPUB: Boolean) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString(),
        1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(path)
        writeInt((if (isEPUB) 1 else 0))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FindData> = object : Parcelable.Creator<FindData> {
            override fun createFromParcel(source: Parcel): FindData = FindData(source)
            override fun newArray(size: Int): Array<FindData?> = arrayOfNulls(size)
        }
    }
}