/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.annotation.TargetApi
 *  android.content.ContentResolver
 *  android.content.Context
 *  android.content.UriMatcher
 *  android.net.Uri
 *  android.os.Build
 *  android.os.Build$VERSION
 *  android.provider.ContactsContract
 *  android.provider.ContactsContract$Contacts
 */
package com.squareup.picasso;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.IOException;
import java.io.InputStream;

class ContactsPhotoRequestHandler
extends RequestHandler {
    private static final int ID_LOOKUP = 1;
    private static final int ID_THUMBNAIL = 2;
    private static final int ID_CONTACT = 3;
    private static final int ID_DISPLAY_PHOTO = 4;
    private static final UriMatcher matcher = new UriMatcher(-1);
    private final Context context;

    ContactsPhotoRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        Uri uri = data.uri;
        return "content".equals(uri.getScheme()) && ContactsContract.Contacts.CONTENT_URI.getHost().equals(uri.getHost()) && matcher.match(data.uri) != -1;
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        InputStream is = this.getInputStream(request);
        return is != null ? new RequestHandler.Result(is, Picasso.LoadedFrom.DISK) : null;
    }

    private InputStream getInputStream(Request data) throws IOException {
        ContentResolver contentResolver = this.context.getContentResolver();
        Uri uri = data.uri;
        switch (matcher.match(uri)) {
            case 1: {
                uri = ContactsContract.Contacts.lookupContact((ContentResolver)contentResolver, (Uri)uri);
                if (uri == null) {
                    return null;
                }
            }
            case 3: {
                if (Build.VERSION.SDK_INT < 14) {
                    return ContactsContract.Contacts.openContactPhotoInputStream((ContentResolver)contentResolver, (Uri)uri);
                }
                return ContactPhotoStreamIcs.get(contentResolver, uri);
            }
            case 2: 
            case 4: {
                return contentResolver.openInputStream(uri);
            }
        }
        throw new IllegalStateException("Invalid uri: " + (Object)uri);
    }

    static {
        matcher.addURI("com.android.contacts", "contacts/lookup/*/#", 1);
        matcher.addURI("com.android.contacts", "contacts/lookup/*", 1);
        matcher.addURI("com.android.contacts", "contacts/#/photo", 2);
        matcher.addURI("com.android.contacts", "contacts/#", 3);
        matcher.addURI("com.android.contacts", "display_photo/#", 4);
    }

    @TargetApi(value=14)
    private static class ContactPhotoStreamIcs {
        private ContactPhotoStreamIcs() {
        }

        static InputStream get(ContentResolver contentResolver, Uri uri) {
            return ContactsContract.Contacts.openContactPhotoInputStream((ContentResolver)contentResolver, (Uri)uri, (boolean)true);
        }
    }

}

