package com.ebuddy.twitter.client;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Ramkumar S
 * Date: 21/3/13
 * Time: 6:08 AM
 */
public class cli {
    public static void main(String[] args) throws IOException, URISyntaxException {
        if(args.length < 3){
            System.err.println("Server host, Server port and search keyword are mandatory");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String filter = args[2];
        int requestSleepTimeInSeconds = 5;
        if(args.length > 3){
            requestSleepTimeInSeconds = Integer.parseInt(args[3]);
        }

        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost(host).setPort(port);
        builder.setParameter("filter", filter);
        URI uri = builder.build();
        final StringBuffer buffer = new StringBuffer();
        final Gson gson = new Gson();
        int requestCount = 0;

        while(true){
            requestCount++;
            HttpPost httpPost = new HttpPost(uri);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
                @Override
                public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                    return 30 * 1000;
                }
            });
            HttpResponse httpResponse =  httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            if(entity != null){
                String line;
                buffer.setLength(0);
                InputStream is;
                try {
                    is = entity.getContent();
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    while((line = in.readLine()) != null) {
                        buffer.append(line);
                    }

                    if(buffer.length() > 0 && !buffer.toString().equalsIgnoreCase("null")){
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Collection<String> tweets = gson.fromJson(buffer.toString(), Collection.class);
                                for(String tweet : tweets){
                                    System.out.println(tweet);
                                }
                            }
                        }) ;
                        t.start();
                    } else if(requestCount == 1){
                        System.out.println(String.format("No tweets available for keyword : %s ", filter));
                    }
                    Thread.sleep(1000 * requestSleepTimeInSeconds);
                } catch (Exception e) {
                    System.err.println("Error connection to server: " + e.getMessage());
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
