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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.view.MotionEventCompat;

import kr.puze.weddingphotobook.R;
import kr.puze.weddingphotobook.ThumbnailActivity;

// Panel specialized in visualizing EPUB pages
public class BookView extends SplitPanel {
	public ViewStateEnum state = ViewStateEnum.books;
	protected String viewedPage;
	protected WebView view;
	protected View left;
	protected View right;
	protected RelativeLayout view_bottom;
	protected TextView quit;
	protected TextView thumbnail;
	protected TextView page;
	protected SeekBar seekBar;
	protected float swipeOriginX, swipeOriginY;
	
	@Override
	public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState)	{
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.activity_book_view, container, false);
		Log.d("LOGTAG", "BookView Oncreate");
		return v;
	}
	
	@Override
    public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		left = getView().findViewById(R.id.book_view_left);
		right = getView().findViewById(R.id.book_view_right);
		view_bottom = getView().findViewById(R.id.view_bottom);
		quit = getView().findViewById(R.id.button_quit);
		thumbnail = getView().findViewById(R.id.button_thumbnail);
		page = getView().findViewById(R.id.text_page);
		seekBar = getView().findViewById(R.id.seek_bar);
		view = getView().findViewById(R.id.Viewport);

		// enable JavaScript for cool things to happen!
		view.getSettings().setJavaScriptEnabled(true);
		WebSettings webSettings = view.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setSupportZoom(true);

		left.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					navigator.goToPrevChapter(index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotTurnPage));
				}
			}
		});

		right.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					navigator.goToNextChapter(index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotTurnPage));
				}
			}
		});

		quit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().finish();
			}
		});
		thumbnail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { getActivity().startActivity(new Intent(getActivity(), ThumbnailActivity.class));
			}
		});

		seekBar.setMax(navigator.getBookPage(index));
		seekBar.setProgress(index);
		page.setText(navigator.getBookPage(index) +"/" + navigator.getBookLength(index));
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				try {
					navigator.goToChapter(index, progress);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotTurnPage));
				}
			}
		});

		// ----- NOTE & LINK
		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if(view_bottom.getVisibility() == View.GONE){
					view_bottom.setVisibility(View.VISIBLE);
				}else{
					view_bottom.setVisibility(View.GONE);
				}
				
				return false;
			}
		});

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
		
		loadPage(viewedPage);
	}
	
	public void loadPage(String path)
	{
		viewedPage = path;
		if(created)
			view.loadUrl(path);
	}
	
	// Change page
	protected void swipePage(View v, MotionEvent event, int book) {
		int action = MotionEventCompat.getActionMasked(event);

		switch (action) {
		case (MotionEvent.ACTION_DOWN):
			swipeOriginX = event.getX();
			swipeOriginY = event.getY();
			break;

		case (MotionEvent.ACTION_UP):
			int quarterWidth = (int) (screenWidth * 0.25);
			float diffX = swipeOriginX - event.getX();
			float diffY = swipeOriginY - event.getY();
			float absDiffX = Math.abs(diffX);
			float absDiffY = Math.abs(diffY);

			if ((diffX > quarterWidth) && (absDiffX > absDiffY)) {
				try {
					navigator.goToNextChapter(index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotTurnPage));
				}
			} else if ((diffX < -quarterWidth) && (absDiffX > absDiffY)) {
				try {
					navigator.goToPrevChapter(index);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_cannotTurnPage));
				}
			}
			break;
		}

	}
	
	@Override
	public void saveState(Editor editor) {
		super.saveState(editor);
		editor.putString("state"+index, state.name());
		editor.putString("page"+index, viewedPage);
	}
	
	@Override
	public void loadState(SharedPreferences preferences)
	{
		super.loadState(preferences);
		loadPage(preferences.getString("page"+index, ""));
		state = ViewStateEnum.valueOf(preferences.getString("state"+index, ViewStateEnum.books.name()));
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

			final WebView myWebView = view;


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
