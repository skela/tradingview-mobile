package com.spreadco.tradetradingview;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ChartView extends LinearLayout
{
    public final String TAG = "ChartView";

    boolean loading = false;
    List<String> messageQueue = new ArrayList<String>();

    public ChartView(Context context)
    {
        super(context);
        initialize(context);
    }

    public ChartView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    WebView web;

    void initialize (Context context)
    {
        web = new WebView(context);
        web.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        web.setClickable(false);
        web.setFocusable(false);
        web.setFocusableInTouchMode(false);
        web.getSettings().setUseWideViewPort(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);
        web.setWebChromeClient(new ChartViewChromeClient(this));
        web.setWebViewClient(new ChartViewWebClient());
        addView(web);
    }

    public void start()
    {
        Log.d(TAG, "Starting");

        loading = true;

        web.loadUrl("http://demo_chart.tradingview.com");

        // Local Implementation (This approach works perfectly on iOS, but not on Android)
        //String baseUrl = getBaseUrl();
        //String html = createHtml();
        //web.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null);
    }

    public void stop()
    {
        Log.d(TAG, "Stopping");
    }

    class ChartViewChromeClient extends WebChromeClient
    {
        ChartView chart;

        ChartViewChromeClient(ChartView chartView)
        {
            chart = chartView;
        }

        @Override
        public void onConsoleMessage(String message, int lineNumber, String sourceID)
        {
            handleConsoleMessage(message);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage)
        {
            if (consoleMessage != null)
            {
                String msg = consoleMessage.message();
                return handleConsoleMessage(msg);
            }
            return super.onConsoleMessage(consoleMessage);
        }

        boolean handleConsoleMessage(String msg)
        {
            Log.d(TAG, "Web console message " + msg);
            if (chart != null)
                return chart.handleWebMessage(msg);
            return false;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress)
        {
            Log.d(TAG, "Progress is " + newProgress);
            if (newProgress == 100)
            {
                chart.loading = false;

                if (chart.messageQueue.size() > 0)
                {
                    String msg = chart.messageQueue.get(chart.messageQueue.size() - 1);
                    chart.sendMessageToWebChart(msg);
                    chart.messageQueue.clear();
                }
            }
        }
    }

    class ChartViewWebClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
        {

        }
    }

    void sendMessageToWebChart(String msg)
    {
        web.loadUrl("javascript:" + msg);
    }

    boolean handleWebMessage(String msg)
    {
        return false;
    }

    String getBaseUrl()
    {
        return "file:///android_asset/html/trading_view/";
    }

    String createHtml()
    {
        String description = "BitCoin";
        String type = "stock";
        boolean hasVolume = true;
        String ticker = "BTC";

        StringBuilder buf=new StringBuilder();

        String path = "html/trading_view/graph_trading_view.html";
        Context ctx = getContext();
        try
        {
            InputStream stream = ctx.getAssets().open(path);
            BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null)
            {
                buf.append(str);
            }
            in.close();
            stream.close();
        }
        catch(Exception ex)
        {

        }

        String html = buf.toString();

        String config = ""
                + "name: \"{TICKER}\","
                + "timezone:\"UTC\","
                + "minmov:1,"
                + "minmov2:0,"
                + "pricescale:10,"
                + "pointvalue:1,"
                + "session:\"24x7\","
                + "has_no_volume:{NO_VOLUME},"
                + "has_intraday:true,"
                + "has_daily:true,"
                + "has_weekly_and_monthly:true,"
                + "has_empty_bars:true,"
                + "ticker:\"{TICKER}\","
                + "description:\"{DESCRIPTION}\","
                + "type:\"{TYPE}\","
                + "data_status:\"streaming\","
                + "supported_resolutions:[\"D\",\"2D\",\"3D\",\"W\",\"3W\",\"M\",\"6M\"],"
                + "intraday_multipliers:[1, 5, 15, 30, 60],"
                + "\"exchange-traded\":\"\","
                + "\"exchange-listed\":\"\""
                + "";

        config = config.replace("{DESCRIPTION}", description);
        config = config.replace("{TYPE}", type);
        config = config.replace("{NO_VOLUME}", hasVolume ? "false" : "true");

        // Info about these arguments here: https://github.com/tradingview/charting_library/wiki/Symbology
        html = html.replace("{SYMBOL_CONFIG}", config);
        html = html.replace("{TICKER}", ticker);
        html = html.replace("{INTERVAL}", "D");
        html = html.replace("{PRESET}", "mobile");
        //Html = Html.Replace ("{PRESET}", "");
        return html;
    }
}
