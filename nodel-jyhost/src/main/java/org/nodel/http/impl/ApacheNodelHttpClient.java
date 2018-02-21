package org.nodel.http.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.nodel.Strings;
import org.nodel.Version;
import org.nodel.io.Stream;
import org.nodel.io.UnexpectedIOException;
import org.nodel.net.NodelHTTPClient;

public class ApacheNodelHttpClient extends NodelHTTPClient {
    
    private Object _lock = new Object();
    
    /**
     * The Apache Http Client
     * (created lazily)
     */
    private CloseableHttpClient _httpClient;
    
    /**
     * Required with 'applySecurity'
     */
    private CredentialsProvider _credentialsProvider;

    /**
     * Mainly used for adjusting timeouts
     */
    private RequestConfig _requestConfig;

    /**
     * This needs to be done lazily because proxy can only be set up once
     * 
     * (uses double-check singleton)
     */
    private void lazyInit() {
        if (_httpClient == null) {
            synchronized (_lock) {
                if (_httpClient != null)
                    return;
                
                HttpClientBuilder builder = HttpClients.custom()
                        .setUserAgent("Nodel/" + Version.shared().version)
                        
                        // need to reference this later
                        .setDefaultCredentialsProvider(_credentialsProvider = new BasicCredentialsProvider())
                        
                        // unrestricted connections
                        .setMaxConnTotal(1000)
                        .setMaxConnPerRoute(1000)
                       
                        // default timeouts
                        .setDefaultRequestConfig(_requestConfig = RequestConfig.custom()
                                .setConnectTimeout(DEFAULT_CONNECTTIMEOUT)
                                .setSocketTimeout(DEFAULT_READTIMEOUT)
                                .build());
                
                // using a proxy?
                if (!Strings.isBlank(_proxyAddress))
                    builder.setProxy(prepareForProxyUse(_proxyAddress, _proxyUsername, _proxyPassword));
                
                // ignore all SSL verifications errors?
                if (_ignoreSSL)
                    prepareForNoSSL(builder);
                
                // build the client
                _httpClient = builder.build();
            }
        }
    }

    /**
     * (convenience method) 
     */
    private HttpHost prepareForProxyUse(String proxyAddress, String proxyUsername, String proxyPassword) {
        HttpHost proxy;
        
        String proxyHost = null;
        int proxyPort = -1;
        
        try {
            int lastIndexOfColon = proxyAddress.lastIndexOf(':');
            proxyHost = proxyAddress.substring(0, lastIndexOfColon - 1);
            proxyPort = Integer.parseInt(proxyAddress.substring(lastIndexOfColon + 1));
        } catch (Exception ignore) {
        }
        
        if (Strings.isBlank(proxyHost) || proxyPort <= 0)
            throw new IllegalArgumentException("Proxy address is not in form host:port");
        
        proxy = new HttpHost(proxyHost, proxyPort);
        
        // using proxy credentials?
        if (!Strings.isBlank(proxyUsername) && proxyPassword != null) {
            String userPart = proxyUsername;
            String domainPart = null;
            int indexOfBackSlash = proxyUsername.indexOf('\\');
            if (indexOfBackSlash > 0) {
                domainPart = proxyUsername.substring(0, indexOfBackSlash);
                userPart = proxyUsername.substring(Math.min(proxyUsername.length() - 1, indexOfBackSlash + 1));
            }
            AuthScope authScope = new AuthScope(proxyHost, proxyPort);
            if (domainPart == null) {
                _credentialsProvider.setCredentials(authScope, new UsernamePasswordCredentials(proxyUsername, proxyPassword));
            } else {
                // normally used with NTLM
                _credentialsProvider.setCredentials(authScope, new NTCredentials(userPart, proxyPassword, getLocalHostName(), domainPart));
            }
        }
        
        return proxy;
    }
    
    /**
     * (convenience method)
     */
    private void prepareForNoSSL(HttpClientBuilder builder) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[] { new X509TrustManager() {
                
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
            } }, new SecureRandom());
            builder.setSSLContext(sslContext);
            
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
            builder.setSSLSocketFactory(sslSocketFactory);
            
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            builder.setConnectionManager(connMgr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
    
    @Override
    public HTTPSimpleResponse makeRequest(String urlStr, Map<String, String> query, 
                         String username, String password, 
                         Map<String, String> headers, String contentType, 
                         String post, 
                         Integer connectTimeout, Integer readTimeout) {
        lazyInit();
        
        // record rate of new connections
        s_attemptRate.incrementAndGet();
        
        // construct the full URL (includes query string)
        String fullURL = buildQueryString(urlStr, query);

        // (out of scope for clean up purposes)
        InputStream inputStream = null;
        
        URI uri;
        try {
            uri = new URI(fullURL);
            
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        
        try {
            s_activeConnections.incrementAndGet();
            
            // 'get' or 'post'?
            HttpRequestBase request = (post == null ? new HttpGet(uri) : new HttpPost(uri));
            
            // if username is supplied, apply security
            if (!Strings.isBlank(username))
                applySecurity(request, username, !Strings.isEmpty(password) ? password : "");            
            
            // set 'Content-Type' header
            if (!Strings.isBlank(contentType))
                request.setHeader("Content-Type", contentType);

            // add (or override) any request headers
            if (headers != null) {
                for (Entry<String, String> entry : headers.entrySet())
                    request.setHeader(entry.getKey(), entry.getValue());
            }
            
            if (!Strings.isEmpty(post)) {
                HttpPost httpPost = (HttpPost) request;
                httpPost.setEntity(new StringEntity(post));
            }
            
            // set any timeouts that apply
            if (connectTimeout != null || readTimeout != null) {
                int actualConnTimeout = connectTimeout != null ? connectTimeout : DEFAULT_CONNECTTIMEOUT;
                int actualReadTimeout = readTimeout != null ? readTimeout : DEFAULT_READTIMEOUT;

                request.setConfig(RequestConfig.copy(_requestConfig)
                        .setConnectTimeout(actualConnTimeout)
                        .setSocketTimeout(actualReadTimeout)
                        .build());
            }
            
            // perform the request
            HttpResponse httpResponse;
            httpResponse = _httpClient.execute(request);
            
            // count the post now
            if (!Strings.isEmpty(post))
                s_sendRate.addAndGet(post.length());
            
            // safely get the response (regardless of response code for now)
            
            // safely get the content encoding
            HttpEntity entity = httpResponse.getEntity();
            Header contentEncodingHeader = entity.getContentEncoding();
            String contentEncoding = null;
            if (contentEncodingHeader != null)
                contentEncoding = contentEncodingHeader.getValue();
            
            inputStream = entity.getContent();
            
            // deals with encoding
            InputStreamReader isr = null;
            
            // try using the given encoding
            if (!Strings.isBlank(contentEncoding)) {
                // any unknown content encodings will cause an exception to propagate
                isr = new InputStreamReader(inputStream,  contentEncoding);
            } else {
                isr = new InputStreamReader(inputStream);
            }
            
            String content = Stream.readFully(isr);
            if (content != null)
                s_receiveRate.addAndGet(content.length());
            
            StatusLine statusLine = httpResponse.getStatusLine();
            
            HTTPSimpleResponse result = new HTTPSimpleResponse();
            result.content = content;
            result.statusCode = statusLine.getStatusCode();
            result.reasonPhrase = statusLine.getReasonPhrase();
            
            for (Header header : httpResponse.getAllHeaders())
                result.put(header.getName(), header.getValue());
            
            return result;
            
        } catch (IOException exc) {
            throw new UnexpectedIOException(exc);
            
        } finally {
            Stream.safeClose(inputStream);
            
            s_activeConnections.decrementAndGet();
        }
    }

    /**
     * Builds up query string if args given, e.g. ...?name=My%20Name&surname=My%20Surname
     */
    private static String buildQueryString(String urlStr, Map<String, String> query) {
        StringBuilder queryArg = null;
        if (query != null) {
            StringBuilder sb = new StringBuilder();

            for (Entry<String, String> entry : query.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 'key' must have length, 'value' doesn't have to
                if (Strings.isEmpty(key) || value == null)
                    continue;

                if (sb.length() > 0)
                    sb.append('&');

                sb.append(urlEncode(key))
                  .append('=')
                  .append(urlEncode(value));
            }
            
            if (sb.length() > 0)
                queryArg = sb;
        }

        String fullURL;
        if (queryArg == null)
            fullURL = urlStr;
        else
            fullURL = String.format("%s?%s", urlStr, queryArg);
        return fullURL;
    }
    
    /**
     * (exception-less, convenience function)
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Applies security for a given HTTP request.
     * @throws AuthenticationException 
     */
    private void applySecurity(HttpRequestBase httpRequest, String username, String password) {
        Credentials creds;

        // in case of NTLM, check for '\' in username
        String userPart = username;
        String domainPart = null;
        int indexOfBackSlash = username.indexOf('\\');
        if (indexOfBackSlash > 0) {
            // NTLM
            domainPart = username.substring(0, indexOfBackSlash);
            userPart = username.substring(Math.min(username.length() - 1, indexOfBackSlash + 1));

            creds = new NTCredentials(userPart, password, getLocalHostName(), domainPart);

        } else {
            // BasicAuth
            creds = new UsernamePasswordCredentials(username, password);

            // pre-emptive
            try {
                httpRequest.setHeader(new BasicScheme().authenticate(creds, httpRequest, null));
            } catch (AuthenticationException e) {
                throw new RuntimeException(e);
            }
        }

        _credentialsProvider.setCredentials(new AuthScope(httpRequest.getURI().getHost(), httpRequest.getURI().getPort()), creds);
    }    

    @Override
    public void close() throws IOException {
        Stream.safeClose(_httpClient);
    }
    
    // static convenience methods
    
    /**
     * Returns the local host name (does not throw exceptions).
     */
    private static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exc) {
            throw new UnexpectedIOException(exc);
        }
    }
    
}