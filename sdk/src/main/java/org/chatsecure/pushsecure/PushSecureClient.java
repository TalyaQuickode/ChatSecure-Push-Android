package org.chatsecure.pushsecure;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.chatsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.DeviceList;
import org.chatsecure.pushsecure.response.Message;
import org.chatsecure.pushsecure.response.PushToken;
import org.chatsecure.pushsecure.response.TokenList;

import java.util.Date;
import java.util.List;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import rx.Observable;

/**
 * An API client for the ChatSecure Push Server
 * Created by davidbrodsky on 6/23/15.
 */
public class PushSecureClient {

    private PushSecureApi api;
    private String token;

    public PushSecureClient(@NonNull String apiHost) {
        this(apiHost, null);
    }

    public PushSecureClient(@NonNull String apiHost, @Nullable Account account) {

        if (account != null) token = account.token;

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new org.chatsecure.pushsecure.response.typeadapter.DjangoDateTypeAdapter())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(apiHost)
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(request -> {
                    if (token != null) request.addHeader("Authorization", "Token " + token);
                })
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(PushSecureApi.class);
    }

    public void setAccount(@NonNull Account account) {
        this.token = account.token;
    }

    /**
     * Authenticate an account with the given credentials, creating one if none exists.
     *
     * @return an {@link Account} representing the newly created or existing account matching
     * the passed credentials. This should be passed to {@link #setAccount(Account)} before
     * performing any other operations with this client.
     */
    public Observable<Account> authenticateAccount(@NonNull String username,
                                                   @NonNull String password,
                                                   @Nullable String email) {

        return api.authenticateAccount(username, password, email);
    }

    public Observable<Device> createDevice(@NonNull String gcmRegistrationId,
                                           @Nullable String name,
                                           @Nullable String gcmDeviceId) {

        return api.createDevice(name, gcmRegistrationId, gcmDeviceId);
    }

    public Observable<PushToken> createToken(@NonNull Device device, @Nullable String name) {
        return api.createToken(name, device.id);
    }

    public Observable<Response> deleteToken(@NonNull PushToken token) {
        return api.deleteToken(token.token);
    }

    public Observable<TokenList> getTokens() {
        return api.getTokens();
    }

    public Observable<Message> sendMessage(@NonNull String recipientToken,
                                           @Nullable String data) {

        return api.sendMessage(recipientToken, data);
    }

    public Observable<DeviceList> getGcmDevices() {

        return api.getGcmDevices();
    }

    public Observable<DeviceList> getApnsDevices() {

        return api.getApnsDevices();
    }

    public Observable<List<Device>> getAllDevices() {
        return Observable.concat(
                getGcmDevices()
                        .flatMap(gcmDeviceList -> Observable.from(gcmDeviceList.results))
                        .map(gcmDevice -> new Device(gcmDevice, Device.Type.GCM)),
                getApnsDevices()
                        .flatMap(apnsDeviceList -> Observable.from(apnsDeviceList.results))
                        .map(apnsDevice -> new Device(apnsDevice, Device.Type.APNS)))
                .toList();
    }

    /**
     * Update properties of the current device. Note that changes to {@link Device#id} will
     * not be respected
     */
    public Observable<Device> updateDevice(@NonNull Device device) {
        return api.updateDevice(device.id, device);
    }

    public Observable<Response> deleteDevice(@NonNull String id) {
        return api.deleteDevice(id);
    }
}
