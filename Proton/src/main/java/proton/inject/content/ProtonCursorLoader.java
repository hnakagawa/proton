package proton.inject.content;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import proton.inject.Proton;

/**
 * @author keishin.yokomaku
 */
public class ProtonCursorLoader extends CursorLoader {
    public ProtonCursorLoader(Context context) {
        super(context);
        Proton.getInjector(context).inject(this);
    }

    public ProtonCursorLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
        Proton.getInjector(context).inject(this);
    }
}
