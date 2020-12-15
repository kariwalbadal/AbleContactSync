package com.example.ablecontactsync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

public class ContactUtils {

    private static final String NAME_KEY = "Name";
    private static final String CONTACT_KEY = "Contact";
    private static final String EMAIL_KEY = "Email";
    private static final String TAG = "ContactUtils";


    /**
     * Logic to add a valid contact to the phone's contact list
     *
     * @param contact An object map returned from the query(one map contains one user's DB information)
     */
    public static void addContact(Context context, Map<String, Object> contact) {
        if (!contact.containsKey(NAME_KEY) || !contact.containsKey(CONTACT_KEY)) {
            Log.d(TAG, "Invalid contact. Check DB! " + contact); //A valid contact should have a Name and the number in the least
            return;
        }

        String contactName = contact.get(NAME_KEY).toString();
        String contactNumber = contact.get(CONTACT_KEY).toString();
        String emailID = null;                            // we can add more fields here depending upon the database schema and our requirements in storing the contact. Information like Company, home number and work number can be added to a contact as well
        if (contact.containsKey(EMAIL_KEY)) {
            emailID = contact.get(EMAIL_KEY).toString(); //in case email data is present in the contact
        }
        if (contactExists(context, contactNumber)) {
            Log.d(TAG, "Contact " + contactNumber + " already exists");
            return;
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        contactName).build());

        ops.add(ContentProviderOperation.
                newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contactNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        if (emailID != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, emailID)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());
        }
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Toast.makeText(context, "Error while saving contact", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Logic to check if a contact exists already on the device, to avoid saving repeated contacts in case of multiple entries with the same phone number in the DB
     *
     * @param context The application context
     * @param number  The phone number to be checked
     * @return true if contact exists, false otherwise
     */
    public static boolean contactExists(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
        try {
            if (cur.moveToFirst()) {
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

}
