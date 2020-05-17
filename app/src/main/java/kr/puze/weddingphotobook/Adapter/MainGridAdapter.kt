package kr.puze.weddingphotobook.Adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_grid.view.*
import kr.puze.weddingphotobook.R
import kr.puze.weddingphotobook.Utils.PrefUtil

import nl.siegmann.epublib.domain.*
import nl.siegmann.epublib.epub.BookProcessor
import nl.siegmann.epublib.epub.EpubProcessorSupport
import nl.siegmann.epublib.epub.PackageDocumentReader
import nl.siegmann.epublib.service.MediatypeService

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.FileInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.ParserConfigurationException
import kotlin.collections.ArrayList

class MainGridAdapter(private val items: ArrayList<String>, var isOnEdit: Boolean) :
    BaseAdapter() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var view = convertView
        val context = parent.context
        var prefUtil = PrefUtil(context)
        if (view == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item_grid, parent, false)
        }

        var fs = Book()
        var file = FileInputStream(items[position])
        Log.d("LOGTAG,  Grid", items[position] + position)
        try {
            if (file != null) {
                var result = Book()
                val resources: Resources? = this.readResources(file, "UTF-8")
                if (resources != null) {
                    this.handleMimeType(result, resources)
                    val packageResourceHref: String? = this.getPackageResourceHref(resources)
                    if(packageResourceHref != null){
                        val packageResource: Resource? = this.processPackageResource(packageResourceHref, result, resources)
                        if(packageResource != null){
                            result.opfResource = packageResource
                            val ncxResource: Resource? = this.processNcxResource(packageResource, result)
                            if(ncxResource != null){
                                result.ncxResource = ncxResource
                                result = this.postProcessBook(result)!!
                                fs = result
                            }
                        }
                    }
                }

            }
        } catch (e: NullPointerException) {
            Log.d("LOGTAG,  Grid cover", e.toString())
        }

        var coverImage = BitmapFactory.decodeStream(fs.coverImage.inputStream)
        Glide.with(context).load(coverImage).into(view!!.image_item)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.image_item.clipToOutline = true
        }
        Log.d("LOGTAG,  Grid cover", "${coverImage}}")

        var title = fs.title
        var contents = fs.contents
        Log.d("LOGTAG,  Grid title", title)
        Log.d("LOGTAG,  Grid contents", contents.toString())
        view!!.text_title.text = title
        if (isOnEdit) {
            view.image_uncheck.visibility = View.VISIBLE
            view.image_check.visibility = View.INVISIBLE
            view.setOnClickListener {
                if (view.image_uncheck.visibility == View.VISIBLE) {
                    view.image_uncheck.visibility = View.GONE
                    view.image_check.visibility = View.VISIBLE
                    prefUtil.addStringArrayPref("check", items[position]!!)
                } else {
                    view.image_uncheck.visibility = View.VISIBLE
                    view.image_check.visibility = View.GONE
                    prefUtil.deleteStringArrayPref("check", items[position]!!)
                }
            }
        } else {
            view.image_uncheck.visibility = View.GONE
            view.image_check.visibility = View.GONE
        }
        return view
    }

    @Throws(IOException::class)
    private fun readResources(input: FileInputStream, defaultHtmlEncoding: String): Resources? {
        val zipInput = ZipInputStream(input)
        val result = Resources()
        var zipEntry = zipInput.nextEntry
        zipEntry.name
        while (zipEntry != null) {
            if (!zipEntry.isDirectory) {
                val resource = createResource(zipEntry, zipInput)
                if(resource !== null){
                    if (resource.mediaType === MediatypeService.XHTML) {
                        resource.inputEncoding = defaultHtmlEncoding
                    }
                    result.add(resource)
                }
            }
            zipEntry = zipInput.nextEntry
        }
        return result
    }

    private fun handleMimeType(result: Book, resources: Resources) {
        resources.remove("mimetype")
    }

    private fun getPackageResourceHref(resources: Resources): String? {
        val defaultResult = "OEBPS/content.opf"
        var result = defaultResult
        val containerResource =
            resources.remove("META-INF/container.xml")
        return if (containerResource == null) {
            defaultResult
        } else {
            try {
                val document = getAsDocument(containerResource)
                if(document != null){
                    val rootFileElement = (document.documentElement.getElementsByTagName("rootfiles").item(0) as Element).getElementsByTagName(
                        "rootfile"
                    ).item(0) as Element
                    result = rootFileElement.getAttribute("full-path")
                }
            } catch (var7: Exception) {
                Log.d("EPUBREADER", var7.message, var7)
            }
            if (result == null) {
                result = defaultResult
            }
            result
        }
    }

    private fun processPackageResource(packageResourceHref: String, book: Book, resources: Resources): Resource? {
        val packageResource = resources.remove(packageResourceHref)
        try {

            PackageDocumentReader.read(packageResource,  null, book, resources)
        } catch (var6: java.lang.Exception) {
            Log.d("EPUBREADER", var6.message, var6)
        }
        return packageResource
    }

    private fun processNcxResource(packageResource: Resource, book: Book): Resource? {
        return read(book)
    }

    fun read(book: Book): Resource? {
        var ncxResource: Resource? = null
        return if (book.spine.tocResource == null) {
            Log.d("NCXDocument", "Book does not contain a table of contents file")
            ncxResource
        } else {
            try {
                ncxResource = book.spine.tocResource
                if (ncxResource == null) {
                    return ncxResource
                }
                val ncxDocument = getAsDocument(ncxResource)
                if(ncxDocument != null){
                    val navMapElement = getFirstElementByTagNameNS(ncxDocument.documentElement, "http://www.daisy.org/z3986/2005/ncx/", "navMap")
                    val tableOfContents = TableOfContents(
                        readTOCReferences(navMapElement?.childNodes, book)
                    )
                    book.tableOfContents = tableOfContents
                }
            } catch (var6: java.lang.Exception) {
                Log.d("NCXDocument", var6.message, var6)
            }
            ncxResource
        }
    }

    fun getFirstElementByTagNameNS(parentElement: Element, namespace: String?, tagName: String?): Element? {
        val nodes = parentElement.getElementsByTagNameNS(namespace, tagName)
        return if (nodes.length == 0) null else nodes.item(0) as Element
    }

    private fun readTOCReferences(navpoints: NodeList?, book: Book): List<TOCReference?>? {
        return if (navpoints == null) {
            val result: ArrayList<TOCReference?> = ArrayList()
            var resultList = result.toMutableList()
            resultList
        } else {
            val result: ArrayList<TOCReference?> = ArrayList()
            for (i in 0 until navpoints.length) {
                val node = navpoints.item(i)
                if (node.nodeType.toInt() == 1 && node.localName == "navPoint") {
                    val tocReference = readTOCReference(node as Element, book)
                    result.add(tocReference)
                }
            }
            var resultList = result.toMutableList()
            resultList
        }
    }

    private fun readTOCReference(navpointElement: Element, book: Book): TOCReference? {
        val label = readNavLabel(navpointElement)
        val reference = readNavReference(navpointElement)
        val href = substringBefore(reference!!, '#')
        val fragmentId = substringAfter(reference, '#')
        val resource = book.resources.getByHref(href)
        if (resource == null) {
            Log.d("NCXDocument", "Resource with href $href in NCX document not found")
        }
        val result = TOCReference(label, resource, fragmentId)
        readTOCReferences(navpointElement.childNodes, book)
        result.children = readTOCReferences(navpointElement.childNodes, book)
        return result
    }

    fun substringBefore(text: String, separator: Char): String? {
        return if (text == null || text.isEmpty()) {
            text
        } else {
            val sepPos = text.indexOf(separator)
            if (sepPos < 0) text else text.substring(0, sepPos)
        }
    }

    private fun readNavLabel(navpointElement: Element): String? {
        val navLabel = getFirstElementByTagNameNS(navpointElement, "http://www.daisy.org/z3986/2005/ncx/", "navLabel")
        return getTextChildrenContent(getFirstElementByTagNameNS(navLabel!!, "http://www.daisy.org/z3986/2005/ncx/", "text"))
    }

    fun getTextChildrenContent(parentElement: Element?): String? {
        return if (parentElement == null) {
            null
        } else {
            val result = StringBuilder()
            val childNodes = parentElement.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node != null && node.nodeType.toInt() == 3) {
                    result.append((node as Text).data)
                }
            }
            result.toString().trim { it <= ' ' }
        }
    }

    private fun readNavReference(navpointElement: Element): String? {
        val contentElement = getFirstElementByTagNameNS(navpointElement, "http://www.daisy.org/z3986/2005/ncx/", "content")
        var result = getAttribute(contentElement!!, "http://www.daisy.org/z3986/2005/ncx/", "src")
        try {
            result = URLDecoder.decode(result, "UTF-8")
        } catch (var4: UnsupportedEncodingException) {
            Log.d("NCXDocument",var4.message, var4)
        }
        return result
    }

    fun getAttribute(element: Element, namespace: String?, attribute: String?): String? {
        var result = element.getAttributeNS(namespace, attribute)
        if (result == null || result.isEmpty()) {
            result = element.getAttribute(attribute)
        }
        return result
    }

    private fun postProcessBook(book: Book): Book? {
        var bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR
        var book: Book? = book
        if (bookProcessor != null) {
            book = bookProcessor.processBook(book)
        }
        return book
    }

    fun substringAfter(text: String, c: Char): String? {
        return if (text == null || text.isEmpty()) {
            text
        } else {
            val cPos = text.indexOf(c)
            if (cPos < 0) "" else text.substring(cPos + 1)
        }
    }

    @Throws(IOException::class)
    fun createResource(zipEntry: ZipEntry, zipInputStream: ZipInputStream?): Resource? {
        Log.d("createResource", zipEntry.name)
        var name = zipEntry.name
        if(name.contains(".mp3") || name.contains(".mp4")){
            return null
        }else{
            return Resource(zipInputStream, zipEntry.name)
        }
    }

    @Throws(UnsupportedEncodingException::class, SAXException::class, IOException::class, ParserConfigurationException::class)
    fun getAsDocument(resource: Resource?): Document? {
        return getAsDocument(resource, EpubProcessorSupport.createDocumentBuilder())
    }

    @Throws(UnsupportedEncodingException::class, SAXException::class, IOException::class, ParserConfigurationException::class)
    fun getAsDocument(resource: Resource?, documentBuilder: DocumentBuilder): Document? {
        val inputSource = getInputSource(resource)
        return if (inputSource == null) {
            null
        } else {
            documentBuilder.parse(inputSource)
        }
    }

    @Throws(IOException::class)
    fun getInputSource(resource: Resource?): InputSource? {
        return if (resource == null) {
            null
        } else {
            val reader = resource.reader
            reader?.let { InputSource(it) }
        }
    }
}