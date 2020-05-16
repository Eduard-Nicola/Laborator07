package ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.entities.XKCDCartoonInformation;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.general.Constants;

public class XKCDCartoonDisplayerAsyncTask extends AsyncTask<String, Void, XKCDCartoonInformation> {

    private TextView xkcdCartoonTitleTextView;
    private ImageView xkcdCartoonImageView;
    private TextView xkcdCartoonUrlTextView;
    private Button previousButton, nextButton;

    private class XKCDCartoonButtonClickListener implements Button.OnClickListener {

        private String xkcdComicUrl;

        public XKCDCartoonButtonClickListener(String xkcdComicUrl) {
            this.xkcdComicUrl = xkcdComicUrl;
        }

        @Override
        public void onClick(View view) {
            new XKCDCartoonDisplayerAsyncTask(xkcdCartoonTitleTextView, xkcdCartoonImageView, xkcdCartoonUrlTextView, previousButton, nextButton).execute(xkcdComicUrl);
        }

    }

    public XKCDCartoonDisplayerAsyncTask(TextView xkcdCartoonTitleTextView, ImageView xkcdCartoonImageView, TextView xkcdCartoonUrlTextView, Button previousButton, Button nextButton) {
        this.xkcdCartoonTitleTextView = xkcdCartoonTitleTextView;
        this.xkcdCartoonImageView = xkcdCartoonImageView;
        this.xkcdCartoonUrlTextView = xkcdCartoonUrlTextView;
        this.previousButton = previousButton;
        this.nextButton = nextButton;
    }

    @Override
    public XKCDCartoonInformation doInBackground(String... urls) {
        XKCDCartoonInformation xkcdCartoonInformation = new XKCDCartoonInformation();

        // TODO exercise 5a)
        // 1. obtain the content of the web page (whose Internet address is stored in urls[0])
        // - create an instance of a HttpClient object
        // - create an instance of a HttpGet object
        // - create an instance of a ResponseHandler object
        // - execute the request, thus obtaining the web page source code
        HttpClient httpClient = new DefaultHttpClient();
        String pageSourceCode = null;
        try {
            HttpGet httpGet = new HttpGet(urls[0]);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            pageSourceCode = httpClient.execute(httpGet, responseHandler);
        } catch (Exception exception) {
            Log.e(Constants.TAG, exception.getMessage());
            if (Constants.DEBUG) {
                exception.printStackTrace();
            }
        }

        // 2. parse the web page source code
        Document document = Jsoup.parse(pageSourceCode);
        Element htmlTag = document.child(0);

        // - cartoon title: get the tag whose id equals "ctitle"
        Element divTagIdCtitle = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.CTITLE_VALUE).first();
        xkcdCartoonInformation.setCartoonTitle(divTagIdCtitle.ownText());

        // - cartoon url
        //   * get the first tag whose id equals "comic"
        //   * get the embedded <img> tag
        //   * get the value of the attribute "src"
        //   * prepend the protocol: "http:"
        // - cartoon bitmap (only if using Apache HTTP Components)
        //   * create the HttpGet object
        //   * execute the request and obtain the HttpResponse object
        //   * get the HttpEntity object from the response
        //   * get the bitmap from the HttpEntity stream (obtained by getContent()) using BitmapFactory.decodeStream() method
        Element divTagIdComic = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.COMIC_VALUE).first();
        String cartoonInternetAddress =  divTagIdComic.getElementsByTag(Constants.IMG_TAG).attr(Constants.SRC_ATTRIBUTE);
        String cartoonUrl = Constants.HTTP_PROTOCOL + cartoonInternetAddress;
        xkcdCartoonInformation.setCartoonUrl(cartoonUrl);
        try {
            HttpGet httpGet = new HttpGet(cartoonUrl);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            Bitmap bitmap = BitmapFactory.decodeStream(httpEntity.getContent());
            if (bitmap != null) {
                // do something with the response
                xkcdCartoonInformation.setCartoonBitmap(bitmap);
            }
        } catch (Exception exception) {
            Log.e(Constants.TAG, exception.getMessage());
            if (Constants.DEBUG) {
                exception.printStackTrace();
            }
        }

        // - previous cartoon address
        //   * get the first tag whose rel attribute equals "prev"
        //   * get the href attribute of the tag
        //   * prepend the value with the base url: http://www.xkcd.com
        //   * attach the previous button a click listener with the address attached
        Element prevTagRel = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.PREVIOUS_VALUE).first();
        String prevHrefAttribute =  prevTagRel.attr(Constants.HREF_ATTRIBUTE);
        String prevCartoonUrl = Constants.XKCD_INTERNET_ADDRESS + prevHrefAttribute;
        xkcdCartoonInformation.setPreviousCartoonUrl(prevCartoonUrl);

        // - next cartoon address
        //   * get the first tag whose rel attribute equals "next"
        //   * get the href attribute of the tag
        //   * prepend the value with the base url: http://www.xkcd.com
        //   * attach the next button a click listener with the address attached
        Element nextTagRel = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.NEXT_VALUE).first();
        String nextHrefAttribute =  nextTagRel.attr(Constants.HREF_ATTRIBUTE);
        String nextCartoonUrl = Constants.XKCD_INTERNET_ADDRESS + nextHrefAttribute;
        xkcdCartoonInformation.setNextCartoonUrl(nextCartoonUrl);

        return  xkcdCartoonInformation;
    }

    @Override
    protected void onPostExecute(final XKCDCartoonInformation xkcdCartoonInformation) {

        // TODO exercise 5b)
        // map each member of xkcdCartoonInformation object to the corresponding widget
        // cartoonTitle -> xkcdCartoonTitleTextView
        // cartoonBitmap -> xkcdCartoonImageView (only if using Apache HTTP Components)
        // cartoonUrl -> xkcdCartoonUrlTextView
        // based on cartoonUrl fetch the bitmap
        // and put it into xkcdCartoonImageView
        // previousCartoonUrl, nextCartoonUrl -> set the XKCDCartoonUrlButtonClickListener for previousButton, nextButton
        xkcdCartoonTitleTextView.setText(xkcdCartoonInformation.getCartoonTitle());
        xkcdCartoonImageView.setImageBitmap(xkcdCartoonInformation.getCartoonBitmap());
        xkcdCartoonUrlTextView.setText(xkcdCartoonInformation.getCartoonUrl());
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (xkcdCartoonInformation.getPreviousCartoonUrl() != null ||
                        !xkcdCartoonInformation.getPreviousCartoonUrl().isEmpty()) {
                    new XKCDCartoonDisplayerAsyncTask(xkcdCartoonTitleTextView, xkcdCartoonImageView,
                            xkcdCartoonUrlTextView, previousButton, nextButton).execute(xkcdCartoonInformation.getPreviousCartoonUrl());
                }
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (xkcdCartoonInformation.getNextCartoonUrl() != null ||
                        !xkcdCartoonInformation.getNextCartoonUrl().isEmpty()) {
                    new XKCDCartoonDisplayerAsyncTask(xkcdCartoonTitleTextView, xkcdCartoonImageView,
                            xkcdCartoonUrlTextView, previousButton, nextButton).execute(xkcdCartoonInformation.getNextCartoonUrl());
                }
            }
        });
    }

}
