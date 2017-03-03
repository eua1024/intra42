package com.paulvarry.intra42.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.paulvarry.intra42.AppClass;
import com.paulvarry.intra42.BuildConfig;
import com.paulvarry.intra42.Credential;
import com.paulvarry.intra42.Tools.AppSettings;
import com.paulvarry.intra42.Tools.Token;
import com.paulvarry.intra42.api.model.AccessToken;
import com.paulvarry.intra42.api.model.Messages;
import com.paulvarry.intra42.api.model.Slots;
import com.paulvarry.intra42.api.model.UsersLTE;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    public static final String API_BASE_URL = "https://api.intra.42.fr";
    private static OkHttpClient.Builder httpClient;
    private static Retrofit.Builder builder;
    private static AccessToken mToken;
    private static AppClass app;

    public static <S> S createService(Class<S> serviceClass) {

        httpClient = getBaseClient(true);
        httpClient.addInterceptor(getHeaderInterceptor());

        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();

        return retrofit.create(serviceClass);
    }

    public static <S> S createService(Class<S> serviceClass, AccessToken accessToken, Context c, AppClass app, boolean allowRedirectWrongAuth) {

        httpClient = getBaseClient(allowRedirectWrongAuth);

        if (accessToken != null) {
            mToken = accessToken;
            ServiceGenerator.app = app;
            httpClient.addInterceptor(getHeaderInterceptor(accessToken));

            httpClient.authenticator(getAuthenticator(c));

        }

        OkHttpClient client = httpClient.build();
        Retrofit retrofit = builder.client(client).build();

        return retrofit.create(serviceClass);
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    private static OkHttpClient.Builder getBaseClient(boolean allowRedirectWrongAuth) {
        httpClient = new OkHttpClient.Builder();
        builder = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(getGson()));

        getLogInterceptor(httpClient);
        if (allowRedirectWrongAuth)
            httpClient.addNetworkInterceptor(new AuthInterceptorRedirectActivity());

        httpClient.readTimeout(20, TimeUnit.SECONDS);
        httpClient.connectTimeout(5, TimeUnit.SECONDS);

        return httpClient;
    }

    private static Authenticator getAuthenticator(final Context context) {
        return new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {

                if (responseCount(response) >= 2) {
                    // If both the original call and the call with refreshed token failed,
                    // it will probably keep failing, so don't try again.

                    return null;
                }

                if (app.accessToken == null)
                    return null;

                //noinspection SynchronizeOnNonFinalField
                synchronized (app.accessToken.refreshToken) {
                    // We need a new client, since we don't want to make another call using our client with access token
                    ApiService tokenClient = createService(ApiService.class);
                    Call<AccessToken> call = tokenClient.getRefreshAccessToken(mToken.refreshToken, Credential.UID, Credential.SECRET, Credential.API_OAUTH_REDIRECT, "refresh_token");
                    try {
                        retrofit2.Response<AccessToken> tokenResponse = call.execute();
                        if (tokenResponse.code() == 200) {
                            AccessToken newToken = tokenResponse.body();
                            mToken = newToken;
                            Token.save(context, mToken);
                            if (app != null)
                                app.accessToken = newToken;

                            return response.request().newBuilder()
                                    .header("Authorization", newToken.tokenType + " " + newToken.accessToken)
                                    .header("Users-Agent", getUserAgent())
                                    .header("Accept", "application/json")
                                    .build();
                        } else {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    } catch (NullPointerException e) {
                        return null;
                    }
                }
            }
        };
    }

    private static Interceptor getHeaderInterceptor(final AccessToken accessToken) {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Accept", "application/json")
//                        .header("Content-type", "application/json")
                        .header("Authorization", accessToken.tokenType + " " + accessToken.accessToken)
                        .header("Users-Agent", getUserAgent())
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        };
    }

    private static Interceptor getHeaderInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Accept", "application/json")
//                        .header("Content-type", "application/json")
                        .header("Users-Agent", getUserAgent())
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        };
    }

    private static void getLogInterceptor(OkHttpClient.Builder httpClient) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        if (BuildConfig.DEBUG || AppSettings.Advanced.getAllowSaveLogs(app))
            httpClient.addInterceptor(logging);// add logging as last interceptor

    }

    static public Gson getGson() {

        final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

        class DateDeserializer implements JsonDeserializer<Date> {

            private final String TAG = DateDeserializer.class.getSimpleName();

            @Override
            public java.util.Date deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
                String date = element.getAsString();

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                java.util.Date returnDate;
                try {
                    returnDate = formatter.parse(date);
                } catch (ParseException | IllegalArgumentException e) {
                    Log.e(TAG, "Failed to parse dateString: (" + date + "),  due to:", e);

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                    try {
                        returnDate = format.parse(date);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                        returnDate = null;
                    }
                }
                return returnDate;
            }
        }

        class DateSerializer implements JsonSerializer<Date> {

            @Override
            public JsonElement serialize(java.util.Date src, Type typeOfSrc, JsonSerializationContext context) {
                SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                String dateFormatAsString = formatter.format(src);
                return new JsonPrimitive(dateFormatAsString);
            }

        }

        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .registerTypeAdapter(Slots.class, new Slots.SlotsDeserializer())
                .registerTypeAdapter(Slots.class, new Slots.SlotsSerializer())
                .registerTypeAdapter(java.util.Date.class, new DateDeserializer())
                .registerTypeAdapter(java.util.Date.class, new DateSerializer())
                .registerTypeAdapter(UsersLTE.class, new UsersLTE.UserLTEDeserializer())

                .registerTypeAdapter(Messages.UserVotes.class, new Messages.UserVotes.UserVotesDeserializer())

                .registerTypeAdapter(UsersLTE.getListType(), new UsersLTE.ListUserLTEDeserializer())

//                .registerTypeAdapter(ScaleTeams.class, new ScaleTeams.ScaleTeamsDeserializer())
                .create();
    }

    private static String getUserAgent() {
        return "Intra42Android/" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE +
                " (Android/" + Build.VERSION.RELEASE + " ; " + Build.MODEL + ") retrofit2/2.1.0";
    }

    private static class AuthInterceptorRedirectActivity implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {

            Response response = chain.proceed(chain.request());
            if (response.code() == 401 && chain.request().url().encodedPath().contains("/oauth/token"))
                app.logoutAndRedirect();
            return response;
        }
    }
}