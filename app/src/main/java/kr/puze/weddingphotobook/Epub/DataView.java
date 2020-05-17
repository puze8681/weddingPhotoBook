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

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import kr.puze.weddingphotobook.R;

//Panel specialized in visualizing HTML-data
public class DataView extends SplitPanel {
	protected WebView view;
	protected String data;
	
	@Override
	public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState)	{
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.activity_data_view, container, false);
		return v;
	}
	
	@Override
    public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		view = (WebView) getView().findViewById(R.id.Viewport);
		WebSettings webSettings = view.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setSupportZoom(true);
		view.setWebViewClient(new MyWebClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				try {
					navigator.setBookPage(url, index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_LoadPage));
				}
				return true;
			}
		});
		
		loadData(data);
	}
	
	public void loadData(String source)
	{
		data=source;
		
		if(created)
			view.loadData(data, getActivity().getApplicationContext().getResources().getString(R.string.textOrHTML), null);
	}

	@Override
	public void saveState(Editor editor)
	{
		super.saveState(editor);
		editor.putString("data"+index, data);
	}
	
	@Override
	public void loadState(SharedPreferences preferences)
	{
		super.loadState(preferences);
		loadData(preferences.getString("data"+index, ""));
	}

	//여기 아래부턴 내가 추가함
	private class MyWebClient extends WebViewClient
	{
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {

			super.onPageStarted(view, url, favicon);
		}
		@RequiresApi(api = Build.VERSION_CODES.M)
		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);

			final WebView myWebView = (WebView) view;


			String varMySheet = "var mySheet = document.styleSheets[0];";

			String addCSSRule = "function addCSSRule(selector, newRule) {"
					+ "ruleIndex = mySheet.cssRules.length;"
					+ "mySheet.insertRule(selector + '{' + newRule + ';}', ruleIndex);"

					+ "}";

			String insertRule1 = "addCSSRule('html', 'padding: 0px; height: "
					+ (myWebView.getMeasuredHeight()/getContext().getResources().getDisplayMetrics().density )
					+ "px; -webkit-column-gap: 0px; -webkit-column-width: "
					+ myWebView.getMeasuredWidth() + "px;')";
			myWebView.loadUrl("javascript:" + varMySheet);
			myWebView.loadUrl("javascript:" + addCSSRule);
			myWebView.loadUrl("javascript:" + insertRule1);
		}
	}
}
