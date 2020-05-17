/*
The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package kr.puze.weddingphotobook.Epub;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import kr.puze.weddingphotobook.R;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.BookProcessor;
import nl.siegmann.epublib.epub.EpubProcessorSupport;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.PackageDocumentReader;
import nl.siegmann.epublib.service.MediatypeService;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

public class EpubManipulator {
	private Book book;
	private int currentSpineElementIndex;
	private String currentPage;
	private String[] spineElementPaths;
	// NOTE: currently, counting the number of XHTML pages
	private int pageCount;
	private int pageLength;
	private Bitmap coverImage;
	private int currentLanguage;
	private List<String> availableLanguages;
	// tells whether a page has a translation available
	private List<Boolean> translations;
	private String decompressedFolder;
	private String pathOPF;
	private static Context context;
	private static String location = Environment.getExternalStorageDirectory()
			+ "/epubtemp/";

	private String fileName;
	FileInputStream fs;
	private String actualCSS = "";
	private String[][] audio;

	// book from fileName
	public EpubManipulator(String fileName, String destFolder,
			Context theContext) throws Exception {

		List<String> spineElements;
		List<SpineReference> spineList;

		if (context == null) {
			context = theContext;
		}

		this.fs = new FileInputStream(fileName);
		this.book = readEpub(fs);
		this.pageLength = book.getTableOfContents().size();
		this.coverImage = BitmapFactory.decodeStream(this.book.getCoverImage().getInputStream());
		Log.i("epublib", "Coverimage is " + coverImage.getWidth() + " by " + coverImage.getHeight() + " pixels");
		Log.i("epublib", "getTableOfContents is " + book.getTableOfContents());
		this.fileName = fileName;
		this.decompressedFolder = destFolder;

		Spine spine = book.getSpine();
		spineList = spine.getSpineReferences();

		this.currentSpineElementIndex = 0;
		this.currentLanguage = 0;

		spineElements = new ArrayList<String>();
		pages(spineList, spineElements);
		this.pageCount = spineElements.size();

		this.spineElementPaths = new String[spineElements.size()];

		unzip(fileName, location + decompressedFolder);

		pathOPF = getPathOPF(location + decompressedFolder);

		for (int i = 0; i < spineElements.size(); ++i) {
			// TODO: is there a robust path joiner in the java libs?
			this.spineElementPaths[i] = "file://" + location
					+ decompressedFolder + "/" + pathOPF + "/"
					+ spineElements.get(i);
		}

		if (spineElements.size() > 0) {
			goToPage(0);
		}
		createTocFile();
	}

	// book from already decompressed folder
	public EpubManipulator(String fileName, String folder, int spineIndex,
			int language, Context theContext) throws Exception {
		List<String> spineElements;
		List<SpineReference> spineList;

		if (context == null) {
			context = theContext;
		}

		this.fs = new FileInputStream(fileName);
		this.book = readEpub(fs);
		this.fileName = fileName;
		this.decompressedFolder = folder;

		Spine spine = book.getSpine();
		spineList = spine.getSpineReferences();
		this.currentSpineElementIndex = spineIndex;
		this.currentLanguage = language;
		spineElements = new ArrayList<String>();
		pages(spineList, spineElements);
		this.pageCount = spineElements.size();
		this.spineElementPaths = new String[spineElements.size()];

		pathOPF = getPathOPF(location + folder);

		for (int i = 0; i < spineElements.size(); ++i) {
			// TODO: is there a robust path joiner in the java libs?
			this.spineElementPaths[i] = "file://" + location + folder + "/"
					+ pathOPF + "/" + spineElements.get(i);
		}
		goToPage(spineIndex);
	}

	// set language from index
	public void setLanguage(int lang) throws Exception {
		if ((lang >= 0) && (lang <= this.availableLanguages.size())) {
			this.currentLanguage = lang;
		}
		goToPage(this.currentSpineElementIndex);
	}

	// set language from an identifier string
	public void setLanguage(String lang) throws Exception {
		int i = 0;
		while ((i < this.availableLanguages.size())
				&& (!(this.availableLanguages.get(i).equals(lang)))) {
			i++;
		}
		setLanguage(i);
	}

	// TODO: lookup table of language names from language codes
	public String[] getLanguages() {
		String[] lang = new String[availableLanguages.size()];
		for (int i = 0; i < availableLanguages.size(); i++) {
			lang[i] = availableLanguages.get(i);
		}
		return lang;
	}

	// create parallel text mapping
	private void pages(List<SpineReference> spineList, List<String> pages) {
		int langIndex;
		String lang;
		String actualPage;

		this.translations = new ArrayList<Boolean>();
		this.availableLanguages = new ArrayList<String>();

		for (int i = 0; i < spineList.size(); ++i) {
			actualPage = (spineList.get(i)).getResource().getHref();
			lang = getPageLanguage(actualPage);
			if (lang != "") {
				// parallel text available
				langIndex = languageIndexFromID(lang);

				if (langIndex == this.availableLanguages.size())
					this.availableLanguages.add(lang);

				if (langIndex == 0) {
					this.translations.add(true);
					pages.add(actualPage);
				}
			} else {
				// parallel text NOT available
				this.translations.add(false);
				pages.add(actualPage);
			}
		}
	}

	// language index from language string (id)
	private int languageIndexFromID(String id) {
		int i = 0;
		while ((i < availableLanguages.size())
				&& (!(availableLanguages.get(i).equals(id)))) {
			i++;
		}
		return i;
	}

	// TODO: better parsing
	private static String getPathOPF(String unzipDir) throws IOException {
		String pathOPF = "";
		// get the OPF path, directly from container.xml
		BufferedReader br = new BufferedReader(new FileReader(unzipDir
				+ "/META-INF/container.xml"));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.indexOf(getS(R.string.full_path)) > -1) {
				int start = line.indexOf(getS(R.string.full_path));
				int start2 = line.indexOf("\"", start);
				int stop2 = line.indexOf("\"", start2 + 1);
				if (start2 > -1 && stop2 > start2) {
					pathOPF = line.substring(start2 + 1, stop2).trim();
					break;
				}
			}
		}
		br.close();

		// in case the OPF file is in the root directory
		if (!pathOPF.contains("/"))
			pathOPF = "";

		// remove the OPF file name and the preceding '/'
		int last = pathOPF.lastIndexOf('/');
		if (last > -1) {
			pathOPF = pathOPF.substring(0, last);
		}

		return pathOPF;
	}

	// TODO: more efficient unzipping
	public void unzip(String inputZip, String destinationDirectory)
			throws IOException {
		int BUFFER = 2048;
		List zipFiles = new ArrayList();
		File sourceZipFile = new File(inputZip);
		File unzipDestinationDirectory = new File(destinationDirectory);
		unzipDestinationDirectory.mkdir();

		ZipFile zipFile;
		zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
		Enumeration zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {

			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(unzipDestinationDirectory, currentEntry);

			if (currentEntry.endsWith(getS(R.string.zip))) {
				zipFiles.add(destFile.getAbsolutePath());
			}

			File destinationParent = destFile.getParentFile();
			destinationParent.mkdirs();

			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(
						zipFile.getInputStream(entry));
				int currentByte;
				// buffer for writing file
				byte data[] = new byte[BUFFER];

				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos,
						BUFFER);

				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();

			}

		}
		zipFile.close();

		for (Iterator iter = zipFiles.iterator(); iter.hasNext();) {
			String zipName = (String) iter.next();
			unzip(zipName,
					destinationDirectory
							+ File.separatorChar
							+ zipName.substring(0,
									zipName.lastIndexOf(getS(R.string.zip))));
		}
	}

	public void closeStream() throws IOException {
		fs.close();
		book = null;
	}

	// close the stream and delete the extraction folder
	public void destroy() throws IOException {
		closeStream();
		File c = new File(location + decompressedFolder);
		deleteDir(c);
	}

	// recursively delete a directory
	private void deleteDir(File f) {
		if (f.isDirectory())
			for (File child : f.listFiles())
				deleteDir(child);
		f.delete();
	}

	// change the decompressedFolder name
	public void changeDirName(String newName) {
		File dir = new File(location + decompressedFolder);
		File newDir = new File(location + newName);
		dir.renameTo(newDir);

		for (int i = 0; i < spineElementPaths.length; ++i)
			// TODO: is there a robust path joiner in the java libs?
			spineElementPaths[i] = spineElementPaths[i].replace("file://"
					+ location + decompressedFolder, "file://" + location
					+ newName);
		decompressedFolder = newName;
		try {
			goToPage(currentSpineElementIndex);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// obtain a page in the current language
	public String goToPage(int page) throws Exception {
		return goToPage(page, this.currentLanguage);
	}

	// obtain a page in the given language
	public String goToPage(int page, int lang) throws Exception {
		String spineElement;
		String extension;
		if (page < 0) {
			page = 0;
		}
		if (page >= this.pageCount) {
			page = this.pageCount - 1;
		}
		this.currentSpineElementIndex = page;

		spineElement = this.spineElementPaths[currentSpineElementIndex];

		// TODO: better parsing
		if (this.translations.get(page)) {
			extension = spineElement.substring(spineElement.lastIndexOf("."));
			spineElement = spineElement.substring(0,
					spineElement.lastIndexOf(this.availableLanguages.get(0)));

			spineElement = spineElement + this.availableLanguages.get(lang)
					+ extension;
		}

		this.currentPage = spineElement;

		audioExtractor(currentPage);

		return spineElement;
	}

	public String goToNextChapter() throws Exception {
		return goToPage(this.currentSpineElementIndex + 1);
	}

	public String goToChapter(int page) throws Exception {
		return goToPage(page);
	}

	public String goToPreviousChapter() throws Exception {
		return goToPage(this.currentSpineElementIndex - 1);
	}

	// create an HTML page with book metadata
	// TODO: style it and escape metadata values
	// TODO: use StringBuilder
	public String metadata() {
		List<String> tmp;
		Metadata metadata = book.getMetadata();
		String html = getS(R.string.htmlBodyTableOpen);

		// Titles
		tmp = metadata.getTitles();
		if (tmp.size() > 0) {
			html += getS(R.string.titlesMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Authors
		List<Author> authors = metadata.getAuthors();
		if (authors.size() > 0) {
			html += getS(R.string.authorsMeta);
			html += "<td>" + authors.get(0).getFirstname() + " "
					+ authors.get(0).getLastname() + "</td></tr>";
			for (int i = 1; i < authors.size(); i++)
				html += "<tr><td></td><td>" + authors.get(i).getFirstname()
						+ " " + authors.get(i).getLastname() + "</td></tr>";
		}

		// Contributors
		authors = metadata.getContributors();
		if (authors.size() > 0) {
			html += getS(R.string.contributorsMeta);
			html += "<td>" + authors.get(0).getFirstname() + " "
					+ authors.get(0).getLastname() + "</td></tr>";
			for (int i = 1; i < authors.size(); i++) {
				html += "<tr><td></td><td>" + authors.get(i).getFirstname()
						+ " " + authors.get(i).getLastname() + "</td></tr>";
			}
		}

		// TODO: extend lib to get multiple languages?
		// Language
		html += getS(R.string.languageMeta) + metadata.getLanguage()
				+ "</td></tr>";

		// Publishers
		tmp = metadata.getPublishers();
		if (tmp.size() > 0) {
			html += getS(R.string.publishersMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Types
		tmp = metadata.getTypes();
		if (tmp.size() > 0) {
			html += getS(R.string.typesMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Descriptions
		tmp = metadata.getDescriptions();
		if (tmp.size() > 0) {
			html += getS(R.string.descriptionsMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		// Rights
		tmp = metadata.getRights();
		if (tmp.size() > 0) {
			html += getS(R.string.rightsMeta);
			html += "<td>" + tmp.get(0) + "</td></tr>";
			for (int i = 1; i < tmp.size(); i++)
				html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
		}

		html += getS(R.string.tablebodyhtmlClose);
		return html;
	}

	public String r_createTocFile(TOCReference e) {

		String childrenPath = "file://" + location + decompressedFolder + "/"
				+ pathOPF + "/" + e.getCompleteHref();

		String html = "<ul><li>" + "<a href=\"" + childrenPath + "\">"
				+ e.getTitle() + "</a>" + "</li></ul>";

		List<TOCReference> children = e.getChildren();

		for (int j = 0; j < children.size(); j++)
			html += r_createTocFile(children.get(j));

		return html;
	}

	// Create an html file, which contain the TOC, in the EPUB folder
	public void createTocFile() {
		List<TOCReference> tmp;
		TableOfContents toc = book.getTableOfContents();
		String html = "<html><body><ul>";

		tmp = toc.getTocReferences();

		if (tmp.size() > 0) {
			html += getS(R.string.tocReference);
			for (int i = 0; i < tmp.size(); i++) {
				String path = "file://" + location + decompressedFolder + "/"
						+ pathOPF + "/" + tmp.get(i).getCompleteHref();

				html += "<li>" + "<a href=\"" + path + "\">"
						+ tmp.get(i).getTitle() + "</a>" + "</li>";

				// pre-order traversal?
				List<TOCReference> children = tmp.get(i).getChildren();

				for (int j = 0; j < children.size(); j++)
					html += r_createTocFile(children.get(j));

			}
		}

		html += getS(R.string.tablebodyhtmlClose);

		// write down the html file
		String filePath = location + decompressedFolder + "/Toc.html";
		try {
			File file = new File(filePath);
			FileWriter fw = new FileWriter(file);
			fw.write(html);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// return the path of the Toc.html file
	public String tableOfContents() {
		return "File://" + location + decompressedFolder + "/Toc.html";
	}

	// determine whether a book has the requested page
	// if so, return its index; return -1 otherwise
	public int getPageIndex(String page) {
		int result = -1;
		String lang;

		lang = getPageLanguage(page);
		if ((this.availableLanguages.size() > 0) && (lang != "")) {
			page = page.substring(0, page.lastIndexOf(lang))
					+ this.availableLanguages.get(0)
					+ page.substring(page.lastIndexOf("."));
		}
		for (int i = 0; i < this.spineElementPaths.length && result == -1; i++) {
			if (page.equals(this.spineElementPaths[i])) {
				result = i;
			}
		}

		return result;
	}

	// set the current page and its language
	public boolean goToPage(String page) {
		int index = getPageIndex(page);
		boolean res = false;
		if (index >= 0) {
			String newLang = getPageLanguage(page);
			try {
				goToPage(index);
				if (newLang != "") {
					setLanguage(newLang);
				}
				res = true;
			} catch (Exception e) {
				res = false;
				Log.e(getS(R.string.error_goToPage), e.getMessage());
			}
		}
		return res;
	}

	// return the language of the page according to the
	// ISO 639-1 naming convention:
	// foo.XX.html where X \in [a-z]
	// or an empty string if language not found
	public String getPageLanguage(String page) {
		String[] tmp = page.split("\\.");
		// Language XY is present if the string format is "pagename.XY.xhtml",
		// where XY are 2 non-numeric characters that identify the language
		if (tmp.length > 2) {
			String secondFromLastItem = tmp[tmp.length - 2];
			if (secondFromLastItem.matches("[a-z][a-z]")) {
				return secondFromLastItem;
			}
		}
		return "";
	}

	// TODO work in progress
	public void addCSS(String[] settings) {
		// CSS
		String css = "<style type=\"text/css\">\n";

		if (!settings[0].isEmpty()) {
			css = css + "body{color:" + settings[0] + ";}";
			css = css + "a:link{color:" + settings[0] + ";}";
		}

		if (!settings[1].isEmpty())
			css = css + "body {background-color:" + settings[1] + ";}";

		if (!settings[2].isEmpty())
			css = css + "p{font-family:" + settings[2] + ";}";

		if (!settings[3].isEmpty())
			css = css + "p{\n\tfont-size:" + settings[3] + "%\n}\n";

		if (!settings[4].isEmpty())
			css = css + "p{line-height:" + settings[4] + "em;}";

		if (!settings[5].isEmpty())
			css = css + "p{text-align:" + settings[5] + ";}";

		if (!settings[6].isEmpty())
			css = css + "body{margin-left:" + settings[6] + "%;}";

		if (!settings[7].isEmpty())
			css = css + "body{margin-right:" + settings[7] + "%;}";

		css = css + "</style>";

		for (int i = 0; i < spineElementPaths.length; i++) {
			String path = spineElementPaths[i].replace("file:///", "");
			String source = readPage(path);

			source = source.replace(actualCSS + "</head>", css + "</head>");

			writePage(path, source);
		}
		actualCSS = css;

	}

	// change from relative path (that begin with ./ or ../) to absolute path
	private void adjustAudioLinks() {
		for (int i = 0; i < audio.length; i++)
			for (int j = 0; j < audio[i].length; j++) {
				if (audio[i][j].startsWith("./"))
					audio[i][j] = currentPage.substring(0,
							currentPage.lastIndexOf("/"))
							+ audio[i][j].substring(1);

				if (audio[i][j].startsWith("../")) {
					String temp = currentPage.substring(0,
							currentPage.lastIndexOf("/"));
					audio[i][j] = temp.substring(0, temp.lastIndexOf("/"))
							+ audio[i][j].substring(2);
				}
			}
	}

	// Extract all the src field of an audio tag
	private ArrayList<String> getAudioSources(String audioTag) {
		ArrayList<String> srcs = new ArrayList<String>();
		Pattern p = Pattern.compile("src=\"[^\"]*\"");
		Matcher m = p.matcher(audioTag);
		while (m.find())
			srcs.add(m.group().replace("src=\"", "").replace("\"", ""));

		return srcs;
	}

	// Extract all audio tags from an xhtml page
	private ArrayList<String> getAudioTags(String page) {
		ArrayList<String> res = new ArrayList<String>();

		String source = readPage(page);

		Pattern p = Pattern.compile("<audio(?s).*?</audio>|<audio(?s).*?/>");
		Matcher m = p.matcher(source);
		while (m.find())
			res.add(m.group(0));

		return res;
	}

	private void audioExtractor(String page) {
		ArrayList<String> tags = getAudioTags(page.replace("file:///", ""));
		ArrayList<String> srcs;
		audio = new String[tags.size()][];

		for (int i = 0; i < tags.size(); i++) {
			srcs = getAudioSources(tags.get(i));
			audio[i] = new String[srcs.size()];
			for (int j = 0; j < srcs.size(); j++)
				audio[i][j] = srcs.get(j);
		}
		adjustAudioLinks();
	}

	public String[][] getAudio() {
		return audio;
	}

	/*
	 * TODO don't work properly, forse non necessario public boolean
	 * deleteCSS(String path) { path = path.replace("file:///", ""); String
	 * source = readPage(path); source =
	 * source.replace("<style type=\"text/css\">.</style></head>", "</head>");
	 * return writePage(path, source); }
	 */

	// TODO work in progress
	private String readPage(String path) {
		try {
			FileInputStream input = new FileInputStream(path);
			byte[] fileData = new byte[input.available()];

			input.read(fileData);
			input.close();

			String xhtml = new String(fileData);
			return xhtml;
		} catch (IOException e) {
			return "";
		}
	}

	// TODO work in progress
	private boolean writePage(String path, String xhtml) {
		try {
			File file = new File(path);
			FileWriter fw = new FileWriter(file);
			fw.write(xhtml);
			fw.flush();
			fw.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public int getCurrentSpineElementIndex() {
		return currentSpineElementIndex;
	}

	public String getSpineElementPath(int elementIndex) {
		return spineElementPaths[elementIndex];
	}

	public String getCurrentPageURL() {
		return currentPage;
	}

	public int getCurrentLanguage() {
		return currentLanguage;
	}

	public Bitmap getCoverImage() { return coverImage;}

	public int getPageCount() { return pageCount;}

	public int getPageLength() { return pageLength;}

	public String getFileName() {
		return fileName;
	}

	public String getDecompressedFolder() {
		return decompressedFolder;
	}

	public static String getS(int id) {
		return context.getResources().getString(id);
	}

	public Book readEpub(FileInputStream fileInput) throws IOException {
		String encoding = "UTF-8";
		Book result = new Book();
		Resources resources = this.readResources(fileInput, encoding);
		this.handleMimeType(result, resources);
		String packageResourceHref = this.getPackageResourceHref(resources);
		Resource packageResource = this.processPackageResource(packageResourceHref, result, resources);
		result.setOpfResource(packageResource);
		Resource ncxResource = this.processNcxResource(packageResource, result);
		result.setNcxResource(ncxResource);
		result = this.postProcessBook(result);
		return result;
	}

	private Resources readResources(FileInputStream input, String defaultHtmlEncoding) throws IOException {
		ZipInputStream in =  new ZipInputStream(input);
		Resources result = new Resources();

		for(ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
			if (!zipEntry.isDirectory()) {
				Resource resource = createResource(zipEntry, in);
				if (resource.getMediaType() == MediatypeService.XHTML) {
					resource.setInputEncoding(defaultHtmlEncoding);
				}

				result.add(resource);
			}
		}

		return result;
	}

	private void handleMimeType(Book result, Resources resources) {
		resources.remove("mimetype");
	}

	private String getPackageResourceHref(Resources resources) {
		String defaultResult = "OEBPS/content.opf";
		String result = defaultResult;
		Resource containerResource = resources.remove("META-INF/container.xml");
		if (containerResource == null) {
			return defaultResult;
		} else {
			try {
				Document document = getAsDocument(containerResource);
				Element rootFileElement = (Element)((Element)document.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
				result = rootFileElement.getAttribute("full-path");
			} catch (Exception var7) {
				Log.d("getPackageResourceHref", var7.getMessage(), var7);
			}

			if (isBlank(result)) {
				result = defaultResult;
			}

			return result;
		}
	}

	private Resource processPackageResource(String packageResourceHref, Book book, Resources resources) {
		Resource packageResource = resources.remove(packageResourceHref);

		try {
			PackageDocumentReader.read(packageResource, null, book, resources);
		} catch (Exception var6) {
			Log.d("processPackageResource", var6.getMessage(), var6);
		}

		return packageResource;
	}

	private Resource processNcxResource(Resource packageResource, Book book) {
		return read(book);
	}


	public static Resource read(Book book) {
		Resource ncxResource = null;
		if (book.getSpine().getTocResource() == null) {
			Log.d("read", "Book does not contain a table of contents file");
			return null;
		} else {
			try {
				ncxResource = book.getSpine().getTocResource();
				if (ncxResource == null) {
					return null;
				}else{
					Log.d("read ncxR", ncxResource.toString());
					Document ncxDocument = getAsDocument(ncxResource);
					Element navMapElement = getFirstElementByTagNameNS(ncxDocument.getDocumentElement(), "http://www.daisy.org/z3986/2005/ncx/", "navMap");
					if(navMapElement != null){
						Log.d("read navM", navMapElement.toString());
						if(navMapElement.getChildNodes() != null){
							TableOfContents tableOfContents = new TableOfContents(readTOCReferences(navMapElement.getChildNodes(), book));
							book.setTableOfContents(tableOfContents);
						}
					}
				}
			} catch (Exception var6) {
				Log.d("read", var6.getMessage(), var6);
			}

			return ncxResource;
		}
	}

	public static Element getFirstElementByTagNameNS(Element parentElement, String namespace, String tagName) {
		NodeList nodes = parentElement.getElementsByTagNameNS(namespace, tagName);
		return nodes.getLength() == 0 ? null : (Element)nodes.item(0);
	}

	private static List<TOCReference> readTOCReferences(NodeList navpoints, Book book) {
		if (navpoints == null) {
			return new ArrayList();
		} else {
			List<TOCReference> result = new ArrayList(navpoints.getLength());

			for(int i = 0; i < navpoints.getLength(); ++i) {
				Node node = navpoints.item(i);
				if (node.getNodeType() == 1 && node.getLocalName().equals("navPoint")) {
					TOCReference tocReference = readTOCReference((Element)node, book);
					result.add(tocReference);
				}
			}

			return result;
		}
	}

	private static TOCReference readTOCReference(Element navpointElement, Book book) {
		String label = readNavLabel(navpointElement);
		String reference = readNavReference(navpointElement);
		String href = substringBefore(reference, '#');
		String fragmentId = substringAfter(reference, '#');
		Resource resource = book.getResources().getByHref(href);
		if (resource == null) {
			Log.d("readTOCReference", "Resource with href " + href + " in NCX document not found");
		}

		TOCReference result = new TOCReference(label, resource, fragmentId);
		readTOCReferences(navpointElement.getChildNodes(), book);
		result.setChildren(readTOCReferences(navpointElement.getChildNodes(), book));
		return result;
	}

	public static String substringBefore(String text, char separator) {
		if (isEmpty(text)) {
			return text;
		} else {
			int sepPos = text.indexOf(separator);
			return sepPos < 0 ? text : text.substring(0, sepPos);
		}
	}

	public static boolean isBlank(String text) {
		if (isEmpty(text)) {
			return true;
		} else {
			for(int i = 0; i < text.length(); ++i) {
				if (!Character.isWhitespace(text.charAt(i))) {
					return false;
				}
			}

			return true;
		}
	}

	public static boolean isEmpty(String text) {
		return text == null || text.length() == 0;
	}

	private static String readNavLabel(Element navpointElement) {
		Element navLabel = getFirstElementByTagNameNS(navpointElement, "http://www.daisy.org/z3986/2005/ncx/", "navLabel");
		return getTextChildrenContent(getFirstElementByTagNameNS(navLabel, "http://www.daisy.org/z3986/2005/ncx/", "text"));
	}

	public static String getTextChildrenContent(Element parentElement) {
		if (parentElement == null) {
			return null;
		} else {
			StringBuilder result = new StringBuilder();
			NodeList childNodes = parentElement.getChildNodes();

			for(int i = 0; i < childNodes.getLength(); ++i) {
				Node node = childNodes.item(i);
				if (node != null && node.getNodeType() == 3) {
					result.append(((Text)node).getData());
				}
			}

			return result.toString().trim();
		}
	}

	private static String readNavReference(Element navpointElement) {
		Element contentElement = getFirstElementByTagNameNS(navpointElement, "http://www.daisy.org/z3986/2005/ncx/", "content");
		String result = getAttribute(contentElement, "http://www.daisy.org/z3986/2005/ncx/", "src");

		try {
			result = URLDecoder.decode(result, "UTF-8");
		} catch (UnsupportedEncodingException var4) {
			Log.d("readNavReference", var4.getMessage(), var4);
		}

		return result;
	}

	public static String getAttribute(Element element, String namespace, String attribute) {
		String result = element.getAttributeNS(namespace, attribute);
		if (isEmpty(result)) {
			result = element.getAttribute(attribute);
		}

		return result;
	}

	private Book postProcessBook(Book book) {
		BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;
		if (bookProcessor != null) {
			book = bookProcessor.processBook(book);
		}

		return book;
	}

	public static String substringAfter(String text, char c) {
		if (isEmpty(text)) {
			return text;
		} else {
			int cPos = text.indexOf(c);
			return cPos < 0 ? "" : text.substring(cPos + 1);
		}
	}

	public static Resource createResource(ZipEntry zipEntry, ZipInputStream zipInputStream) throws IOException {
		Log.d("Epub createResource", zipEntry.getName());
		return new Resource(zipInputStream, zipEntry.getName());
	}

	public static Document getAsDocument(Resource resource) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
		return getAsDocument(resource, EpubProcessorSupport.createDocumentBuilder());
	}

	public static Document getAsDocument(Resource resource, DocumentBuilder documentBuilder) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
		InputSource inputSource = getInputSource(resource);
		if (inputSource == null) {
			return null;
		} else {
			Document result = documentBuilder.parse(inputSource);
			return result;
		}
	}

	public static InputSource getInputSource(Resource resource) throws IOException {
		if (resource == null) {
			return null;
		} else {
			Reader reader = resource.getReader();
			if (reader == null) {
				return null;
			} else {
				InputSource inputSource = new InputSource(reader);
				return inputSource;
			}
		}
	}

}
